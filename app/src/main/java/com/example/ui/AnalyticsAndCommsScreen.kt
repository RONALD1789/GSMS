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
import com.example.model.GsmsModule
import com.example.ui.theme.*

@Composable
fun AnalyticsAndCommsScreen(
    workspace: GsmsWorkspaceState,
    searchQuery: String,
    viewModel: GsmsViewModel
) {
    val school = workspace.activeSchool ?: return
    val tabs = remember(school.enabledModules) {
        buildList {
            if (school.hasModule(GsmsModule.ANALYTICS)) add("Analytics & Alerts")
            if (school.hasModule(GsmsModule.COMMUNICATION)) add("Communications")
            add("Audit Logs")
        }
    }
    var selectedTab by remember(tabs) { mutableIntStateOf(0) }
    var showNewMessageDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab.coerceIn(0, tabs.lastIndex)) {
            tabs.forEachIndexed { idx, label ->
                Tab(selected = selectedTab == idx, onClick = { selectedTab = idx }, text = { Text(label) })
            }
        }

        when (tabs.getOrNull(selectedTab)) {
            "Analytics & Alerts" -> {
                val filteredAlerts = workspace.alerts.filter {
                    searchQuery.isBlank() || it.targetName.contains(searchQuery, true) || it.reason.contains(searchQuery, true)
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        BarChartCard(
                            title = "Class & Learner Mastery Analytics",
                            subtitle = "Live student average score breakdown",
                            bars = workspace.students.take(5).map {
                                it.fullName.substringBefore(" ") to it.averageScore.toFloat()
                            },
                            barColor = CobaltBlue
                        )
                    }
                    item {
                        TrendSparklineCard(
                            title = "School Attendance Stability",
                            headlineValue = "${if (workspace.students.isEmpty()) 94.0 else workspace.students.map { it.attendanceRate }.average().toInt()}%",
                            points = workspace.students.map { it.attendanceRate.toFloat() }.ifEmpty { listOf(91f, 93f, 95f, 94f) },
                            lineColor = EmeraldTeal
                        )
                    }
                    item {
                        SectionHeader(
                            title = "Early Warning AI Alerts",
                            subtitle = "${filteredAlerts.count { it.status != "RESOLVED" }} active risk alerts"
                        )
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
                                    if (alert.status != "RESOLVED") {
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
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Communication Hub", style = MaterialTheme.typography.titleLarge)
                                Text("Announcements, SMS, Email, Push & Parent-Teacher", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                    items(workspace.communications) { msg ->
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
                            subtitle = "Every approval, module toggle, and AI generation is permanently logged"
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
            onAiDraft = { channel, audience, topic, onResult ->
                viewModel.generateQuickAiDraft(
                    workspace,
                    "Draft a crisp, professional school $channel message for $audience about: $topic. Keep under 60 words.",
                    "AI_COMM_DRAFT",
                    onResult
                )
            },
            onDismiss = { showNewMessageDialog = false },
            onSend = { channel, audience, title, body, aiDrafted ->
                viewModel.sendCommunication(school, workspace.activeBranchId, channel, audience, title, body, workspace.activeRole, aiDrafted)
                showNewMessageDialog = false
            }
        )
    }
}

@Composable
private fun ComposeMessageDialog(
    onAiDraft: (String, String, String, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSend: (String, String, String, String, Boolean) -> Unit
) {
    var channel by remember { mutableStateOf("ANNOUNCEMENT") }
    var audience by remember { mutableStateOf("All Parents & Staff") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var aiDrafted by remember { mutableStateOf(false) }

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
                OutlinedTextField(value = audience, onValueChange = { audience = it }, label = { Text("Audience") }, singleLine = true)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Subject / Title") }, singleLine = true)
                OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Message Body") })
                OutlinedButton(onClick = {
                    onAiDraft(channel, audience, title.ifBlank { "Upcoming Term 1 Assessments & Fee Schedule" }) { draft ->
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
            Button(onClick = {
                if (title.isNotBlank() && body.isNotBlank()) onSend(channel, audience, title, body, aiDrafted)
            }) { Text("Send") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
