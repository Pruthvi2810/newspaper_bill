package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.BillEntity
import com.example.data.BillWithItems
import com.example.data.NewspaperEntity
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

data class InvoiceDisplayRow(
    val newspaperName: String,
    val isDualRate: Boolean,
    val copy1: String,
    val rate1: String,
    val amount1: String,
    val copy2: String = "",
    val rate2: String = "",
    val amount2: String = ""
)

object Formatters {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val plainAmountFormat = DecimalFormat("#,##0.00")

    val FULL_MONTH_NAMES = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    val SHORT_MONTH_NAMES = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )

    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

    fun formatBillMonth(month: Int, year: Int): String {
        val name = FULL_MONTH_NAMES[(month - 1).coerceIn(0, 11)]
        return "$name-$year"
    }

    fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

    fun formatPlainAmount(amount: Double): String = plainAmountFormat.format(amount)

    fun formatQty(qty: Int): String = if (qty == 0) "00" else qty.toString()
}

object InvoicePdfGenerator {
    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points
    private const val MARGIN = 24f

    fun buildInvoiceFileName(bill: BillEntity): String {
        val monthYear = if (bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL && bill.billMonth in 1..12 && bill.billYear > 0) {
            "${Formatters.SHORT_MONTH_NAMES[bill.billMonth - 1]}_${bill.billYear}"
        } else {
            SimpleDateFormat("MMM_yyyy", Locale.ENGLISH).format(Date(bill.billDateMillis))
        }
        val rawSuffix = when (bill.billCategory) {
            BillEntity.CATEGORY_CENTRAL_JAIL -> "Central_Jail"
            BillEntity.CATEGORY_IG_OFFICE -> "IG_Office"
            else -> bill.customerName.trim().ifBlank { "Customer" }
        }
        val safeSuffix = rawSuffix
            .replace(Regex("[^A-Za-z0-9]+"), "_")
            .trim('_')
            .ifBlank { "Customer" }
        return "${monthYear}_${safeSuffix}.pdf"
    }

    fun buildDisplayRows(
        billWithItems: BillWithItems,
        allNewspapers: List<NewspaperEntity> = emptyList()
    ): List<InvoiceDisplayRow> {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val isDualRate = bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL

        if (bill.billCategory == BillEntity.CATEGORY_CUSTOMER && allNewspapers.isNotEmpty()) {
            val matchedItemIds = mutableSetOf<Long>()
            val rows = mutableListOf<InvoiceDisplayRow>()

            for (paper in allNewspapers) {
                val match = items.firstOrNull {
                    it.newspaperName.trim().equals(paper.name.trim(), ignoreCase = true)
                }
                if (match != null) {
                    matchedItemIds.add(match.id)
                    rows.add(
                        InvoiceDisplayRow(
                            newspaperName = match.newspaperName,
                            isDualRate = false,
                            copy1 = match.quantity1.toString(),
                            rate1 = Formatters.formatPlainAmount(match.price1),
                            amount1 = Formatters.formatPlainAmount(match.amount1)
                        )
                    )
                } else {
                    rows.add(
                        InvoiceDisplayRow(
                            newspaperName = paper.name,
                            isDualRate = false,
                            copy1 = "-",
                            rate1 = "-",
                            amount1 = "-"
                        )
                    )
                }
            }

            // Include any bill items whose newspaper was later removed from Settings DB
            for (item in items) {
                if (item.id !in matchedItemIds &&
                    rows.none { it.newspaperName.trim().equals(item.newspaperName.trim(), ignoreCase = true) }
                ) {
                    rows.add(
                        InvoiceDisplayRow(
                            newspaperName = item.newspaperName,
                            isDualRate = false,
                            copy1 = item.quantity1.toString(),
                            rate1 = Formatters.formatPlainAmount(item.price1),
                            amount1 = Formatters.formatPlainAmount(item.amount1)
                        )
                    )
                }
            }
            return rows
        }

        return items.map { item ->
            InvoiceDisplayRow(
                newspaperName = item.newspaperName,
                isDualRate = isDualRate,
                copy1 = if (isDualRate) Formatters.formatQty(item.quantity1) else item.quantity1.toString(),
                rate1 = Formatters.formatPlainAmount(item.price1),
                amount1 = Formatters.formatPlainAmount(item.amount1),
                copy2 = Formatters.formatQty(item.quantity2),
                rate2 = Formatters.formatPlainAmount(item.price2),
                amount2 = Formatters.formatPlainAmount(item.amount2)
            )
        }
    }

    fun createPdfDocument(
        billWithItems: BillWithItems,
        allNewspapers: List<NewspaperEntity> = emptyList()
    ): PdfDocument {
        val pdfDocument = PdfDocument()
        drawPruthviInvoicePages(pdfDocument, billWithItems, allNewspapers)
        return pdfDocument
    }

    fun writeInvoicePdfToUri(
        context: Context,
        uri: Uri,
        billWithItems: BillWithItems,
        allNewspapers: List<NewspaperEntity> = emptyList()
    ): Result<Unit> {
        return try {
            val document = createPdfDocument(billWithItems, allNewspapers)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
            } ?: return Result.failure(IllegalStateException("Unable to open output stream."))
            document.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun saveInvoiceToDownloads(
        context: Context,
        billWithItems: BillWithItems,
        allNewspapers: List<NewspaperEntity> = emptyList()
    ): Result<String> {
        val fileName = buildInvoiceFileName(billWithItems.bill)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return Result.failure(UnsupportedOperationException("Use document picker on older Android"))
        }
        return try {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(
                    MediaStore.Downloads.RELATIVE_PATH,
                    "${Environment.DIRECTORY_DOWNLOADS}/PruthviNewsAgency"
                )
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return Result.failure(IllegalStateException("Could not create file in Downloads."))

            val document = createPdfDocument(billWithItems, allNewspapers)
            try {
                resolver.openOutputStream(uri)?.use { outputStream ->
                    document.writeTo(outputStream)
                } ?: throw IllegalStateException("Unable to open output stream.")
            } finally {
                document.close()
            }

            contentValues.clear()
            contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)

            Result.success(fileName)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun shareInvoicePdf(
        context: Context,
        billWithItems: BillWithItems,
        allNewspapers: List<NewspaperEntity> = emptyList()
    ): Result<Unit> {
        return try {
            val invoicesDir = java.io.File(context.cacheDir, "invoices").apply { mkdirs() }
            val fileName = buildInvoiceFileName(billWithItems.bill)
            val pdfFile = java.io.File(invoicesDir, fileName)
            val document = createPdfDocument(billWithItems, allNewspapers)
            FileOutputStream(pdfFile).use { out ->
                document.writeTo(out)
            }
            document.close()

            val contentUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
                putExtra(
                    android.content.Intent.EXTRA_SUBJECT,
                    "Pruthvi News Agency - Bill ${billWithItems.bill.billNumber}"
                )
                putExtra(
                    android.content.Intent.EXTRA_TEXT,
                    "Please find attached Newspaper Bill ${billWithItems.bill.billNumber} " +
                        "for ${billWithItems.bill.customerName} " +
                        "(Total: ${Formatters.formatCurrency(billWithItems.bill.grandTotal)})."
                )
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = android.content.Intent.createChooser(shareIntent, "Share Bill via")
            chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildLayout(
        text: String,
        paint: TextPaint,
        maxWidth: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout {
        val safeWidth = maxWidth.coerceAtLeast(16)
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, safeWidth)
            .setAlignment(alignment)
            .setIncludePad(false)
            .setLineSpacing(2f, 1f)
            .build()
    }

    private fun drawStaticLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawPruthviInvoicePages(
        pdfDocument: PdfDocument,
        billWithItems: BillWithItems,
        allNewspapers: List<NewspaperEntity>
    ) {
        val rows = buildDisplayRows(billWithItems, allNewspapers)
        var pageNumber = 1
        var nextRowIndex = 0
        var isFirstPage = true

        while (true) {
            val page = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            )
            val result = drawInvoicePage(
                canvas = page.canvas,
                billWithItems = billWithItems,
                rows = rows,
                startIndex = nextRowIndex,
                isFirstPage = isFirstPage
            )
            pdfDocument.finishPage(page)

            if (result.finished) break
            nextRowIndex = result.nextItemIndex
            isFirstPage = false
            pageNumber++
        }
    }

    private data class PageDrawResult(
        val nextItemIndex: Int,
        val finished: Boolean
    )

    private fun drawInvoicePage(
        canvas: Canvas,
        billWithItems: BillWithItems,
        rows: List<InvoiceDisplayRow>,
        startIndex: Int,
        isFirstPage: Boolean
    ): PageDrawResult {
        val bill = billWithItems.bill
        val pinkColor = Color.parseColor("#E85D90")
        val pageWidth = PAGE_WIDTH.toFloat()
        val pageHeight = PAGE_HEIGHT.toFloat()
        val margin = MARGIN
        val right = pageWidth - margin
        val tableWidth = right - margin
        val col1End = margin + tableWidth * 0.40f
        val col2End = margin + tableWidth * 0.55f
        val col3End = margin + tableWidth * 0.70f
        val col4End = right

        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinkColor
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinkColor
            textSize = 20f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinkColor
            textSize = 11f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val boldCenterPaint = TextPaint(boldPaint).apply { textAlign = Paint.Align.CENTER }
        val boldRightPaint = TextPaint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        val normalPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinkColor
            textSize = 10.5f
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val normalCenterPaint = TextPaint(normalPaint).apply { textAlign = Paint.Align.CENTER }
        val normalRightPaint = TextPaint(normalPaint).apply { textAlign = Paint.Align.RIGHT }

        // StaticLayout always draws left-aligned at (0,0) unless Paint.textAlign = LEFT + Layout.Alignment
        val boldWrapPaint = TextPaint(boldPaint).apply { textAlign = Paint.Align.LEFT }
        val normalWrapPaint = TextPaint(normalPaint).apply { textAlign = Paint.Align.LEFT }

        fun drawTableHeader(startY: Float): Float {
            val headerHeight = 26f
            canvas.drawRect(margin, startY, right, startY + headerHeight, borderPaint)
            canvas.drawLine(col1End, startY, col1End, startY + headerHeight, borderPaint)
            canvas.drawLine(col2End, startY, col2End, startY + headerHeight, borderPaint)
            canvas.drawLine(col3End, startY, col3End, startY + headerHeight, borderPaint)
            canvas.drawText("Name of Paper", (margin + col1End) / 2f, startY + 17f, boldCenterPaint)
            canvas.drawText("Copy", (col1End + col2End) / 2f, startY + 17f, boldCenterPaint)
            canvas.drawText("Rate", (col2End + col3End) / 2f, startY + 17f, boldCenterPaint)
            canvas.drawText("Amount", (col3End + col4End) / 2f, startY + 17f, boldCenterPaint)
            return startY + headerHeight
        }

        var y = margin

        if (isFirstPage) {
            // 1. Top Note & Proprietor Box
            val topBoxHeight = 44f
            canvas.drawRoundRect(RectF(margin, y, right, y + topBoxHeight), 6f, 6f, borderPaint)
            canvas.drawText("Note: Cheque Receive on Propriter's Name", margin + 8f, y + 16f, normalPaint)
            canvas.drawText("M:9924545306", right - 8f, y + 16f, normalRightPaint)
            canvas.drawText("Pro. : Rakeshsinh K. Rajawat", margin + 8f, y + 34f, normalPaint)
            y += topBoxHeight + 4f

            // 2. PRUTHVI NEWS AGENCY Title Box
            val agencyBoxHeight = 32f
            canvas.drawRoundRect(RectF(margin, y, right, y + agencyBoxHeight), 6f, 6f, borderPaint)
            canvas.drawText("PRUTHVI NEWS AGENCY", pageWidth / 2f, y + 23f, titlePaint)
            y += agencyBoxHeight + 4f

            // 3. Address Box
            val addrBoxHeight = 24f
            canvas.drawRoundRect(RectF(margin, y, right, y + addrBoxHeight), 6f, 6f, borderPaint)
            canvas.drawText(
                "A-4, Devalay Apartment,Keshavnagar,Subhashbridge,Ahmedabad-27",
                pageWidth / 2f,
                y + 16f,
                normalCenterPaint
            )
            y += addrBoxHeight + 4f

            // 4. Customer & Bill No/Date/Due Row (with proper text wrapping and zero column overlap)
            val isCustomerBill = bill.billCategory == BillEntity.CATEGORY_CUSTOMER
            val hasDueDates = bill.billStartDateMillis != null || bill.billEndDateMillis != null
            val splitX = margin + (right - margin) * 0.56f
            val leftBoxRight = splitX - 3f
            val rightBoxLeft = splitX + 3f
            val leftTextWidth = ((leftBoxRight - margin) - 16f).toInt().coerceAtLeast(80)
            val rightTextWidth = ((right - rightBoxLeft) - 16f).toInt().coerceAtLeast(80)

            val displayCustomer = if (bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL) {
                BillEntity.CENTRAL_JAIL_GUJARATI_NAME
            } else {
                bill.customerName
            }
            val customerNameLayout = buildLayout("M/s. $displayCustomer", boldWrapPaint, leftTextWidth)

            if (isCustomerBill) {
                // Split Customer Row into 2 distinct rows: Name & Address on left; No. & Due on right
                val addressValue = bill.customerAddress.trim().ifBlank { "-" }
                val addressLayout = buildLayout("Add. : $addressValue", normalWrapPaint, leftTextWidth)

                val noLayout = buildLayout("No. :  ${bill.billNumber}", boldWrapPaint, rightTextWidth)
                val dueValue = if (hasDueDates) {
                    val startStr = bill.billStartDateMillis?.let { Formatters.formatDate(it) } ?: "-"
                    val endStr = bill.billEndDateMillis?.let { Formatters.formatDate(it) } ?: "-"
                    "Due : $startStr to $endStr"
                } else {
                    "Due : -"
                }
                val dueLayout = buildLayout(dueValue, boldWrapPaint, rightTextWidth)

                val topRowHeight = max(customerNameLayout.height + 12f, noLayout.height + 12f).coerceAtLeast(22f)
                val bottomRowHeight = max(addressLayout.height + 12f, dueLayout.height + 12f).coerceAtLeast(22f)
                val infoHeight = topRowHeight + bottomRowHeight

                // Left Box (Split into Name Row + Address Row)
                canvas.save()
                canvas.clipRect(margin, y, leftBoxRight, y + infoHeight)
                canvas.drawRoundRect(RectF(margin, y, leftBoxRight, y + infoHeight), 6f, 6f, borderPaint)
                canvas.drawLine(margin, y + topRowHeight, leftBoxRight, y + topRowHeight, borderPaint)
                drawStaticLayout(
                    canvas,
                    customerNameLayout,
                    margin + 8f,
                    y + (topRowHeight - customerNameLayout.height) / 2f
                )
                drawStaticLayout(
                    canvas,
                    addressLayout,
                    margin + 8f,
                    y + topRowHeight + (bottomRowHeight - addressLayout.height) / 2f
                )
                canvas.restore()

                // Right Box (Split into No. Row + Due Row, Date removed for Customer Bill)
                canvas.save()
                canvas.clipRect(rightBoxLeft, y, right, y + infoHeight)
                canvas.drawRoundRect(RectF(rightBoxLeft, y, right, y + infoHeight), 6f, 6f, borderPaint)
                canvas.drawLine(rightBoxLeft, y + topRowHeight, right, y + topRowHeight, borderPaint)
                drawStaticLayout(
                    canvas,
                    noLayout,
                    rightBoxLeft + 8f,
                    y + (topRowHeight - noLayout.height) / 2f
                )
                drawStaticLayout(
                    canvas,
                    dueLayout,
                    rightBoxLeft + 8f,
                    y + topRowHeight + (bottomRowHeight - dueLayout.height) / 2f
                )
                canvas.restore()

                y += infoHeight + 4f
            } else {
                // Central Jail & IG Office: wrapped M/s. Customer Name inside left box + No. / Date (/ Bill Month or Due) on right
                val isCentralJailBill = bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL
                val showThirdRow = isCentralJailBill || hasDueDates
                val rightRowMin = if (showThirdRow) 58f else 40f
                val leftContentHeight = customerNameLayout.height + 16f
                val infoHeight = max(leftContentHeight, rightRowMin)

                // Left Box with wrapped customer name clipped safely to left box bounds
                canvas.save()
                canvas.clipRect(margin, y, leftBoxRight, y + infoHeight)
                canvas.drawRoundRect(RectF(margin, y, leftBoxRight, y + infoHeight), 6f, 6f, borderPaint)
                drawStaticLayout(
                    canvas,
                    customerNameLayout,
                    margin + 8f,
                    y + (infoHeight - customerNameLayout.height) / 2f
                )
                canvas.restore()

                // Right Box
                canvas.save()
                canvas.clipRect(rightBoxLeft, y, right, y + infoHeight)
                canvas.drawRoundRect(RectF(rightBoxLeft, y, right, y + infoHeight), 6f, 6f, borderPaint)
                if (showThirdRow) {
                    val rowH = infoHeight / 3f
                    canvas.drawLine(rightBoxLeft, y + rowH, right, y + rowH, borderPaint)
                    canvas.drawLine(rightBoxLeft, y + rowH * 2f, right, y + rowH * 2f, borderPaint)
                    canvas.drawText("No. :   ${bill.billNumber}", rightBoxLeft + 8f, y + rowH * 0.68f, boldPaint)
                    canvas.drawText(
                        "Date :  ${Formatters.formatDate(bill.billDateMillis)}",
                        rightBoxLeft + 8f,
                        y + rowH + rowH * 0.68f,
                        boldPaint
                    )
                    val thirdLine = if (isCentralJailBill) {
                        "Bill Month : ${Formatters.formatBillMonth(bill.billMonth, bill.billYear)}"
                    } else {
                        val startStr = bill.billStartDateMillis?.let { Formatters.formatDate(it) } ?: "-"
                        val endStr = bill.billEndDateMillis?.let { Formatters.formatDate(it) } ?: "-"
                        "Due : $startStr to $endStr"
                    }
                    canvas.drawText(
                        thirdLine,
                        rightBoxLeft + 8f,
                        y + rowH * 2f + rowH * 0.68f,
                        boldPaint
                    )
                } else {
                    val rowH = infoHeight / 2f
                    canvas.drawLine(rightBoxLeft, y + rowH, right, y + rowH, borderPaint)
                    canvas.drawText("No. :   ${bill.billNumber}", rightBoxLeft + 8f, y + rowH * 0.68f, boldPaint)
                    canvas.drawText(
                        "Date :  ${Formatters.formatDate(bill.billDateMillis)}",
                        rightBoxLeft + 8f,
                        y + rowH + rowH * 0.68f,
                        boldPaint
                    )
                }
                canvas.restore()

                y += infoHeight + 4f
            }
        }

        // 5. Table Header
        y = drawTableHeader(y)

        // 6. Table Rows
        val subRowHeight = 22f
        val singleRowMinHeight = 26f
        val paperColWidth = ((col1End - margin) - 8f).toInt().coerceAtLeast(60)
        val footerReserve = if (bill.paymentInstructions.isNotBlank()) 92f else 74f
        val maxContentBottom = pageHeight - margin - footerReserve

        var index = startIndex
        while (index < rows.size) {
            val row = rows[index]
            val paperLayout = buildLayout(
                row.newspaperName,
                boldWrapPaint,
                paperColWidth,
                Layout.Alignment.ALIGN_CENTER
            )
            val baseRowHeight = if (row.isDualRate) subRowHeight * 2f else singleRowMinHeight
            val rowHeight = max(baseRowHeight, paperLayout.height + 12f)

            if (y + rowHeight > maxContentBottom && index > startIndex) {
                return PageDrawResult(nextItemIndex = index, finished = false)
            }

            canvas.drawRect(margin, y, right, y + rowHeight, borderPaint)
            canvas.drawLine(col1End, y, col1End, y + rowHeight, borderPaint)
            canvas.drawLine(col2End, y, col2End, y + rowHeight, borderPaint)
            canvas.drawLine(col3End, y, col3End, y + rowHeight, borderPaint)

            // Draw wrapped Paper Name centered inside Column 1
            canvas.save()
            canvas.clipRect(margin + 2f, y, col1End - 2f, y + rowHeight)
            drawStaticLayout(
                canvas,
                paperLayout,
                margin + 4f,
                y + (rowHeight - paperLayout.height) / 2f
            )
            canvas.restore()

            if (row.isDualRate) {
                val midY = y + rowHeight / 2f
                canvas.drawLine(col1End, midY, right, midY, borderPaint)
                val row1TextY = y + (midY - y) / 2f + 4f
                val row2TextY = midY + (y + rowHeight - midY) / 2f + 4f

                canvas.drawText(row.copy1, (col1End + col2End) / 2f, row1TextY, boldCenterPaint)
                canvas.drawText(row.rate1, (col2End + col3End) / 2f, row1TextY, boldCenterPaint)
                canvas.drawText(row.amount1, (col3End + col4End) / 2f, row1TextY, boldCenterPaint)

                canvas.drawText(row.copy2, (col1End + col2End) / 2f, row2TextY, boldCenterPaint)
                canvas.drawText(row.rate2, (col2End + col3End) / 2f, row2TextY, boldCenterPaint)
                canvas.drawText(row.amount2, (col3End + col4End) / 2f, row2TextY, boldCenterPaint)
            } else {
                val textY = y + rowHeight / 2f + 4f
                canvas.drawText(row.copy1, (col1End + col2End) / 2f, textY, boldCenterPaint)
                canvas.drawText(row.rate1, (col2End + col3End) / 2f, textY, boldCenterPaint)
                canvas.drawText(row.amount1, (col3End + col4End) / 2f, textY, boldCenterPaint)
            }

            y += rowHeight
            index++
        }

        // 7. Total Row
        val totalHeight = 28f
        canvas.drawRect(margin, y, right, y + totalHeight, borderPaint)
        canvas.drawLine(col3End, y, col3End, y + totalHeight, borderPaint)
        canvas.drawText(
            "Total: ₹ ${Formatters.formatPlainAmount(bill.grandTotal)}",
            (col3End + col4End) / 2f,
            y + 18f,
            boldCenterPaint
        )
        y += totalHeight + 28f

        // 8. Payment Instructions & Footer Signature
        if (bill.paymentInstructions.isNotBlank()) {
            canvas.drawText("Payment Note: ${bill.paymentInstructions}", margin + 4f, y, boldPaint)
            y += 18f
        }
        canvas.drawText("For,Pruthvi News Agency", right - 4f, y, boldRightPaint)
        canvas.drawLine(margin, y + 10f, right, y + 10f, borderPaint)

        return PageDrawResult(nextItemIndex = index, finished = true)
    }
}
