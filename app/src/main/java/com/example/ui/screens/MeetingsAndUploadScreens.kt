package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import com.example.ui.MeetFlowUiState
import com.example.ui.MeetFlowViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun MeetingsListScreen(
    uiState: MeetFlowUiState,
    onOpenMeeting: (String) -> Unit,
    onNavigateUpload: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf("All") }

    val projects = remember(uiState.meetings) {
        listOf("All") + uiState.meetings.map { it.project }.distinct()
    }
    val filtered = remember(uiState.meetings, searchQuery, selectedProject) {
        uiState.meetings.filter { mtg ->
            val matchesSearch = searchQuery.isBlank() ||
                mtg.title.contains(searchQuery, ignoreCase = true) ||
                mtg.participantsCsv.contains(searchQuery, ignoreCase = true) ||
                mtg.project.contains(searchQuery, ignoreCase = true)
            val matchesProj = selectedProject == "All" || mtg.project == selectedProject
            matchesSearch && matchesProj
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Meetings", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${uiState.meetings.size} recorded & analyzed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = onNavigateUpload,
                    modifier = Modifier.testTag("meetings_upload_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New Meeting")
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search meetings, participants, projects...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("meetings_search_input")
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                projects.forEach { project ->
                    FilterChip(
                        selected = selectedProject == project,
                        onClick = { selectedProject = project },
                        label = { Text(project) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.VideoCall,
                    title = "No matching meetings",
                    subtitle = "Upload a meeting recording or transcript to start extracting decisions and tasks.",
                    actionLabel = "Upload Meeting",
                    onAction = onNavigateUpload
                )
            }
        } else {
            items(filtered, key = { it.id }) { meeting ->
                val decCount = uiState.decisions.count { it.meetingId == meeting.id }
                val taskCount = uiState.tasks.count { it.meetingId == meeting.id }
                val conflictCount = uiState.conflicts.count { it.meetingId == meeting.id && it.status != ConflictStatus.RESOLVED.name }

                MeetFlowCard(
                    modifier = Modifier.fillMaxWidth().testTag("meeting_card_${meeting.id}"),
                    onClick = { onOpenMeeting(meeting.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(meeting.title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${meeting.project} • ${meeting.meetingDate} • ${meeting.sourceType}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (meeting.isDemo) {
                            StatusPill("DEMO", RoyalBlue100, Navy900)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        meeting.description.ifBlank { meeting.summaryDiscussions },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatusPill("$decCount Decisions", Indigo100, Navy900)
                        StatusPill("$taskCount Tasks", Emerald100, Emerald900)
                        if (conflictCount > 0) {
                            StatusPill("$conflictCount Conflict", Crimson100, Crimson900)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UploadMeetingScreen(
    uiState: MeetFlowUiState,
    aiNoticeMessage: String,
    onProcessMeeting: (
        title: String,
        date: String,
        project: String,
        description: String,
        participants: String,
        sourceType: String,
        fileName: String,
        transcript: String,
        onComplete: (String) -> Unit
    ) -> Unit,
    onClearSteps: () -> Unit,
    onOpenMeeting: (String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var meetingDate by remember { mutableStateOf("2026-11-21") }
    var project by remember { mutableStateOf("Website Launch") }
    var participants by remember { mutableStateOf("Alex Rivera, Rahul Verma, Priya Nair") }
    var description by remember { mutableStateOf("") }
    var sourceType by remember { mutableStateOf("TEXT") }
    var selectedFileName by remember { mutableStateOf("") }
    var transcriptText by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun readFileName(uri: Uri): String {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) return cursor.getString(idx)
        }
        return uri.lastPathSegment ?: "uploaded_file"
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceType = "AUDIO"
            selectedFileName = readFileName(uri)
        }
    }
    val videoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceType = "VIDEO"
            selectedFileName = readFileName(uri)
        }
    }
    val transcriptLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            sourceType = "TRANSCRIPT"
            selectedFileName = readFileName(uri)
            try {
                val content = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                if (!content.isNullOrBlank()) {
                    transcriptText = content
                }
            } catch (e: Exception) {
                validationError = "Could not read transcript file: ${e.localizedMessage}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Upload & Analyze Meeting", style = MaterialTheme.typography.headlineSmall)
        FirebaseAiNoticeBanner(noticeText = aiNoticeMessage)

        MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
            Text("1. Source Media or Transcript", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Import MP3/WAV/M4A audio, MP4 video, TXT/VTT transcripts, or paste transcript text directly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { audioLauncher.launch(arrayOf("audio/*")) },
                    modifier = Modifier.weight(1f).testTag("pick_audio_button")
                ) {
                    Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Audio", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { videoLauncher.launch(arrayOf("video/mp4", "video/*")) },
                    modifier = Modifier.weight(1f).testTag("pick_video_button")
                ) {
                    Icon(Icons.Default.VideoFile, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Video", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { transcriptLauncher.launch(arrayOf("text/*", "application/octet-stream")) },
                    modifier = Modifier.weight(1f).testTag("pick_transcript_button")
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("TXT/VTT", maxLines = 1)
                }
            }
            if (selectedFileName.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                StatusPill(
                    text = "Selected ($sourceType): $selectedFileName",
                    containerColor = RoyalBlue100,
                    contentColor = Navy900
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    title = "Sprint Sync – Launch Check"
                    project = "Website Launch"
                    description = "Testing cross-meeting memory & conflict detection pipeline"
                    sourceType = "TEXT"
                    selectedFileName = "sample_conflict_transcript.txt"
                    transcriptText = """
                        Priya Nair: API integration is nearly ready, so I propose moving website launch to December 22.
                        Rahul Verma: I will complete final regression UI fixes by December 18.
                        Alex Rivera: Is there any risk with checkout webhooks before December 22?
                    """.trimIndent()
                    validationError = null
                },
                modifier = Modifier.fillMaxWidth().testTag("load_sample_transcript_button")
            ) {
                Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Load Sample Conflict Transcript (Demo)")
            }
        }

        MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
            Text("2. Meeting Metadata", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; validationError = null },
                label = { Text("Meeting Title *") },
                placeholder = { Text("e.g., Q4 Architecture & Launch Sync") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("upload_title_input")
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = project,
                    onValueChange = { project = it },
                    label = { Text("Project") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = meetingDate,
                    onValueChange = { meetingDate = it },
                    label = { Text("Date") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = participants,
                onValueChange = { participants = it },
                label = { Text("Participants (comma-separated)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = transcriptText,
                onValueChange = { transcriptText = it; validationError = null },
                label = { Text("Transcript Text (Speaker: message per line)") },
                minLines = 5,
                maxLines = 9,
                modifier = Modifier.fillMaxWidth().testTag("upload_transcript_input")
            )
        }

        if (validationError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    validationError!!,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Button(
            onClick = {
                if (title.isBlank()) {
                    validationError = "Please enter a meeting title."
                    return@Button
                }
                if (transcriptText.isBlank() && selectedFileName.isBlank()) {
                    validationError = "Provide transcript text or select an audio/video/transcript file."
                    return@Button
                }
                onClearSteps()
                onProcessMeeting(
                    title,
                    meetingDate,
                    project,
                    description,
                    participants,
                    sourceType,
                    selectedFileName,
                    transcriptText
                ) { createdId ->
                    onOpenMeeting(createdId)
                }
            },
            enabled = !uiState.isProcessingMeeting,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("run_agent_pipeline_button")
        ) {
            if (uiState.isProcessingMeeting) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(10.dp))
                Text("Running 7-Agent Pipeline...")
            } else {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Run 7-Agent AI Pipeline")
            }
        }

        if (uiState.pipelineSteps.isNotEmpty()) {
            AgentPipelineTracker(steps = uiState.pipelineSteps)
        }

        Spacer(Modifier.height(20.dp))
    }
}

@Composable
fun MeetingDetailScreen(
    meetingId: String,
    viewModel: MeetFlowViewModel,
    onBack: () -> Unit,
    onNavigateConflicts: () -> Unit,
    onNavigateTasks: () -> Unit
) {
    BackHandler(onBack = onBack)

    val meeting by viewModel.observeMeeting(meetingId).collectAsStateWithLifecycle(initialValue = null)
    val segments by viewModel.observeTranscript(meetingId).collectAsStateWithLifecycle(initialValue = emptyList())
    val decisions by viewModel.observeMeetingDecisions(meetingId).collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by viewModel.observeMeetingTasks(meetingId).collectAsStateWithLifecycle(initialValue = emptyList())
    val conflicts by viewModel.observeMeetingConflicts(meetingId).collectAsStateWithLifecycle(initialValue = emptyList())
    val logs by viewModel.observeMeetingLogs(meetingId).collectAsStateWithLifecycle(initialValue = emptyList())

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Overview", "Transcript", "Decisions", "Actions", "Conflicts", "AI Insights")

    var showEditSummaryDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("meeting_detail_back")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = meeting?.title ?: "Loading Meeting...",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (meeting != null) {
                            Text(
                                text = "${meeting!!.project} • ${meeting!!.meetingDate}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (meeting != null) {
                        IconButton(onClick = { showEditSummaryDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Summary")
                        }
                    }
                }
                ScrollableTabRow(selectedTabIndex = selectedTab, edgePadding = 12.dp) {
                    tabs.forEachIndexed { idx, label ->
                        Tab(
                            selected = selectedTab == idx,
                            onClick = { selectedTab = idx },
                            text = { Text(label) }
                        )
                    }
                }
            }
        }

        val currentMeeting = meeting
        if (currentMeeting == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
        ) {
            item {
                FirebaseAiNoticeBanner(noticeText = viewModel.aiNoticeMessage)
            }

            when (selectedTab) {
                0 -> { // Overview
                    item {
                        MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Meeting Summary", style = MaterialTheme.typography.titleMedium)
                                StatusPill(currentMeeting.sourceType, RoyalBlue100, Navy900)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Topics", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(currentMeeting.summaryTopics, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Key Discussions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(currentMeeting.summaryDiscussions, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Conclusions", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(currentMeeting.summaryConclusions, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(8.dp))
                            Text("Participants", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                            Text(currentMeeting.participantsCsv, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    item {
                        val issues = currentMeeting.unresolvedIssuesSerialized.split("|", "\n").filter { it.isNotBlank() }
                        MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Unresolved Issues & Risks", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            issues.forEach { issue ->
                                Text("• $issue", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 2.dp))
                            }
                        }
                    }
                    item {
                        val quotes = currentMeeting.importantQuotesSerialized.lines().filter { it.isNotBlank() }
                        MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                            Text("Important Quotes", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(8.dp))
                            quotes.forEach { raw ->
                                val parts = raw.split("|")
                                val speaker = parts.getOrNull(0) ?: "Speaker"
                                val ts = parts.getOrNull(1) ?: ""
                                val quote = parts.getOrNull(2) ?: raw
                                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                    Text(
                                        "$speaker [$ts]",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text("\"$quote\"", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                1 -> { // Transcript
                    if (segments.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Subtitles,
                                title = "No Transcript Segments",
                                subtitle = "No timestamped segments were found for this meeting."
                            )
                        }
                    } else {
                        items(segments, key = { it.id }) { seg ->
                            MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(seg.speaker, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    Text(seg.timestampLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(seg.text, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                2 -> { // Decisions
                    if (decisions.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Gavel,
                                title = "No Decisions Extracted",
                                subtitle = "No decision proposals were extracted from this meeting."
                            )
                        }
                    } else {
                        items(decisions, key = { it.id }) { dec ->
                            MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(dec.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    StatusPill(
                                        dec.status,
                                        if (dec.status == DecisionStatus.APPROVED.name) Emerald100 else Amber100,
                                        if (dec.status == DecisionStatus.APPROVED.name) Emerald900 else Amber900
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(dec.description, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Proposer: ${dec.proposer} • Approver: ${dec.approver}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                3 -> { // Action Items
                    if (tasks.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Checklist,
                                title = "No Action Items",
                                subtitle = "No tasks extracted from this meeting."
                            )
                        }
                    } else {
                        items(tasks, key = { it.id }) { task ->
                            MeetFlowCard(modifier = Modifier.fillMaxWidth(), onClick = onNavigateTasks) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(task.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                    StatusPill(task.priority, RoyalBlue100, Navy900)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(task.description, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Owner: ${task.assignee} • Due: ${task.deadline} • ${task.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                4 -> { // Conflicts
                    if (conflicts.isEmpty()) {
                        item {
                            EmptyStateCard(
                                icon = Icons.Default.Verified,
                                title = "No Conflicts Detected",
                                subtitle = "All proposals in this meeting align with Meeting Memory."
                            )
                        }
                    } else {
                        items(conflicts, key = { it.id }) { conf ->
                            MeetFlowCard(
                                modifier = Modifier.fillMaxWidth(),
                                borderColor = Crimson500.copy(alpha = 0.6f),
                                onClick = onNavigateConflicts
                            ) {
                                Text("CONFLICT DETECTED", style = MaterialTheme.typography.labelLarge, color = Crimson600)
                                Spacer(Modifier.height(6.dp))
                                Text("Previous: ${conf.previousDecisionText}", style = MaterialTheme.typography.bodySmall)
                                Text("Proposed: ${conf.newProposalText}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(6.dp))
                                Text("AI Recommendation: ${conf.aiRecommendation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(8.dp))
                                Text("Tap to resolve in Conflict Detection Center", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
                5 -> { // AI Insights
                    items(logs, key = { it.id }) { log ->
                        MeetFlowCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(log.agentName, style = MaterialTheme.typography.titleSmall)
                                StatusPill(log.status, Emerald100, Emerald900)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text("${log.operation}: ${log.resultSummary}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }

    if (showEditSummaryDialog && meeting != null) {
        var topics by remember { mutableStateOf(meeting!!.summaryTopics) }
        var discussions by remember { mutableStateOf(meeting!!.summaryDiscussions) }
        var conclusions by remember { mutableStateOf(meeting!!.summaryConclusions) }

        AlertDialog(
            onDismissRequest = { showEditSummaryDialog = false },
            title = { Text("Edit AI Summary") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = topics, onValueChange = { topics = it }, label = { Text("Topics") })
                    OutlinedTextField(value = discussions, onValueChange = { discussions = it }, label = { Text("Discussions") }, minLines = 2)
                    OutlinedTextField(value = conclusions, onValueChange = { conclusions = it }, label = { Text("Conclusions") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateMeetingSummary(meetingId, topics, discussions, conclusions)
                    showEditSummaryDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showEditSummaryDialog = false }) { Text("Cancel") }
            }
        )
    }
}
