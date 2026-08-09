package com.maryhairsalon.ui.manager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.maryhairsalon.data.models.Booking
import com.maryhairsalon.data.models.Category
import com.maryhairsalon.data.models.Service
import com.maryhairsalon.data.models.User

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagerDashboardScreen(
    onLogout: () -> Unit,
    viewModel: ManagerViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(when (selectedTab) { 0 -> "All Bookings"; 1 -> "Staff"; else -> "Catalog" })
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                    label = { Text("Bookings") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Group, contentDescription = null) },
                    label = { Text("Staff") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.ContentCut, contentDescription = null) },
                    label = { Text("Catalog") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (selectedTab) {
                0 -> ManagerBookingsTab(viewModel)
                1 -> ManagerStaffTab(viewModel)
                else -> ManagerCatalogTab(viewModel)
            }
        }
    }
}

@Composable
private fun ManagerBookingsTab(viewModel: ManagerViewModel) {
    val uiState by viewModel.bookingsUiState.collectAsStateWithLifecycle()
    val staffState by viewModel.staffUiState.collectAsStateWithLifecycle()
    val employees = staffState.users.filter { it.role == "employee" }
    val actionMessage by viewModel.bookingActionMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var reassignTarget by remember { mutableStateOf<Booking?>(null) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearBookingActionMessage()
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all", "pending", "upcoming", "completed", "cancelled").forEach { status ->
                    FilterChip(
                        selected = uiState.activeFilter.equals(status, ignoreCase = true),
                        onClick = { viewModel.setFilter(status) },
                        label = { Text(status.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(uiState.errorMessage!!) }
                uiState.bookings.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No bookings match this filter.") }
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.bookings, key = { it.id }) { booking ->
                        ManagerBookingCard(
                            booking = booking,
                            onStatusChange = { status -> viewModel.updateStatus(booking.id, status) },
                            onReassignClick = { reassignTarget = booking }
                        )
                    }
                }
            }
        }
    }

    reassignTarget?.let { booking ->
        ReassignEmployeeDialog(
            options = employees,
            current = booking.employee,
            onDismiss = { reassignTarget = null },
            onConfirm = { user ->
                viewModel.reassignEmployee(booking.id, user.id, user.name)
                reassignTarget = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerBookingCard(
    booking: Booking,
    onStatusChange: (String) -> Unit,
    onReassignClick: () -> Unit
) {
    var statusMenuOpen by remember { mutableStateOf(false) }

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(booking.service.ifBlank { "Service" }, style = MaterialTheme.typography.titleMedium)
                    Text("${booking.name} ${booking.surname}".trim(), style = MaterialTheme.typography.bodySmall)
                }
                Box {
                    AssistChip(onClick = { statusMenuOpen = true }, label = { Text(statusLabel(booking.status)) })
                    DropdownMenu(expanded = statusMenuOpen, onDismissRequest = { statusMenuOpen = false }) {
                        listOf("pending", "upcoming", "completed", "cancelled").forEach { status ->
                            DropdownMenuItem(
                                text = { Text(statusLabel(status)) },
                                onClick = { onStatusChange(status); statusMenuOpen = false }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("${booking.dateFormatted.ifBlank { booking.date }} · ${booking.time}")
            Text("${booking.type}${if (booking.type == "Outcall" && booking.location.isNotBlank()) " — ${booking.location}" else ""}")
            Text("Contact: ${booking.contact}", style = MaterialTheme.typography.bodySmall)
            if (booking.price.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("R${booking.price}", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Stylist: ${booking.employee.ifBlank { "Unassigned" }}", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onReassignClick) { Text("Reassign") }
            }
        }
    }
}

@Composable
private fun ReassignEmployeeDialog(
    options: List<User>,
    current: String,
    onDismiss: () -> Unit,
    onConfirm: (User) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Reassign stylist") },
        text = {
            if (options.isEmpty()) {
                Text("No employee accounts yet — promote a customer to Employee on the Staff tab first.")
            } else {
                Column {
                    options.forEach { user ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = user.name == current, onClick = { onConfirm(user) })
                            Text(user.name)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ManagerStaffTab(viewModel: ManagerViewModel) {
    val uiState by viewModel.staffUiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(uiState.errorMessage!!) }
        uiState.users.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No staff or customers found.") }
        else -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.users, key = { it.id }) { user ->
                StaffRow(
                    user = user,
                    onToggleRole = {
                        val newRole = if (user.role == "employee") "customer" else "employee"
                        viewModel.setUserRole(user.id, newRole)
                    }
                )
            }
        }
    }
}

@Composable
private fun StaffRow(user: User, onToggleRole: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(user.name, style = MaterialTheme.typography.titleMedium)
                Text(user.email, style = MaterialTheme.typography.bodySmall)
                Text(
                    if (user.role == "employee") "Employee" else "Customer",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            OutlinedButton(onClick = onToggleRole) {
                Text(if (user.role == "employee") "Make Customer" else "Make Employee")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerCatalogTab(viewModel: ManagerViewModel) {
    val uiState by viewModel.catalogUiState.collectAsStateWithLifecycle()
    val actionMessage by viewModel.catalogActionMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var subTab by remember { mutableIntStateOf(0) }
    var editingCategory by remember { mutableStateOf<Category?>(null) }
    var showNewCategory by remember { mutableStateOf(false) }
    var editingService by remember { mutableStateOf<Service?>(null) }
    var showNewService by remember { mutableStateOf(false) }
    var pendingDeleteCategoryId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteServiceId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearCatalogActionMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(onClick = { if (subTab == 0) showNewCategory = true else showNewService = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = subTab) {
                Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text("Categories") })
                Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text("Services") })
            }

            when {
                uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                uiState.errorMessage != null -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text(uiState.errorMessage!!) }
                subTab == 0 -> {
                    if (uiState.categories.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No categories yet — tap + to add one.") }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(uiState.categories, key = { it.id }) { category ->
                                CategoryRow(
                                    category = category,
                                    onEdit = { editingCategory = category },
                                    onDelete = { pendingDeleteCategoryId = category.id }
                                )
                            }
                        }
                    }
                }
                else -> {
                    if (uiState.services.isEmpty()) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No services yet — tap + to add one.") }
                    } else {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(uiState.services, key = { it.id }) { service ->
                                ServiceRow(
                                    service = service,
                                    categoryLabel = uiState.categories.find { it.id == service.category }?.label ?: "Uncategorized",
                                    onEdit = { editingService = service },
                                    onDelete = { pendingDeleteServiceId = service.id }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewCategory) {
        CategoryEditDialog(
            initial = null,
            onDismiss = { showNewCategory = false },
            onSave = { label, desc -> viewModel.addCategory(label, desc); showNewCategory = false }
        )
    }
    editingCategory?.let { category ->
        CategoryEditDialog(
            initial = category,
            onDismiss = { editingCategory = null },
            onSave = { label, desc -> viewModel.updateCategory(category.id, label, desc); editingCategory = null }
        )
    }
    pendingDeleteCategoryId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCategoryId = null },
            title = { Text("Delete this category?") },
            text = { Text("This can't be undone. Services still in this category must be moved or deleted first.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteCategory(id); pendingDeleteCategoryId = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDeleteCategoryId = null }) { Text("Cancel") } }
        )
    }

    if (showNewService) {
        ServiceEditDialog(
            initial = null,
            categories = uiState.categories,
            onDismiss = { showNewService = false },
            onSave = { title, categoryId, price, discount, desc ->
                viewModel.addService(title, categoryId, price, discount, desc)
                showNewService = false
            }
        )
    }
    editingService?.let { service ->
        ServiceEditDialog(
            initial = service,
            categories = uiState.categories,
            onDismiss = { editingService = null },
            onSave = { title, categoryId, price, discount, desc ->
                viewModel.updateService(service.id, title, categoryId, price, discount, desc)
                editingService = null
            }
        )
    }
    pendingDeleteServiceId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteServiceId = null },
            title = { Text("Delete this service?") },
            text = { Text("This can't be undone. Existing bookings for this service are unaffected.") },
            confirmButton = { TextButton(onClick = { viewModel.deleteService(id); pendingDeleteServiceId = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { pendingDeleteServiceId = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun CategoryRow(category: Category, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(category.label, style = MaterialTheme.typography.titleMedium)
                if (category.desc.isNotBlank()) {
                    Text(category.desc, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
            }
        }
    }
}

@Composable
private fun ServiceRow(service: Service, categoryLabel: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(service.title, style = MaterialTheme.typography.titleMedium)
                Text(categoryLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    service.price + if (service.discount > 0) "  •  ${service.discount.toInt()}% off" else "",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row {
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
            }
        }
    }
}

@Composable
private fun CategoryEditDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onSave: (label: String, desc: String) -> Unit
) {
    var label by remember { mutableStateOf(initial?.label ?: "") }
    var desc by remember { mutableStateOf(initial?.desc ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New category" else "Edit category") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(label, { label = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(label.trim(), desc.trim()) }, enabled = label.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceEditDialog(
    initial: Service?,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (title: String, categoryId: String, price: String, discount: Double, desc: String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var selectedCategoryId by remember { mutableStateOf(initial?.category ?: categories.firstOrNull()?.id ?: "") }
    var price by remember { mutableStateOf(initial?.price ?: "") }
    var discount by remember { mutableStateOf(if ((initial?.discount ?: 0.0) > 0) initial!!.discount.toInt().toString() else "") }
    var desc by remember { mutableStateOf(initial?.description ?: "") }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New service" else "Edit service") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(title, { title = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                ExposedDropdownMenuBox(expanded = categoryMenuExpanded, onExpandedChange = { categoryMenuExpanded = it }) {
                    OutlinedTextField(
                        value = categories.find { it.id == selectedCategoryId }?.label ?: "Select a category",
                        onValueChange = {}, readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = categoryMenuExpanded, onDismissRequest = { categoryMenuExpanded = false }) {
                        categories.forEach { cat ->
                            DropdownMenuItem(text = { Text(cat.label) }, onClick = { selectedCategoryId = cat.id; categoryMenuExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    price, { price = it }, label = { Text("Price (e.g. R450)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    discount, { discount = it.filter { c -> c.isDigit() } }, label = { Text("Discount % (optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(desc, { desc = it }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(title.trim(), selectedCategoryId, price.trim(), discount.toDoubleOrNull() ?: 0.0, desc.trim()) },
                enabled = title.isNotBlank() && selectedCategoryId.isNotBlank() && price.isNotBlank()
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun statusLabel(status: String) = when (status) {
    "pending" -> "Pending"
    "upcoming" -> "Upcoming"
    "completed" -> "Completed"
    "cancelled" -> "Cancelled"
    else -> status.replaceFirstChar { it.uppercase() }
}