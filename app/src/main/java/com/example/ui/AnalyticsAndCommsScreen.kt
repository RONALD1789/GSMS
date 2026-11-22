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
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun AnalyticsAndCommsScreen(
    workspace: GsmsWorkspaceState,
    searchQuery: String,
    viewModel: GsmsViewModel
) {
    val school = workspace.activeSchool ?: return
    val role = workspace.activeRole
    val extra = workspace.activeExtraCapabilities
    val tabs = remember(role, school.enabledModules, extra) {
        RolePolicy.analyticsSubTabs(role, school, extra)
    }
    var selectedTab by remember(tabs) { mutableIntStateOf(0) }
    var showNewMessageDialog by remember { mutableStateOf(false) }

    if (tabs.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            EmptyStateCard(
                Icons.Rounded.Lock,
                "Analytics & Notices Disabled",
                "${role.shortTitle} has no enabled analytics or notice modules."
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
            "Analytics & Alerts", "Role Alerts" -> {
                val showCharts = role.has(RoleCapability.VIEW_ANALYTICS_CHARTS, extra)
                val canManageAlerts = role.has(RoleCapability.MANAGE_ALERTS, extra)
                val scopedStudents = RolePolicy.filterStudentsForRole(
                    role, workspace.students, workspace.assignedClasses, workspace.linkedStudentIds
                )
                val roleAlerts = RolePolicy.filterAlertsForRole(role, workspace.alerts)
                val filteredAlerts = roleAlerts.filter {
                    searchQuery.isBlank() || it.targetName.contains(searchQuery, true) || it.reason.contains(searchQuery, true)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (showCharts) {
                        item {
                            BarChartCard(
                                title = "Class & Learner Mastery Analytics",
                                subtitle = "Live student average score breakdown",
                                bars = scopedStudents.take(5).map {
                                    it.fullName.substringBefore(" ") to it.averageScore.toFloat()
                                },
                                barColor = CobaltBlue
                            )
                        }
                        item {
                            TrendSparklineCard(
                                title = "Attendance Stability",
                                headlineValue = "${if (scopedStudents.isEmpty()) 94 else scopedStudents.map { it.attendanceRate }.average().toInt()}%",
                                points = scopedStudents.map { it.attendanceRate.toFloat() }.ifEmpty { listOf(91f, 93f, 95f, 94f) },
                                lineColor = EmeraldTeal
                            )
                        }
                    }
                    item {
                        SectionHeader(
                            title = if (showCharts) "Early Warning AI Alerts" else "${role.shortTitle} AI Alerts",
                            subtitle = "${filteredAlerts.count { it.status != "RESOLVED" }} active risk alerts in ${role.shortTitle} scope"
                        )
                    }
                    if (filteredAlerts.isEmpty()) {
                        item {
                            EmptyStateCard(
                                Icons.Rounded.VerifiedUser,
                                "No Active Alerts in ${role.shortTitle} Scope",
                                "Early-warning risks assigned to your department will appear here."
                            )
                        }
                    }
                    items(filteredAlerts) { alert ->
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("${alert.category} • ${alert.targetName}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    StatusPill(alert.riskLevel, riskColor(alert.riskLevel))
                                }
                                Text(alert.reason, style = MaterialTheme.typography.bodySmall)
                                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                                    Text(
                                        "Recommended (${alert.assignedRole}): ${alert.recommendedAction}",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(alert.status, if (alert.status == "RESOLVED") EmeraldTeal else WarmAmber)
                                    if (canManageAlerts && alert.status != "RESOLVED") {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (alert.status == "OPEN") {
                                                OutlinedButton(
                                                    onClick = { viewModel.updateAlertStatus(school.id, alert, "IN_PROGRESS", workspace.activeRole) },
                                                    modifier = Modifier.minimumInteractiveComponentSize()
                                                ) { Text("Assign") }
                                            }
                                            FilledTonalButton(
                                                onClick = { viewModel.updateAlertStatus(school.id, alert, "RESOLVED", workspace.activeRole) },
                                                modifier = Modifier.minimumInteractiveComponentSize()
                                            ) { Text("Resolve") }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "Communications" -> {
                val canCompose = role.has(RoleCapability.COMPOSE_COMMUNICATIONS, extra)
                val visibleComms = RolePolicy.filterCommunicationsForRole(role, workspace.communications).filter {
                    searchQuery.isBlank() || it.title.contains(searchQuery, true) || it.body.contains(searchQuery, true)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (canCompose) "Communication Hub" else "School Notices & Updates",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    if (canCompose) {
                                        "Announcements, SMS, Email, Push & Parent-Teacher"
                                    } else {
                                        "Official school announcements for ${role.shortTitle}"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (canCompose) {
                                Button(
                                    onClick = { showNewMessageDialog = true },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Send, contentDescription = null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Compose")
                                }
                            }
                        }
                    }
                    items(visibleComms) { msg ->
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(msg.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                    StatusPill(msg.channel, CobaltBlue)
                                }
                                Text(msg.body, style = MaterialTheme.typography.bodySmall)
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("To: ${msg.audience} • By: ${msg.senderName}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    if (msg.aiDrafted) StatusPill("AI Drafted", VioletAi)
                                }
                            }
                        }
                    }
                }
            }

            "Audit Logs" -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        SectionHeader(
                            title = "Immutable Security & AI Audit Trail",
                            subtitle = "Every approval, permission delegation, and AI generation is permanently logged"
                        )
                    }
                    items(workspace.auditLogs) { log ->
                        OutlinedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(log.actionType, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                    StatusPill(if (log.aiAction) "AI LOG" else log.moduleName, if (log.aiAction) VioletAi else EmeraldTeal)
                                }
                                Text(log.summary, style = MaterialTheme.typography.bodySmall)
                                Text("Actor: ${log.actorName} (${log.actorRole})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewMessageDialog) {
        ComposeMessageDialog(
            role = workspace.activeRole,
            assignedClasses = workspace.assignedClasses,
            onAiDraft = { channel, audience, topic, onResult ->
                viewModel.generateQuickAiDraft(
                    workspace,
                    "Draft a crisp, professional school $channel message for $audience about: $topic. Keep under 60 words.",
                    "AI_COMM_DRAFT",
                    onResult
                )
            },
            onDismiss = { showNewMessageDialog = false },
            onSend = { channel, audience, title, body, aiDrafted, targetClass ->
                viewModel.sendCommunication(school, workspace.activeBranchId, channel, audience, title, body, workspace.activeRole, aiDrafted, targetClass)
                showNewMessageDialog = false
            }
        )
    }
}

@Composable
private fun ComposeMessageDialog(
    role: PortalRole,
    assignedClasses: List<String>,
    onAiDraft: (String, String, String, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSend: (String, String, String, String, Boolean, String?) -> Unit
) {
    var channel by remember { mutableStateOf("ANNOUNCEMENT") }
    var audience by remember { mutableStateOf("All Parents & Staff") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var aiDrafted by remember { mutableStateOf(false) }
    var targetClass by remember(assignedClasses) { mutableStateOf(assignedClasses.firstOrNull()) }
    var classMenuExpanded by remember { mutableStateOf(false) }
    val teacherTargeting = role in setOf(PortalRole.TEACHER, PortalRole.CLASS_TEACHER)
    val effectiveAudience = if (teacherTargeting) "Parents of Class" else audience

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dispatch School Message") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("ANNOUNCEMENT", "SMS", "EMAIL").forEach { ch ->
                        FilterChip(selected = channel == ch, onClick = { channel = ch }, label = { Text(ch) })
                    }
                }
                if (teacherTargeting) {
                    Text("Send only to parents of your assigned class.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box {
                        OutlinedButton(onClick = { classMenuExpanded = true }, modifier = Modifier.fillMaxWidth(), enabled = assignedClasses.isNotEmpty()) {
                            Text(targetClass ?: "No assigned classes")
                        }
                        DropdownMenu(expanded = classMenuExpanded, onDismissRequest = { classMenuExpanded = false }) {
                            assignedClasses.forEach { className ->
                                DropdownMenuItem(
                                    text = { Text(className) },
                                    onClick = { targetClass = className; classMenuExpanded = false }
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(value = audience, onValueChange = { audience = it }, label = { Text("Audience") }, singleLine = true)
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Subject / Title") }, singleLine = true)
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message Body") })
                OutlinedButton(onClick = {
                    onAiDraft(channel, if (teacherTargeting) "$effectiveAudience • $targetClass" else effectiveAudience, title.ifBlank { "Upcoming Term 1 Assessments & Fee Schedule" }) { draft ->
                        body = draft
                        if (title.isBlank()) title = "Important School Update"
                        aiDrafted = true
                    }
                }) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("AI Draft Notice")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && body.isNotBlank() && (!teacherTargeting || targetClass?.let { it in assignedClasses } == true),
                onClick = {
                    if (title.isNotBlank() && body.isNotBlank()) {
                        onSend(channel, effectiveAudience, title, body, aiDrafted, targetClass.takeIf { teacherTargeting })
                    }
                }
            ) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
