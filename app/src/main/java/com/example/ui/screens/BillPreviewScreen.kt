package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BillEntity
import com.example.data.BillWithItems
import com.example.ui.components.PrintPreviewDialog
import com.example.ui.theme.InkNavy
import com.example.ui.theme.InvoicePink
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.Terracotta
import com.example.ui.theme.UnpaidRed
import com.example.util.Formatters
import com.example.util.InvoicePdfGenerator
import com.example.util.PdfPrintOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillPreviewScreen(
    billWithItems: BillWithItems,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onTogglePaid: () -> Unit,
    onMessage: (String) -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val bill = billWithItems.bill
    val isPaid = bill.paymentStatus == BillEntity.PAYMENT_PAID
    var showPrintPreviewDialog by remember { mutableStateOf(false) }
    var lastPrintOptions by remember { mutableStateOf(PdfPrintOptions()) }

    val pdfExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            val result = InvoicePdfGenerator.writeInvoicePdfToUri(
                context = context,
                uri = uri,
                billWithItems = billWithItems,
                options = lastPrintOptions
            )
            result.fold(
                onSuccess = { onMessage("PDF exported successfully.") },
                onFailure = { onMessage("Failed to export PDF: ${it.message}") }
            )
        }
    }

    if (showPrintPreviewDialog) {
        PrintPreviewDialog(
            billWithItems = billWithItems,
            initialOptions = lastPrintOptions,
            onDismiss = { showPrintPreviewDialog = false },
            onConfirmPrint = { selectedOptions ->
                lastPrintOptions = selectedOptions
                showPrintPreviewDialog = false
                InvoicePdfGenerator.printInvoice(context, billWithItems, selectedOptions)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Preview", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            InvoicePdfGenerator.shareInvoicePdf(context, billWithItems).onFailure {
                                onMessage("Unable to share bill: ${it.message}")
                            }
                        },
                        modifier = Modifier.testTag("preview_top_share_button")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share Bill", tint = Color.White)
                    }
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.testTag("preview_edit_button")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Bill", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = InkNavy)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Exact Pruthvi News Agency Pink Invoice Layout (Image 3)
            PruthviInvoiceTemplate(billWithItems = billWithItems)

            // Primary Actions: Print & Export PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { showPrintPreviewDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = InkNavy),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("preview_print_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Print", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        val safeName = bill.billNumber.replace("/", "_")
                        pdfExportLauncher.launch("Pruthvi_Invoice_$safeName.pdf")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                        .testTag("preview_export_pdf_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF", fontWeight = FontWeight.Bold)
                }
            }

            // Share Bill Button (WhatsApp, Gmail, Drive, etc.)
            Button(
                onClick = {
                    InvoicePdfGenerator.shareInvoicePdf(context, billWithItems).onFailure {
                        onMessage("Unable to share bill: ${it.message}")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Terracotta),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("preview_share_bill_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share Bill (PDF)", fontWeight = FontWeight.Bold)
            }

            // Toggle Payment Status
            OutlinedButton(
                onClick = onTogglePaid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("preview_toggle_status_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = if (isPaid) UnpaidRed else PaidGreen
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isPaid) "Mark as Unpaid" else "Mark as Paid",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun PruthviInvoiceTemplate(
    billWithItems: BillWithItems,
    modifier: Modifier = Modifier
) {
    val bill = billWithItems.bill
    val items = billWithItems.items
    val displayCustomer = if (bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL) {
        BillEntity.CENTRAL_JAIL_GUJARATI_NAME
    } else {
        bill.customerName
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shadowElevation = 3.dp,
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // 1. Top Note & Proprietor Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, InvoicePink, RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Note: Cheque Receive on Propriter's Name",
                        color = InvoicePink,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "M:9924545306",
                        color = InvoicePink,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "Pro. : Rakeshsinh K. Rajawat",
                    color = InvoicePink,
                    fontSize = 10.sp
                )
            }

            // 2. Agency Title Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, InvoicePink, RoundedCornerShape(6.dp))
                    .padding(vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PRUTHVI NEWS AGENCY",
                    color = InvoicePink,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
            }

            // 3. Address Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, InvoicePink, RoundedCornerShape(6.dp))
                    .padding(vertical = 4.dp, horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A-4, Devalay Apartment,Keshavnagar,Subhashbridge,Ahmedabad-27",
                    color = InvoicePink,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }

            // 4. Customer & Bill No/Date Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.25f)
                        .fillMaxHeight()
                        .border(1.dp, InvoicePink, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "M/s.  $displayCustomer",
                            color = InvoicePink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 15.sp
                        )
                        if (bill.customerAddress.isNotBlank() && bill.billCategory == BillEntity.CATEGORY_CUSTOMER) {
                            Text(
                                text = bill.customerAddress,
                                color = InvoicePink,
                                fontSize = 10.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(0.95f)
                        .fillMaxHeight()
                        .border(1.dp, InvoicePink, RoundedCornerShape(6.dp))
                ) {
                    Text(
                        text = "No. :   ${bill.billNumber}",
                        color = InvoicePink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    HorizontalDivider(color = InvoicePink, thickness = 1.dp)
                    Text(
                        text = "Date :  ${Formatters.formatDate(bill.billDateMillis)}",
                        color = InvoicePink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                    if (bill.billStartDateMillis != null || bill.billEndDateMillis != null) {
                        HorizontalDivider(color = InvoicePink, thickness = 1.dp)
                        val startStr = bill.billStartDateMillis?.let { Formatters.formatDate(it) } ?: "-"
                        val endStr = bill.billEndDateMillis?.let { Formatters.formatDate(it) } ?: "-"
                        Text(
                            text = "Due: $startStr to $endStr",
                            color = InvoicePink,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // 5. Invoice Table
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, InvoicePink)
            ) {
                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    TableHeaderCell("Name of Paper", Modifier.weight(2.0f))
                    VerticalDivider(color = InvoicePink, thickness = 1.dp)
                    TableHeaderCell("Copy", Modifier.weight(0.8f))
                    VerticalDivider(color = InvoicePink, thickness = 1.dp)
                    TableHeaderCell("Rate", Modifier.weight(0.9f))
                    VerticalDivider(color = InvoicePink, thickness = 1.dp)
                    TableHeaderCell("Amount", Modifier.weight(1.3f))
                }
                HorizontalDivider(color = InvoicePink, thickness = 1.dp)

                // Table Rows
                val isDualRateBill = bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL
                items.forEach { item ->
                    if (isDualRateBill) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(2.0f)
                                    .fillMaxHeight()
                                    .padding(horizontal = 4.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.newspaperName,
                                    color = InvoicePink,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }
                            VerticalDivider(color = InvoicePink, thickness = 1.dp)

                            // Right side: 2 sub-rows for Copy | Rate | Amount
                            Column(modifier = Modifier.weight(3.0f)) {
                                // Sub-row 1
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    TableValueCell(Formatters.formatQty(item.quantity1), Modifier.weight(0.8f))
                                    VerticalDivider(color = InvoicePink, thickness = 1.dp)
                                    TableValueCell(Formatters.formatPlainAmount(item.price1), Modifier.weight(0.9f))
                                    VerticalDivider(color = InvoicePink, thickness = 1.dp)
                                    TableValueCell(Formatters.formatPlainAmount(item.amount1), Modifier.weight(1.3f))
                                }
                                HorizontalDivider(color = InvoicePink, thickness = 1.dp)
                                // Sub-row 2
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(IntrinsicSize.Min)
                                ) {
                                    TableValueCell(Formatters.formatQty(item.quantity2), Modifier.weight(0.8f))
                                    VerticalDivider(color = InvoicePink, thickness = 1.dp)
                                    TableValueCell(Formatters.formatPlainAmount(item.price2), Modifier.weight(0.9f))
                                    VerticalDivider(color = InvoicePink, thickness = 1.dp)
                                    TableValueCell(Formatters.formatPlainAmount(item.amount2), Modifier.weight(1.3f))
                                }
                            }
                        }
                    } else {
                        // Single-row format matching IG Office Bill preview image
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            TableValueCell(item.newspaperName, Modifier.weight(2.0f))
                            VerticalDivider(color = InvoicePink, thickness = 1.dp)
                            TableValueCell(item.quantity1.toString(), Modifier.weight(0.8f))
                            VerticalDivider(color = InvoicePink, thickness = 1.dp)
                            TableValueCell(Formatters.formatPlainAmount(item.price1), Modifier.weight(0.9f))
                            VerticalDivider(color = InvoicePink, thickness = 1.dp)
                            TableValueCell(Formatters.formatPlainAmount(item.amount1), Modifier.weight(1.3f))
                        }
                    }
                    HorizontalDivider(color = InvoicePink, thickness = 1.dp)
                }

                // Total Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Spacer(modifier = Modifier.weight(3.7f))
                    VerticalDivider(color = InvoicePink, thickness = 1.dp)
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .padding(vertical = 8.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Total: ₹ ${Formatters.formatPlainAmount(bill.grandTotal)}",
                            color = InvoicePink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 6. Payment Instructions & Footer Signature
            if (bill.paymentInstructions.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Payment Note: ${bill.paymentInstructions}",
                    color = InvoicePink,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "For,Pruthvi News Agency",
                color = InvoicePink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp)
            )
            HorizontalDivider(color = InvoicePink, thickness = 1.dp)
        }
    }
}

@Composable
private fun TableHeaderCell(text: String, modifier: Modifier) {
    Box(
        modifier = modifier.padding(vertical = 6.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = InvoicePink,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TableValueCell(text: String, modifier: Modifier) {
    Box(
        modifier = modifier.padding(vertical = 5.dp, horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = InvoicePink,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
