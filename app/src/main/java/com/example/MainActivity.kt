package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BillEntity
import com.example.ui.AppScreen
import com.example.ui.BillingViewModel
import com.example.ui.screens.BillPreviewScreen
import com.example.ui.screens.BillsListScreen
import com.example.ui.screens.CentralJailHubScreen
import com.example.ui.screens.CreateBillScreen
import com.example.ui.screens.CustomersScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.IgOfficeHubScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.InkNavy
import com.example.ui.theme.NewspaperBillingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NewspaperBillingTheme {
                NewspaperBillingApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewspaperBillingApp(viewModel: BillingViewModel = viewModel()) {
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val loginError by viewModel.loginError.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val filteredBills by viewModel.filteredBills.collectAsStateWithLifecycle()
    val allBills by viewModel.allBills.collectAsStateWithLifecycle()
    val billFilter by viewModel.billFilter.collectAsStateWithLifecycle()
    val filteredCustomers by viewModel.filteredCustomers.collectAsStateWithLifecycle()
    val allCustomers by viewModel.allCustomers.collectAsStateWithLifecycle()
    val customerSearch by viewModel.customerSearch.collectAsStateWithLifecycle()
    val newspapers by viewModel.allNewspapers.collectAsStateWithLifecycle()
    val billForm by viewModel.billForm.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            snackbarHostState.showSnackbar(snackbarMessage!!)
            viewModel.clearSnackbarMessage()
        }
    }

    Crossfade(
        targetState = showSplash,
        animationSpec = tween(durationMillis = 320),
        label = "splash_to_login_transition"
    ) { isSplashVisible ->
        if (isSplashVisible) {
            SplashScreen(onFinished = { showSplash = false })
        } else if (!isLoggedIn) {
            LoginScreen(
                errorMessage = loginError,
                onLogin = viewModel::login,
                onClearError = viewModel::clearLoginError
            )
        } else {
            MainAppContent(
                viewModel = viewModel,
                currentScreen = currentScreen,
                filteredBills = filteredBills,
                allBills = allBills,
                billFilter = billFilter,
                filteredCustomers = filteredCustomers,
                allCustomers = allCustomers,
                customerSearch = customerSearch,
                newspapers = newspapers,
                billForm = billForm,
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainAppContent(
    viewModel: BillingViewModel,
    currentScreen: AppScreen,
    filteredBills: List<com.example.data.BillWithItems>,
    allBills: List<com.example.data.BillWithItems>,
    billFilter: com.example.ui.BillFilterState,
    filteredCustomers: List<com.example.data.CustomerEntity>,
    allCustomers: List<com.example.data.CustomerEntity>,
    customerSearch: com.example.ui.CustomerSearchState,
    newspapers: List<com.example.data.NewspaperEntity>,
    billForm: com.example.ui.BillFormState,
    snackbarHostState: SnackbarHostState
) {

    when (val screen = currentScreen) {
        is AppScreen.Dashboard -> {
            DashboardScreen(
                totalBillsCount = allBills.size,
                totalCustomersCount = allCustomers.size,
                onOpenCentralJail = { viewModel.navigateTo(AppScreen.CentralJailHub) },
                onOpenIgOffice = { viewModel.navigateTo(AppScreen.IgOfficeHub) },
                onOpenCustomerBill = { viewModel.navigateTo(AppScreen.CustomerBills) },
                onOpenSettings = { viewModel.navigateTo(AppScreen.Settings) },
                onLogout = viewModel::logout
            )
        }

        is AppScreen.CentralJailHub -> {
            val jailCount = allBills.count { it.bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL }
            CentralJailHubScreen(
                jailBillsCount = jailCount,
                onBack = { viewModel.navigateTo(AppScreen.Dashboard) },
                onCreateNewBill = {
                    viewModel.startNewBill(
                        category = BillEntity.CATEGORY_CENTRAL_JAIL,
                        fromScreen = AppScreen.CentralJailHub
                    )
                },
                onOpenHistory = { viewModel.navigateTo(AppScreen.CentralJailHistory) }
            )
        }

        is AppScreen.CentralJailHistory -> {
            BackHandler { viewModel.navigateTo(AppScreen.CentralJailHub) }
            val jailBills = remember(filteredBills) {
                filteredBills.filter { it.bill.billCategory == BillEntity.CATEGORY_CENTRAL_JAIL }
            }
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text("Central Jail Bill History", fontWeight = FontWeight.Bold, color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.navigateTo(AppScreen.CentralJailHub) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = InkNavy)
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.startNewBill(
                                category = BillEntity.CATEGORY_CENTRAL_JAIL,
                                fromScreen = AppScreen.CentralJailHistory
                            )
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("New Bill", fontWeight = FontWeight.Bold) },
                        containerColor = InkNavy,
                        contentColor = Color.White
                    )
                }
            ) { innerPadding ->
                BillsListScreen(
                    bills = jailBills,
                    filterState = billFilter,
                    onSearchChange = viewModel::updateBillSearchQuery,
                    onPaymentFilterChange = viewModel::updateBillPaymentFilter,
                    onDateRangeFilterChange = viewModel::updateBillDateRangeFilter,
                    onCustomDateRangeChange = viewModel::updateCustomDateRange,
                    onBillClick = { viewModel.openBillPreview(it.bill.id, AppScreen.CentralJailHistory) },
                    onEditBill = { viewModel.startEditBill(it, AppScreen.CentralJailHistory) },
                    onTogglePaid = viewModel::toggleBillPaymentStatus,
                    onDeleteBill = viewModel::deleteBill,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        is AppScreen.IgOfficeHub -> {
            val igCount = allBills.count { it.bill.billCategory == BillEntity.CATEGORY_IG_OFFICE }
            IgOfficeHubScreen(
                igBillsCount = igCount,
                onBack = { viewModel.navigateTo(AppScreen.Dashboard) },
                onCreateNewBill = {
                    viewModel.startNewBill(
                        category = BillEntity.CATEGORY_IG_OFFICE,
                        fromScreen = AppScreen.IgOfficeHub
                    )
                },
                onOpenHistory = { viewModel.navigateTo(AppScreen.IgOfficeHistory) }
            )
        }

        is AppScreen.IgOfficeHistory -> {
            BackHandler { viewModel.navigateTo(AppScreen.IgOfficeHub) }
            val igBills = remember(filteredBills) {
                filteredBills.filter { it.bill.billCategory == BillEntity.CATEGORY_IG_OFFICE }
            }
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = { Text("IG Office Bill History", fontWeight = FontWeight.Bold, color = Color.White) },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.navigateTo(AppScreen.IgOfficeHub) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = InkNavy)
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = {
                            viewModel.startNewBill(
                                category = BillEntity.CATEGORY_IG_OFFICE,
                                fromScreen = AppScreen.IgOfficeHistory
                            )
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("New IG Bill", fontWeight = FontWeight.Bold) },
                        containerColor = InkNavy,
                        contentColor = Color.White
                    )
                }
            ) { innerPadding ->
                BillsListScreen(
                    bills = igBills,
                    filterState = billFilter,
                    onSearchChange = viewModel::updateBillSearchQuery,
                    onPaymentFilterChange = viewModel::updateBillPaymentFilter,
                    onDateRangeFilterChange = viewModel::updateBillDateRangeFilter,
                    onCustomDateRangeChange = viewModel::updateCustomDateRange,
                    onBillClick = { viewModel.openBillPreview(it.bill.id, AppScreen.IgOfficeHistory) },
                    onEditBill = { viewModel.startEditBill(it, AppScreen.IgOfficeHistory) },
                    onTogglePaid = viewModel::toggleBillPaymentStatus,
                    onDeleteBill = viewModel::deleteBill,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        is AppScreen.CustomerBills -> {
            BackHandler { viewModel.navigateTo(AppScreen.Dashboard) }
            var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }
            var showAddCustomerDialog by remember { mutableStateOf(false) }
            val customerBills = remember(filteredBills) {
                filteredBills.filter { it.bill.billCategory == BillEntity.CATEGORY_CUSTOMER }
            }

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = if (selectedSubTab == 0) "Customer Bills" else "Customers Directory",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.navigateTo(AppScreen.Dashboard) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = InkNavy)
                    )
                },
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedSubTab == 0,
                            onClick = { selectedSubTab = 0 },
                            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) },
                            label = { Text("Bills") }
                        )
                        NavigationBarItem(
                            selected = selectedSubTab == 1,
                            onClick = { selectedSubTab = 1 },
                            icon = { Icon(Icons.Default.People, contentDescription = null) },
                            label = { Text("Customers") }
                        )
                    }
                },
                floatingActionButton = {
                    if (selectedSubTab == 0) {
                        ExtendedFloatingActionButton(
                            onClick = {
                                viewModel.startNewBill(
                                    category = BillEntity.CATEGORY_CUSTOMER,
                                    fromScreen = AppScreen.CustomerBills
                                )
                            },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("New Bill", fontWeight = FontWeight.Bold) },
                            containerColor = InkNavy,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("fab_new_bill")
                        )
                    } else {
                        ExtendedFloatingActionButton(
                            onClick = { showAddCustomerDialog = true },
                            icon = { Icon(Icons.Default.Add, contentDescription = null) },
                            text = { Text("Add Customer", fontWeight = FontWeight.Bold) },
                            containerColor = InkNavy,
                            contentColor = Color.White,
                            modifier = Modifier.testTag("fab_add_customer")
                        )
                    }
                }
            ) { innerPadding ->
                Box(modifier = Modifier.padding(innerPadding)) {
                    if (selectedSubTab == 0) {
                        BillsListScreen(
                            bills = customerBills,
                            filterState = billFilter,
                            onSearchChange = viewModel::updateBillSearchQuery,
                            onPaymentFilterChange = viewModel::updateBillPaymentFilter,
                            onDateRangeFilterChange = viewModel::updateBillDateRangeFilter,
                            onCustomDateRangeChange = viewModel::updateCustomDateRange,
                            onBillClick = { viewModel.openBillPreview(it.bill.id, AppScreen.CustomerBills) },
                            onEditBill = { viewModel.startEditBill(it, AppScreen.CustomerBills) },
                            onTogglePaid = viewModel::toggleBillPaymentStatus,
                            onDeleteBill = viewModel::deleteBill
                        )
                    } else {
                        CustomersScreen(
                            customers = filteredCustomers,
                            searchState = customerSearch,
                            onSearchQueryChange = viewModel::updateCustomerSearchQuery,
                            onSearchFieldChange = viewModel::updateCustomerSearchField,
                            onSaveCustomer = viewModel::saveCustomer,
                            onDeleteCustomer = viewModel::deleteCustomer,
                            onCreateBillForCustomer = { customer ->
                                viewModel.startNewBill(
                                    category = BillEntity.CATEGORY_CUSTOMER,
                                    preselectedCustomer = customer,
                                    fromScreen = AppScreen.CustomerBills
                                )
                            },
                            showAddDialog = showAddCustomerDialog,
                            onDismissAddDialog = { showAddCustomerDialog = false }
                        )
                    }
                }
            }
        }

        is AppScreen.Settings -> {
            SettingsScreen(
                newspapers = newspapers,
                onBack = { viewModel.navigateTo(AppScreen.Dashboard) },
                onAddNewspaper = viewModel::addNewspaper,
                onDeleteNewspaper = viewModel::deleteNewspaper,
                onLogout = viewModel::logout
            )
        }

        is AppScreen.CreateEditBill -> {
            CreateBillScreen(
                formState = billForm,
                customers = allCustomers,
                newspapers = newspapers,
                onBack = viewModel::navigateBackFromBill,
                onMonthChange = viewModel::updateBillMonth,
                onBillNumberChange = viewModel::updateBillNumber,
                onCustomerNameChange = viewModel::updateCustomerName,
                onSelectCustomer = viewModel::selectExistingCustomer,
                onDateChange = viewModel::updateBillDate,
                onPaymentStatusChange = viewModel::updateBillPaymentStatus,
                onItemNewspaperChange = viewModel::updateLineItemNewspaper,
                onItemPrice1Change = viewModel::updateLineItemPrice1,
                onItemQty1Change = viewModel::updateLineItemQty1,
                onItemPrice2Change = viewModel::updateLineItemPrice2,
                onItemQty2Change = viewModel::updateLineItemQty2,
                onAddItem = viewModel::addLineItem,
                onRemoveItem = viewModel::removeLineItem,
                onSaveAndPreview = viewModel::saveAndPreviewBill
            )
        }

        is AppScreen.BillPreview -> {
            val selectedBill = allBills.firstOrNull { it.bill.id == screen.billId }
            if (selectedBill != null) {
                BillPreviewScreen(
                    billWithItems = selectedBill,
                    onBack = viewModel::navigateBackFromBill,
                    onEdit = { viewModel.startEditBill(selectedBill) },
                    onTogglePaid = { viewModel.toggleBillPaymentStatus(selectedBill.bill) },
                    onMessage = viewModel::showMessage
                )
            } else {
                viewModel.navigateBackFromBill()
            }
        }
    }
}
