package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.ReminderEntity
import com.example.ui.MeetFlowUiState
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindersBottomSheet(
    reminders: List<ReminderEntity>,
    onDismiss: () -> Unit,
    onRunCheck: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Reminders & Notifications", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${reminders.count { !it.isRead }} unread alert(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = onMarkAllRead) { Text("Mark Read") }
                    FilledTonalButton(onClick = onRunCheck) { Text("Scan Now") }
                }
            }

            if (reminders.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.NotificationsNone,
                    title = "All Caught Up",
                    subtitle = "No active deadline or conflict alerts."
                )
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(reminders, key = { it.id }) { rem ->
                        MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rem.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                val (bg, fg) = when (rem.reminderType) {
                                    "CONFLICT", "OVERDUE" -> Crimson100 to Crimson900
                                    "APPROVAL" -> Amber100 to Amber900
                                    else -> RoyalBlue100 to Navy900
                                }
                                StatusPill(rem.reminderType, bg, fg)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(rem.message, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Due: ${rem.dueLabel} • ${if (rem.isSent) "System notification sent" else "In-app alert"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchSheet(
    uiState: MeetFlowUiState,
    onDismiss: () -> Unit,
    onOpenMeeting: (String) -> Unit,
    onNavigateRoute: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    val matchingMeetings = remember(uiState.meetings, query) {
        if (query.isBlank()) uiState.meetings.take(3)
        else uiState.meetings.filter { it.title.contains(query, true) || it.project.contains(query, true) }
    }
    val matchingDecisions = remember(uiState.decisions, query) {
        if (query.isBlank()) emptyList()
        else uiState.decisions.filter { it.title.contains(query, true) || it.description.contains(query, true) }
    }
    val matchingTasks = remember(uiState.tasks, query) {
        if (query.isBlank()) emptyList()
        else uiState.tasks.filter { it.title.contains(query, true) || it.assignee.contains(query, true) }
    }
    val matchingMemories = remember(uiState.memories, query) {
        if (query.isBlank()) emptyList()
        else uiState.memories.filter { it.title.contains(query, true) || it.content.contains(query, true) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Search Workspace", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search meetings, decisions, tasks, memories...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("global_search_input")
            )

            LazyColumn(
                modifier = Modifier.heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(matchingMeetings, key = { "sm_${it.id}" }) { mtg ->
                    MeetFlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDismiss(); onOpenMeeting(mtg.id) }
                    ) {
                        Text("Meeting: ${mtg.title}", style = MaterialTheme.typography.titleSmall)
                        Text("${mtg.project} • ${mtg.meetingDate}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(matchingDecisions, key = { "sd_${it.id}" }) { dec ->
                    MeetFlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDismiss(); onNavigateRoute("decisions") }
                    ) {
                        Text("Decision: ${dec.title}", style = MaterialTheme.typography.titleSmall)
                        Text("${dec.status} • ${dec.proposer}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(matchingTasks, key = { "st_${it.id}" }) { task ->
                    MeetFlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDismiss(); onNavigateRoute("tasks") }
                    ) {
                        Text("Task: ${task.title}", style = MaterialTheme.typography.titleSmall)
                        Text("${task.assignee} • Due: ${task.deadline}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                items(matchingMemories, key = { "smem_${it.id}" }) { mem ->
                    MeetFlowCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { onDismiss(); onNavigateRoute("memory") }
                    ) {
                        Text("Memory: ${mem.title}", style = MaterialTheme.typography.titleSmall)
                        Text(mem.content, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}
