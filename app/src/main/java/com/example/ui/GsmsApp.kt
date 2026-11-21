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
import com.example.model.GsmsTab
import com.example.model.PortalRole
import com.example.model.RolePolicy
import com.example.ui.theme.EmeraldTeal
import com.example.ui.theme.GsmsTheme
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
fun GsmsAppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow()
        .collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        GsmsTheme {
            AuthScreen(onAuthSuccess = {})
        }
    } else {
        key(user.uid) {
            GsmsTheme {
                GsmsAuthenticatedShell(
                    currentUserId = user.uid,
                    userDisplayName = user.displayName ?: user.email ?: "GSMS User",
                    userEmail = user.email
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GsmsAuthenticatedShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String?
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: GsmsViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val repo = GsmsRepository(app.applicationContext)
                val aiRepo = AiRepository()
                GsmsViewModel(repo, aiRepo, currentUserId, userDisplayName, userEmail)
            }
        }
    )

    val workspaceState by viewModel.uiState.collectAsStateWithLifecycle()
    val aiState by viewModel.aiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.statusBanner.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(GsmsTab.DASHBOARD) }
    var showSearchBar by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val snackbarHostState = remember { SnackbarHostState() }

    val successWorkspace = (workspaceState as? UiState.Success)?.data
    val activeRole = successWorkspace?.activeRole ?: PortalRole.SCHOOL_ADMIN
    val activeSchool = successWorkspace?.activeSchool
    val visibleTabs = remember(activeRole, activeSchool?.enabledModules) {
        RolePolicy.visibleTabs(activeRole, activeSchool)
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
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 320.dp)) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("GSMS Role Portals", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Each portal strictly enforces role permissions & data access",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                                    Text(
                                        text = "${activeRole.shortTitle} • ${activeSchool?.activeAcademicYear ?: "2026/2027"} ${activeSchool?.activeTerm ?: "Term 1"}",
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
                                        viewModel = viewModel,
                                        onSignOut = {
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
        PortalRole.TEACHER -> Icons.Rounded.MenuBook
        PortalRole.STUDENT -> Icons.Rounded.School
        PortalRole.PARENT -> Icons.Rounded.FamilyRestroom
        PortalRole.FINANCE_OFFICER -> Icons.Rounded.AccountBalanceWallet
        PortalRole.LIBRARIAN -> Icons.Rounded.LocalLibrary
        PortalRole.TRANSPORT_MANAGER -> Icons.Rounded.DirectionsBus
        PortalRole.HR_MANAGER -> Icons.Rounded.Badge
        PortalRole.WELFARE_NURSE -> Icons.Rounded.MedicalServices
        PortalRole.ADMISSIONS_RECEPTION -> Icons.Rounded.HowToReg
    }
}
