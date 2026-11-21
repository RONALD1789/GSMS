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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.GsmsModule
import com.example.model.PortalRole
import com.example.ui.theme.*

@Composable
fun AiAndGovernanceScreen(
    workspace: GsmsWorkspaceState,
    aiState: AiUiState,
    viewModel: GsmsViewModel,
    onSignOut: () -> Unit
) {
    val school = workspace.activeSchool
    var selectedTab by remember { mutableIntStateOf(0) }
    var promptText by remember { mutableStateOf("") }
    var showCreateSchoolDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Gemini AI Copilot") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Tenants & Modules") })
        }

        if (selectedTab == 0) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SectionHeader(
                        title = "Role-Scoped AI Assistant",
                        subtitle = "Active Scope: ${workspace.activeRole.displayName}"
                    )
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(workspace.activeRole.aiCapabilities) { cap ->
                            AssistChip(
                                onClick = {
                                    val prompt = "Run '$cap' for ${workspace.activeRole.displayName} based strictly on the school context."
                                    promptText = prompt
                                    viewModel.askAiCopilot(workspace, prompt)
                                },
                                label = { Text(cap) },
                                leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = promptText,
                        onValueChange = { promptText = it },
                        label = { Text("Ask GSMS Gemini AI (${workspace.activeRole.shortTitle} scope)") },
                        placeholder = { Text("e.g. Summarize risks, generate lesson plan, forecast fees...") },
                        modifier = Modifier.fillMaxWidth().testTag("ai_prompt_input"),
                        minLines = 3
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            if (promptText.isNotBlank()) viewModel.askAiCopilot(workspace, promptText)
                        },
                        enabled = aiState !is AiUiState.Loading && promptText.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("ask_ai_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Bolt, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Run Context-Aware AI")
                    }
                }
                item {
                    when (aiState) {
                        is AiUiState.Idle -> EmptyStateCard(
                            Icons.Rounded.Psychology,
                            "Ready for ${workspace.activeRole.shortTitle} Workflows",
                            "Select a quick AI preset above or type any academic, operational, or analytics prompt."
                        )
                        is AiUiState.Loading -> ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                Spacer(Modifier.width(12.dp))
                                Text("Gemini 3.8 Flash analyzing scoped tenant records...")
                            }
                        }
                        is AiUiState.Streaming -> ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusPill("STREAMING LIVE", EmeraldTeal)
                                Text(aiState.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        is AiUiState.Success -> ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                StatusPill("GEMINI AI RESPONSE", CobaltBlue)
                                Text(aiState.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        is AiUiState.Error -> Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.width(10.dp))
                                Text(aiState.message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("School Tenants", style = MaterialTheme.typography.titleLarge)
                        FilledTonalButton(
                            onClick = { showCreateSchoolDialog = true },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.AddBusiness, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("New School")
                        }
                    }
                }
                items(workspace.schools) { tenant ->
                    val isSelected = tenant.id == school?.id
                    ElevatedCard(
                        onClick = { viewModel.switchSchool(tenant) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(tenant.name, style = MaterialTheme.typography.titleMedium)
                                Text("${tenant.code} • ${tenant.city}, ${tenant.country} • ${tenant.activeAcademicYear} (${tenant.activeTerm})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            StatusPill(if (isSelected) "ACTIVE" else tenant.plan, if (isSelected) EmeraldTeal else CobaltBlue)
                        }
                    }
                }

                if (workspace.branches.isNotEmpty()) {
                    item {
                        SectionHeader(title = "Campuses & Branches", subtitle = "Switch branch context within ${school?.name ?: ""}")
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(workspace.branches) { br ->
                                FilterChip(
                                    selected = br.id == workspace.activeBranchId,
                                    onClick = { viewModel.switchBranch(br) },
                                    label = { Text(br.name) }
                                )
                            }
                        }
                    }
                }

                if (school != null) {
                    item {
                        SectionHeader(
                            title = "Module Registry (${school.enabledModules.size}/${GsmsModule.entries.size})",
                            subtitle = "Toggle modules on/off; disabled modules hide immediately across UI"
                        )
                    }
                    items(GsmsModule.entries) { mod ->
                        val enabled = school.hasModule(mod)
                        OutlinedCard(shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(mod.label, style = MaterialTheme.typography.titleSmall)
                                    Text(mod.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Switch(
                                    checked = enabled,
                                    onCheckedChange = { viewModel.toggleModule(school, mod, workspace.activeRole) }
                                )
                            }
                        }
                    }
                }

                item {
                    val lowData = workspace.userProfile?.lowDataMode == true
                    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("Low-Data & Offline Sync Mode", style = MaterialTheme.typography.titleSmall)
                                Text("Reduces hero artwork & caches Firestore writes offline", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(checked = lowData, onCheckedChange = { viewModel.toggleLowDataMode(it) })
                        }
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CrimsonRed)
                    ) {
                        Icon(Icons.Rounded.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out (${workspace.userProfile?.email ?: "Google Account"})")
                    }
                }
            }
        }
    }

    if (showCreateSchoolDialog) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("") }
        var city by remember { mutableStateOf("") }
        var country by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateSchoolDialog = false },
            title = { Text("Provision New School Tenant") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("School Name") }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Code (e.g. GVA-01)") }, singleLine = true)
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true)
                    OutlinedTextField(value = country, onValueChange = { country = it }, label = { Text("Country") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.trim().length >= 2 && code.trim().length >= 2) {
                        viewModel.createSchool(name, code, city, country)
                        showCreateSchoolDialog = false
                    }
                }) { Text("Provision Tenant") }
            },
            dismissButton = { TextButton(onClick = { showCreateSchoolDialog = false }) { Text("Cancel") } }
        )
    }
}
