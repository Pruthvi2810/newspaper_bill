package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BillEntity
import com.example.data.BillWithItems
import com.example.data.NewspaperEntity
import com.example.ui.BillFilterState
import com.example.ui.DateRangeFilter
import com.example.ui.PaymentStatusFilter
import com.example.ui.theme.PaidGreen
import com.example.ui.theme.PaidGreenBg
import com.example.ui.theme.UnpaidRed
import com.example.ui.theme.UnpaidRedBg
import com.example.util.Formatters
import com.example.util.InvoicePdfGenerator
import java.util.Calendar

@Composable
fun BillsListScreen(
    bills: List<BillWithItems>,
    allNewspapers: List<NewspaperEntity> = emptyList(),
    filterState: BillFilterState,
    onSearchChange: (String) -> Unit,
    onPaymentFilterChange: (PaymentStatusFilter) -> Unit,
    onDateRangeFilterChange: (DateRangeFilter) -> Unit,
    onCustomDateRangeChange: (Long?, Long?) -> Unit,
    onBillClick: (BillWithItems) -> Unit,
    onEditBill: (BillWithItems) -> Unit,
    onTogglePaid: (BillEntity) -> Unit,
    onDeleteBill: (BillEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var billToDelete by remember { mutableStateOf<BillEntity?>(null) }

    val totalAmount = remember(bills) { bills.sumOf { it.bill.grandTotal } }
    val unpaidAmount = remember(bills) {
        bills.filter { it.bill.paymentStatus == BillEntity.PAYMENT_UNPAID }.sumOf { it.bill.grandTotal }
    }

    fun pickDate(initialMillis: Long?, onSelected: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply {
            if (initialMillis != null) timeInMillis = initialMillis
        }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, day, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onSelected(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Summary Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryMetricCard(
                label = "Bills (${bills.size})",
                value = Formatters.formatCurrency(totalAmount),
                modifier = Modifier.weight(1f)
            )
            SummaryMetricCard(
                label = "Unpaid Due",
                value = Formatters.formatCurrency(unpaidAmount),
                isHighlight = unpaidAmount > 0,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = filterState.searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Search bill no. or customer...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (filterState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("bills_search_input")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Payment Status Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filterState.paymentStatus == PaymentStatusFilter.ALL,
                onClick = { onPaymentFilterChange(PaymentStatusFilter.ALL) },
                label = { Text("All Status") },
                modifier = Modifier.testTag("filter_status_all")
            )
            FilterChip(
                selected = filterState.paymentStatus == PaymentStatusFilter.PAID,
                onClick = { onPaymentFilterChange(PaymentStatusFilter.PAID) },
                label = { Text("Paid") },
                modifier = Modifier.testTag("filter_status_paid")
            )
            FilterChip(
                selected = filterState.paymentStatus == PaymentStatusFilter.UNPAID,
                onClick = { onPaymentFilterChange(PaymentStatusFilter.UNPAID) },
                label = { Text("Unpaid") },
                modifier = Modifier.testTag("filter_status_unpaid")
            )
        }

        // Date Range Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = filterState.dateRange == DateRangeFilter.ALL,
                onClick = { onDateRangeFilterChange(DateRangeFilter.ALL) },
                label = { Text("All Dates") },
                modifier = Modifier.testTag("filter_date_all")
            )
            FilterChip(
                selected = filterState.dateRange == DateRangeFilter.LAST_WEEK,
                onClick = { onDateRangeFilterChange(DateRangeFilter.LAST_WEEK) },
                label = { Text("Last Week") },
                modifier = Modifier.testTag("filter_date_week")
            )
            FilterChip(
                selected = filterState.dateRange == DateRangeFilter.LAST_MONTH,
                onClick = { onDateRangeFilterChange(DateRangeFilter.LAST_MONTH) },
                label = { Text("Last Month") },
                modifier = Modifier.testTag("filter_date_month")
            )
            FilterChip(
                selected = filterState.dateRange == DateRangeFilter.CUSTOM,
                onClick = { onDateRangeFilterChange(DateRangeFilter.CUSTOM) },
                label = { Text("Custom Range") },
                leadingIcon = {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("filter_date_custom")
            )
        }

        // Custom Range Date Pickers
        if (filterState.dateRange == DateRangeFilter.CUSTOM) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        pickDate(filterState.customStartMillis) { picked ->
                            onCustomDateRangeChange(picked, filterState.customEndMillis)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = filterState.customStartMillis?.let { "From: ${Formatters.formatDate(it)}" }
                            ?: "Start Date",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                OutlinedButton(
                    onClick = {
                        pickDate(filterState.customEndMillis) { picked ->
                            onCustomDateRangeChange(filterState.customStartMillis, picked)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = filterState.customEndMillis?.let { "To: ${Formatters.formatDate(it)}" }
                            ?: "End Date",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (bills.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No bills found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + New Bill to create your first invoice.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 96.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bills, key = { it.bill.id }) { billWithItems ->
                    BillCard(
                        billWithItems = billWithItems,
                        allNewspapers = allNewspapers,
                        onClick = { onBillClick(billWithItems) },
                        onEdit = { onEditBill(billWithItems) },
                        onTogglePaid = { onTogglePaid(billWithItems.bill) },
                        onDelete = { billToDelete = billWithItems.bill }
                    )
                }
            }
        }
    }

    if (billToDelete != null) {
        val target = billToDelete!!
        AlertDialog(
            onDismissRequest = { billToDelete = null },
            title = { Text("Delete Bill?") },
            text = { Text("Delete bill ${target.billNumber} for ${target.customerName}? This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteBill(target)
                        billToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { billToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SummaryMetricCard(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlight) UnpaidRedBg else MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (isHighlight) UnpaidRed else MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) UnpaidRed else MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun BillCard(
    billWithItems: BillWithItems,
    allNewspapers: List<NewspaperEntity>,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onTogglePaid: () -> Unit,
    onDelete: () -> Unit
) {
    val bill = billWithItems.bill
    val isPaid = bill.paymentStatus == BillEntity.PAYMENT_PAID

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("bill_card_${bill.billNumber}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bill.customerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${bill.billNumber}  •  ${Formatters.formatDate(bill.billDateMillis)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isPaid) PaidGreenBg else UnpaidRedBg
                ) {
                    Text(
                        text = if (isPaid) "PAID" else "UNPAID",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaid) PaidGreen else UnpaidRed,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = billWithItems.items.joinToString(", ") { "${it.newspaperName} (×${it.quantity1 + it.quantity2})" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = Formatters.formatCurrency(bill.grandTotal),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                val cardContext = LocalContext.current
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            InvoicePdfGenerator.shareInvoicePdf(
                                context = cardContext,
                                billWithItems = billWithItems,
                                allNewspapers = allNewspapers
                            )
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Bill",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }
                    IconButton(onClick = onTogglePaid) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = if (isPaid) "Mark Unpaid" else "Mark Paid",
                            tint = if (isPaid) PaidGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Bill",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Bill",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}
