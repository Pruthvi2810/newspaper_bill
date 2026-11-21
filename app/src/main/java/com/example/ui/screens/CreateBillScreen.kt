package com.example.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Preview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import com.example.data.BillEntity
import com.example.data.CustomerEntity
import com.example.data.NewspaperEntity
import com.example.ui.BillFormState
import com.example.ui.BillLineItemDraft
import com.example.ui.theme.InkNavy
import com.example.util.Formatters
import java.util.Calendar

private val MONTH_NAMES = listOf(
    "January (01)", "February (02)", "March (03)", "April (04)",
    "May (05)", "June (06)", "July (07)", "August (08)",
    "September (09)", "October (10)", "November (11)", "December (12)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBillScreen(
    formState: BillFormState,
    customers: List<CustomerEntity>,
    newspapers: List<NewspaperEntity>,
    onBack: () -> Unit,
    onMonthChange: (Int) -> Unit,
    onMonthYearChange: (Int, Int) -> Unit = { m, _ -> onMonthChange(m) },
    onBillNumberChange: (String) -> Unit,
    onCustomerNameChange: (String) -> Unit,
    onCustomerAddressChange: (String) -> Unit,
    onSelectCustomer: (CustomerEntity) -> Unit,
    onDateChange: (Long) -> Unit,
    onStartDateChange: (Long?) -> Unit,
    onEndDateChange: (Long?) -> Unit,
    onPaymentInstructionsChange: (String) -> Unit,
    onPaymentStatusChange: (String) -> Unit,
    onItemNewspaperChange: (Int, String, Double?) -> Unit,
    onItemPrice1Change: (Int, String) -> Unit,
    onItemQty1Change: (Int, String) -> Unit,
    onItemPrice2Change: (Int, String) -> Unit,
    onItemQty2Change: (Int, String) -> Unit,
    onAddItem: () -> Unit,
    onRemoveItem: (Int) -> Unit,
    onSaveAndPreview: () -> Unit
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var customerDropdownExpanded by remember { mutableStateOf(false) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    fun openDatePicker(initialMillis: Long = formState.billDateMillis, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = initialMillis }
        DatePickerDialog(
            context,
            { _, year, month, day ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, day, 12, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                onPicked(picked.timeInMillis)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val screenTitle = when {
        formState.editingBillId != 0L -> "Edit Bill"
        formState.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL -> "Central Jail - New Bill"
        formState.billCategory == BillEntity.CATEGORY_IG_OFFICE -> "IG Office - New Bill"
        else -> "Customer - New Bill"
    }

    val isCentralJail = formState.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL
    var showCalculator by remember { mutableStateOf(false) }
    var showCalendar by remember { mutableStateOf(false) }
    var calcOffsetX by remember { mutableFloatStateOf(0f) }
    var calcOffsetY by remember { mutableFloatStateOf(0f) }
    var calOffsetX by remember { mutableFloatStateOf(0f) }
    var calOffsetY by remember { mutableFloatStateOf(0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(screenTitle, fontWeight = FontWeight.Bold, color = Color.White) },
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
                            val next = !showCalendar
                            showCalendar = next
                            if (next) showCalculator = false
                        },
                        modifier = Modifier.testTag("bill_calendar_toggle")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = "Toggle Calendar",
                            tint = if (showCalendar) Color(0xFFF38218) else Color.White
                        )
                    }
                    if (isCentralJail) {
                        IconButton(
                            onClick = {
                                val next = !showCalculator
                                showCalculator = next
                                if (next) showCalendar = false
                            },
                            modifier = Modifier.testTag("central_jail_calculator_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Toggle Calculator",
                                tint = if (showCalculator) Color(0xFFF38218) else Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = InkNavy)
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
        ) {
            val density = LocalDensity.current
            val maxXPx = with(density) { (maxWidth - 260.dp).coerceAtLeast(40.dp).toPx() }
            val maxYPx = with(density) { (maxHeight - 160.dp).coerceAtLeast(40.dp).toPx() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // Bill Metadata Card
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Bill Details / બિલ વિગત",
                        style = MaterialTheme.typography.titleMedium,
                        color = InkNavy,
                        fontWeight = FontWeight.Bold
                    )

                    // Customer Name (Fixed for Central Jail, editable/dropdown for others)
                    if (formState.isCustomerNameFixed) {
                        OutlinedTextField(
                            value = formState.customerName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Customer Name (Fixed)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bill_customer_name_fixed")
                        )
                    } else {
                        ExposedDropdownMenuBox(
                            expanded = customerDropdownExpanded && customers.isNotEmpty(),
                            onExpandedChange = { customerDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = formState.customerName,
                                onValueChange = {
                                    onCustomerNameChange(it)
                                    customerDropdownExpanded = true
                                },
                                label = { Text("Customer Name / ગ્રાહકનું નામ") },
                                trailingIcon = {
                                    if (customers.isNotEmpty()) {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = customerDropdownExpanded)
                                    }
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryEditable)
                                    .testTag("bill_customer_name_input")
                            )

                            val matchingCustomers = remember(customers, formState.customerName) {
                                val q = formState.customerName.trim().lowercase()
                                if (q.isEmpty()) customers
                                else customers.filter {
                                    it.name.lowercase().contains(q) || it.phone.contains(q)
                                }
                            }

                            if (matchingCustomers.isNotEmpty()) {
                                ExposedDropdownMenu(
                                    expanded = customerDropdownExpanded,
                                    onDismissRequest = { customerDropdownExpanded = false }
                                ) {
                                    matchingCustomers.take(6).forEach { customer ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(customer.name, fontWeight = FontWeight.Medium)
                                                    if (customer.phone.isNotBlank()) {
                                                        Text(
                                                            customer.phone,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            },
                                            onClick = {
                                                onSelectCustomer(customer)
                                                customerDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Address Text Box for Customer Bills
                        if (formState.billCategory == BillEntity.CATEGORY_CUSTOMER) {
                            OutlinedTextField(
                                value = formState.customerAddress,
                                onValueChange = onCustomerAddressChange,
                                label = { Text("Customer Address / સરનામું") },
                                maxLines = 2,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("bill_customer_address_input")
                            )
                        }
                    }

                    // Month Selector Dropdown (Select menu only, no keyboard)
                    ExposedDropdownMenuBox(
                        expanded = monthDropdownExpanded,
                        onExpandedChange = { next ->
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                            monthDropdownExpanded = next
                        }
                    ) {
                        val monthDisplayText = if (isCentralJail) {
                            Formatters.formatBillMonth(formState.billMonth, formState.billYear)
                        } else {
                            MONTH_NAMES[(formState.billMonth - 1).coerceIn(0, 11)]
                        }
                        val monthFieldLabel = if (isCentralJail) {
                            "Bill Month"
                        } else {
                            "Select Month of Bill / મહિનો પસંદ કરો"
                        }
                        OutlinedTextField(
                            value = monthDisplayText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(monthFieldLabel) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                .testTag("bill_month_dropdown")
                        )

                        ExposedDropdownMenu(
                            expanded = monthDropdownExpanded,
                            onDismissRequest = { monthDropdownExpanded = false }
                        ) {
                            if (isCentralJail) {
                                // Year switcher row inside select menu (no keyboard)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            onMonthYearChange(formState.billMonth, formState.billYear - 1)
                                        }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous Year",
                                            tint = InkNavy
                                        )
                                    }
                                    Text(
                                        text = "Year: ${formState.billYear}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = InkNavy
                                    )
                                    IconButton(
                                        onClick = {
                                            onMonthYearChange(formState.billMonth, formState.billYear + 1)
                                        }
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next Year",
                                            tint = InkNavy
                                        )
                                    }
                                }
                                HorizontalDivider()
                                Formatters.FULL_MONTH_NAMES.forEachIndexed { idx, monthName ->
                                    val label = "$monthName-${formState.billYear}"
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = label,
                                                fontWeight = if (formState.billMonth == idx + 1) FontWeight.Bold else FontWeight.Normal,
                                                color = if (formState.billMonth == idx + 1) InkNavy else MaterialTheme.colorScheme.onSurface
                                            )
                                        },
                                        onClick = {
                                            onMonthYearChange(idx + 1, formState.billYear)
                                            monthDropdownExpanded = false
                                        }
                                    )
                                }
                            } else {
                                MONTH_NAMES.forEachIndexed { idx, monthLabel ->
                                    DropdownMenuItem(
                                        text = { Text(monthLabel) },
                                        onClick = {
                                            onMonthChange(idx + 1)
                                            monthDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Bill Number & Bill Date Row (Date of bill removed for Customer Bill)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = formState.billNumber,
                            onValueChange = onBillNumberChange,
                            label = { Text("Bill No.") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("bill_number_input")
                        )

                        if (formState.billCategory != BillEntity.CATEGORY_CUSTOMER) {
                            OutlinedButton(
                                onClick = { openDatePicker(formState.billDateMillis, onDateChange) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .padding(top = 4.dp)
                                    .testTag("bill_date_button")
                            ) {
                                Icon(
                                    Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(Formatters.formatDate(formState.billDateMillis))
                            }
                        }
                    }

                    // Due / Billing Period Start & End Dates + Payment Instructions for Customer Bills
                    if (formState.billCategory == BillEntity.CATEGORY_CUSTOMER) {
                        Text(
                            text = "Bill Due Dates (Start & End)",
                            style = MaterialTheme.typography.labelLarge,
                            color = InkNavy
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    openDatePicker(
                                        formState.billStartDateMillis ?: System.currentTimeMillis(),
                                        onStartDateChange
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("bill_start_date_button")
                            ) {
                                Text(
                                    text = formState.billStartDateMillis?.let { "Start: ${Formatters.formatDate(it)}" }
                                        ?: "Start Date"
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    openDatePicker(
                                        formState.billEndDateMillis ?: System.currentTimeMillis(),
                                        onEndDateChange
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("bill_end_date_button")
                            ) {
                                Text(
                                    text = formState.billEndDateMillis?.let { "End: ${Formatters.formatDate(it)}" }
                                        ?: "End Date"
                                )
                            }
                        }

                        OutlinedTextField(
                            value = formState.paymentInstructions,
                            onValueChange = onPaymentInstructionsChange,
                            label = { Text("Payment Instructions (e.g. Pay via UPI)") },
                            maxLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bill_payment_instructions_input")
                        )
                    }

                    // Payment Status
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(text = "Status:", style = MaterialTheme.typography.labelLarge)
                        FilterChip(
                            selected = formState.paymentStatus == BillEntity.PAYMENT_UNPAID,
                            onClick = { onPaymentStatusChange(BillEntity.PAYMENT_UNPAID) },
                            label = { Text("Unpaid") }
                        )
                        FilterChip(
                            selected = formState.paymentStatus == BillEntity.PAYMENT_PAID,
                            onClick = { onPaymentStatusChange(BillEntity.PAYMENT_PAID) },
                            label = { Text("Paid") }
                        )
                    }
                }
            }

            // Newspaper Entries
            Text(
                text = if (formState.usesDualRate) {
                    "Newspaper Entries (Dual Rate & Qty)"
                } else {
                    "Newspaper Entries (Price × Quantity)"
                },
                style = MaterialTheme.typography.titleMedium,
                color = InkNavy,
                fontWeight = FontWeight.Bold
            )

            formState.items.forEachIndexed { index, item ->
                DualRateNewspaperCard(
                    index = index + 1,
                    item = item,
                    newspapers = newspapers,
                    usesDualRate = formState.usesDualRate,
                    canRemove = formState.items.size > 1,
                    onNewspaperChange = { name, price -> onItemNewspaperChange(item.id, name, price) },
                    onPrice1Change = { onItemPrice1Change(item.id, it) },
                    onQty1Change = { onItemQty1Change(item.id, it) },
                    onPrice2Change = { onItemPrice2Change(item.id, it) },
                    onQty2Change = { onItemQty2Change(item.id, it) },
                    onRemove = { onRemoveItem(item.id) }
                )
            }

            OutlinedButton(
                onClick = onAddItem,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("add_newspaper_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Another Newspaper", style = MaterialTheme.typography.labelLarge)
            }

            // Grand Total Summary
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Grand Total / કુલ રકમ",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = Formatters.formatCurrency(formState.grandTotal),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.testTag("bill_grand_total_text")
                    )
                }
            }

            if (formState.errorMessage != null) {
                Text(
                    text = formState.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onSaveAndPreview,
                enabled = !formState.isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = InkNavy),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_preview_bill_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Preview, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (formState.editingBillId == 0L) "Save & Preview Bill" else "Update & Preview Bill",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

            // Floating Pop-on-Screen Calculator Overlay (Dynamic / Draggable, no scroll needed)
            if (isCentralJail) {
                AnimatedVisibility(
                    visible = showCalculator,
                    enter = fadeIn() + scaleIn(initialScale = 0.92f),
                    exit = fadeOut() + scaleOut(targetScale = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .offset { IntOffset(calcOffsetX.roundToInt(), calcOffsetY.roundToInt()) }
                        .widthIn(max = 360.dp)
                        .zIndex(10f)
                ) {
                    QuickCalculatorCard(
                        onClose = { showCalculator = false },
                        headerDragModifier = Modifier.pointerInput(maxXPx, maxYPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                calcOffsetX = (calcOffsetX + dragAmount.x).coerceIn(-maxXPx / 2f, maxXPx / 2f)
                                calcOffsetY = (calcOffsetY + dragAmount.y).coerceIn(0f, maxYPx)
                            }
                        }
                    )
                }
            }

            // Floating Pop-on-Screen Calendar Overlay (Dynamic / Draggable, no scroll needed)
            AnimatedVisibility(
                visible = showCalendar,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .offset { IntOffset(calOffsetX.roundToInt(), calOffsetY.roundToInt()) }
                    .widthIn(max = 360.dp)
                    .zIndex(10f)
            ) {
                QuickCalendarCard(
                    onClose = { showCalendar = false },
                    headerDragModifier = Modifier.pointerInput(maxXPx, maxYPx) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            calOffsetX = (calOffsetX + dragAmount.x).coerceIn(-maxXPx / 2f, maxXPx / 2f)
                            calOffsetY = (calOffsetY + dragAmount.y).coerceIn(0f, maxYPx)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DualRateNewspaperCard(
    index: Int,
    item: BillLineItemDraft,
    newspapers: List<NewspaperEntity>,
    usesDualRate: Boolean,
    canRemove: Boolean,
    onNewspaperChange: (String, Double?) -> Unit,
    onPrice1Change: (String) -> Unit,
    onQty1Change: (String) -> Unit,
    onPrice2Change: (String) -> Unit,
    onQty2Change: (String) -> Unit,
    onRemove: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Newspaper #$index",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = InkNavy
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Remove Newspaper $index",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { next ->
                    focusManager.clearFocus(force = true)
                    keyboardController?.hide()
                    expanded = next
                }
            ) {
                OutlinedTextField(
                    value = item.newspaperName,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Tap to select newspaper") },
                    label = { Text("Select Newspaper from Database") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        .testTag("newspaper_name_input_$index")
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    newspapers.forEach { paper ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(paper.name)
                                    Text(
                                        Formatters.formatCurrency(paper.defaultPrice),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                onNewspaperChange(paper.name, paper.defaultPrice)
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Rate 1 & Quantity 1 (or single Price & Quantity for IG Office / Customer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = item.price1Text,
                    onValueChange = onPrice1Change,
                    label = { Text(if (usesDualRate) "Price 1 (₹)" else "Price (₹)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("newspaper_price1_input_$index")
                )
                OutlinedTextField(
                    value = item.quantity1Text,
                    onValueChange = onQty1Change,
                    label = { Text(if (usesDualRate) "Quantity 1" else "Quantity") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("newspaper_qty1_input_$index")
                )
            }

            if (usesDualRate) {
                Text(
                    text = "Amount 1: ${item.quantity1} × ₹${Formatters.formatPlainAmount(item.price1)} = ₹${Formatters.formatPlainAmount(item.amount1)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Rate 2 & Quantity 2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = item.price2Text,
                        onValueChange = onPrice2Change,
                        label = { Text("Price 2 (₹)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("newspaper_price2_input_$index")
                    )
                    OutlinedTextField(
                        value = item.quantity2Text,
                        onValueChange = onQty2Change,
                        label = { Text("Quantity 2") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("newspaper_qty2_input_$index")
                    )
                }
                Text(
                    text = "Amount 2: ${item.quantity2} × ₹${Formatters.formatPlainAmount(item.price2)} = ₹${Formatters.formatPlainAmount(item.amount2)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Amount: ${item.quantity1} × ₹${Formatters.formatPlainAmount(item.price1)} = ₹${Formatters.formatPlainAmount(item.amount1)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Newspaper Total",
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    text = Formatters.formatCurrency(item.itemTotal),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkNavy
                )
            }
        }
    }
}
