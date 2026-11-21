package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.*
import com.example.ui.MeetFlowUiState
import com.example.ui.components.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DecisionsScreen(
    uiState: MeetFlowUiState,
    onOpenMeeting: (String) -> Unit,
    onUpdateDecision: (DecisionEntity, String) -> Unit
) {
    var statusFilter by remember { mutableStateOf("ALL") }
    var editingDecision by remember { mutableStateOf<DecisionEntity?>(null) }

    val filtered = remember(uiState.decisions, statusFilter) {
        if (statusFilter == "ALL") uiState.decisions
        else uiState.decisions.filter { it.status == statusFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Text("Decisions Ledger", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Track proposed, approved, and superseded decisions across all meetings.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "PROPOSED", "APPROVED", "SUPERSEDED").forEach { status ->
                    FilterChip(
                        selected = statusFilter == status,
                        onClick = { statusFilter = status },
                        label = { Text(status) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Gavel,
                    title = "No Decisions Found",
                    subtitle = "No decisions match the selected filter."
                )
            }
        } else {
            items(filtered, key = { it.id }) { dec ->
                MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dec.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${dec.project} • ${dec.decisionDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val (bg, fg) = when (dec.status) {
                            DecisionStatus.APPROVED.name -> Emerald100 to Emerald900
                            DecisionStatus.PROPOSED.name -> Amber100 to Amber900
                            DecisionStatus.SUPERSEDED.name -> Slate200 to Slate700
                            else -> Crimson100 to Crimson900
                        }
                        StatusPill(dec.status, bg, fg)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(dec.description, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Proposer: ${dec.proposer} • Approver: ${dec.approver}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { onOpenMeeting(dec.meetingId) },
                            modifier = Modifier.weight(1f)
                        ) { Text("Source Meeting") }
                        OutlinedButton(
                            onClick = { editingDecision = dec },
                            modifier = Modifier.weight(1f).testTag("edit_decision_${dec.id}")
                        ) { Text("Edit / Status") }
                    }
                }
            }
        }
    }

    editingDecision?.let { dec ->
        var title by remember { mutableStateOf(dec.title) }
        var desc by remember { mutableStateOf(dec.description) }
        var status by remember { mutableStateOf(dec.status) }
        AlertDialog(
            onDismissRequest = { editingDecision = null },
            title = { Text("Edit Decision") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") })
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, minLines = 2)
                    Text("Status", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("PROPOSED", "APPROVED", "REJECTED").forEach { st ->
                            FilterChip(
                                selected = status == st,
                                onClick = { status = st },
                                label = { Text(st) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    onUpdateDecision(
                        dec.copy(
                            title = title,
                            description = desc,
                            status = status,
                            approver = if (status == "APPROVED") (uiState.currentUser?.name ?: "Approver") else dec.approver
                        ),
                        "Edited via Decisions Ledger"
                    )
                    editingDecision = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { editingDecision = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun ConflictDetectionScreen(
    uiState: MeetFlowUiState,
    aiNoticeMessage: String,
    onResolveConflict: (String, String) -> Unit
) {
    var statusFilter by remember { mutableStateOf("UNRESOLVED") }

    val filtered = remember(uiState.conflicts, statusFilter) {
        if (statusFilter == "ALL") uiState.conflicts
        else uiState.conflicts.filter { it.status == statusFilter }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Text("Conflict Detection", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Cross-meeting contradiction engine protecting canonical decisions.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            FirebaseAiNoticeBanner(noticeText = aiNoticeMessage)
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("UNRESOLVED", "CLARIFICATION_REQUESTED", "RESOLVED", "ALL").forEach { st ->
                    FilterChip(
                        selected = statusFilter == st,
                        onClick = { statusFilter = st },
                        label = { Text(st.replace("_", " ")) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Verified,
                    title = "No Conflicts in '$statusFilter'",
                    subtitle = "All proposals in this filter are clear."
                )
            }
        } else {
            items(filtered, key = { it.id }) { conflict ->
                val isResolved = conflict.status == ConflictStatus.RESOLVED.name
                MeetFlowCard(
                    modifier = Modifier.fillMaxWidth().testTag("conflict_card_${conflict.id}"),
                    borderColor = if (isResolved) Emerald500.copy(alpha = 0.5f) else Crimson500.copy(alpha = 0.6f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (isResolved) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isResolved) Emerald600 else Crimson600,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isResolved) "CONFLICT RESOLVED" else "CONFLICT DETECTED",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (isResolved) Emerald600 else Crimson600
                            )
                        }
                        StatusPill(
                            text = conflict.project,
                            containerColor = RoyalBlue100,
                            contentColor = Navy900
                        )
                    }
                    Spacer(Modifier.height(10.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Previous Decision:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            Text(conflict.previousDecisionText, style = MaterialTheme.typography.bodySmall)
                            HorizontalDivider()
                            Text("New Proposal:", style = MaterialTheme.typography.labelSmall, color = Crimson600)
                            Text(conflict.newProposalText, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Text("Reason: ${conflict.reason}", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "AI Recommendation: ${conflict.aiRecommendation}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!isResolved) {
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onResolveConflict(conflict.id, "ACCEPT_NEW") },
                                modifier = Modifier.weight(1f).testTag("accept_new_${conflict.id}")
                            ) { Text("Accept New", maxLines = 1) }
                            OutlinedButton(
                                onClick = { onResolveConflict(conflict.id, "KEEP_PREVIOUS") },
                                modifier = Modifier.weight(1f).testTag("keep_prev_${conflict.id}")
                            ) { Text("Keep Previous", maxLines = 1) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { onResolveConflict(conflict.id, "REQUEST_CLARIFICATION") },
                                modifier = Modifier.weight(1f).testTag("clarify_${conflict.id}")
                            ) { Text("Clarify", maxLines = 1) }
                            TextButton(
                                onClick = { onResolveConflict(conflict.id, "MARK_RESOLVED") },
                                modifier = Modifier.weight(1f).testTag("resolve_${conflict.id}")
                            ) { Text("Mark Resolved", maxLines = 1) }
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                        StatusPill(
                            text = "${conflict.resolutionAction} by ${conflict.resolvedBy}",
                            containerColor = Emerald100,
                            contentColor = Emerald900
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MeetingMemoryScreen(
    uiState: MeetFlowUiState,
    onOpenMeeting: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("ALL") }
    val formatter = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    val filteredMemories = remember(uiState.memories, query, selectedType) {
        uiState.memories.filter { mem ->
            val matchesQuery = query.isBlank() ||
                mem.title.contains(query, ignoreCase = true) ||
                mem.content.contains(query, ignoreCase = true) ||
                mem.relatedPeopleCsv.contains(query, ignoreCase = true) ||
                mem.project.contains(query, ignoreCase = true)
            val matchesType = selectedType == "ALL" || mem.memoryType == selectedType
            matchesQuery && matchesType
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Text("Meeting Memory Explorer", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Persistent organizational memory, commitments, and decision lineage.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Search by keyword, person, project...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("memory_search_input")
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("ALL", "DECISION", "COMMITMENT", "PROPOSAL", "CHANGE").forEach { type ->
                    FilterChip(
                        selected = selectedType == type,
                        onClick = { selectedType = type },
                        label = { Text(type) }
                    )
                }
            }
        }

        item {
            Text("Interactive Decision Timeline", style = MaterialTheme.typography.titleMedium)
        }

        if (uiState.decisionHistory.isEmpty()) {
            item {
                MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No decision changes logged yet.", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            items(uiState.decisionHistory, key = { it.id }) { hist ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(64.dp)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }
                    MeetFlowCard(modifier = Modifier.weight(1f)) {
                        Text(
                            "${hist.previousValue} → ${hist.newValue}",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(hist.reason, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "By ${hist.changedBy} • ${formatter.format(Date(hist.changedAt))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Text("Indexed Memories (${filteredMemories.size})", style = MaterialTheme.typography.titleMedium)
        }

        items(filteredMemories, key = { it.id }) { mem ->
            MeetFlowCard(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onOpenMeeting(mem.meetingId) }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(mem.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    StatusPill(mem.memoryType, RoyalBlue100, Navy900)
                }
                Spacer(Modifier.height(6.dp))
                Text(mem.content, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "${mem.meetingTitle} • ${mem.memoryDate} • ${mem.relatedPeopleCsv}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ApprovalCenterScreen(
    uiState: MeetFlowUiState,
    onDecideApproval: (String, Boolean, String?, String?) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingApproval by remember { mutableStateOf<ApprovalEntity?>(null) }

    val pending = remember(uiState.approvals) {
        uiState.approvals.filter { it.status == ApprovalStatus.PENDING.name }
    }
    val auditLogs = remember(uiState.approvals) {
        uiState.approvals.filter { it.status != ApprovalStatus.PENDING.name }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text("Human Approval Center", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Zero autonomous external execution — every AI action requires human sign-off.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!uiState.canApproveAndReset) {
                Spacer(Modifier.height(8.dp))
                StatusPill(
                    "Team Member View — Manager/Admin role required to approve",
                    Amber100,
                    Amber900
                )
            }
        }

        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Pending (${pending.size})") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Audit Log (${auditLogs.size})") }
            )
        }

        val listToShow = if (selectedTab == 0) pending else auditLogs

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            if (listToShow.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Default.VerifiedUser,
                        title = if (selectedTab == 0) "Approval Queue Clear" else "No Audit Entries Yet",
                        subtitle = if (selectedTab == 0) "All AI-proposed actions have been reviewed." else "Approved and rejected actions appear here."
                    )
                }
            } else {
                items(listToShow, key = { it.id }) { apr ->
                    MeetFlowCard(modifier = Modifier.fillMaxWidth().testTag("approval_card_${apr.id}")) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(apr.proposedAction, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            val (bg, fg) = when (apr.status) {
                                ApprovalStatus.APPROVED.name -> Emerald100 to Emerald900
                                ApprovalStatus.REJECTED.name -> Crimson100 to Crimson900
                                else -> Amber100 to Amber900
                            }
                            StatusPill(apr.status, bg, fg)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(apr.description, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Owner: ${apr.responsiblePerson} • Dest: ${apr.destination}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Source: ${apr.meetingTitle}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (apr.status == ApprovalStatus.PENDING.name) {
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onDecideApproval(apr.id, true, null, null) },
                                    enabled = uiState.canApproveAndReset,
                                    modifier = Modifier.weight(1f).testTag("approve_btn_${apr.id}")
                                ) { Text("Approve") }
                                OutlinedButton(
                                    onClick = { onDecideApproval(apr.id, false, null, null) },
                                    enabled = uiState.canApproveAndReset,
                                    modifier = Modifier.weight(1f).testTag("reject_btn_${apr.id}")
                                ) { Text("Reject") }
                            }
                            Spacer(Modifier.height(6.dp))
                            TextButton(
                                onClick = { editingApproval = apr },
                                enabled = uiState.canApproveAndReset,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit Before Approval")
                            }
                        } else {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Reviewed by ${apr.reviewedBy}: ${apr.executionResult}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    editingApproval?.let { apr ->
        var actionText by remember { mutableStateOf(apr.proposedAction) }
        var descText by remember { mutableStateOf(apr.description) }
        AlertDialog(
            onDismissRequest = { editingApproval = null },
            title = { Text("Edit Before Approval") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = actionText, onValueChange = { actionText = it }, label = { Text("Proposed Action") })
                    OutlinedTextField(value = descText, onValueChange = { descText = it }, label = { Text("Description") }, minLines = 3)
                }
            },
            confirmButton = {
                Button(onClick = {
                    onDecideApproval(apr.id, true, actionText, descText)
                    editingApproval = null
                }) { Text("Save & Approve") }
            },
            dismissButton = {
                TextButton(onClick = { editingApproval = null }) { Text("Cancel") }
            }
        )
    }
}
