package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.notifications.ReminderNotificationHelper
import com.example.ui.MeetFlowUiState
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun TasksScreen(
    uiState: MeetFlowUiState,
    onSaveTask: (
        existingId: String?,
        meetingId: String,
        project: String,
        title: String,
        description: String,
        assignee: String,
        deadline: String,
        priority: String,
        dependencies: String,
        status: String
    ) -> Unit,
    onUpdateTaskStatus: (TaskEntity, TaskStatus) -> Unit,
    onDeleteTask: (String) -> Unit
) {
    var isKanban by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var statusFilter by remember { mutableStateOf("ALL") }
    var priorityFilter by remember { mutableStateOf("ALL") }
    var showTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }

    val filtered = remember(uiState.tasks, search, statusFilter, priorityFilter) {
        uiState.tasks.filter { t ->
            val mSearch = search.isBlank() ||
                t.title.contains(search, ignoreCase = true) ||
                t.assignee.contains(search, ignoreCase = true) ||
                t.project.contains(search, ignoreCase = true)
            val mStatus = statusFilter == "ALL" || t.status == statusFilter
            val mPrio = priorityFilter == "ALL" || t.priority == priorityFilter
            mSearch && mStatus && mPrio
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Action Items", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${uiState.tasks.size} tracked tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(
                        onClick = { isKanban = !isKanban },
                        modifier = Modifier.testTag("toggle_kanban_button")
                    ) {
                        Icon(
                            imageVector = if (isKanban) Icons.Default.ViewList else Icons.Default.ViewKanban,
                            contentDescription = "Toggle Kanban or List View"
                        )
                    }
                    Button(
                        onClick = { editingTask = null; showTaskDialog = true },
                        modifier = Modifier.testTag("create_task_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Task")
                    }
                }
            }

            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search tasks, assignee, project...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "TODO", "IN_PROGRESS", "COMPLETED", "OVERDUE").forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = st },
                        label = { Text(st.replace("_", " ")) }
                    )
                }
                listOf("HIGH", "MEDIUM", "LOW").forEach { pr ->
                    FilterChip(
                        selected = priorityFilter == pr,
                        onClick = { priorityFilter = if (priorityFilter == pr) "ALL" else pr },
                        label = { Text("Priority: $pr") }
                    )
                }
            }
        }

        if (isKanban) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TaskStatus.entries.forEach { columnStatus ->
                    val columnTasks = filtered.filter { it.status == columnStatus.name }
                    MeetFlowCard(modifier = Modifier.width(270.dp).fillMaxHeight()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(columnStatus.label, style = MaterialTheme.typography.titleMedium)
                            StatusPill("${columnTasks.size}", RoyalBlue100, Navy900)
                        }
                        Spacer(Modifier.height(10.dp))
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(columnTasks, key = { it.id }) { task ->
                                TaskCardItem(
                                    task = task,
                                    onEdit = { editingTask = task; showTaskDialog = true },
                                    onStatusChange = { onUpdateTaskStatus(task, it) },
                                    onDelete = { onDeleteTask(task.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                if (filtered.isEmpty()) {
                    item {
                        EmptyStateCard(
                            icon = Icons.Default.Checklist,
                            title = "No Tasks Match Filter",
                            subtitle = "Create a new task or extract tasks by uploading a meeting.",
                            actionLabel = "Create Task",
                            onAction = { editingTask = null; showTaskDialog = true }
                        )
                    }
                } else {
                    items(filtered, key = { it.id }) { task ->
                        TaskCardItem(
                            task = task,
                            onEdit = { editingTask = task; showTaskDialog = true },
                            onStatusChange = { onUpdateTaskStatus(task, it) },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        TaskEditorDialog(
            existing = editingTask,
            onDismiss = { showTaskDialog = false },
            onSave = { title, desc, project, assignee, deadline, priority, deps, status ->
                onSaveTask(
                    editingTask?.id,
                    editingTask?.meetingId ?: "manual",
                    project,
                    title,
                    desc,
                    assignee,
                    deadline,
                    priority,
                    deps,
                    status
                )
                showTaskDialog = false
            }
        )
    }
}

@Composable
private fun TaskCardItem(
    task: TaskEntity,
    onEdit: () -> Unit,
    onStatusChange: (TaskStatus) -> Unit,
    onDelete: () -> Unit
) {
    MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                task.title,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            val (bg, fg) = when (task.priority) {
                "HIGH" -> Crimson100 to Crimson900
                "MEDIUM" -> Amber100 to Amber900
                else -> Slate200 to Slate700
            }
            StatusPill(task.priority, bg, fg)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            task.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Owner: ${task.assignee} • Due: ${task.deadline} • ${task.project}",
            style = MaterialTheme.typography.labelSmall
        )
        Text(
            "Sync: ${task.syncStatus}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                val nextStatus = when (task.status) {
                    TaskStatus.TODO.name -> TaskStatus.IN_PROGRESS
                    TaskStatus.IN_PROGRESS.name -> TaskStatus.COMPLETED
                    TaskStatus.OVERDUE.name -> TaskStatus.COMPLETED
                    else -> TaskStatus.TODO
                }
                TextButton(onClick = { onStatusChange(nextStatus) }) {
                    Text("→ ${nextStatus.label}", maxLines = 1)
                }
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit Task", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Task", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun TaskEditorDialog(
    existing: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String, String, String, String, String) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var desc by remember { mutableStateOf(existing?.description ?: "") }
    var project by remember { mutableStateOf(existing?.project ?: "Website Launch") }
    var assignee by remember { mutableStateOf(existing?.assignee ?: "Rahul") }
    var deadline by remember { mutableStateOf(existing?.deadline ?: "December 12") }
    var priority by remember { mutableStateOf(existing?.priority ?: TaskPriority.MEDIUM.name) }
    var deps by remember { mutableStateOf(existing?.dependencies ?: "None") }
    var status by remember { mutableStateOf(existing?.status ?: TaskStatus.TODO.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "New Action Item" else "Edit Action Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title *") }, singleLine = true)
                OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = assignee, onValueChange = { assignee = it }, label = { Text("Assignee") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = deadline, onValueChange = { deadline = it }, label = { Text("Deadline") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                OutlinedTextField(value = project, onValueChange = { project = it }, label = { Text("Project") }, singleLine = true)
                OutlinedTextField(value = deps, onValueChange = { deps = it }, label = { Text("Dependencies") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TaskPriority.entries.forEach { p ->
                        FilterChip(selected = priority == p.name, onClick = { priority = p.name }, label = { Text(p.label) })
                    }
                }
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TaskStatus.entries.forEach { st ->
                        FilterChip(selected = status == st.name, onClick = { status = st.name }, label = { Text(st.label) })
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) onSave(title, desc, project, assignee, deadline, priority, deps, status)
                },
                modifier = Modifier.testTag("save_task_dialog_button")
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun WeeklyDigestScreen(
    uiState: MeetFlowUiState,
    aiNoticeMessage: String,
    onGenerateDigest: (String) -> Unit
) {
    val context = LocalContext.current
    val memberOptions = remember(uiState.members) {
        listOf("All Team Members") + uiState.members.map { it.displayName }.distinct()
    }
    var selectedMember by remember { mutableStateOf("All Team Members") }

    val activeDigest = remember(uiState.digests, selectedMember) {
        uiState.digests.firstOrNull { it.memberName == selectedMember } ?: uiState.digests.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Weekly Digest", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Personalized execution report & AI progress summary",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onGenerateDigest(selectedMember) },
                    modifier = Modifier.testTag("generate_digest_button")
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Generate")
                }
            }
        }

        item {
            FirebaseAiNoticeBanner(noticeText = aiNoticeMessage)
        }

        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                memberOptions.forEach { member ->
                    FilterChip(
                        selected = selectedMember == member,
                        onClick = { selectedMember = member },
                        label = { Text(member) }
                    )
                }
            }
        }

        if (activeDigest == null) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Summarize,
                    title = "No Digest Yet",
                    subtitle = "Tap Generate to compile a personalized weekly digest.",
                    actionLabel = "Generate Digest",
                    onAction = { onGenerateDigest(selectedMember) }
                )
            }
        } else {
            item {
                MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(activeDigest.memberName, style = MaterialTheme.typography.titleLarge)
                            Text(activeDigest.weekRange, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = {
                                val reportText = buildString {
                                    appendLine("MeetFlow AI Weekly Digest (${activeDigest.weekRange})")
                                    appendLine("Member: ${activeDigest.memberName}")
                                    appendLine("Assigned: ${activeDigest.assignedCount} | Completed: ${activeDigest.completedCount} | Pending: ${activeDigest.pendingCount} | Overdue: ${activeDigest.overdueCount}")
                                    appendLine()
                                    appendLine("Progress Summary:")
                                    appendLine(activeDigest.aiProgressSummary)
                                    appendLine()
                                    appendLine("Upcoming Deadlines:")
                                    appendLine(activeDigest.upcomingDeadlinesSummary)
                                    appendLine()
                                    appendLine("Relevant Decisions:")
                                    appendLine(activeDigest.relevantDecisionsSummary)
                                }
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "MeetFlow AI Weekly Digest - ${activeDigest.memberName}")
                                    putExtra(Intent.EXTRA_TEXT, reportText)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Export Weekly Digest"))
                            },
                            modifier = Modifier.testTag("export_digest_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Export")
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    // Breakdown Canvas Bar Chart
                    val total = activeDigest.assignedCount.coerceAtLeast(1).toFloat()
                    val primary = MaterialTheme.colorScheme.primary
                    Canvas(modifier = Modifier.fillMaxWidth().height(26.dp)) {
                        val compW = (activeDigest.completedCount / total) * size.width
                        val pendW = (activeDigest.pendingCount / total) * size.width
                        val overW = (activeDigest.overdueCount / total) * size.width
                        drawRoundRect(Emerald500, Offset.Zero, Size(compW, size.height), CornerRadius(6.dp.toPx()))
                        drawRoundRect(primary, Offset(compW, 0f), Size(pendW, size.height), CornerRadius(6.dp.toPx()))
                        drawRoundRect(Crimson500, Offset(compW + pendW, 0f), Size(overW, size.height), CornerRadius(6.dp.toPx()))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Assigned: ${activeDigest.assignedCount} • Completed: ${activeDigest.completedCount} • Pending: ${activeDigest.pendingCount} • Overdue: ${activeDigest.overdueCount}",
                        style = MaterialTheme.typography.labelSmall
                    )

                    Spacer(Modifier.height(12.dp))
                    Text("AI Progress Summary", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(activeDigest.aiProgressSummary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    Text("Upcoming Deadlines", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(activeDigest.upcomingDeadlinesSummary, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(10.dp))
                    Text("Relevant Decisions", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    Text(activeDigest.relevantDecisionsSummary, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun IntegrationsScreen(
    uiState: MeetFlowUiState,
    onSaveConfig: (IntegrationEntity, String) -> Unit,
    onDisconnect: (IntegrationEntity) -> Unit
) {
    var configuring by remember { mutableStateOf<IntegrationEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Text("Integrations", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Configure official external tool connections. Services remain Disconnected until authorized.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(uiState.integrations, key = { it.id }) { integration ->
            val isSaved = integration.status != "DISCONNECTED"
            MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(integration.provider, style = MaterialTheme.typography.titleMedium)
                        Text(integration.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    StatusPill(
                        text = if (isSaved) "Credentials Saved" else "Disconnected",
                        containerColor = if (isSaved) Amber100 else Slate200,
                        contentColor = if (isSaved) Amber900 else Slate700
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(integration.description, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    integration.requiredNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (integration.configDetail.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Endpoint/Workspace: ${integration.configDetail}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { configuring = integration },
                        modifier = Modifier.weight(1f).testTag("configure_${integration.provider}")
                    ) { Text(if (isSaved) "Update Config" else "Configure") }
                    if (isSaved) {
                        TextButton(
                            onClick = { onDisconnect(integration) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Disconnect") }
                    }
                }
            }
        }
    }

    configuring?.let { item ->
        var endpoint by remember { mutableStateOf(item.configDetail) }
        AlertDialog(
            onDismissRequest = { configuring = null },
            title = { Text("Configure ${item.provider}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.requiredNote, style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = endpoint,
                        onValueChange = { endpoint = it },
                        label = { Text("Workspace URL / Webhook / Board ID") },
                        singleLine = true
                    )
                    Text(
                        "Note: Live OAuth handshake requires registering client credentials in your developer console; tasks remain safely stored internally until verified.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    onSaveConfig(item, endpoint)
                    configuring = null
                }) { Text("Save Local Config") }
            },
            dismissButton = {
                TextButton(onClick = { configuring = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SettingsScreen(
    uiState: MeetFlowUiState,
    onSwitchRole: (UserRole) -> Unit,
    onSwitchWorkspace: (String) -> Unit,
    onCreateWorkspace: (String, String) -> Unit,
    onSetDarkMode: (Boolean?) -> Unit,
    onResetDemoData: () -> Unit,
    onRunReminderCheck: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var showNewWorkspaceDialog by remember { mutableStateOf(false) }
    var hasNotifPerm by remember { mutableStateOf(ReminderNotificationHelper.hasNotificationPermission(context)) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasNotifPerm = granted
        onRunReminderCheck()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Text("Settings & Workspace", style = MaterialTheme.typography.headlineSmall)
        }

        // Profile & Role Card
        item {
            MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                Text("User Profile & Role", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${uiState.currentUser?.name ?: "User"} (${uiState.currentUser?.email ?: ""})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text("Active Role (enforces approval permissions):", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    UserRole.entries.forEach { role ->
                        FilterChip(
                            selected = uiState.userRole == role,
                            onClick = { onSwitchRole(role) },
                            label = { Text(role.displayName) }
                        )
                    }
                }
            }
        }

        // Workspace Management
        item {
            MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Workspaces", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { showNewWorkspaceDialog = true }) { Text("+ New") }
                }
                uiState.workspaces.forEach { ws ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ws.name, style = MaterialTheme.typography.titleSmall)
                            Text(ws.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(
                            selected = ws.id == uiState.session.activeWorkspaceId,
                            onClick = { onSwitchWorkspace(ws.id) }
                        )
                    }
                }
            }
        }

        // Appearance
        item {
            MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                Text("Appearance", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.session.darkModeOverride == null,
                        onClick = { onSetDarkMode(null) },
                        label = { Text("System") }
                    )
                    FilterChip(
                        selected = uiState.session.darkModeOverride == false,
                        onClick = { onSetDarkMode(false) },
                        label = { Text("Light") }
                    )
                    FilterChip(
                        selected = uiState.session.darkModeOverride == true,
                        onClick = { onSetDarkMode(true) },
                        label = { Text("Dark") }
                    )
                }
            }
        }

        // Reminder Agent & Notifications
        item {
            MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                Text("Reminder Agent & Notifications", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Scan pending/overdue tasks and post local Android reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = {
                        if (!hasNotifPerm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            onRunReminderCheck()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("run_reminder_agent_button")
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Run Reminder Agent Check Now")
                }
            }
        }

        // Hackathon Demo Mode
        item {
            MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                Text("Hackathon Demo Mode", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Restores the 3 interactive sample meetings (Project Planning Dec 10, Dev Review Dec 20 conflict, Final Review), Rahul & Priya tasks, and Approval items.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onResetDemoData,
                    enabled = uiState.canApproveAndReset,
                    modifier = Modifier.fillMaxWidth().testTag("reset_demo_data_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Reset Demo Data")
                }
            }
        }

        // Sign Out
        item {
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Crimson600)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Sign Out")
            }
        }
    }

    if (showNewWorkspaceDialog) {
        var wsName by remember { mutableStateOf("") }
        var wsDesc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewWorkspaceDialog = false },
            title = { Text("Create Workspace") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = wsName, onValueChange = { wsName = it }, label = { Text("Workspace Name *") }, singleLine = true)
                    OutlinedTextField(value = wsDesc, onValueChange = { wsDesc = it }, label = { Text("Description") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (wsName.isNotBlank()) {
                        onCreateWorkspace(wsName, wsDesc)
                        showNewWorkspaceDialog = false
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewWorkspaceDialog = false }) { Text("Cancel") }
            }
        )
    }
}
