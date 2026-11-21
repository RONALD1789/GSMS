package com.example.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.supabase.SupabaseSyncRepository
import com.example.data.supabase.SupabaseSyncState
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun AiAndGovernanceScreen(
    workspace: GsmsWorkspaceState,
    aiState: AiUiState,
    supabaseState: SupabaseSyncState,
    viewModel: GsmsViewModel,
    onSignOut: () -> Unit
) {
    val school = workspace.activeSchool
    val role = workspace.activeRole
    val extra = workspace.activeExtraCapabilities
    val canCreateSchool = role.has(RoleCapability.CREATE_SCHOOL_TENANT, extra)
    val canSwitchSchool = role.has(RoleCapability.SWITCH_SCHOOL_TENANT, extra)
    val canSwitchBranch = role.has(RoleCapability.SWITCH_BRANCH, extra)
    val canManageBranches = role.has(RoleCapability.MANAGE_BRANCHES, extra)
    val canManageModules = role.has(RoleCapability.MANAGE_MODULES, extra)
    val canUpdatePeriod = role.has(RoleCapability.UPDATE_ACADEMIC_PERIOD, extra)
    val canDelegatePermissions = role.has(RoleCapability.MANAGE_USER_PERMISSIONS, extra)
    val canSyncSupabase = SupabaseSyncRepository.canSyncSupabase(role, extra)
    val aiEnabled = school?.hasModule(GsmsModule.AI_TOOLS) != false

    val secondTabTitle = when {
        canDelegatePermissions || canManageModules || canSwitchSchool -> "Access & Modules"
        canSwitchBranch -> "Branch & Profile"
        else -> "Role & Profile"
    }

    var selectedTab by remember(role) { mutableIntStateOf(0) }
    var promptText by remember { mutableStateOf("") }
    var showCreateSchoolDialog by remember { mutableStateOf(false) }
    var showCreateBranchDialog by remember { mutableStateOf(false) }
    var delegationTarget by remember { mutableStateOf<UserAccessAssignment?>(null) }
    var showProvisionUserDialog by remember { mutableStateOf(false) }
    var showSupabaseAuthDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Gemini AI Copilot") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text(secondTabTitle) })
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
                        subtitle = "Active Scope: ${role.displayName}"
                    )
                }
                if (!aiEnabled) {
                    item {
                        EmptyStateCard(
                            Icons.Rounded.Lock,
                            "AI Tools Module Disabled",
                            "A School Admin can re-enable AI Tools in the Module Registry."
                        )
                    }
                } else {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(role.aiCapabilities) { cap ->
                                AssistChip(
                                    onClick = {
                                        val prompt = "Run '$cap' for ${role.displayName} based strictly on the scoped school context."
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
                            label = { Text("Ask GSMS Gemini AI (${role.shortTitle} scope)") },
                            placeholder = { Text("e.g. ${role.aiCapabilities.firstOrNull() ?: "Summarize scoped insights"}...") },
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
                                "Ready for ${role.shortTitle} Workflows",
                                "Select a quick AI preset above or type a prompt restricted to ${role.shortTitle} data."
                            )
                            is AiUiState.Loading -> ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.5.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text("Gemini Flash analyzing scoped tenant records...")
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
            }
        } else {
            val allowedTabLabels = RolePolicy.visibleTabs(role, school, extra).map { RolePolicy.tabDisplayLabel(role, it) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(role.displayName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                StatusPill("RBAC ACTIVE", EmeraldTeal)
                            }
                            Text(RolePolicy.accessSummary(role), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "Permitted Navigation: ${allowedTabLabels.joinToString(" • ")}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (extra.isNotEmpty()) {
                                Text(
                                    "Delegated Extra Capabilities (${extra.size}): ${extra.joinToString { it.label }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldTeal
                                )
                            }
                        }
                    }
                }

                if (canDelegatePermissions && school != null) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("User Access & Delegated Permissions", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Assign extra capabilities or class/student scopes without changing primary role",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            FilledTonalButton(
                                onClick = { showProvisionUserDialog = true },
                                modifier = Modifier.minimumInteractiveComponentSize().testTag("provision_user_button")
                            ) {
                                Icon(Icons.Rounded.PersonAdd, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add User")
                            }
                        }
                    }
                    items(workspace.accessAssignments) { assignment ->
                        val assignedRole = assignment.resolvedRole()
                        ElevatedCard(shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(assignment.fullName, style = MaterialTheme.typography.titleMedium)
                                        Text(assignment.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    StatusPill(assignedRole.shortTitle, CobaltBlue)
                                }
                                val scopeParts = buildList {
                                    if (assignment.assignedClasses.isNotEmpty()) add("Classes: ${assignment.assignedClasses.joinToString()}")
                                    if (assignment.assignedSubjects.isNotEmpty()) add("Subjects: ${assignment.assignedSubjects.joinToString()}")
                                    if (assignment.linkedStudentIds.isNotEmpty()) add("Linked Students: ${assignment.linkedStudentIds.joinToString()}")
                                }
                                if (scopeParts.isNotEmpty()) {
                                    Text(scopeParts.joinToString(" • "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    StatusPill(
                                        text = if (assignment.extraPermissions.isEmpty()) "Base Role Only" else "+${assignment.extraPermissions.size} Delegated",
                                        color = if (assignment.extraPermissions.isEmpty()) InkSecondary else EmeraldTeal
                                    )
                                    FilledTonalButton(
                                        onClick = { delegationTarget = assignment },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Delegate Permissions")
                                    }
                                }
                            }
                        }
                    }
                }

                if (canSwitchSchool) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("School Tenants", style = MaterialTheme.typography.titleLarge)
                            if (canCreateSchool) {
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
                }

                if ((canSwitchBranch || canManageBranches) && school != null) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Campuses & Branches", style = MaterialTheme.typography.titleLarge)
                                Text("Switch branch context within ${school.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (canManageBranches) {
                                TextButton(
                                    onClick = { showCreateBranchDialog = true },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) { Text("+ Add Branch") }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(workspace.branches) { br ->
                                FilterChip(
                                    selected = br.id == workspace.activeBranchId,
                                    onClick = { if (canSwitchBranch) viewModel.switchBranch(br) },
                                    label = { Text(br.name) }
                                )
                            }
                        }
                    }
                }

                if (canUpdatePeriod && school != null) {
                    item {
                        SectionHeader(title = "Active Academic Term", subtitle = "${school.activeAcademicYear} • ${school.activeTerm}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Term 1", "Term 2", "Term 3").forEach { termOption ->
                                FilterChip(
                                    selected = school.activeTerm == termOption,
                                    onClick = { viewModel.updateAcademicPeriod(school.id, school.activeAcademicYear, termOption) },
                                    label = { Text(termOption) }
                                )
                            }
                        }
                    }
                }

                if (canManageModules && school != null) {
                    item {
                        SectionHeader(
                            title = "Module Registry (${school.enabledModules.size}/${GsmsModule.entries.size})",
                            subtitle = "Toggle modules on/off; disabled modules hide immediately across UI & API"
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

                if (canSyncSupabase) {
                    item {
                        SupabaseCloudCard(
                            supabaseState = supabaseState,
                            schoolName = school?.name ?: "GSMS Tenant",
                            onOpenAuth = { showSupabaseAuthDialog = true },
                            onSignOutSupabase = { viewModel.signOutSupabase() },
                            onTestConnection = { viewModel.testSupabaseConnection() },
                            onPushWorkspace = { viewModel.pushWorkspaceToSupabase() },
                            onRefreshCounts = { viewModel.refreshSupabaseCounts() },
                            onToggleAutoMirror = { enabled -> viewModel.toggleSupabaseAutoMirror(enabled) }
                        )
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

    if (showCreateSchoolDialog && canCreateSchool) {
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

    if (showCreateBranchDialog && canManageBranches && school != null) {
        var name by remember { mutableStateOf("") }
        var code by remember { mutableStateOf("BR-03") }
        var city by remember { mutableStateOf(school.city) }
        AlertDialog(
            onDismissRequest = { showCreateBranchDialog = false },
            title = { Text("Add Campus Branch") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Branch Name") }, singleLine = true)
                    OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Branch Code") }, singleLine = true)
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (name.trim().length >= 2 && code.trim().length >= 2) {
                        viewModel.createBranch(school.id, name, code, city)
                        showCreateBranchDialog = false
                    }
                }) { Text("Create Branch") }
            },
            dismissButton = { TextButton(onClick = { showCreateBranchDialog = false }) { Text("Cancel") } }
        )
    }

    delegationTarget?.let { target ->
        if (school != null) {
            DelegatePermissionsDialog(
                assignment = target,
                onDismiss = { delegationTarget = null },
                onSave = { extraPerms, classes, subjects, studentIds ->
                    viewModel.delegateUserPermissions(
                        schoolId = school.id,
                        assignment = target,
                        extraPermissions = extraPerms,
                        assignedClasses = classes,
                        assignedSubjects = subjects,
                        linkedStudentIds = studentIds
                    )
                    delegationTarget = null
                }
            )
        }
    }

    if (showProvisionUserDialog && school != null) {
        ProvisionUserAccessDialog(
            onDismiss = { showProvisionUserDialog = false },
            onSave = { fullName, email, primaryRole, classes, subjects, studentIds, extraPerms ->
                viewModel.createAccessAssignment(
                    schoolId = school.id,
                    fullName = fullName,
                    email = email,
                    primaryRole = primaryRole,
                    assignedClasses = classes,
                    assignedSubjects = subjects,
                    linkedStudentIds = studentIds,
                    extraPermissions = extraPerms
                )
                showProvisionUserDialog = false
            }
        )
    }

    if (showSupabaseAuthDialog && canSyncSupabase) {
        SupabaseAuthDialog(
            initialEmail = workspace.userProfile?.email.orEmpty(),
            onDismiss = { showSupabaseAuthDialog = false },
            onSignIn = { email, password ->
                viewModel.signInSupabase(email, password)
                showSupabaseAuthDialog = false
            },
            onSignUp = { email, password ->
                viewModel.signUpSupabase(email, password)
                showSupabaseAuthDialog = false
            }
        )
    }
}

@Composable
private fun SupabaseCloudCard(
    supabaseState: SupabaseSyncState,
    schoolName: String,
    onOpenAuth: () -> Unit,
    onSignOutSupabase: () -> Unit,
    onTestConnection: () -> Unit,
    onPushWorkspace: () -> Unit,
    onRefreshCounts: () -> Unit,
    onToggleAutoMirror: (Boolean) -> Unit
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("supabase_cloud_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Rounded.Storage,
                        contentDescription = null,
                        tint = if (supabaseState.isConfigured) EmeraldTeal else WarmAmber
                    )
                    Column {
                        Text("Supabase PostgreSQL Cloud", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Endpoint: ${supabaseState.endpointLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                StatusPill(
                    text = when {
                        !supabaseState.isConfigured -> "SECRETS NEEDED"
                        supabaseState.isAuthenticated -> "AUTH ACTIVE"
                        else -> "READY"
                    },
                    color = when {
                        !supabaseState.isConfigured -> WarmAmber
                        supabaseState.isAuthenticated -> EmeraldTeal
                        else -> CobaltBlue
                    }
                )
            }

            if (!supabaseState.isConfigured) {
                Surface(
                    color = WarmAmber.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Add SUPABASE_URL & SUPABASE_ANON_KEY in AI Studio Secrets",
                            style = MaterialTheme.typography.labelLarge,
                            color = WarmAmber
                        )
                        Text(
                            "1. Open AI Studio Secrets panel and set SUPABASE_URL and SUPABASE_ANON_KEY.\n2. Run supabase/schema.sql in your Supabase SQL Editor to create all 15 RLS-protected tables.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val latency = supabaseState.lastLatencyMs?.let { " • ${it}ms" } ?: ""
                    Text(
                        "Status: ${supabaseState.connectionStatus}$latency",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (supabaseState.isAuthenticated) {
                        TextButton(
                            onClick = onSignOutSupabase,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Text("Sign Out (${supabaseState.authEmail ?: "Session"})")
                        }
                    } else {
                        FilledTonalButton(
                            onClick = onOpenAuth,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("supabase_auth_button")
                        ) {
                            Icon(Icons.Rounded.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Supabase Sign In")
                        }
                    }
                }

                if (supabaseState.isBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                supabaseState.lastSyncSummary?.let { summary ->
                    Text(
                        summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = EmeraldTeal
                    )
                }

                supabaseState.lastError?.let { err ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestConnection,
                        enabled = !supabaseState.isBusy,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("supabase_test_button")
                    ) {
                        Text("Test Ping")
                    }
                    Button(
                        onClick = onPushWorkspace,
                        enabled = !supabaseState.isBusy && supabaseState.isAuthenticated,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("supabase_push_button")
                    ) {
                        Text("Push $schoolName")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Auto-Mirror Tenant Writes", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "Mirrors mutations to Supabase Postgres when authenticated",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = supabaseState.autoMirrorEnabled,
                        onCheckedChange = onToggleAutoMirror,
                        enabled = supabaseState.isAuthenticated && !supabaseState.isBusy
                    )
                }

                if (supabaseState.isAuthenticated) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Live PostgreSQL Tables", style = MaterialTheme.typography.labelLarge)
                        TextButton(
                            onClick = onRefreshCounts,
                            enabled = !supabaseState.isBusy,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh Supabase table counts", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Refresh Counts")
                        }
                    }

                    if (supabaseState.remoteCounts.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(supabaseState.remoteCounts.entries.toList()) { (table, count) ->
                                Surface(
                                    color = CobaltBlue.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text(
                                        "$table: $count",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CobaltBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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

@Composable
private fun SupabaseAuthDialog(
    initialEmail: String,
    onDismiss: () -> Unit,
    onSignIn: (String, String) -> Unit,
    onSignUp: (String, String) -> Unit
) {
    var email by remember { mutableStateOf(initialEmail) }
    var password by remember { mutableStateOf("") }
    var isCreateAccount by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isCreateAccount) "Create Supabase Sync User" else "Supabase GoTrue Sign In")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Authenticates via Supabase Auth so PostgreSQL Row-Level Security permits tenant read/write.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password (min 6 chars)") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { isCreateAccount = !isCreateAccount }) {
                    Text(
                        if (isCreateAccount) "Already have a Supabase account? Sign In"
                        else "Need a Supabase user? Sign Up"
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (isCreateAccount) onSignUp(email, password) else onSignIn(email, password)
                },
                enabled = email.contains("@") && password.length >= 6
            ) {
                Text(if (isCreateAccount) "Sign Up" else "Sign In")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun DelegatePermissionsDialog(
    assignment: UserAccessAssignment,
    onDismiss: () -> Unit,
    onSave: (List<String>, List<String>, List<String>, List<String>) -> Unit
) {
    val primary = assignment.resolvedRole()
    val baseCaps = remember(primary) { RolePolicy.baseCapabilities(primary) }
    val selectedExtra = remember(assignment) {
        mutableStateListOf<String>().apply { addAll(assignment.extraPermissions) }
    }
    var classesCsv by remember(assignment) { mutableStateOf(assignment.assignedClasses.joinToString(", ")) }
    var subjectsCsv by remember(assignment) { mutableStateOf(assignment.assignedSubjects.joinToString(", ")) }
    var studentIdsCsv by remember(assignment) { mutableStateOf(assignment.linkedStudentIds.joinToString(", ")) }

    val delegatableCaps = remember(primary) {
        RoleCapability.entries.filter {
            it !in baseCaps &&
                it != RoleCapability.CREATE_SCHOOL_TENANT &&
                it != RoleCapability.SWITCH_SCHOOL_TENANT
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delegate Permissions: ${assignment.fullName}") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(color = CobaltBlue.copy(alpha = 0.14f), shape = RoundedCornerShape(10.dp)) {
                    Text(
                        "Primary Role (Locked): ${primary.displayName}",
                        style = MaterialTheme.typography.labelLarge,
                        color = CobaltBlue,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                OutlinedTextField(
                    value = classesCsv,
                    onValueChange = { classesCsv = it },
                    label = { Text("Assigned Classes (comma separated)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = subjectsCsv,
                    onValueChange = { subjectsCsv = it },
                    label = { Text("Assigned Subjects (comma separated)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = studentIdsCsv,
                    onValueChange = { studentIdsCsv = it },
                    label = { Text("Linked Student IDs (comma separated)") },
                    singleLine = true
                )
                Text(
                    "Additional Capabilities (${selectedExtra.size} selected):",
                    style = MaterialTheme.typography.labelLarge
                )
                delegatableCaps.forEach { cap ->
                    val checked = cap.name in selectedExtra
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = checked,
                            onCheckedChange = { isChecked ->
                                if (isChecked) {
                                    if (cap.name !in selectedExtra) selectedExtra.add(cap.name)
                                } else {
                                    selectedExtra.remove(cap.name)
                                }
                            }
                        )
                        Column(Modifier.weight(1f)) {
                            Text(cap.label, style = MaterialTheme.typography.bodySmall)
                            Text(cap.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(
                    selectedExtra.toList(),
                    classesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    subjectsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                    studentIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                )
            }) { Text("Save Grants") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun ProvisionUserAccessDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, PortalRole, List<String>, List<String>, List<String>, List<String>) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    val schoolRoles = remember { PortalRole.entries.filter { it != PortalRole.SUPER_ADMIN } }
    var selectedRole by remember { mutableStateOf(PortalRole.TEACHER) }
    var classesCsv by remember { mutableStateOf("Grade 10A") }
    var subjectsCsv by remember { mutableStateOf("Mathematics") }
    var studentIdsCsv by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Provision School Portal Access") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                Text("Select Primary Role:", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(schoolRoles) { r ->
                        FilterChip(
                            selected = r == selectedRole,
                            onClick = { selectedRole = r },
                            label = { Text(r.shortTitle) }
                        )
                    }
                }
                OutlinedTextField(value = classesCsv, onValueChange = { classesCsv = it }, label = { Text("Assigned Classes") }, singleLine = true)
                OutlinedTextField(value = subjectsCsv, onValueChange = { subjectsCsv = it }, label = { Text("Assigned Subjects") }, singleLine = true)
                OutlinedTextField(value = studentIdsCsv, onValueChange = { studentIdsCsv = it }, label = { Text("Linked Student IDs") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(onClick = {
                if (fullName.trim().length >= 2 && email.trim().length >= 3) {
                    onSave(
                        fullName,
                        email,
                        selectedRole,
                        classesCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        subjectsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        studentIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        emptyList()
                    )
                }
            }) { Text("Provision") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
