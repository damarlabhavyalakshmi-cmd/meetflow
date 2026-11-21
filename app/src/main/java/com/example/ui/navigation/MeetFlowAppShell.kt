package com.example.ui.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.ApprovalStatus
import com.example.data.ConflictStatus
import com.example.ui.MeetFlowUiState
import com.example.ui.MeetFlowViewModel
import com.example.ui.components.GlobalSearchSheet
import com.example.ui.components.RemindersBottomSheet
import com.example.ui.screens.*
import kotlinx.coroutines.launch

data class SidebarDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val ALL_SIDEBAR_DESTINATIONS = listOf(
    SidebarDestination("dashboard", "Dashboard", Icons.Default.Dashboard),
    SidebarDestination("meetings", "Meetings", Icons.Default.VideoCall),
    SidebarDestination("upload", "Upload Meeting", Icons.Default.UploadFile),
    SidebarDestination("decisions", "Decisions", Icons.Default.Gavel),
    SidebarDestination("tasks", "Action Items", Icons.Default.Checklist),
    SidebarDestination("conflicts", "Conflict Detection", Icons.Default.WarningAmber),
    SidebarDestination("memory", "Meeting Memory", Icons.Default.Psychology),
    SidebarDestination("approvals", "Approval Center", Icons.Default.VerifiedUser),
    SidebarDestination("digest", "Weekly Digest", Icons.Default.Summarize),
    SidebarDestination("integrations", "Integrations", Icons.Default.Extension),
    SidebarDestination("settings", "Settings", Icons.Default.Settings)
)

private val PRIMARY_BAR_ROUTES = setOf("dashboard", "meetings", "upload", "tasks", "conflicts")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetFlowAppShell(
    uiState: MeetFlowUiState,
    viewModel: MeetFlowViewModel
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSearchSheet by remember { mutableStateOf(false) }
    var showRemindersSheet by remember { mutableStateOf(false) }

    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route ?: "dashboard"

    val unreadReminders = remember(uiState.reminders) { uiState.reminders.count { !it.isRead } }
    val unresolvedConflicts = remember(uiState.conflicts) { uiState.conflicts.count { it.status != ConflictStatus.RESOLVED.name } }
    val pendingApprovals = remember(uiState.approvals) { uiState.approvals.count { it.status == ApprovalStatus.PENDING.name } }

    LaunchedEffect(uiState.bannerMessage) {
        val msg = uiState.bannerMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearBanner()
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val isDetailRoute = currentRoute.startsWith("meeting_detail/")
    val activeTitle = ALL_SIDEBAR_DESTINATIONS.firstOrNull { it.route == currentRoute }?.label
        ?: if (isDetailRoute) "Meeting Intelligence" else "MeetFlow AI"

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.widthIn(max = 300.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            "MeetFlow AI",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            uiState.activeWorkspace?.name ?: "Workspace",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    ALL_SIDEBAR_DESTINATIONS.forEach { dest ->
                        val badgeCount = when (dest.route) {
                            "conflicts" -> unresolvedConflicts
                            "approvals" -> pendingApprovals
                            else -> 0
                        }
                        NavigationDrawerItem(
                            label = { Text(dest.label) },
                            selected = currentRoute == dest.route,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navigateTo(dest.route)
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            badge = if (badgeCount > 0) {
                                { Badge { Text(badgeCount.toString()) } }
                            } else null,
                            modifier = Modifier
                                .padding(NavigationDrawerItemDefaults.ItemPadding)
                                .testTag("drawer_${dest.route}")
                        )
                    }
                }
            }
        }
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    if (!isDetailRoute) {
                        TopAppBar(
                            title = {
                                Column {
                                    Text(activeTitle, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        uiState.activeWorkspace?.name ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = { scope.launch { drawerState.open() } },
                                    modifier = Modifier.testTag("open_drawer_button")
                                ) {
                                    Icon(Icons.Default.Menu, contentDescription = "Open Navigation Menu")
                                }
                            },
                            actions = {
                                IconButton(
                                    onClick = { showSearchSheet = true },
                                    modifier = Modifier.testTag("topbar_search_button")
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = "Search Workspace")
                                }
                                IconButton(
                                    onClick = { showRemindersSheet = true },
                                    modifier = Modifier.testTag("topbar_notifications_button")
                                ) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadReminders > 0) {
                                                Badge { Text(unreadReminders.toString()) }
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Default.Notifications, contentDescription = "Notifications")
                                    }
                                }
                                IconButton(
                                    onClick = { navigateTo("settings") },
                                    modifier = Modifier.testTag("topbar_profile_button")
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = uiState.currentUser?.avatarInitials ?: "MF",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        )
                    }
                },
                bottomBar = {
                    if (!useRail && !isDetailRoute) {
                        NavigationBar {
                            ALL_SIDEBAR_DESTINATIONS.filter { it.route in PRIMARY_BAR_ROUTES }.forEach { dest ->
                                NavigationBarItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateTo(dest.route) },
                                    icon = {
                                        BadgedBox(
                                            badge = {
                                                if (dest.route == "conflicts" && unresolvedConflicts > 0) {
                                                    Badge { Text(unresolvedConflicts.toString()) }
                                                }
                                            }
                                        ) {
                                            Icon(dest.icon, contentDescription = dest.label)
                                        }
                                    },
                                    label = {
                                        Text(
                                            when (dest.route) {
                                                "upload" -> "Upload"
                                                "tasks" -> "Tasks"
                                                "conflicts" -> "Conflicts"
                                                else -> dest.label
                                            },
                                            maxLines = 1
                                        )
                                    }
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
                    if (useRail && !isDetailRoute) {
                        NavigationRail {
                            ALL_SIDEBAR_DESTINATIONS.filter { it.route in PRIMARY_BAR_ROUTES }.forEach { dest ->
                                NavigationRailItem(
                                    selected = currentRoute == dest.route,
                                    onClick = { navigateTo(dest.route) },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label, maxLines = 1) }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        NavHost(
                            navController = navController,
                            startDestination = "dashboard"
                        ) {
                            composable("dashboard") {
                                DashboardScreen(
                                    uiState = uiState,
                                    aiNoticeMessage = viewModel.aiNoticeMessage,
                                    onNavigateUpload = { navigateTo("upload") },
                                    onNavigateMeetings = { navigateTo("meetings") },
                                    onOpenMeeting = { id -> navController.navigate("meeting_detail/$id") },
                                    onNavigateTasks = { navigateTo("tasks") },
                                    onNavigateConflicts = { navigateTo("conflicts") },
                                    onNavigateApprovals = { navigateTo("approvals") },
                                    onNavigateDecisions = { navigateTo("decisions") }
                                )
                            }
                            composable("meetings") {
                                MeetingsListScreen(
                                    uiState = uiState,
                                    onOpenMeeting = { id -> navController.navigate("meeting_detail/$id") },
                                    onNavigateUpload = { navigateTo("upload") }
                                )
                            }
                            composable("upload") {
                                UploadMeetingScreen(
                                    uiState = uiState,
                                    aiNoticeMessage = viewModel.aiNoticeMessage,
                                    onProcessMeeting = { title, date, project, desc, parts, srcType, fileName, transcript, onDone ->
                                        viewModel.uploadAndAnalyzeMeeting(
                                            title, date, project, desc, parts, srcType, fileName, transcript, onDone
                                        )
                                    },
                                    onClearSteps = { viewModel.clearPipelineSteps() },
                                    onOpenMeeting = { id -> navController.navigate("meeting_detail/$id") }
                                )
                            }
                            composable(
                                route = "meeting_detail/{meetingId}",
                                arguments = listOf(navArgument("meetingId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val mtgId = backStackEntry.arguments?.getString("meetingId").orEmpty()
                                MeetingDetailScreen(
                                    meetingId = mtgId,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onNavigateConflicts = { navigateTo("conflicts") },
                                    onNavigateTasks = { navigateTo("tasks") }
                                )
                            }
                            composable("decisions") {
                                DecisionsScreen(
                                    uiState = uiState,
                                    onOpenMeeting = { id -> navController.navigate("meeting_detail/$id") },
                                    onUpdateDecision = { dec, reason -> viewModel.updateDecision(dec, reason) }
                                )
                            }
                            composable("tasks") {
                                TasksScreen(
                                    uiState = uiState,
                                    onSaveTask = { existingId, mtgId, proj, t, d, a, dl, p, dep, st ->
                                        viewModel.saveTask(existingId, mtgId, proj, t, d, a, dl, p, dep, st)
                                    },
                                    onUpdateTaskStatus = { task, status -> viewModel.updateTaskStatus(task, status) },
                                    onDeleteTask = { id -> viewModel.deleteTask(id) }
                                )
                            }
                            composable("conflicts") {
                                ConflictDetectionScreen(
                                    uiState = uiState,
                                    aiNoticeMessage = viewModel.aiNoticeMessage,
                                    onResolveConflict = { id, action -> viewModel.resolveConflict(id, action) }
                                )
                            }
                            composable("memory") {
                                MeetingMemoryScreen(
                                    uiState = uiState,
                                    onOpenMeeting = { id -> navController.navigate("meeting_detail/$id") }
                                )
                            }
                            composable("approvals") {
                                ApprovalCenterScreen(
                                    uiState = uiState,
                                    onDecideApproval = { id, approve, t, d -> viewModel.decideApproval(id, approve, t, d) }
                                )
                            }
                            composable("digest") {
                                WeeklyDigestScreen(
                                    uiState = uiState,
                                    aiNoticeMessage = viewModel.aiNoticeMessage,
                                    onGenerateDigest = { member -> viewModel.generateWeeklyDigest(member) }
                                )
                            }
                            composable("integrations") {
                                IntegrationsScreen(
                                    uiState = uiState,
                                    onSaveConfig = { item, detail -> viewModel.saveIntegrationConfig(item, detail) },
                                    onDisconnect = { item -> viewModel.disconnectIntegration(item) }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    uiState = uiState,
                                    onSwitchRole = { viewModel.switchRole(it) },
                                    onSwitchWorkspace = { viewModel.switchWorkspace(it) },
                                    onCreateWorkspace = { name, desc -> viewModel.createWorkspace(name, desc) },
                                    onSetDarkMode = { viewModel.setDarkMode(it) },
                                    onResetDemoData = { viewModel.resetDemoData() },
                                    onRunReminderCheck = { viewModel.runReminderAgentCheck() },
                                    onSignOut = { viewModel.signOut() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRemindersSheet) {
        RemindersBottomSheet(
            reminders = uiState.reminders,
            onDismiss = { showRemindersSheet = false },
            onRunCheck = { viewModel.runReminderAgentCheck() },
            onMarkAllRead = { viewModel.markRemindersRead() }
        )
    }

    if (showSearchSheet) {
        GlobalSearchSheet(
            uiState = uiState,
            onDismiss = { showSearchSheet = false },
            onOpenMeeting = { id -> navController.navigate("meeting_detail/$id") },
            onNavigateRoute = { route -> navigateTo(route) }
        )
    }
}
