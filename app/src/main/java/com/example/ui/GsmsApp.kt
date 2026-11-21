package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.data.AiRepository
import com.example.data.GsmsRepository
import com.example.data.supabase.SupabaseSyncRepository
import com.example.model.GsmsTab
import com.example.model.PortalRole
import com.example.model.RolePolicy
import com.example.ui.theme.CobaltBlue
import com.example.ui.theme.EmeraldTeal
import com.example.ui.theme.GsmsTheme
import com.example.ui.theme.WarmAmber
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

@Composable
fun GsmsAppNavigation(
    auth: FirebaseAuth = Firebase.auth,
    supabaseRepository: SupabaseSyncRepository = SupabaseSyncRepository.shared
) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val supabaseSession by supabaseRepository.sessionFlow.collectAsStateWithLifecycle()
    val firebaseUser = currentUser
    val emailSession = supabaseSession

    when {
        firebaseUser != null -> {
            key("fb_${firebaseUser.uid}") {
                GsmsTheme {
                    GsmsAuthenticatedShell(
                        authBackend = AuthBackend.FIREBASE,
                        currentUserId = firebaseUser.uid,
                        userDisplayName = firebaseUser.displayName ?: firebaseUser.email ?: "GSMS User",
                        userEmail = firebaseUser.email,
                        supabaseRepository = supabaseRepository
                    )
                }
            }
        }
        emailSession != null -> {
            key("sb_${emailSession.userId}") {
                GsmsTheme {
                    GsmsAuthenticatedShell(
                        authBackend = AuthBackend.SUPABASE,
                        currentUserId = emailSession.userId,
                        userDisplayName = emailSession.displayName,
                        userEmail = emailSession.email,
                        supabaseRepository = supabaseRepository
                    )
                }
            }
        }
        else -> {
            GsmsTheme {
                AuthScreen(
                    supabaseRepository = supabaseRepository,
                    onAuthSuccess = {}
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GsmsAuthenticatedShell(
    authBackend: AuthBackend,
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?,
    supabaseRepository: SupabaseSyncRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: GsmsViewModel = viewModel(
        key = "${authBackend.name}_$currentUserId",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val repo = GsmsRepository(app.applicationContext)
                val aiRepo = AiRepository()
                GsmsViewModel(
                    repository = repo,
                    aiRepository = aiRepo,
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    supabaseRepository = supabaseRepository,
                    authBackend = authBackend
                )
            }
        }
    )

    val workspaceState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val supabaseState by viewModel.supabaseState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val focusStudentId by viewModel.focusStudentId.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBanner.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(GsmsTab.DASHBOARD) }
    var showSearchBar by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val successWorkspace = (workspaceState as? UiState.Success)?.data
    val activeRole = successWorkspace?.activeRole ?: PortalRole.SCHOOL_ADMIN
    val primaryRole = successWorkspace?.userProfile?.resolvedPrimaryRole() ?: PortalRole.SUPER_ADMIN
    val canSwitchPortal = primaryRole == PortalRole.SUPER_ADMIN
    val activeSchool = successWorkspace?.activeSchool
    val activeExtra = successWorkspace?.activeExtraCapabilities ?: emptySet()
    val visibleTabs = remember(activeRole, activeSchool?.enabledModules, activeExtra) {
        RolePolicy.visibleTabs(activeRole, activeSchool, activeExtra)
    }

    LaunchedEffect(visibleTabs) {
        if (selectedTab !in visibleTabs) {
            selectedTab = visibleTabs.firstOrNull() ?: GsmsTab.DASHBOARD
        }
    }

    LaunchedEffect(bannerMessage) {
        val msg = bannerMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearBanner()
        }
    }

    if (selectedTab != GsmsTab.DASHBOARD) {
        BackHandler { selectedTab = GsmsTab.DASHBOARD }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 336.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GSMS Role Portals (${PortalRole.entries.size})", style = MaterialTheme.typography.titleLarge)
                        StatusPill(
                            text = if (canSwitchPortal) "SWITCHABLE" else "LOCKED",
                            color = if (canSwitchPortal) EmeraldTeal else WarmAmber
                        )
                    }
                    Text(
                        if (canSwitchPortal) {
                            "Primary Role: ${primaryRole.shortTitle}. Switch portals to inspect RBAC isolation & delegated permissions."
                        } else {
                            "Locked to Primary Role: ${primaryRole.displayName}. Cross-portal switching is blocked."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeExtra.isNotEmpty()) {
                        StatusPill("+${activeExtra.size} Delegated Permissions Active", CobaltBlue)
                    }
                }
                HorizontalDivider()
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(PortalRole.entries) { role ->
                        val isSelected = role == activeRole
                        NavigationDrawerItem(
                            label = {
                                Column {
                                    Text(role.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        role.scopeDescription,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            selected = isSelected,
                            badge = {
                                if (isSelected) {
                                    StatusPill("ACTIVE", EmeraldTeal)
                                } else if (!canSwitchPortal) {
                                    Icon(
                                        Icons.Rounded.Lock,
                                        contentDescription = "Locked",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            onClick = {
                                viewModel.switchRole(role)
                                scope.launch { drawerState.close() }
                            },
                            icon = {
                                Icon(
                                    imageVector = roleIcon(role),
                                    contentDescription = role.displayName
                                )
                            }
                        )
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isWideScreen = maxWidth >= 680.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Column {
                        TopAppBar(
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("portal_drawer_button")
                                ) {
                                    Icon(Icons.Rounded.Menu, contentDescription = "Switch Role Portal")
                                }
                            },
                            title = {
                                Column {
                                    Text(
                                        text = activeSchool?.name ?: "GSMS Platform",
                                        style = MaterialTheme.typography.titleMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val extraTag = if (activeExtra.isNotEmpty()) " (+${activeExtra.size})" else ""
                                    Text(
                                        text = "${activeRole.shortTitle}$extraTag • ${activeSchool?.activeAcademicYear ?: "2026/2027"} ${activeSchool?.activeTerm ?: "Term 1"}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { showSearchBar = !showSearchBar }) {
                                    Icon(Icons.Rounded.Search, contentDescription = "Search GSMS")
                                }
                                if (GsmsTab.ANALYTICS in visibleTabs) {
                                    IconButton(onClick = { selectedTab = GsmsTab.ANALYTICS }) {
                                        Icon(Icons.Rounded.NotificationsActive, contentDescription = "Alerts & Notices")
                                    }
                                }
                            }
                        )
                        if (showSearchBar) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = viewModel::updateSearchQuery,
                                placeholder = { Text("Search within ${activeRole.shortTitle} portal...") },
                                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                                trailingIcon = {
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                                    .testTag("global_search_input")
                            )
                        }
                    }
                },
                bottomBar = {
                    if (!isWideScreen) {
                        NavigationBar(windowInsets = WindowInsets.navigationBars) {
                            visibleTabs.forEach { tab ->
                                val label = RolePolicy.tabDisplayLabel(activeRole, tab)
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = { Icon(tabIcon(tab), contentDescription = label) },
                                    label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (isWideScreen) {
                        NavigationRail {
                            visibleTabs.forEach { tab ->
                                val label = RolePolicy.tabDisplayLabel(activeRole, tab)
                                NavigationRailItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = { Icon(tabIcon(tab), contentDescription = label) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        when (val state = workspaceState) {
                            is UiState.Loading -> {
                                Column(
                                    modifier = Modifier.align(Alignment.Center),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator()
                                    Text("Syncing multi-tenant school workspace...")
                                }
                            }
                            is UiState.Error -> {
                                Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                                    EmptyStateCard(
                                        Icons.Rounded.CloudOff,
                                        "Workspace Sync Error",
                                        state.message
                                    )
                                }
                            }
                            is UiState.Success -> {
                                val workspace = state.data
                                val safeTab = if (selectedTab in visibleTabs) selectedTab else GsmsTab.DASHBOARD
                                when (safeTab) {
                                    GsmsTab.DASHBOARD -> PortalDashboardScreen(
                                        workspace = workspace,
                                        searchQuery = searchQuery,
                                        isWideScreen = isWideScreen,
                                        focusStudentId = focusStudentId,
                                        onSelectFocusStudent = { student -> viewModel.selectFocusStudent(student) },
                                        onRunAiCapability = { prompt -> viewModel.askAiCopilot(workspace, prompt) },
                                        onOpenAiTab = { selectedTab = GsmsTab.AI_GOVERNANCE },
                                        onNavigateToTab = { targetTab ->
                                            if (targetTab in visibleTabs) {
                                                selectedTab = targetTab
                                            } else {
                                                viewModel.showBanner("Access restricted for ${workspace.activeRole.shortTitle} role.")
                                            }
                                        },
                                        onSelectSchool = { school -> viewModel.switchSchool(school) },
                                        onBannerMessage = { msg -> viewModel.showBanner(msg) }
                                    )
                                    GsmsTab.ACADEMICS -> AcademicsAndReportsScreen(
                                        workspace = workspace,
                                        searchQuery = searchQuery,
                                        viewModel = viewModel,
                                        onBannerMessage = { msg -> viewModel.showBanner(msg) }
                                    )
                                    GsmsTab.PEOPLE -> PeopleAndLeaveScreen(
                                        workspace = workspace,
                                        searchQuery = searchQuery,
                                        viewModel = viewModel
                                    )
                                    GsmsTab.OPERATIONS -> OperationsAndFinanceScreen(
                                        workspace = workspace,
                                        searchQuery = searchQuery,
                                        viewModel = viewModel
                                    )
                                    GsmsTab.ANALYTICS -> AnalyticsAndCommsScreen(
                                        workspace = workspace,
                                        searchQuery = searchQuery,
                                        viewModel = viewModel
                                    )
                                    GsmsTab.AI_GOVERNANCE -> AiAndGovernanceScreen(
                                        workspace = workspace,
                                        aiState = aiState,
                                        supabaseState = supabaseState,
                                        viewModel = viewModel,
                                        onSignOut = {
                                            scope.launch { supabaseRepository.signOut() }
                                            performSignOut(credentialManager, scope)
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

private fun tabIcon(tab: GsmsTab): ImageVector {
    return when (tab) {
        GsmsTab.DASHBOARD -> Icons.Rounded.Dashboard
        GsmsTab.ACADEMICS -> Icons.Rounded.School
        GsmsTab.PEOPLE -> Icons.Rounded.Groups
        GsmsTab.OPERATIONS -> Icons.Rounded.AccountBalanceWallet
        GsmsTab.ANALYTICS -> Icons.Rounded.Insights
        GsmsTab.AI_GOVERNANCE -> Icons.Rounded.AutoAwesome
    }
}

private fun roleIcon(role: PortalRole): ImageVector {
    return when (role) {
        PortalRole.SUPER_ADMIN -> Icons.Rounded.AdminPanelSettings
        PortalRole.SCHOOL_ADMIN -> Icons.Rounded.Domain
        PortalRole.HEADTEACHER -> Icons.Rounded.WorkspacePremium
        PortalRole.DEPUTY_HEADTEACHER -> Icons.Rounded.SupervisorAccount
        PortalRole.TEACHER -> Icons.Rounded.MenuBook
        PortalRole.CLASS_TEACHER -> Icons.Rounded.CoPresent
        PortalRole.PARENT -> Icons.Rounded.FamilyRestroom
        PortalRole.STUDENT -> Icons.Rounded.School
        PortalRole.FINANCE_OFFICER -> Icons.Rounded.AccountBalanceWallet
        PortalRole.HR_MANAGER -> Icons.Rounded.Badge
        PortalRole.LIBRARIAN -> Icons.Rounded.LocalLibrary
        PortalRole.TRANSPORT_MANAGER -> Icons.Rounded.DirectionsBus
        PortalRole.WELFARE_NURSE -> Icons.Rounded.MedicalServices
        PortalRole.ADMISSIONS_RECEPTION -> Icons.Rounded.HowToReg
        PortalRole.PROCUREMENT_OFFICER -> Icons.Rounded.ShoppingCartCheckout
        PortalRole.STOREKEEPER -> Icons.Rounded.Warehouse
        PortalRole.EXAMINATION_OFFICER -> Icons.Rounded.FactCheck
        PortalRole.SCHOOL_COUNSELLOR -> Icons.Rounded.Psychology
        PortalRole.ICT_ADMIN -> Icons.Rounded.Security
    }
}
