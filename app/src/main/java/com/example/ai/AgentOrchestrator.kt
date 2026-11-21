package com.example.ai

import com.example.data.AgentStatus
import com.example.data.AiActivityLogEntity
import com.example.data.ApprovalEntity
import com.example.data.ApprovalStatus
import com.example.data.ConflictEntity
import com.example.data.ConflictStatus
import com.example.data.DecisionEntity
import com.example.data.MeetFlowDao
import com.example.data.MeetingEntity
import com.example.data.MeetingMemoryEntity
import com.example.data.ReminderEntity
import com.example.data.TaskEntity
import com.example.data.TaskStatus
import com.example.data.TranscriptSegmentEntity
import kotlinx.coroutines.delay
import java.util.UUID

data class AgentStepState(
    val agentName: String,
    val operation: String,
    val status: AgentStatus,
    val detail: String = ""
)

data class MeetingPipelineInput(
    val workspaceId: String,
    val title: String,
    val meetingDate: String,
    val project: String,
    val description: String,
    val participantsCsv: String,
    val sourceType: String,
    val sourceFileName: String,
    val rawTranscriptText: String
)

class AgentOrchestrator(
    private val dao: MeetFlowDao,
    private val aiRepository: AiRepository
) {
    suspend fun runMeetingPipeline(
        input: MeetingPipelineInput,
        onProgress: (List<AgentStepState>) -> Unit
    ): Result<String> {
        val meetingId = "mtg_${UUID.randomUUID().toString().take(8)}"
        val steps = mutableListOf(
            AgentStepState("Transcription Agent", "Process ${input.sourceType} input", AgentStatus.RUNNING),
            AgentStepState("Meeting Intelligence Agent", "Extract summary, decisions & tasks", AgentStatus.RUNNING),
            AgentStepState("Memory Agent", "Retrieve historical decisions & context", AgentStatus.RUNNING),
            AgentStepState("Conflict Detection Agent", "Compare new proposals vs memory", AgentStatus.RUNNING),
            AgentStepState("Task Execution Agent", "Stage tasks & human approvals", AgentStatus.RUNNING),
            AgentStepState("Reminder Agent", "Schedule deadline & conflict alerts", AgentStatus.RUNNING),
            AgentStepState("Weekly Digest Agent", "Refresh weekly workspace metrics", AgentStatus.RUNNING)
        )

        fun updateStep(index: Int, status: AgentStatus, detail: String) {
            steps[index] = steps[index].copy(status = status, detail = detail)
            onProgress(steps.toList())
        }

        return try {
            onProgress(steps.toList())
            val participants = input.participantsCsv.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }

            // 1. Transcription Agent
            val effectiveTranscript = if (input.rawTranscriptText.isNotBlank()) {
                input.rawTranscriptText
            } else {
                val generated = aiRepository.transcribeMediaPlaceholder(
                    fileName = input.sourceFileName.ifBlank { input.title },
                    sourceType = input.sourceType,
                    participantsHint = input.participantsCsv
                )
                generated.joinToString("\n") { "${it.speaker}: ${it.text}" }
            }
            updateStep(0, AgentStatus.COMPLETED, "Prepared transcript (${effectiveTranscript.lines().size} lines)")
            logAgent(input.workspaceId, meetingId, "Transcription Agent", "Process ${input.sourceType}", AgentStatus.COMPLETED, "Parsed transcript input")

            // 2. Meeting Intelligence Agent
            delay(180)
            val analysis = aiRepository.analyzeTranscript(
                title = input.title,
                project = input.project,
                participants = participants,
                rawTranscript = effectiveTranscript
            )

            val meeting = MeetingEntity(
                id = meetingId,
                workspaceId = input.workspaceId,
                title = input.title,
                meetingDate = input.meetingDate,
                project = input.project,
                description = input.description,
                participantsCsv = input.participantsCsv,
                sourceType = input.sourceType,
                sourceFileName = input.sourceFileName.ifBlank { "Direct Input" },
                processingStatus = "COMPLETED",
                summaryTopics = analysis.summaryTopics,
                summaryDiscussions = analysis.summaryDiscussions,
                summaryConclusions = analysis.summaryConclusions,
                unresolvedIssuesSerialized = analysis.unresolvedIssues.joinToString("\n"),
                importantQuotesSerialized = analysis.importantQuotes.joinToString("\n") { "${it.speaker}|${it.timestamp}|${it.quote}" },
                isDemo = false
            )
            dao.upsertMeeting(meeting)

            val segmentEntities = analysis.segments.mapIndexed { idx, seg ->
                TranscriptSegmentEntity(
                    id = "seg_${meetingId}_$idx",
                    meetingId = meetingId,
                    workspaceId = input.workspaceId,
                    speaker = seg.speaker,
                    timestampLabel = seg.timestamp,
                    text = seg.text,
                    sequenceOrder = idx
                )
            }
            dao.insertTranscriptSegments(segmentEntities)

            val decisionEntities = analysis.decisions.mapIndexed { idx, d ->
                DecisionEntity(
                    id = "dec_${meetingId}_$idx",
                    workspaceId = input.workspaceId,
                    meetingId = meetingId,
                    meetingTitle = input.title,
                    project = input.project,
                    topicKey = d.topicKey,
                    title = d.title,
                    description = d.description,
                    proposer = d.proposer,
                    approver = d.approver,
                    decisionDate = input.meetingDate,
                    status = d.status,
                    isDemo = false
                )
            }
            dao.insertDecisions(decisionEntities)

            updateStep(1, AgentStatus.COMPLETED, "Extracted ${decisionEntities.size} decision(s), ${analysis.tasks.size} task(s)")
            logAgent(input.workspaceId, meetingId, "Meeting Intelligence Agent", "Extract Intelligence", AgentStatus.COMPLETED, "Extracted ${decisionEntities.size} decisions and ${analysis.tasks.size} tasks")

            // 3. Memory Agent
            delay(160)
            val historicalDecisions = dao.getDecisionsSnapshot(input.workspaceId)
                .filter { it.meetingId != meetingId }
            val existingMemories = dao.getMemoriesSnapshot(input.workspaceId)

            val newMemories = decisionEntities.mapIndexed { idx, dec ->
                MeetingMemoryEntity(
                    id = "mem_${meetingId}_$idx",
                    workspaceId = input.workspaceId,
                    meetingId = meetingId,
                    meetingTitle = input.title,
                    project = input.project,
                    memoryType = "PROPOSAL",
                    title = dec.title,
                    content = dec.description,
                    relatedPeopleCsv = input.participantsCsv,
                    tagsCsv = "${input.project},proposal",
                    memoryDate = input.meetingDate
                )
            }
            dao.insertMemories(newMemories)
            updateStep(2, AgentStatus.COMPLETED, "Retrieved ${existingMemories.size} memories, indexed ${newMemories.size}")
            logAgent(input.workspaceId, meetingId, "Memory Agent", "Retrieve & Index Memory", AgentStatus.COMPLETED, "Cross-referenced ${historicalDecisions.size} prior decisions")

            // 4. Conflict Detection Agent
            delay(180)
            val conflicts = aiRepository.detectConflicts(
                newDecisions = analysis.decisions,
                historicalDecisions = historicalDecisions,
                memories = existingMemories
            )
            val conflictEntities = conflicts.mapIndexed { idx, c ->
                ConflictEntity(
                    id = "conf_${meetingId}_$idx",
                    workspaceId = input.workspaceId,
                    meetingId = meetingId,
                    meetingTitle = input.title,
                    project = input.project,
                    previousDecisionId = c.previousDecisionId,
                    previousDecisionText = c.previousDecisionText,
                    newProposalText = c.newProposalText,
                    reason = c.reason,
                    aiRecommendation = c.aiRecommendation,
                    status = ConflictStatus.UNRESOLVED.name
                )
            }
            if (conflictEntities.isNotEmpty()) {
                dao.insertConflicts(conflictEntities)
            }
            updateStep(
                3,
                AgentStatus.COMPLETED,
                if (conflictEntities.isEmpty()) "No contradictions found" else "${conflictEntities.size} conflict(s) detected!"
            )
            logAgent(input.workspaceId, meetingId, "Conflict Detection Agent", "Compare Decisions", AgentStatus.COMPLETED, "Detected ${conflictEntities.size} cross-meeting conflict(s)")

            // 5. Task Execution Agent (Stages Human Approvals + Internal Tasks)
            delay(160)
            val taskEntities = analysis.tasks.mapIndexed { idx, t ->
                TaskEntity(
                    id = "tsk_${meetingId}_$idx",
                    workspaceId = input.workspaceId,
                    meetingId = meetingId,
                    project = input.project,
                    title = t.title,
                    description = t.description,
                    assignee = t.assignee,
                    deadline = t.deadline,
                    priority = t.priority,
                    dependencies = t.dependencies,
                    status = TaskStatus.TODO.name,
                    syncStatus = "Awaiting Approval",
                    isDemo = false
                )
            }
            dao.insertTasks(taskEntities)

            val approvalEntities = taskEntities.mapIndexed { idx, t ->
                ApprovalEntity(
                    id = "apr_${meetingId}_t$idx",
                    workspaceId = input.workspaceId,
                    meetingId = meetingId,
                    meetingTitle = input.title,
                    proposedAction = "Publish Task: ${t.title}",
                    description = "Assign '${t.title}' to ${t.assignee} (Due: ${t.deadline}, Priority: ${t.priority})",
                    responsiblePerson = t.assignee,
                    destination = "Internal Tracker",
                    actionType = "CREATE_TASK",
                    payloadSummary = t.id,
                    status = ApprovalStatus.PENDING.name
                )
            } + decisionEntities.mapIndexed { idx, d ->
                ApprovalEntity(
                    id = "apr_${meetingId}_d$idx",
                    workspaceId = input.workspaceId,
                    meetingId = meetingId,
                    meetingTitle = input.title,
                    proposedAction = "Ratify Decision: ${d.title}",
                    description = d.description,
                    responsiblePerson = d.proposer,
                    destination = "Meeting Memory",
                    actionType = "UPDATE_DECISION",
                    payloadSummary = d.id,
                    status = ApprovalStatus.PENDING.name
                )
            }
            dao.insertApprovals(approvalEntities)
            updateStep(4, AgentStatus.COMPLETED, "Staged ${approvalEntities.size} approval item(s)")
            logAgent(input.workspaceId, meetingId, "Task Execution Agent", "Stage Approvals & Tasks", AgentStatus.COMPLETED, "Created ${taskEntities.size} tasks & ${approvalEntities.size} human approval requests")

            // 6. Reminder Agent
            delay(120)
            val reminders = mutableListOf<ReminderEntity>()
            conflictEntities.forEach { conf ->
                reminders.add(
                    ReminderEntity(
                        id = "rem_${conf.id}",
                        workspaceId = input.workspaceId,
                        taskId = conf.id,
                        title = "Conflict Alert: ${input.title}",
                        message = conf.reason,
                        dueLabel = "Immediate",
                        reminderType = "CONFLICT",
                        isSent = false
                    )
                )
            }
            approvalEntities.firstOrNull()?.let { firstApr ->
                reminders.add(
                    ReminderEntity(
                        id = "rem_apr_${meetingId}",
                        workspaceId = input.workspaceId,
                        taskId = firstApr.id,
                        title = "Human Approval Needed (${approvalEntities.size})",
                        message = "Review proposed tasks and decisions from '${input.title}'.",
                        dueLabel = "Pending",
                        reminderType = "APPROVAL",
                        isSent = false
                    )
                )
            }
            if (reminders.isNotEmpty()) dao.insertReminders(reminders)
            updateStep(5, AgentStatus.COMPLETED, "Queued ${reminders.size} alert(s)")
            logAgent(input.workspaceId, meetingId, "Reminder Agent", "Queue Alerts", AgentStatus.COMPLETED, "Queued ${reminders.size} in-app notifications")

            // 7. Weekly Digest Agent
            delay(120)
            updateStep(6, AgentStatus.COMPLETED, "Workspace digest metrics updated")
            logAgent(input.workspaceId, meetingId, "Weekly Digest Agent", "Refresh Digest", AgentStatus.COMPLETED, "Updated digest summary pool")

            Result.success(meetingId)
        } catch (e: Exception) {
            logAgent(input.workspaceId, meetingId, "Agent Orchestrator", "Pipeline Error", AgentStatus.FAILED, "Failed", e.localizedMessage ?: "Unknown error")
            Result.failure(e)
        }
    }

    private suspend fun logAgent(
        workspaceId: String,
        meetingId: String,
        agentName: String,
        operation: String,
        status: AgentStatus,
        resultSummary: String,
        errorMessage: String = ""
    ) {
        dao.insertAiLog(
            AiActivityLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                workspaceId = workspaceId,
                meetingId = meetingId,
                agentName = agentName,
                operation = operation,
                status = status.name,
                resultSummary = resultSummary,
                errorMessage = errorMessage
            )
        )
    }
}
