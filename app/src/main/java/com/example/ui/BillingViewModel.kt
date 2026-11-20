package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AuthPreferences
import com.example.data.BillEntity
import com.example.data.BillItemEntity
import com.example.data.BillWithItems
import com.example.data.BillingRepository
import com.example.data.CustomerEntity
import com.example.data.NewspaperEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

sealed interface AppScreen {
    data object Dashboard : AppScreen
    data object CentralJailHub : AppScreen
    data object CentralJailHistory : AppScreen
    data object IgOfficeHub : AppScreen
    data object IgOfficeHistory : AppScreen
    data object CustomerHub : AppScreen
    data object CustomerHistory : AppScreen
    data object CustomerDirectory : AppScreen
    data object Settings : AppScreen
    data object CreateEditBill : AppScreen
    data class BillPreview(val billId: Long) : AppScreen
}

enum class PaymentStatusFilter {
    ALL,
    PAID,
    UNPAID
}

enum class DateRangeFilter {
    ALL,
    LAST_WEEK,
    LAST_MONTH,
    CUSTOM
}

enum class CustomerSearchField {
    ALL,
    NAME,
    PHONE,
    ADDRESS
}

data class BillLineItemDraft(
    val id: Int,
    val newspaperName: String = "",
    val price1Text: String = "",
    val quantity1Text: String = "",
    val price2Text: String = "",
    val quantity2Text: String = ""
) {
    val price1: Double get() = price1Text.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val quantity1: Int get() = quantity1Text.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val amount1: Double get() = price1 * quantity1

    val price2: Double get() = price2Text.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val quantity2: Int get() = quantity2Text.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val amount2: Double get() = price2 * quantity2

    val itemTotal: Double get() = amount1 + amount2
}

data class BillFormState(
    val editingBillId: Long = 0L,
    val billCategory: String = BillEntity.CATEGORY_CENTRAL_JAIL,
    val billMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1,
    val billYear: Int = Calendar.getInstance().get(Calendar.YEAR),
    val billNumber: String = "",
    val customerId: Long? = null,
    val customerName: String = "",
    val customerPhone: String = "",
    val customerAddress: String = "",
    val billDateMillis: Long = System.currentTimeMillis(),
    val billStartDateMillis: Long? = null,
    val billEndDateMillis: Long? = null,
    val paymentInstructions: String = "",
    val paymentStatus: String = BillEntity.PAYMENT_UNPAID,
    val items: List<BillLineItemDraft> = listOf(BillLineItemDraft(id = 1)),
    val errorMessage: String? = null,
    val isSaving: Boolean = false
) {
    val grandTotal: Double get() = items.sumOf { it.itemTotal }
    val isCustomerNameFixed: Boolean
        get() = billCategory == BillEntity.CATEGORY_CENTRAL_JAIL || billCategory == BillEntity.CATEGORY_IG_OFFICE
    val usesDualRate: Boolean
        get() = billCategory == BillEntity.CATEGORY_CENTRAL_JAIL
}

data class BillFilterState(
    val searchQuery: String = "",
    val paymentStatus: PaymentStatusFilter = PaymentStatusFilter.ALL,
    val dateRange: DateRangeFilter = DateRangeFilter.ALL,
    val customStartMillis: Long? = null,
    val customEndMillis: Long? = null
)

data class CustomerSearchState(
    val query: String = "",
    val field: CustomerSearchField = CustomerSearchField.ALL
)

class BillingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getInstance(application)
    private val repository = BillingRepository(database)
    private val authPreferences = AuthPreferences(application)

    val isLoggedIn: StateFlow<Boolean> = authPreferences.isLoggedIn.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Dashboard)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private var returnScreenAfterBill: AppScreen = AppScreen.Dashboard

    private val _billFilter = MutableStateFlow(BillFilterState())
    val billFilter: StateFlow<BillFilterState> = _billFilter.asStateFlow()

    private val _customerSearch = MutableStateFlow(CustomerSearchState())
    val customerSearch: StateFlow<CustomerSearchState> = _customerSearch.asStateFlow()

    private val _billForm = MutableStateFlow(BillFormState())
    val billForm: StateFlow<BillFormState> = _billForm.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var nextDraftItemId = 2

    val allNewspapers: StateFlow<List<NewspaperEntity>> = repository.allNewspapers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCustomers: StateFlow<List<CustomerEntity>> = repository.allCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allBills: StateFlow<List<BillWithItems>> = repository.allBillsWithItems.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredBills: StateFlow<List<BillWithItems>> = combine(
        repository.allBillsWithItems,
        _billFilter
    ) { bills, filter ->
        val query = filter.searchQuery.trim().lowercase()
        val now = System.currentTimeMillis()
        val startOfRange: Long? = when (filter.dateRange) {
            DateRangeFilter.ALL -> null
            DateRangeFilter.LAST_WEEK -> now - 7L * 24 * 60 * 60 * 1000
            DateRangeFilter.LAST_MONTH -> now - 30L * 24 * 60 * 60 * 1000
            DateRangeFilter.CUSTOM -> filter.customStartMillis?.let { startOfDay(it) }
        }
        val endOfRange: Long? = when (filter.dateRange) {
            DateRangeFilter.CUSTOM -> filter.customEndMillis?.let { endOfDay(it) }
            else -> null
        }

        bills.filter { billWithItems ->
            val bill = billWithItems.bill
            val matchesQuery = query.isEmpty() ||
                bill.billNumber.lowercase().contains(query) ||
                bill.customerName.lowercase().contains(query) ||
                bill.customerPhone.lowercase().contains(query)

            val matchesStatus = when (filter.paymentStatus) {
                PaymentStatusFilter.ALL -> true
                PaymentStatusFilter.PAID -> bill.paymentStatus == BillEntity.PAYMENT_PAID
                PaymentStatusFilter.UNPAID -> bill.paymentStatus == BillEntity.PAYMENT_UNPAID
            }

            val matchesDate = (startOfRange == null || bill.billDateMillis >= startOfRange) &&
                (endOfRange == null || bill.billDateMillis <= endOfRange)

            matchesQuery && matchesStatus && matchesDate
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredCustomers: StateFlow<List<CustomerEntity>> = combine(
        repository.allCustomers,
        _customerSearch
    ) { customers, search ->
        val query = search.query.trim().lowercase()
        if (query.isEmpty()) {
            customers
        } else {
            customers.filter { customer ->
                when (search.field) {
                    CustomerSearchField.ALL ->
                        customer.name.lowercase().contains(query) ||
                            customer.phone.lowercase().contains(query) ||
                            customer.address.lowercase().contains(query)
                    CustomerSearchField.NAME -> customer.name.lowercase().contains(query)
                    CustomerSearchField.PHONE -> customer.phone.lowercase().contains(query)
                    CustomerSearchField.ADDRESS -> customer.address.lowercase().contains(query)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            repository.seedDefaultNewspapersIfEmpty()
        }
    }

    fun login(username: String, password: String) {
        viewModelScope.launch {
            val success = authPreferences.authenticate(username, password)
            if (success) {
                _loginError.value = null
            } else {
                _loginError.value = "Invalid username or password. Default: admin / admin123"
            }
        }
    }

    fun clearLoginError() {
        _loginError.value = null
    }

    fun logout() {
        viewModelScope.launch {
            authPreferences.logout()
            _currentScreen.value = AppScreen.Dashboard
        }
    }

    fun navigateTo(screen: AppScreen) {
        if (screen is AppScreen.CentralJailHistory || screen is AppScreen.IgOfficeHistory || screen is AppScreen.CustomerHistory) {
            _billFilter.value = BillFilterState()
        }
        _currentScreen.value = screen
    }

    fun navigateBackFromBill() {
        _currentScreen.value = returnScreenAfterBill
    }

    fun openBillPreview(billId: Long, fromScreen: AppScreen = _currentScreen.value) {
        if (fromScreen !is AppScreen.CreateEditBill && fromScreen !is AppScreen.BillPreview) {
            returnScreenAfterBill = fromScreen
        }
        _currentScreen.value = AppScreen.BillPreview(billId)
    }

    fun startNewBill(
        category: String,
        preselectedCustomer: CustomerEntity? = null,
        fromScreen: AppScreen = _currentScreen.value
    ) {
        returnScreenAfterBill = fromScreen
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val month = cal.get(Calendar.MONTH) + 1
            val year = cal.get(Calendar.YEAR)
            val nextNumber = repository.generateNextBillNumber(category, month, year)
            nextDraftItemId = 2

            val defaultCustomerName = when (category) {
                BillEntity.CATEGORY_CENTRAL_JAIL -> BillEntity.CENTRAL_JAIL_FIXED_NAME
                BillEntity.CATEGORY_IG_OFFICE -> BillEntity.IG_OFFICE_FIXED_NAME
                else -> preselectedCustomer?.name.orEmpty()
            }

            val defaultInstructions = if (category == BillEntity.CATEGORY_CUSTOMER) {
                "Pay via UPI: 9924545306 / Cash"
            } else {
                ""
            }

            _billForm.value = BillFormState(
                editingBillId = 0L,
                billCategory = category,
                billMonth = month,
                billYear = year,
                billNumber = nextNumber,
                customerId = preselectedCustomer?.id,
                customerName = defaultCustomerName,
                customerPhone = preselectedCustomer?.phone.orEmpty(),
                customerAddress = preselectedCustomer?.address.orEmpty(),
                billDateMillis = System.currentTimeMillis(),
                billStartDateMillis = null,
                billEndDateMillis = null,
                paymentInstructions = defaultInstructions,
                paymentStatus = BillEntity.PAYMENT_UNPAID,
                items = listOf(BillLineItemDraft(id = 1))
            )
            _currentScreen.value = AppScreen.CreateEditBill
        }
    }

    fun startEditBill(billWithItems: BillWithItems, fromScreen: AppScreen = _currentScreen.value) {
        if (fromScreen !is AppScreen.CreateEditBill && fromScreen !is AppScreen.BillPreview) {
            returnScreenAfterBill = fromScreen
        }
        val bill = billWithItems.bill
        nextDraftItemId = billWithItems.items.size + 1
        val drafts = billWithItems.items.mapIndexed { index, item ->
            BillLineItemDraft(
                id = index + 1,
                newspaperName = item.newspaperName,
                price1Text = formatNum(item.price1),
                quantity1Text = if (item.quantity1 == 0) "" else item.quantity1.toString(),
                price2Text = if (item.price2 == 0.0) "" else formatNum(item.price2),
                quantity2Text = if (item.quantity2 == 0) "" else item.quantity2.toString()
            )
        }.ifEmpty {
            nextDraftItemId = 2
            listOf(BillLineItemDraft(id = 1))
        }

        _billForm.value = BillFormState(
            editingBillId = bill.id,
            billCategory = bill.billCategory,
            billMonth = bill.billMonth,
            billYear = bill.billYear,
            billNumber = bill.billNumber,
            customerId = bill.customerId,
            customerName = bill.customerName,
            customerPhone = bill.customerPhone,
            customerAddress = bill.customerAddress,
            billDateMillis = bill.billDateMillis,
            billStartDateMillis = bill.billStartDateMillis,
            billEndDateMillis = bill.billEndDateMillis,
            paymentInstructions = bill.paymentInstructions,
            paymentStatus = bill.paymentStatus,
            items = drafts
        )
        _currentScreen.value = AppScreen.CreateEditBill
    }

    private fun formatNum(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }

    // Bill form updates
    fun updateBillMonth(month: Int) {
        _billForm.update { form ->
            val updatedNumber = when (form.billCategory) {
                BillEntity.CATEGORY_CENTRAL_JAIL -> repository.formatCentralJailBillNumber(month, form.billYear)
                BillEntity.CATEGORY_IG_OFFICE -> repository.formatIgOfficeBillNumber(month, form.billYear)
                BillEntity.CATEGORY_CUSTOMER -> repository.formatCustomerBillNumber(month, form.billYear)
                else -> form.billNumber
            }
            form.copy(billMonth = month, billNumber = updatedNumber, errorMessage = null)
        }
    }

    fun updateCustomerAddress(value: String) {
        _billForm.update { it.copy(customerAddress = value) }
    }

    fun updateBillStartDate(millis: Long?) {
        _billForm.update { it.copy(billStartDateMillis = millis) }
    }

    fun updateBillEndDate(millis: Long?) {
        _billForm.update { it.copy(billEndDateMillis = millis) }
    }

    fun updatePaymentInstructions(value: String) {
        _billForm.update { it.copy(paymentInstructions = value) }
    }

    fun updateBillNumber(value: String) {
        _billForm.update { it.copy(billNumber = value, errorMessage = null) }
    }

    fun updateCustomerName(value: String) {
        _billForm.update { form ->
            if (form.isCustomerNameFixed) form
            else form.copy(customerName = value, customerId = null, errorMessage = null)
        }
    }

    fun selectExistingCustomer(customer: CustomerEntity) {
        _billForm.update { form ->
            if (form.isCustomerNameFixed) form
            else form.copy(
                customerId = customer.id,
                customerName = customer.name,
                customerPhone = customer.phone,
                customerAddress = customer.address,
                errorMessage = null
            )
        }
    }

    fun updateBillDate(millis: Long) {
        _billForm.update { it.copy(billDateMillis = millis) }
    }

    fun updateBillPaymentStatus(status: String) {
        _billForm.update { it.copy(paymentStatus = status) }
    }

    fun updateLineItemNewspaper(itemId: Int, newspaperName: String, defaultPrice: Double?) {
        _billForm.update { form ->
            form.copy(
                items = form.items.map { item ->
                    if (item.id == itemId) {
                        val updatedPrice1 = if (defaultPrice != null && item.price1Text.isBlank()) {
                            formatNum(defaultPrice)
                        } else {
                            item.price1Text
                        }
                        item.copy(newspaperName = newspaperName, price1Text = updatedPrice1)
                    } else item
                },
                errorMessage = null
            )
        }
    }

    fun updateLineItemPrice1(itemId: Int, text: String) {
        val sanitized = text.filter { it.isDigit() || it == '.' }
        _billForm.update { form ->
            form.copy(
                items = form.items.map { if (it.id == itemId) it.copy(price1Text = sanitized) else it },
                errorMessage = null
            )
        }
    }

    fun updateLineItemQty1(itemId: Int, text: String) {
        val sanitized = text.filter { it.isDigit() }
        _billForm.update { form ->
            form.copy(
                items = form.items.map { if (it.id == itemId) it.copy(quantity1Text = sanitized) else it },
                errorMessage = null
            )
        }
    }

    fun updateLineItemPrice2(itemId: Int, text: String) {
        val sanitized = text.filter { it.isDigit() || it == '.' }
        _billForm.update { form ->
            form.copy(
                items = form.items.map { if (it.id == itemId) it.copy(price2Text = sanitized) else it },
                errorMessage = null
            )
        }
    }

    fun updateLineItemQty2(itemId: Int, text: String) {
        val sanitized = text.filter { it.isDigit() }
        _billForm.update { form ->
            form.copy(
                items = form.items.map { if (it.id == itemId) it.copy(quantity2Text = sanitized) else it },
                errorMessage = null
            )
        }
    }

    fun addLineItem() {
        val newId = nextDraftItemId++
        _billForm.update { form ->
            form.copy(items = form.items + BillLineItemDraft(id = newId))
        }
    }

    fun removeLineItem(itemId: Int) {
        _billForm.update { form ->
            if (form.items.size <= 1) form
            else form.copy(items = form.items.filterNot { it.id == itemId })
        }
    }

    fun saveAndPreviewBill() {
        val form = _billForm.value
        val trimmedCustomer = form.customerName.trim()
        val trimmedBillNumber = form.billNumber.trim()

        if (trimmedBillNumber.isEmpty()) {
            _billForm.update { it.copy(errorMessage = "Please enter a bill number.") }
            return
        }
        if (trimmedCustomer.isEmpty()) {
            _billForm.update { it.copy(errorMessage = "Please enter or select a customer name.") }
            return
        }

        val validItems = form.items.filter { it.newspaperName.isNotBlank() && it.itemTotal > 0.0 }
        if (validItems.isEmpty()) {
            _billForm.update {
                it.copy(errorMessage = "Please add at least one newspaper with valid price and quantity.")
            }
            return
        }

        viewModelScope.launch {
            _billForm.update { it.copy(isSaving = true, errorMessage = null) }
            val grandTotal = validItems.sumOf { it.itemTotal }
            val billEntity = BillEntity(
                id = form.editingBillId,
                billNumber = trimmedBillNumber,
                billCategory = form.billCategory,
                billMonth = form.billMonth,
                billYear = form.billYear,
                customerId = form.customerId,
                customerName = trimmedCustomer,
                customerPhone = form.customerPhone.trim(),
                customerAddress = form.customerAddress.trim(),
                billDateMillis = form.billDateMillis,
                billStartDateMillis = form.billStartDateMillis,
                billEndDateMillis = form.billEndDateMillis,
                paymentInstructions = form.paymentInstructions.trim(),
                grandTotal = grandTotal,
                paymentStatus = form.paymentStatus
            )
            val itemEntities = validItems.map { draft ->
                BillItemEntity(
                    billId = form.editingBillId,
                    newspaperName = draft.newspaperName.trim(),
                    price1 = draft.price1,
                    quantity1 = draft.quantity1,
                    amount1 = draft.amount1,
                    price2 = draft.price2,
                    quantity2 = draft.quantity2,
                    amount2 = draft.amount2,
                    itemTotal = draft.itemTotal
                )
            }

            val result = repository.saveBillWithItems(billEntity, itemEntities)
            result.fold(
                onSuccess = { saved ->
                    _billForm.update { it.copy(isSaving = false) }
                    _snackbarMessage.value = "Bill ${saved.bill.billNumber} saved."
                    _currentScreen.value = AppScreen.BillPreview(saved.bill.id)
                },
                onFailure = { error ->
                    _billForm.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = error.message ?: "Could not save bill."
                        )
                    }
                }
            )
        }
    }

    fun toggleBillPaymentStatus(bill: BillEntity) {
        val nextStatus = if (bill.paymentStatus == BillEntity.PAYMENT_PAID) {
            BillEntity.PAYMENT_UNPAID
        } else {
            BillEntity.PAYMENT_PAID
        }
        viewModelScope.launch {
            repository.updatePaymentStatus(bill.id, nextStatus)
            _snackbarMessage.value = "Bill ${bill.billNumber} marked as $nextStatus."
        }
    }

    fun deleteBill(bill: BillEntity) {
        viewModelScope.launch {
            repository.deleteBill(bill.id)
            _snackbarMessage.value = "Bill ${bill.billNumber} deleted."
            if (_currentScreen.value is AppScreen.BillPreview) {
                _currentScreen.value = returnScreenAfterBill
            }
        }
    }

    // Bill filter updates
    fun updateBillSearchQuery(query: String) {
        _billFilter.update { it.copy(searchQuery = query) }
    }

    fun updateBillPaymentFilter(filter: PaymentStatusFilter) {
        _billFilter.update { it.copy(paymentStatus = filter) }
    }

    fun updateBillDateRangeFilter(filter: DateRangeFilter) {
        _billFilter.update { it.copy(dateRange = filter) }
    }

    fun updateCustomDateRange(startMillis: Long?, endMillis: Long?) {
        _billFilter.update {
            it.copy(
                dateRange = DateRangeFilter.CUSTOM,
                customStartMillis = startMillis,
                customEndMillis = endMillis
            )
        }
    }

    // Customer search & CRUD
    fun updateCustomerSearchQuery(query: String) {
        _customerSearch.update { it.copy(query = query) }
    }

    fun updateCustomerSearchField(field: CustomerSearchField) {
        _customerSearch.update { it.copy(field = field) }
    }

    fun saveCustomer(id: Long, name: String, phone: String, address: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            if (id == 0L) {
                repository.insertCustomer(
                    CustomerEntity(name = name.trim(), phone = phone.trim(), address = address.trim())
                )
                _snackbarMessage.value = "Customer added."
            } else {
                repository.updateCustomer(
                    CustomerEntity(id = id, name = name.trim(), phone = phone.trim(), address = address.trim())
                )
                _snackbarMessage.value = "Customer updated."
            }
        }
    }

    fun deleteCustomer(customer: CustomerEntity) {
        viewModelScope.launch {
            repository.deleteCustomer(customer)
            _snackbarMessage.value = "Customer deleted."
        }
    }

    fun addNewspaper(name: String, defaultPrice: Double) {
        if (name.isBlank() || defaultPrice <= 0.0) return
        viewModelScope.launch {
            repository.insertNewspaper(NewspaperEntity(name = name.trim(), defaultPrice = defaultPrice))
            _snackbarMessage.value = "Newspaper added."
        }
    }

    fun deleteNewspaper(newspaper: NewspaperEntity) {
        viewModelScope.launch {
            repository.deleteNewspaper(newspaper)
            _snackbarMessage.value = "Newspaper removed."
        }
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
    }

    private fun startOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun endOfDay(millis: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }
        return cal.timeInMillis
    }
}
