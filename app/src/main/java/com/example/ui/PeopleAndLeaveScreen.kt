package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun PeopleAndLeaveScreen(
    workspace: GsmsWorkspaceState,
    searchQuery: String,
    viewModel: GsmsViewModel
) {
    val school = workspace.activeSchool ?: return
    val role = workspace.activeRole
    val tabs = remember(role, school.enabledModules, workspace.activeExtraCapabilities) {
        RolePolicy.peopleSubTabs(role, school, workspace.activeExtraCapabilities)
    }
    var selectedTab by remember(tabs) { mutableIntStateOf(0) }
    var showEnrollDialog by remember { mutableStateOf(false) }
    var showApplyLeaveDialog by remember { mutableStateOf(false) }

    if (tabs.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
                Icons.Rounded.Lock,
                "People & HR Restricted",
                "${role.shortTitle} does not have directory or HR access."
            )
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab.coerceIn(0, tabs.lastIndex)) {
            tabs.forEachIndexed { idx, label ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(label) })
            }
        }

        when (tabs.getOrNull(selectedTab)) {
            "Students" -> {
                val canEnroll = role.has(RoleCapability.ENROLL_STUDENT) && school.hasModule(GsmsModule.ADMISSIONS)
                val showFeeBalances = RolePolicy.canViewLearnerFeeBalances(role)
                val filtered = workspace.students.filter {
                    searchQuery.isBlank() || it.fullName.contains(searchQuery, true) || it.className.contains(searchQuery, true)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Students Directory", style = MaterialTheme.typography.titleLarge)
                                Text("${filtered.size} active records", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (canEnroll) {
                                Button(
                                    onClick = { showEnrollDialog = true },
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("enroll_student_button")
                                ) {
                                    Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Enroll")
                                }
                            }
                        }
                    }
                    items(filtered) { st ->
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(st.fullName, style = MaterialTheme.typography.titleMedium)
                                        Text("${st.admissionNo} • ${st.className} (${st.stream ?: "General"})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(st.riskLevel, riskColor(st.riskLevel))
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Avg: ${st.averageScore}%", style = MaterialTheme.typography.labelMedium, color = CobaltBlue)
                                    Text("Att: ${st.attendanceRate}%", style = MaterialTheme.typography.labelMedium, color = EmeraldTeal)
                                    if (showFeeBalances) {
                                        Text("Fee Bal: $${st.feeBalanceUsd.toInt()}", style = MaterialTheme.typography.labelMedium, color = if (st.feeBalanceUsd > 0) WarmAmber else EmeraldTeal)
                                    } else {
                                        Text("Welfare: ${st.welfareStatus}", style = MaterialTheme.typography.labelMedium, color = CobaltBlue)
                                    }
                                }
                                Text("Parent: ${st.parentName} • Welfare: ${st.welfareStatus}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            "Staff & Faculty" -> {
                val showLeaveBalances = RolePolicy.canViewStaffLeaveBalances(role)
                val filteredStaff = workspace.staff.filter {
                    searchQuery.isBlank() || it.fullName.contains(searchQuery, true) || it.department.contains(searchQuery, true)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Workforce & Leave Status",
                            subtitle = "${filteredStaff.count { it.onLeave }} staff currently marked On Leave"
                        )
                    }
                    items(filteredStaff) { member ->
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(member.fullName, style = MaterialTheme.typography.titleMedium)
                                        Text("${member.roleTitle} • ${member.department}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(
                                        text = if (member.onLeave) "ON LEAVE" else "ACTIVE",
                                        color = if (member.onLeave) CrimsonRed else EmeraldTeal
                                    )
                                }
                                Text(
                                    "Subjects: ${member.subjectsSummary ?: "-"} • Workload: ${member.workloadHours}h/wk",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                if (showLeaveBalances) {
                                    Text(
                                        "Leave Balance: ${member.leaveBalanceDays} days left • Used: ${member.leaveUsedDays} days",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "Leave Workflow", "My Leave" -> {
                val canApplyLeave = role.has(RoleCapability.APPLY_LEAVE)
                val canApproveLeave = role.has(RoleCapability.APPROVE_LEAVE)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Leave Requests & Approvals", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    if (canApproveLeave) {
                                        "Approving auto-marks staff On Leave & deducts balance"
                                    } else {
                                        "Submit leave requests for HR / Principal approval"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canApplyLeave) {
                                Button(
                                    onClick = { showApplyLeaveDialog = true },
                                    modifier = Modifier.minimumInteractiveComponentSize().testTag("apply_leave_button")
                                ) {
                                    Icon(Icons.Rounded.EventAvailable, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Apply")
                                }
                            }
                        }
                    }
                    items(workspace.leaveRequests) { lv ->
                        val staffMatch = workspace.staff.firstOrNull { it.id == lv.staffId }
                        val statusColor = when (lv.status) {
                            LeaveStatus.APPROVED.name -> EmeraldTeal
                            LeaveStatus.REJECTED.name -> CrimsonRed
                            else -> WarmAmber
                        }
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${lv.staffName} • ${lv.leaveType}", style = MaterialTheme.typography.titleMedium)
                                        Text("${lv.roleTitle} • ${lv.startDate} to ${lv.endDate} (${lv.daysCount} days)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(lv.status, statusColor)
                                }
                                Text("Reason: ${lv.reason}", style = MaterialTheme.typography.bodySmall)
                                if (!lv.aiConflictNote.isNullOrBlank()) {
                                    Surface(color = VioletSoft.copy(alpha = 0.45f), shape = RoundedCornerShape(10.dp)) {
                                        Text("AI Schedule Impact: ${lv.aiConflictNote}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
                                    }
                                }
                                if (lv.status == LeaveStatus.PENDING.name && canApproveLeave) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = {
                                                viewModel.reviewLeaveRequest(school.id, lv, staffMatch, true, workspace.activeRole)
                                            },
                                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                                        ) { Text("Approve & Mark Leave") }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.reviewLeaveRequest(school.id, lv, staffMatch, false, workspace.activeRole)
                                            },
                                            modifier = Modifier.weight(1f).minimumInteractiveComponentSize()
                                        ) { Text("Reject") }
                                    }
                                } else if (!lv.reviewedBy.isNullOrBlank()) {
                                    Text("Reviewed by: ${lv.reviewedBy}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEnrollDialog) {
        EnrollStudentDialog(
            onDismiss = { showEnrollDialog = false },
            onConfirm = { admNo, name, cls, parent ->
                viewModel.addStudent(school, workspace.activeBranchId, admNo, name, cls, parent)
                showEnrollDialog = false
            }
        )
    }

    if (showApplyLeaveDialog && workspace.staff.isNotEmpty()) {
        ApplyLeaveDialog(
            staffList = workspace.staff,
            onGenerateAiConflict = { staffMember, start, end, onResult ->
                viewModel.generateQuickAiDraft(
                    workspace,
                    "Analyze timetable conflict & substitute cover if ${staffMember.fullName} (${staffMember.subjectsSummary ?: staffMember.roleTitle}) takes leave from $start to $end. Suggest 1 qualified substitute from current staff.",
                    "AI_LEAVE_CONFLICT",
                    onResult
                )
            },
            onDismiss = { showApplyLeaveDialog = false },
            onSubmit = { staffMember, type, start, end, days, reason, aiNote ->
                viewModel.submitLeaveRequest(school, workspace.activeBranchId, staffMember, type, start, end, days, reason, aiNote)
                showApplyLeaveDialog = false
            }
        )
    }
}

@Composable
private fun EnrollStudentDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String, String) -> Unit
) {
    var admNo by remember { mutableStateOf("ADM-2026-109") }
    var fullName by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("Grade 10A") }
    var parentName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enroll New Student") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = admNo, onValueChange = { admNo = it }, label = { Text("Admission No") }, singleLine = true)
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, singleLine = true)
                OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Class") }, singleLine = true)
                OutlinedTextField(value = parentName, onValueChange = { parentName = it }, label = { Text("Parent / Guardian Name") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = { if (fullName.trim().length >= 2) onConfirm(admNo, fullName, className, parentName) }) {
                Text("Enroll")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ApplyLeaveDialog(
    staffList: List<StaffMember>,
    onGenerateAiConflict: (StaffMember, String, String, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSubmit: (StaffMember, String, String, String, Long, String, String?) -> Unit
) {
    var selectedStaff by remember { mutableStateOf(staffList.first()) }
    var leaveType by remember { mutableStateOf("ANNUAL") }
    var startDate by remember { mutableStateOf("2026-12-08") }
    var endDate by remember { mutableStateOf("2026-12-10") }
    var daysText by remember { mutableStateOf("3") }
    var reason by remember { mutableStateOf("") }
    var aiConflictNote by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apply for Staff Leave") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    staffList.take(3).forEach { st ->
                        FilterChip(
                            selected = st.id == selectedStaff.id,
                            onClick = { selectedStaff = st },
                            label = { Text(st.fullName.substringBefore(" ")) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ANNUAL", "SICK", "STUDY").forEach { t ->
                        FilterChip(selected = leaveType == t, onClick = { leaveType = t }, label = { Text(t) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = startDate, onValueChange = { startDate = it }, label = { Text("Start") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = endDate, onValueChange = { endDate = it }, label = { Text("End") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = daysText, onValueChange = { daysText = it }, label = { Text("Days Count") }, singleLine = true)
                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason") })
                if (aiConflictNote.isNotBlank()) {
                    Text("AI Conflict Check: $aiConflictNote", style = MaterialTheme.typography.bodySmall, color = VioletAi)
                }
                OutlinedButton(onClick = {
                    onGenerateAiConflict(selectedStaff, startDate, endDate) { note -> aiConflictNote = note }
                }) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Conflict & Cover Check")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (reason.trim().length >= 2) {
                    onSubmit(selectedStaff, leaveType, startDate, endDate, (daysText.toLongOrNull() ?: 1L).coerceAtLeast(1L), reason, aiConflictNote.ifBlank { null })
                }
            }) { Text("Submit Leave") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
