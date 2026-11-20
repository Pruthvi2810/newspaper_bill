package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.BillEntity
import com.example.data.BillWithItems
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class PageOrientation {
    PORTRAIT,
    LANDSCAPE
}

data class PdfPrintOptions(
    val orientation: PageOrientation = PageOrientation.PORTRAIT,
    val marginPoints: Float = 24f
) {
    val pageWidth: Int
        get() = if (orientation == PageOrientation.PORTRAIT) 595 else 842

    val pageHeight: Int
        get() = if (orientation == PageOrientation.PORTRAIT) 842 else 595

    val clampedMargin: Float
        get() = marginPoints.coerceIn(12f, 64f)
}

object Formatters {
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    private val plainAmountFormat = DecimalFormat("#,##0.00")

    fun formatDate(millis: Long): String = dateFormat.format(Date(millis))

    fun formatCurrency(amount: Double): String = currencyFormat.format(amount)

    fun formatPlainAmount(amount: Double): String = plainAmountFormat.format(amount)

    fun formatQty(qty: Int): String = if (qty == 0) "00" else qty.toString()
}

object InvoicePdfGenerator {

    fun createPdfDocument(
        billWithItems: BillWithItems,
        options: PdfPrintOptions = PdfPrintOptions()
    ): PdfDocument {
        val pdfDocument = PdfDocument()
        drawPruthviInvoicePages(pdfDocument, billWithItems, options)
        return pdfDocument
    }

    fun renderPreviewBitmap(
        billWithItems: BillWithItems,
        options: PdfPrintOptions = PdfPrintOptions(),
        maxDimensionPx: Int = 900
    ): Bitmap {
        val pageWidth = options.pageWidth
        val pageHeight = options.pageHeight
        val scale = maxDimensionPx.toFloat() / maxOf(pageWidth, pageHeight).toFloat()
        val bmpWidth = (pageWidth * scale).roundToInt().coerceAtLeast(1)
        val bmpHeight = (pageHeight * scale).roundToInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.save()
        canvas.scale(scale, scale)

        // Subtle dashed margin guide so users can visually inspect page margins
        val margin = options.clampedMargin
        val guidePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
            pathEffect = DashPathEffect(floatArrayOf(5f, 4f), 0f)
        }
        canvas.drawRect(
            margin,
            margin,
            pageWidth - margin,
            pageHeight - margin,
            guidePaint
        )

        drawInvoicePage(
            canvas = canvas,
            billWithItems = billWithItems,
            options = options,
            startIndex = 0,
            isFirstPage = true
        )
        canvas.restore()
        return bitmap
    }

    fun writeInvoicePdfToUri(
        context: Context,
        uri: Uri,
        billWithItems: BillWithItems,
        options: PdfPrintOptions = PdfPrintOptions()
    ): Result<Unit> {
        return try {
            val document = createPdfDocument(billWithItems, options)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
            } ?: return Result.failure(IllegalStateException("Unable to open output stream."))
            document.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun printInvoice(
        context: Context,
        billWithItems: BillWithItems,
        options: PdfPrintOptions = PdfPrintOptions()
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Pruthvi_Invoice_${billWithItems.bill.billNumber.replace("/", "_")}"
        val mediaSize = if (options.orientation == PageOrientation.LANDSCAPE) {
            PrintAttributes.MediaSize.ISO_A4.asLandscape()
        } else {
            PrintAttributes.MediaSize.ISO_A4.asPortrait()
        }
        val marginMils = (options.clampedMargin * 1000f / 72f).roundToInt()
        val attributes = PrintAttributes.Builder()
            .setMediaSize(mediaSize)
            .setMinMargins(PrintAttributes.Margins(marginMils, marginMils, marginMils, marginMils))
            .build()
        printManager.print(jobName, InvoicePrintAdapter(billWithItems, options), attributes)
    }

    fun shareInvoicePdf(
        context: Context,
        billWithItems: BillWithItems,
        options: PdfPrintOptions = PdfPrintOptions()
    ): Result<Unit> {
        return try {
            val invoicesDir = java.io.File(context.cacheDir, "invoices").apply { mkdirs() }
            val safeBillNo = billWithItems.bill.billNumber.replace("/", "_").replace(" ", "_")
            val pdfFile = java.io.File(invoicesDir, "Pruthvi_Invoice_$safeBillNo.pdf")
            val document = createPdfDocument(billWithItems, options)
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

    private fun drawPruthviInvoicePages(
        pdfDocument: PdfDocument,
        billWithItems: BillWithItems,
        options: PdfPrintOptions
    ) {
        val pageWidth = options.pageWidth
        val pageHeight = options.pageHeight
        var pageNumber = 1
        var nextItemIndex = 0
        var isFirstPage = true

        while (true) {
            val page = pdfDocument.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            val result = drawInvoicePage(
                canvas = page.canvas,
                billWithItems = billWithItems,
                options = options,
                startIndex = nextItemIndex,
                isFirstPage = isFirstPage
            )
            pdfDocument.finishPage(page)

            if (result.finished) break
            nextItemIndex = result.nextItemIndex
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
        options: PdfPrintOptions,
        startIndex: Int,
        isFirstPage: Boolean
    ): PageDrawResult {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val pinkColor = Color.parseColor("#E85D90")
        val pageWidth = options.pageWidth.toFloat()
        val pageHeight = options.pageHeight.toFloat()
        val margin = options.clampedMargin
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
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val boldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinkColor
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }
        val boldCenterPaint = Paint(boldPaint).apply { textAlign = Paint.Align.CENTER }
        val boldRightPaint = Paint(boldPaint).apply { textAlign = Paint.Align.RIGHT }
        val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = pinkColor
            textSize = 10.5f
            typeface = Typeface.SANS_SERIF
        }
        val normalCenterPaint = Paint(normalPaint).apply { textAlign = Paint.Align.CENTER }
        val normalRightPaint = Paint(normalPaint).apply { textAlign = Paint.Align.RIGHT }

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

            // 4. Customer & Bill No/Date Row
            val hasDueDates = bill.billStartDateMillis != null || bill.billEndDateMillis != null
            val infoHeight = if (hasDueDates) 52f else 36f
            val splitX = margin + (right - margin) * 0.56f
            canvas.drawRoundRect(RectF(margin, y, splitX - 3f, y + infoHeight), 6f, 6f, borderPaint)
            val displayCustomer = if (bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL) {
                BillEntity.CENTRAL_JAIL_GUJARATI_NAME
            } else {
                bill.customerName
            }
            canvas.drawText("M/s. $displayCustomer", margin + 8f, y + 18f, boldPaint)
            if (bill.customerAddress.isNotBlank() && bill.billCategory == BillEntity.CATEGORY_CUSTOMER) {
                canvas.drawText(bill.customerAddress, margin + 8f, y + 34f, normalPaint)
            }

            canvas.drawRoundRect(RectF(splitX + 3f, y, right, y + infoHeight), 6f, 6f, borderPaint)
            if (hasDueDates) {
                val rowH = infoHeight / 3f
                canvas.drawLine(splitX + 3f, y + rowH, right, y + rowH, borderPaint)
                canvas.drawLine(splitX + 3f, y + rowH * 2f, right, y + rowH * 2f, borderPaint)
                canvas.drawText("No. :   ${bill.billNumber}", splitX + 10f, y + 13f, boldPaint)
                canvas.drawText("Date :  ${Formatters.formatDate(bill.billDateMillis)}", splitX + 10f, y + rowH + 13f, boldPaint)
                val startStr = bill.billStartDateMillis?.let { Formatters.formatDate(it) } ?: "-"
                val endStr = bill.billEndDateMillis?.let { Formatters.formatDate(it) } ?: "-"
                canvas.drawText("Due : $startStr to $endStr", splitX + 10f, y + rowH * 2f + 13f, boldPaint)
            } else {
                canvas.drawLine(splitX + 3f, y + infoHeight / 2f, right, y + infoHeight / 2f, borderPaint)
                canvas.drawText("No. :   ${bill.billNumber}", splitX + 10f, y + 13f, boldPaint)
                canvas.drawText("Date :  ${Formatters.formatDate(bill.billDateMillis)}", splitX + 10f, y + 31f, boldPaint)
            }
            y += infoHeight + 4f
        }

        // 5. Table Header
        y = drawTableHeader(y)

        // 6. Table Rows
        val isDualRateBill = bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL
        val subRowHeight = 22f
        val singleRowHeight = 28f
        val footerReserve = if (bill.paymentInstructions.isNotBlank()) 92f else 74f
        val maxContentBottom = pageHeight - margin - footerReserve

        var index = startIndex
        while (index < items.size) {
            val item = items[index]
            val rowHeight = if (isDualRateBill) subRowHeight * 2f else singleRowHeight
            if (y + rowHeight > maxContentBottom && index > startIndex) {
                return PageDrawResult(nextItemIndex = index, finished = false)
            }

            canvas.drawRect(margin, y, right, y + rowHeight, borderPaint)
            canvas.drawLine(col1End, y, col1End, y + rowHeight, borderPaint)
            canvas.drawLine(col2End, y, col2End, y + rowHeight, borderPaint)
            canvas.drawLine(col3End, y, col3End, y + rowHeight, borderPaint)

            if (isDualRateBill) {
                canvas.drawLine(col1End, y + subRowHeight, right, y + subRowHeight, borderPaint)
                canvas.drawText(item.newspaperName, (margin + col1End) / 2f, y + rowHeight / 2f + 4f, boldCenterPaint)

                canvas.drawText(Formatters.formatQty(item.quantity1), (col1End + col2End) / 2f, y + 15f, boldCenterPaint)
                canvas.drawText(Formatters.formatPlainAmount(item.price1), (col2End + col3End) / 2f, y + 15f, boldCenterPaint)
                canvas.drawText(Formatters.formatPlainAmount(item.amount1), (col3End + col4End) / 2f, y + 15f, boldCenterPaint)

                canvas.drawText(Formatters.formatQty(item.quantity2), (col1End + col2End) / 2f, y + subRowHeight + 15f, boldCenterPaint)
                canvas.drawText(Formatters.formatPlainAmount(item.price2), (col2End + col3End) / 2f, y + subRowHeight + 15f, boldCenterPaint)
                canvas.drawText(Formatters.formatPlainAmount(item.amount2), (col3End + col4End) / 2f, y + subRowHeight + 15f, boldCenterPaint)
            } else {
                val textY = y + singleRowHeight / 2f + 4f
                canvas.drawText(item.newspaperName, (margin + col1End) / 2f, textY, boldCenterPaint)
                canvas.drawText(item.quantity1.toString(), (col1End + col2End) / 2f, textY, boldCenterPaint)
                canvas.drawText(Formatters.formatPlainAmount(item.price1), (col2End + col3End) / 2f, textY, boldCenterPaint)
                canvas.drawText(Formatters.formatPlainAmount(item.amount1), (col3End + col4End) / 2f, textY, boldCenterPaint)
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

    private class InvoicePrintAdapter(
        private val billWithItems: BillWithItems,
        private val options: PdfPrintOptions
    ) : PrintDocumentAdapter() {
        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }
            val info = PrintDocumentInfo.Builder("Pruthvi_Invoice_${billWithItems.bill.billNumber}.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val document = createPdfDocument(billWithItems, options)
            try {
                FileOutputStream(destination.fileDescriptor).use { out ->
                    document.writeTo(out)
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: Exception) {
                callback.onWriteFailed(e.message)
            } finally {
                document.close()
            }
        }
    }
}
