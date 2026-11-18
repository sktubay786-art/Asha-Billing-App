package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.data.model.BillingDashboardData
import com.example.data.model.Customer
import com.example.data.model.customerInitials
import com.example.data.model.formatInr
import com.example.ui.components.AshaPanelCard
import com.example.ui.theme.CrimsonContainer
import com.example.ui.theme.CrimsonDue
import com.example.ui.theme.EmeraldDeep
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PaidBadgeBg
import com.example.ui.theme.PaidBadgeFg
import java.util.Locale

private enum class UserFilter(val label: String) {
    ALL("All"),
    HAS_DUE("With Due"),
    SETTLED("Cleared")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomersScreen(
    data: BillingDashboardData,
    openAddDialogImmediately: Boolean,
    onConsumeAddDialogFlag: () -> Unit,
    onOpenCustomerChat: (Customer) -> Unit,
    onSaveCustomer: (
        existingId: String,
        name: String,
        phone: String,
        village: String,
        address: String,
        openingDue: Double,
        landArea: String,
        landOwnerType: String,
        landOwnerName: String,
        onComplete: (Boolean) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(UserFilter.ALL) }
    var editingCustomer by remember { mutableStateOf<Customer?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }

    if (openAddDialogImmediately) {
        onConsumeAddDialogFlag()
        editingCustomer = null
        showFormDialog = true
    }

    val filteredCustomers = remember(data.customers, data.bills, data.payments, searchQuery, activeFilter) {
        val q = searchQuery.trim().lowercase()
        data.customers.filter { c ->
            val matchesSearch = q.isEmpty() ||
                listOf(c.name, c.phone, c.village, c.address, c.landArea, c.landOwnerName)
                    .joinToString(" ")
                    .lowercase()
                    .contains(q)
            val due = data.dueForCustomer(c.id)
            val matchesFilter = when (activeFilter) {
                UserFilter.ALL -> true
                UserFilter.HAS_DUE -> due > 0.001
                UserFilter.SETTLED -> due <= 0.001
            }
            matchesSearch && matchesFilter
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 88.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.search_customers_hint)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = EmeraldPrimary
                            )
                        },
                        trailingIcon = {
                            AnimatedVisibility(visible = searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(28.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("customer_search_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UserFilter.entries.forEach { filter ->
                                val selected = activeFilter == filter
                                FilterChip(
                                    selected = selected,
                                    onClick = { activeFilter = filter },
                                    label = {
                                        Text(
                                            text = filter.label,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = EmeraldMint,
                                        selectedLabelColor = EmeraldPrimary
                                    ),
                                    modifier = Modifier.testTag("user_filter_${filter.name.lowercase()}")
                                )
                            }
                        }

                        Text(
                            text = "${filteredCustomers.size} users",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (filteredCustomers.isEmpty()) {
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        AshaPanelCard {
                            Text(
                                text = stringResource(R.string.no_customers_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(filteredCustomers, key = { _, c -> c.id }) { index, customer ->
                    val bills = data.billsForCustomer(customer.id)
                    val payments = data.payments.filter { it.customerId == customer.id }
                    val outstandingDue = data.dueForCustomer(customer.id)
                    val lastActivity = (bills.map { it.date to "Bill ${formatInr(it.allTotal)}" } +
                        payments.map { it.date to "Paid ${formatInr(it.amount)}" })
                        .maxByOrNull { it.first }

                    val fallbackMeta = listOf(customer.village, customer.phone)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                        .ifBlank { "Tap to open ledger chat" }

                    WhatsAppStyleUserRow(
                        customer = customer,
                        outstandingDue = outstandingDue,
                        invoiceCount = bills.size,
                        lastDate = lastActivity?.first ?: "",
                        lastSummary = lastActivity?.second ?: fallbackMeta,
                        onClick = { onOpenCustomerChat(customer) }
                    )
                    if (index < filteredCustomers.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 78.dp, end = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = {
                editingCustomer = null
                showFormDialog = true
            },
            containerColor = EmeraldPrimary,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("add_customer_button")
        ) {
            Icon(
                imageVector = Icons.Rounded.PersonAdd,
                contentDescription = stringResource(R.string.add_customer)
            )
        }
    }

    if (showFormDialog) {
        CustomerFormDialog(
            initial = editingCustomer,
            onDismiss = { showFormDialog = false },
            onSave = { name, phone, village, address, openingDue, landArea, landOwnerType, landOwnerName ->
                onSaveCustomer(
                    editingCustomer?.id.orEmpty(),
                    name,
                    phone,
                    village,
                    address,
                    openingDue,
                    landArea,
                    landOwnerType,
                    landOwnerName
                ) { success ->
                    if (success) showFormDialog = false
                }
            }
        )
    }
}

@Composable
private fun WhatsAppStyleUserRow(
    customer: Customer,
    outstandingDue: Double,
    invoiceCount: Int,
    lastDate: String,
    lastSummary: String,
    onClick: () -> Unit
) {
    val hasDue = outstandingDue > 0.001
    val landMeta = customer.landSummaryLabel()
    val subtitleMeta = buildString {
        append(lastSummary)
        if (customer.village.isNotBlank() && !lastSummary.contains(customer.village, ignoreCase = true)) {
            append(" · ")
            append(customer.village)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .testTag("customer_card_${customer.id}"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        if (hasDue) listOf(EmeraldPrimary, EmeraldDeep)
                        else listOf(EmeraldMint, EmeraldMint)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = customerInitials(customer.name),
                style = MaterialTheme.typography.titleMedium,
                color = if (hasDue) Color.White else EmeraldPrimary,
                fontWeight = FontWeight.ExtraBold
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = customer.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = lastDate.ifBlank { "$invoiceCount bills" },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasDue) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = subtitleMeta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (hasDue) CrimsonContainer else PaidBadgeBg
                ) {
                    Text(
                        text = if (hasDue) formatInr(outstandingDue) else "Cleared",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (hasDue) CrimsonDue else PaidBadgeFg,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            if (landMeta.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Landscape,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = landMeta,
                        fontSize = 11.sp,
                        color = EmeraldPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerFormDialog(
    initial: Customer?,
    onDismiss: () -> Unit,
    onSave: (
        name: String,
        phone: String,
        village: String,
        address: String,
        openingDue: Double,
        landArea: String,
        landOwnerType: String,
        landOwnerName: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var village by remember { mutableStateOf(initial?.village.orEmpty()) }
    var address by remember { mutableStateOf(initial?.address.orEmpty()) }
    var openingDueText by remember {
        mutableStateOf(
            if (initial != null && initial.openingDue > 0.0) initial.openingDue.toString() else "0"
        )
    }
    var landArea by remember { mutableStateOf(initial?.landArea.orEmpty()) }
    var ownerType by remember {
        mutableStateOf(initial?.normalizedOwnerType() ?: "Own")
    }
    var ownerName by remember { mutableStateOf(initial?.landOwnerName.orEmpty()) }
    var nameError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initial == null) stringResource(R.string.add_customer) else stringResource(R.string.edit_customer),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.close))
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text(stringResource(R.string.full_name)) },
                    isError = nameError,
                    supportingText = if (nameError) ({ Text("Name is required") }) else null,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_name_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(stringResource(R.string.phone)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_phone_input")
                    )
                    OutlinedTextField(
                        value = village,
                        onValueChange = { village = it },
                        label = { Text(stringResource(R.string.village)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_village_input")
                    )
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text(stringResource(R.string.address)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_address_input")
                )

                // Manual Land Area ("জমির পরিমাণ") & Owner ("Own" / "Other")
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = landArea,
                            onValueChange = { landArea = it },
                            label = { Text("জমির পরিমাণ / Area") },
                            placeholder = { Text("e.g. 2.5 Bigha / 40 Dec") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("customer_land_area_input")
                        )

                        Text(
                            text = "Land Owner",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )

                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                                selected = ownerType == "Own",
                                onClick = { ownerType = "Own" },
                                modifier = Modifier.testTag("customer_owner_own")
                            ) {
                                Text("Own", fontWeight = FontWeight.SemiBold)
                            }
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                                selected = ownerType == "Other",
                                onClick = { ownerType = "Other" },
                                modifier = Modifier.testTag("customer_owner_other")
                            ) {
                                Text("Other", fontWeight = FontWeight.SemiBold)
                            }
                        }

                        AnimatedVisibility(visible = ownerType == "Other") {
                            OutlinedTextField(
                                value = ownerName,
                                onValueChange = { ownerName = it },
                                label = { Text("Owner Name") },
                                placeholder = { Text("Enter land owner's name") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("customer_owner_name_input")
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = openingDueText,
                    onValueChange = { openingDueText = it },
                    label = { Text(stringResource(R.string.opening_due)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("customer_opening_due_input")
                )

                Button(
                    onClick = {
                        if (name.trim().isEmpty()) {
                            nameError = true
                            return@Button
                        }
                        val due = (openingDueText.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
                        onSave(
                            name,
                            phone,
                            village,
                            address,
                            due,
                            landArea.trim(),
                            ownerType,
                            if (ownerType == "Other") ownerName.trim() else ""
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("save_customer_button")
                ) {
                    Text(stringResource(R.string.save_customer))
                }
            }
        }
    }
}

@Composable
fun SettleCustomerDialog(
    customer: Customer,
    totalDue: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember {
        mutableStateOf(String.format(Locale.US, "%.2f", totalDue))
    }
    var invalidError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Settle ${customer.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Due: ${formatInr(totalDue)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        invalidError = false
                    },
                    label = { Text(stringResource(R.string.amount_inr)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = invalidError,
                    supportingText = if (invalidError) ({ Text("Enter an amount between 0 and ${formatInr(totalDue)}") }) else null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settle_amount_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val value = amountText.toDoubleOrNull()
                    if (value == null || value <= 0.0 || value > totalDue + 0.001) {
                        invalidError = true
                    } else {
                        onConfirm(value)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                modifier = Modifier.testTag("confirm_settle_button")
            ) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
