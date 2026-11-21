package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.*
import com.example.ui.MeetFlowUiState
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    uiState: MeetFlowUiState,
    aiNoticeMessage: String,
    onNavigateUpload: () -> Unit,
    onNavigateMeetings: () -> Unit,
    onOpenMeeting: (String) -> Unit,
    onNavigateTasks: () -> Unit,
    onNavigateConflicts: () -> Unit,
    onNavigateApprovals: () -> Unit,
    onNavigateDecisions: () -> Unit
) {
    val totalMeetings = uiState.meetings.size
    val totalDecisions = uiState.decisions.size
    val pendingTasks = uiState.tasks.count { it.status == TaskStatus.TODO.name || it.status == TaskStatus.IN_PROGRESS.name }
    val completedTasks = uiState.tasks.count { it.status == TaskStatus.COMPLETED.name }
    val overdueTasks = uiState.tasks.count { it.status == TaskStatus.OVERDUE.name }
    val upcomingDeadlines = uiState.tasks.count { it.status != TaskStatus.COMPLETED.name && it.deadline != "TBD" }
    val unresolvedConflicts = uiState.conflicts.count { it.status != ConflictStatus.RESOLVED.name }
    val pendingApprovals = uiState.approvals.count { it.status == ApprovalStatus.PENDING.name }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_banner),
                    contentDescription = "Workspace Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xDF091120), Color(0x88091120))
                            )
                        )
                        .padding(18.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatusPill(
                                text = uiState.activeWorkspace?.name ?: "Workspace",
                                containerColor = RoyalBlue100.copy(alpha = 0.9f),
                                contentColor = Navy900
                            )
                            StatusPill(
                                text = "${uiState.userRole.displayName} Mode",
                                containerColor = Color.White.copy(alpha = 0.16f),
                                contentColor = Color.White
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Intelligent Execution",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "$unresolvedConflicts conflict(s) • $pendingApprovals awaiting approval",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = onNavigateUpload,
                                modifier = Modifier.testTag("dashboard_upload_meeting_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RoyalBlue500,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Meeting")
                            }
                        }
                    }
                }
            }
        }

        item {
            FirebaseAiNoticeBanner(noticeText = aiNoticeMessage)
        }

        if (unresolvedConflicts > 0 || pendingApprovals > 0) {
            item {
                MeetFlowCard(
                    borderColor = if (unresolvedConflicts > 0) Crimson500.copy(alpha = 0.5f) else Amber500.copy(alpha = 0.5f),
                    containerColor = if (unresolvedConflicts > 0) Crimson100.copy(alpha = 0.35f) else Amber100.copy(alpha = 0.35f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (unresolvedConflicts > 0) "CONFLICT DETECTED" else "APPROVALS PENDING",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (unresolvedConflicts > 0) Crimson600 else Amber600
                            )
                            Text(
                                text = if (unresolvedConflicts > 0)
                                    "$unresolvedConflicts decision contradiction(s) need human review."
                                else
                                    "$pendingApprovals proposed action(s) await your approval.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        TextButton(
                            onClick = if (unresolvedConflicts > 0) onNavigateConflicts else onNavigateApprovals,
                            modifier = Modifier.testTag("dashboard_alert_cta")
                        ) {
                            Text("Review")
                        }
                    }
                }
            }
        }

        // KPI Metrics Grid (2 columns per row cleanly)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        label = "Total Meetings",
                        value = totalMeetings.toString(),
                        icon = Icons.Default.VideoCall,
                        accentColor = RoyalBlue600,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateMeetings
                    )
                    KpiMetricCard(
                        label = "Decisions Extracted",
                        value = totalDecisions.toString(),
                        icon = Icons.Default.Gavel,
                        accentColor = Indigo600,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateDecisions
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        label = "Pending Tasks",
                        value = pendingTasks.toString(),
                        icon = Icons.Default.PendingActions,
                        accentColor = Amber600,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateTasks
                    )
                    KpiMetricCard(
                        label = "Completed Tasks",
                        value = completedTasks.toString(),
                        icon = Icons.Default.TaskAlt,
                        accentColor = Emerald600,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateTasks
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        label = "Overdue Tasks",
                        value = overdueTasks.toString(),
                        icon = Icons.Default.Schedule,
                        accentColor = Crimson600,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateTasks
                    )
                    KpiMetricCard(
                        label = "Upcoming Deadlines",
                        value = upcomingDeadlines.toString(),
                        icon = Icons.Default.Event,
                        accentColor = SkyCyan500,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateTasks
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    KpiMetricCard(
                        label = "Unresolved Conflicts",
                        value = unresolvedConflicts.toString(),
                        icon = Icons.Default.WarningAmber,
                        accentColor = Crimson600,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateConflicts
                    )
                    KpiMetricCard(
                        label = "Approval Queue",
                        value = pendingApprovals.toString(),
                        icon = Icons.Default.VerifiedUser,
                        accentColor = RoyalBlue600,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateApprovals
                    )
                }
            }
        }

        // Weekly Productivity Chart
        item {
            WeeklyProductivityChart(
                completedCount = completedTasks,
                pendingCount = pendingTasks + overdueTasks,
                decisionsCount = totalDecisions
            )
        }

        // Recent Meetings Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Meetings", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = onNavigateMeetings) { Text("View All") }
            }
        }

        if (uiState.meetings.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.VideoCall,
                    title = "No Meetings Processed Yet",
                    subtitle = "Upload an audio, video, or transcript file to launch the 7-agent pipeline.",
                    actionLabel = "Upload Meeting",
                    onAction = onNavigateUpload
                )
            }
        } else {
            items(uiState.meetings.take(3), key = { it.id }) { meeting ->
                MeetFlowCard(
                    modifier = Modifier.fillMaxWidth().testTag("recent_meeting_${meeting.id}"),
                    onClick = { onOpenMeeting(meeting.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = meeting.title,
                                style = MaterialTheme.typography.titleSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${meeting.project} • ${meeting.meetingDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (meeting.isDemo) {
                            StatusPill("DEMO", RoyalBlue100, Navy900)
                        } else {
                            StatusPill(meeting.sourceType, Slate100, Slate700)
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = meeting.summaryConclusions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Recent AI Agent Activity Panel
        item {
            Text(
                text = "Multi-Agent AI Activity Feed",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(uiState.aiLogs.take(5), key = { it.id }) { log ->
            MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
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
                            imageVector = Icons.Default.SmartToy,
                            contentDescription = log.agentName,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = log.agentName,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    StatusPill(
                        text = log.status,
                        containerColor = if (log.status == AgentStatus.COMPLETED.name) Emerald100 else Crimson100,
                        contentColor = if (log.status == AgentStatus.COMPLETED.name) Emerald900 else Crimson900
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${log.operation}: ${log.resultSummary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
