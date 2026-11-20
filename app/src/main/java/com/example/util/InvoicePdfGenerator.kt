package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
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
    private const val PAGE_WIDTH = 595 // A4 width in points
    private const val PAGE_HEIGHT = 842 // A4 height in points

    fun createPdfDocument(billWithItems: BillWithItems): PdfDocument {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        drawPruthviInvoiceOnCanvas(page.canvas, billWithItems)
        pdfDocument.finishPage(page)
        return pdfDocument
    }

    fun writeInvoicePdfToUri(
        context: Context,
        uri: Uri,
        billWithItems: BillWithItems
    ): Result<Unit> {
        return try {
            val document = createPdfDocument(billWithItems)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                document.writeTo(outputStream)
            } ?: return Result.failure(IllegalStateException("Unable to open output stream."))
            document.close()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun printInvoice(context: Context, billWithItems: BillWithItems) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
        val jobName = "Pruthvi_Invoice_${billWithItems.bill.billNumber.replace("/", "_")}"
        printManager.print(jobName, InvoicePrintAdapter(billWithItems), null)
    }

    fun shareInvoicePdf(context: Context, billWithItems: BillWithItems): Result<Unit> {
        return try {
            val invoicesDir = java.io.File(context.cacheDir, "invoices").apply { mkdirs() }
            val safeBillNo = billWithItems.bill.billNumber.replace("/", "_").replace(" ", "_")
            val pdfFile = java.io.File(invoicesDir, "Pruthvi_Invoice_$safeBillNo.pdf")
            val document = createPdfDocument(billWithItems)
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

    private fun drawPruthviInvoiceOnCanvas(canvas: Canvas, billWithItems: BillWithItems) {
        val bill = billWithItems.bill
        val items = billWithItems.items
        val pinkColor = Color.parseColor("#E85D90")
        val margin = 24f
        val right = PAGE_WIDTH - margin
        var y = 24f

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
        canvas.drawText("PRUTHVI NEWS AGENCY", PAGE_WIDTH / 2f, y + 23f, titlePaint)
        y += agencyBoxHeight + 4f

        // 3. Address Box
        val addrBoxHeight = 24f
        canvas.drawRoundRect(RectF(margin, y, right, y + addrBoxHeight), 6f, 6f, borderPaint)
        canvas.drawText(
            "A-4, Devalay Apartment,Keshavnagar,Subhashbridge,Ahmedabad-27",
            PAGE_WIDTH / 2f,
            y + 16f,
            normalCenterPaint
        )
        y += addrBoxHeight + 4f

        // 4. Customer & Bill No/Date Row
        val infoHeight = 36f
        val splitX = margin + (right - margin) * 0.56f
        canvas.drawRoundRect(RectF(margin, y, splitX - 3f, y + infoHeight), 6f, 6f, borderPaint)
        val displayCustomer = if (bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL) {
            BillEntity.CENTRAL_JAIL_GUJARATI_NAME
        } else {
            bill.customerName
        }
        canvas.drawText("M/s. $displayCustomer", margin + 8f, y + 22f, boldPaint)

        canvas.drawRoundRect(RectF(splitX + 3f, y, right, y + infoHeight), 6f, 6f, borderPaint)
        canvas.drawLine(splitX + 3f, y + infoHeight / 2f, right, y + infoHeight / 2f, borderPaint)
        canvas.drawText("No. :   ${bill.billNumber}", splitX + 10f, y + 13f, boldPaint)
        canvas.drawText("Date :  ${Formatters.formatDate(bill.billDateMillis)}", splitX + 10f, y + 31f, boldPaint)
        y += infoHeight + 4f

        // 5. Table Columns
        val tableWidth = right - margin
        val col1End = margin + tableWidth * 0.40f // Name of Paper
        val col2End = margin + tableWidth * 0.55f // Copy
        val col3End = margin + tableWidth * 0.70f // Rate
        val col4End = right                       // Amount

        val headerHeight = 26f
        canvas.drawRect(margin, y, right, y + headerHeight, borderPaint)
        canvas.drawLine(col1End, y, col1End, y + headerHeight, borderPaint)
        canvas.drawLine(col2End, y, col2End, y + headerHeight, borderPaint)
        canvas.drawLine(col3End, y, col3End, y + headerHeight, borderPaint)

        canvas.drawText("Name of Paper", (margin + col1End) / 2f, y + 17f, boldCenterPaint)
        canvas.drawText("Copy", (col1End + col2End) / 2f, y + 17f, boldCenterPaint)
        canvas.drawText("Rate", (col2End + col3End) / 2f, y + 17f, boldCenterPaint)
        canvas.drawText("Amount", (col3End + col4End) / 2f, y + 17f, boldCenterPaint)
        y += headerHeight

        // 6. Table Rows (2 sub-rows for Central Jail, 1 row for IG Office / Customer)
        val isDualRateBill = bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL
        val subRowHeight = 22f
        val singleRowHeight = 28f
        for (item in items) {
            val rowHeight = if (isDualRateBill) subRowHeight * 2f else singleRowHeight
            if (y + rowHeight > PAGE_HEIGHT - 90f) break
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

        // 8. Footer Signature
        canvas.drawText("For,Pruthvi News Agency", right - 4f, y, boldRightPaint)
        canvas.drawLine(margin, y + 10f, right, y + 10f, borderPaint)
    }

    private class InvoicePrintAdapter(
        private val billWithItems: BillWithItems
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
                .setPageCount(1)
                .build()
            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val document = createPdfDocument(billWithItems)
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
