package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun OperationsAndFinanceScreen(
    workspace: GsmsWorkspaceState,
    searchQuery: String,
    viewModel: GsmsViewModel
) {
    val school = workspace.activeSchool ?: return
    val role = workspace.activeRole
    val focusStudentId by viewModel.focusStudentId.collectAsStateWithLifecycle()
    val scopedToLearner = RolePolicy.requiresLearnerScope(role)
    val focusStudent = if (scopedToLearner) {
        RolePolicy.resolveFocusStudent(workspace.students, focusStudentId)
    } else {
        null
    }
    val enabledSections = remember(role, school.enabledModules, workspace.activeExtraCapabilities) {
        RolePolicy.operationSections(role, school, workspace.activeExtraCapabilities)
    }
    var activeSection by remember(enabledSections) { mutableStateOf(enabledSections.firstOrNull() ?: "") }
    var paymentTarget by remember { mutableStateOf<FinanceEntry?>(null) }
    var showAddOpDialog by remember { mutableStateOf(false) }
    var showCreateInvoiceDialog by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<OperationRecord?>(null) }

    if (enabledSections.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
                Icons.Rounded.Lock,
                "Operations Restricted or Disabled",
                "${role.shortTitle} has no enabled operational modules in this workspace."
            )
        }
        return
    }

    if (scopedToLearner && focusStudent == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LearnerScopePicker(
                students = workspace.students,
                focusStudentId = focusStudentId,
                onSelect = viewModel::selectFocusStudent
            )
            EmptyStateCard(
                Icons.Rounded.Person,
                "Select a learner",
                "${role.shortTitle} finance records are scoped to a single learner. Choose one to see their fee invoices."
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (scopedToLearner) {
            LearnerScopePicker(
                students = workspace.students,
                focusStudentId = focusStudentId,
                onSelect = viewModel::selectFocusStudent,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
        }
        if (enabledSections.size > 1) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(enabledSections) { sec ->
                    FilterChip(
                        selected = activeSection == sec,
                        onClick = { activeSection = sec },
                        label = { Text(sec) }
                    )
                }
            }
        }

        if (activeSection == "Finance") {
            val canRecordPayment = role.has(RoleCapability.RECORD_FINANCE_PAYMENT)
            val canCreateInvoice = role.has(RoleCapability.CREATE_FINANCE_INVOICE)
            val roleScopedFinance = RolePolicy.filterFinanceForRole(role, workspace.finance, focusStudent)
            val financeItems = roleScopedFinance.filter {
                searchQuery.isBlank() || it.title.contains(searchQuery, true) || it.partyName.contains(searchQuery, true)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    val summaryEntries = if (role == PortalRole.HR_MANAGER) {
                        roleScopedFinance
                    } else {
                        roleScopedFinance.filter { it.entryType == "INVOICE" }
                    }
                    val totalBilled = summaryEntries.sumOf { it.amountUsd }
                    val totalPaid = summaryEntries.sumOf { it.paidUsd }
                    val headerTitle = when (role) {
                        PortalRole.PARENT -> "Child Fee Invoices (Read-Only)"
                        PortalRole.HR_MANAGER -> "Staff Payroll Ledger (Read-Only)"
                        else -> "Fees Collection Progress"
                    }
                    ElevatedCard(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(headerTitle, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "USD ${totalPaid.toInt()} settled of USD ${totalBilled.toInt()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                StatusPill("${if (totalBilled > 0) ((totalPaid / totalBilled) * 100).toInt() else 100}%", EmeraldTeal)
                                if (canCreateInvoice) {
                                    FilledTonalButton(onClick = { showCreateInvoiceDialog = true }) {
                                        Icon(Icons.Rounded.Add, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("New Invoice")
                                    }
                                }
                            }
                        }
                    }
                }
                if (financeItems.isEmpty()) {
                    item {
                        EmptyStateCard(Icons.Rounded.ReceiptLong, "No Finance Records", "No ledger entries are visible for ${role.shortTitle}.")
                    }
                }
                items(financeItems) { fin ->
                    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${fin.referenceNo} • ${fin.title}", style = MaterialTheme.typography.titleSmall)
                                    Text("${fin.entryType} • ${fin.partyName} • ${fin.category}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                StatusPill(fin.status, if (fin.status == "PAID") EmeraldTeal else if (fin.status == "OVERDUE") CrimsonRed else WarmAmber)
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Paid USD ${fin.paidUsd.toInt()} / USD ${fin.amountUsd.toInt()}", style = MaterialTheme.typography.labelMedium)
                                if (canRecordPayment && fin.entryType == "INVOICE" && fin.paidUsd < fin.amountUsd) {
                                    FilledTonalButton(
                                        onClick = { paymentTarget = fin },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) { Text("Record Payment") }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            val canEditOps = RolePolicy.canManageOperationSection(role, activeSection, workspace.activeExtraCapabilities)
            val targetTypes = when (activeSection) {
                "Library" -> setOf("LIBRARY_BOOK", "LIBRARY_BORROW")
                "Transport" -> setOf("VEHICLE", "ROUTE", "TRIP")
                "Welfare" -> setOf("WELFARE")
                "Discipline", "Counselling" -> setOf("DISCIPLINE", "COUNSELLING", "INTERVENTION", "SAFEGUARDING")
                "Assets", "Stores" -> setOf("INVENTORY", "ASSET", "STOCK", "STOCK_ISSUE", "STOCK_RECEIPT")
                "Hostel" -> setOf("HOSTEL")
                "Admissions", "Front Desk" -> setOf("ADMISSION", "FRONT_DESK")
                "Procurement" -> setOf("PROCUREMENT", "REQUISITION", "PURCHASE_ORDER", "VENDOR", "RFQ")
                "Examinations" -> setOf("EXAM", "EXAM_TIMETABLE", "INVIGILATION", "MARK_MODERATION")
                "ICT", "ICT Systems" -> setOf("ICT_TICKET", "ICT_ASSET", "SYSTEM_INCIDENT", "ACCESS_REQUEST")
                else -> setOf("EVENT")
            }
            val ops = workspace.operations.filter {
                it.moduleType in targetTypes && (searchQuery.isBlank() || it.title.contains(searchQuery, true))
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$activeSection Module", style = MaterialTheme.typography.titleLarge)
                            if (!canEditOps) {
                                Text("Read-only oversight for ${role.shortTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (canEditOps) {
                            FilledTonalButton(
                                onClick = { showAddOpDialog = true },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add Record")
                            }
                        }
                    }
                }
                if (ops.isEmpty()) {
                    item {
                        EmptyStateCard(
                            Icons.Rounded.Widgets,
                            "No $activeSection Entries Yet",
                            if (canEditOps) "Tap Add Record to log a new item in $activeSection." else "No records available in $activeSection."
                        )
                    }
                } else {
                    items(ops) { op ->
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(op.title, style = MaterialTheme.typography.titleMedium)
                                        if (!op.subtitle.isNullOrBlank()) {
                                            Text(op.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    StatusPill(op.status, CobaltBlue)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(op.partyName ?: "", style = MaterialTheme.typography.labelMedium)
                                    Text(op.metricLabel ?: "", style = MaterialTheme.typography.labelMedium, color = EmeraldTeal)
                                }
                                if (!op.notes.isNullOrBlank()) {
                                    Text(op.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (canEditOps) {
                                    val nextStatus = when (op.status.uppercase()) {
                                        "ACTIVE", "OPEN", "PENDING", "SUBMITTED" -> "IN_PROGRESS"
                                        "IN_PROGRESS", "UNDER_REVIEW", "APPROVED" -> "COMPLETED"
                                        "COMPLETED", "CLOSED", "REJECTED" -> "ACTIVE"
                                        else -> "COMPLETED"
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { editTarget = op }) { Text("Edit") }
                                        OutlinedButton(
                                            onClick = { viewModel.updateOperationStatus(school.id, op, nextStatus, role) },
                                            modifier = Modifier.minimumInteractiveComponentSize()
                                        ) {
                                            Text(
                                                when (nextStatus) {
                                                    "IN_PROGRESS" -> "Start / Review"
                                                    "COMPLETED" -> "Mark Complete"
                                                    else -> "Reopen"
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    paymentTarget?.let { inv ->
        var payAmount by remember { mutableStateOf((inv.amountUsd - inv.paidUsd).toInt().toString()) }
        AlertDialog(
            onDismissRequest = { paymentTarget = null },
            title = { Text("Record Fee Payment") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${inv.referenceNo} • ${inv.partyName}")
                    OutlinedTextField(value = payAmount, onValueChange = { payAmount = it }, label = { Text("Amount (USD)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val amt = payAmount.toDoubleOrNull() ?: 0.0
                    if (amt > 0) viewModel.recordFinancePayment(school.id, inv, amt)
                    paymentTarget = null
                }) { Text("Post Payment") }
            },
            dismissButton = { TextButton(onClick = { paymentTarget = null }) { Text("Cancel") } }
        )
    }


    if (showCreateInvoiceDialog) {
        FinanceInvoiceDialog(
            students = workspace.students,
            onDismiss = { showCreateInvoiceDialog = false },
            onCreate = { student, title, category, amount ->
                viewModel.createFinanceInvoice(school, workspace.activeBranchId, student, title, category, amount, role)
                showCreateInvoiceDialog = false
            }
        )
    }

    editTarget?.let { target ->
        var title by remember(target.id) { mutableStateOf(target.title) }
        var subtitle by remember(target.id) { mutableStateOf(target.subtitle.orEmpty()) }
        var party by remember(target.id) { mutableStateOf(target.partyName.orEmpty()) }
        var metric by remember(target.id) { mutableStateOf(target.metricLabel.orEmpty()) }
        var notes by remember(target.id) { mutableStateOf(target.notes.orEmpty()) }
        AlertDialog(
            onDismissRequest = { editTarget = null },
            title = { Text("Edit ${target.moduleType.replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Record Title") }, singleLine = true)
                    OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Location / Class / Description") }, singleLine = true)
                    OutlinedTextField(value = party, onValueChange = { party = it }, label = { Text("Assigned To / Supplier / Person") }, singleLine = true)
                    OutlinedTextField(value = metric, onValueChange = { metric = it }, label = { Text("Metric / Reference") }, singleLine = true)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes") }, minLines = 2, maxLines = 4)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.updateOperationRecord(school.id, target, title, subtitle, party, metric, notes, role)
                            editTarget = null
                        }
                    }
                ) { Text("Save Changes") }
            },
            dismissButton = { TextButton(onClick = { editTarget = null }) { Text("Cancel") } }
        )
    }

    if (showAddOpDialog) {
        val defaultType = when (activeSection) {
            "Library" -> "LIBRARY_BOOK"
            "Transport" -> "VEHICLE"
            "Welfare" -> "WELFARE"
            "Discipline" -> "DISCIPLINE"
            "Assets" -> "ASSET"
            "Hostel" -> "HOSTEL"
            "Admissions" -> "ADMISSION"
            "Front Desk" -> "FRONT_DESK"
            "Procurement" -> "REQUISITION"
            "Stores" -> "STOCK"
            "Examinations" -> "EXAM"
            "Counselling" -> "COUNSELLING"
            "ICT", "ICT Systems" -> "ICT_TICKET"
            else -> "EVENT"
        }
        val fieldLabels = when (activeSection) {
            "Procurement" -> listOf("Requisition / Purchase Order", "Item / Delivery Location", "Supplier / Requester", "PO Number / Budget", "Scope / Justification")
            "Stores", "Assets" -> listOf("Stock Item / Asset", "Store / Location", "Supplier / Custodian", "Quantity / Unit / Reorder Level", "Condition / Stock Notes")
            "Examinations" -> listOf("Exam / Assessment", "Class / Subject", "Exam Officer / Department", "Exam Date / Paper Reference", "Instructions / Moderation Notes")
            "Counselling", "Discipline" -> listOf("Case / Intervention", "Concern / Referral Source", "Learner / Assigned Counsellor", "Priority / Follow-up Date", "Confidential Case Notes")
            "ICT", "ICT Systems" -> listOf("Support Ticket / Incident", "System / Device", "Requester / Assigned Technician", "Priority / Ticket Reference", "Issue Details / Resolution Notes")
            "Front Desk" -> listOf("Visitor / Inquiry", "Visit Purpose", "Visitor / Contact", "Visit Date / Reference", "Reception Notes")
            "Welfare" -> listOf("Clinic / Welfare Case", "Concern / Location", "Learner / Nurse", "Priority / Follow-up Date", "Care Notes")
            "Hostel" -> listOf("Boarding Record", "House / Room", "Learner / Warden", "Room / Bed", "Boarding Notes")
            "Transport" -> listOf("Vehicle / Route / Trip", "Route / Destination", "Driver / Assigned Staff", "Route / Mileage", "Trip / Maintenance Notes")
            "Library" -> listOf("Book / Borrowing Record", "Title / Shelf", "Borrower / Class", "Accession / Due Date", "Library Notes")
            "Admissions" -> listOf("Applicant / Admission", "Requested Class / Intake", "Parent / Guardian", "Application / Reference", "Admission Notes")
            else -> listOf("Record Title", "Description / Location", "Assigned To / Person", "Metric / Reference", "Description / Notes")
        }
        var title by remember { mutableStateOf("") }
        var subtitle by remember { mutableStateOf("") }
        var party by remember { mutableStateOf("") }
        var metric by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var recordStatus by remember { mutableStateOf("PENDING") }
        AlertDialog(
            onDismissRequest = { showAddOpDialog = false },
            title = { Text("Add $activeSection Record") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text(fieldLabels[0]) }, singleLine = true)
                    OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text(fieldLabels[1]) }, singleLine = true)
                    OutlinedTextField(value = party, onValueChange = { party = it }, label = { Text(fieldLabels[2]) }, singleLine = true)
                    OutlinedTextField(value = metric, onValueChange = { metric = it }, label = { Text(fieldLabels[3]) }, singleLine = true)
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text(fieldLabels[4]) }, minLines = 2, maxLines = 4)
                    Text("Workflow status", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PENDING", "IN_PROGRESS", "COMPLETED").forEach { status ->
                            FilterChip(selected = recordStatus == status, onClick = { recordStatus = status }, label = { Text(status.replace('_', ' ')) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (title.isNotBlank()) {
                        viewModel.addOperationRecord(
                            school, workspace.activeBranchId, defaultType, title, subtitle, party, recordStatus, metric, notes, workspace.activeRole
                        )
                        showAddOpDialog = false
                    }
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showAddOpDialog = false }) { Text("Cancel") } }
        )
    }
}
