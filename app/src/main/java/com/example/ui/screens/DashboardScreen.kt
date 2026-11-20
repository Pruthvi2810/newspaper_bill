package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.R
import com.example.ui.theme.InkNavy
import com.example.ui.theme.Terracotta

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    totalBillsCount: Int,
    totalCustomersCount: Int,
    onOpenCentralJail: () -> Unit,
    onOpenIgOffice: () -> Unit,
    onOpenCustomerBill: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_pruthvi_globe_news_1795101867268),
                            contentDescription = "Pruthvi News Agency",
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Pruthvi News Agency",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "News at Your Doorstep...",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onLogout,
                        modifier = Modifier.testTag("dashboard_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = InkNavy)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Orange accent welcome strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = InkNavy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Billing Dashboard",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "$totalBillsCount Saved Bills  •  $totalCustomersCount Customers",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Terracotta
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Text(
                text = "Select Module / મોડ્યુલ પસંદ કરો",
                style = MaterialTheme.typography.titleMedium,
                color = InkNavy,
                fontWeight = FontWeight.Bold
            )

            DashboardModuleCard(
                title = "1. Ahmedabad Central Jail Bill",
                subtitle = "અમદાવાદ મધ્યસ્થ જેલ બિલ • Create Bill & History",
                icon = Icons.Default.AccountBalance,
                accentColor = InkNavy,
                onClick = onOpenCentralJail,
                testTag = "module_central_jail"
            )

            DashboardModuleCard(
                title = "2. IG Office Bill",
                subtitle = "IG ઓફિસ બિલ • Create & Manage IG Office Bills",
                icon = Icons.Default.Gavel,
                accentColor = Terracotta,
                onClick = onOpenIgOffice,
                testTag = "module_ig_office"
            )

            DashboardModuleCard(
                title = "3. Customer Bill",
                subtitle = "ગ્રાહક બિલ • Customer Directory, Search & Billing",
                icon = Icons.Default.People,
                accentColor = Color(0xFF1B7A43),
                onClick = onOpenCustomerBill,
                testTag = "module_customer_bill"
            )

            DashboardModuleCard(
                title = "4. Settings",
                subtitle = "સેટિંગ્સ • Newspapers, Default Rates & Agency Info",
                icon = Icons.Default.Settings,
                accentColor = Color(0xFF5A6270),
                onClick = onOpenSettings,
                testTag = "module_settings"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CentralJailHubScreen(
    jailBillsCount: Int,
    onBack: () -> Unit,
    onCreateNewBill: () -> Unit,
    onOpenHistory: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ahmedabad Central Jail Bill",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fixed Customer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Supritendent of Police,Central Jail Ahmedabad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "અધિક્ષક સાહેબ શ્રી અમદાવાદ મધ્યસ્થ જેલ,અમદાવાદ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            DashboardModuleCard(
                title = "1. Create New Bill",
                subtitle = "નવું બિલ બનાવો • Auto bill no. 10MM/YYYY, Dual Rate & Qty",
                icon = Icons.Default.AddCircleOutline,
                accentColor = Terracotta,
                onClick = onCreateNewBill,
                testTag = "central_jail_create_bill"
            )

            DashboardModuleCard(
                title = "2. Bill History ($jailBillsCount)",
                subtitle = "જૂના બિલ જુઓ • Search, Filter by Date & Paid/Unpaid",
                icon = Icons.Default.History,
                accentColor = InkNavy,
                onClick = onOpenHistory,
                testTag = "central_jail_bill_history"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IgOfficeHubScreen(
    igBillsCount: Int,
    onBack: () -> Unit,
    onCreateNewBill: () -> Unit,
    onOpenHistory: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "IG Office Bill",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fixed Customer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Office of Inspector General of Gujarat Jails, ahmedabad",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            DashboardModuleCard(
                title = "1. Create New Bill",
                subtitle = "નવું બિલ બનાવો • Auto bill no. 20MM/YYYY, Price × Qty",
                icon = Icons.Default.AddCircleOutline,
                accentColor = Terracotta,
                onClick = onCreateNewBill,
                testTag = "ig_office_create_bill"
            )

            DashboardModuleCard(
                title = "2. Bill History ($igBillsCount)",
                subtitle = "જૂના બિલ જુઓ • Search, Filter by Date & Paid/Unpaid",
                icon = Icons.Default.History,
                accentColor = InkNavy,
                onClick = onOpenHistory,
                testTag = "ig_office_bill_history"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHubScreen(
    customerBillsCount: Int,
    customersCount: Int,
    onBack: () -> Unit,
    onCreateNewBill: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenDirectory: () -> Unit
) {
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Customer Bills",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardModuleCard(
                title = "1. Create New Bill",
                subtitle = "નવું ગ્રાહક બિલ • Auto bill no. 30MM/YYYY, Due Dates & UPI",
                icon = Icons.Default.AddCircleOutline,
                accentColor = Terracotta,
                onClick = onCreateNewBill,
                testTag = "customer_create_bill"
            )

            DashboardModuleCard(
                title = "2. Bill History ($customerBillsCount)",
                subtitle = "જૂના બિલ જુઓ • Search, Filter by Date & Paid/Unpaid",
                icon = Icons.Default.History,
                accentColor = InkNavy,
                onClick = onOpenHistory,
                testTag = "customer_bill_history"
            )

            DashboardModuleCard(
                title = "3. Customers Directory ($customersCount)",
                subtitle = "ગ્રાહક યાદી • Search by Name, Phone & Address",
                icon = Icons.Default.People,
                accentColor = Color(0xFF1B7A43),
                onClick = onOpenDirectory,
                testTag = "customer_directory"
            )
        }
    }
}

@Composable
private fun DashboardModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = accentColor.copy(alpha = 0.14f),
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
