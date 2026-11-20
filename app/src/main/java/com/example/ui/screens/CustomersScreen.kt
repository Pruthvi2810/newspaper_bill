package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.CustomerEntity
import com.example.ui.CustomerSearchField
import com.example.ui.CustomerSearchState

@Composable
fun CustomersScreen(
    customers: List<CustomerEntity>,
    searchState: CustomerSearchState,
    onSearchQueryChange: (String) -> Unit,
    onSearchFieldChange: (CustomerSearchField) -> Unit,
    onSaveCustomer: (Long, String, String, String) -> Unit,
    onDeleteCustomer: (CustomerEntity) -> Unit,
    onCreateBillForCustomer: (CustomerEntity) -> Unit,
    showAddDialog: Boolean,
    onDismissAddDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }
    var deletingCustomer by remember { mutableStateOf<CustomerEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Advanced Search Bar (Name, Phone, Address)
        OutlinedTextField(
            value = searchState.query,
            onValueChange = onSearchQueryChange,
            placeholder = {
                Text(
                    when (searchState.field) {
                        CustomerSearchField.ALL -> "Search name, phone, or address..."
                        CustomerSearchField.NAME -> "Search by customer name..."
                        CustomerSearchField.PHONE -> "Search by phone number..."
                        CustomerSearchField.ADDRESS -> "Search by address..."
                    }
                )
            },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchState.query.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("customer_search_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Search Field Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = searchState.field == CustomerSearchField.ALL,
                onClick = { onSearchFieldChange(CustomerSearchField.ALL) },
                label = { Text("All Fields") },
                modifier = Modifier.testTag("customer_filter_all")
            )
            FilterChip(
                selected = searchState.field == CustomerSearchField.NAME,
                onClick = { onSearchFieldChange(CustomerSearchField.NAME) },
                label = { Text("Name") },
                modifier = Modifier.testTag("customer_filter_name")
            )
            FilterChip(
                selected = searchState.field == CustomerSearchField.PHONE,
                onClick = { onSearchFieldChange(CustomerSearchField.PHONE) },
                label = { Text("Phone") },
                modifier = Modifier.testTag("customer_filter_phone")
            )
            FilterChip(
                selected = searchState.field == CustomerSearchField.ADDRESS,
                onClick = { onSearchFieldChange(CustomerSearchField.ADDRESS) },
                label = { Text("Address") },
                modifier = Modifier.testTag("customer_filter_address")
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (customers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No customers found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap + Add Customer to save customer details.",
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
                items(customers, key = { it.id }) { customer ->
                    CustomerCard(
                        customer = customer,
                        onCreateBill = { onCreateBillForCustomer(customer) },
                        onEdit = { editingCustomer = customer },
                        onDelete = { deletingCustomer = customer }
                    )
                }
            }
        }
    }

    if (showAddDialog || editingCustomer != null) {
        val target = editingCustomer
        CustomerEditorDialog(
            initialCustomer = target,
            onDismiss = {
                editingCustomer = null
                onDismissAddDialog()
            },
            onSave = { name, phone, address ->
                onSaveCustomer(target?.id ?: 0L, name, phone, address)
                editingCustomer = null
                onDismissAddDialog()
            }
        )
    }

    if (deletingCustomer != null) {
        val target = deletingCustomer!!
        AlertDialog(
            onDismissRequest = { deletingCustomer = null },
            title = { Text("Delete Customer?") },
            text = { Text("Remove ${target.name} from your customer list?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCustomer(target)
                        deletingCustomer = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCustomer = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun CustomerCard(
    customer: CustomerEntity,
    onCreateBill: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit Customer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Customer",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (customer.phone.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = customer.phone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (customer.address.isNotBlank()) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = customer.address,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = onCreateBill,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Bill", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CustomerEditorDialog(
    initialCustomer: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember(initialCustomer) { mutableStateOf(initialCustomer?.name.orEmpty()) }
    var phone by remember(initialCustomer) { mutableStateOf(initialCustomer?.phone.orEmpty()) }
    var address by remember(initialCustomer) { mutableStateOf(initialCustomer?.address.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialCustomer == null) "Add Customer" else "Edit Customer",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name *") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_customer_name")
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_customer_phone")
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_customer_address")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, address) },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("dialog_customer_save")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
