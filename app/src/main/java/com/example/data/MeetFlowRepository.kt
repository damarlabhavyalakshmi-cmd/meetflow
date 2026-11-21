package com.example.data

import android.content.Context
import com.example.ai.AgentOrchestrator
import com.example.ai.AgentStepState
import com.example.ai.AiRepository
import com.example.ai.MeetingPipelineInput
import com.example.ai.SampleAiRepository
import com.example.notifications.ReminderNotificationHelper
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class MeetFlowRepository(
    private val context: Context,
    private val db: MeetFlowDatabase = MeetFlowDatabase.getInstance(context),
    val prefs: SessionPreferences = SessionPreferences(context),
    val aiRepository: AiRepository = SampleAiRepository()
) {
    private val dao = db.dao()
    private val seeder = DemoDataSeeder(dao)
    private val orchestrator = AgentOrchestrator(dao, aiRepository)

    suspend fun ensureInitialized(alreadySeeded: Boolean) {
        if (!alreadySeeded) {
            seeder.seedInitialWorkspaceAndUser()
            prefs.markDemoSeeded()
        }
    }

    suspend fun resetDemoData(workspaceId: String) {
        seeder.seedIntegrations(workspaceId)
        seeder.resetDemoData(workspaceId)
    }

    // Reactive Streams
    fun observeProfiles(): Flow<List<UserProfileEntity>> = dao.observeProfiles()
    fun observeWorkspaces(): Flow<List<WorkspaceEntity>> = dao.observeWorkspaces()
    fun observeMembers(workspaceId: String): Flow<List<WorkspaceMemberEntity>> = dao.observeWorkspaceMembers(workspaceId)
    fun observeMeetings(workspaceId: String): Flow<List<MeetingEntity>> = dao.observeMeetings(workspaceId)
    fun observeMeeting(meetingId: String): Flow<MeetingEntity?> = dao.observeMeeting(meetingId)
    fun observeTranscriptSegments(meetingId: String): Flow<List<TranscriptSegmentEntity>> = dao.observeTranscriptSegments(meetingId)
    fun observeDecisions(workspaceId: String): Flow<List<DecisionEntity>> = dao.observeDecisions(workspaceId)
    fun observeDecisionsForMeeting(meetingId: String): Flow<List<DecisionEntity>> = dao.observeDecisionsForMeeting(meetingId)
    fun observeDecisionHistory(workspaceId: String): Flow<List<DecisionHistoryEntity>> = dao.observeDecisionHistory(workspaceId)
    fun observeMemories(workspaceId: String): Flow<List<MeetingMemoryEntity>> = dao.observeMemories(workspaceId)
    fun observeConflicts(workspaceId: String): Flow<List<ConflictEntity>> = dao.observeConflicts(workspaceId)
    fun observeConflictsForMeeting(meetingId: String): Flow<List<ConflictEntity>> = dao.observeConflictsForMeeting(meetingId)
    fun observeTasks(workspaceId: String): Flow<List<TaskEntity>> = dao.observeTasks(workspaceId)
    fun observeTasksForMeeting(meetingId: String): Flow<List<TaskEntity>> = dao.observeTasksForMeeting(meetingId)
    fun observeApprovals(workspaceId: String): Flow<List<ApprovalEntity>> = dao.observeApprovals(workspaceId)
    fun observeReminders(workspaceId: String): Flow<List<ReminderEntity>> = dao.observeReminders(workspaceId)
    fun observeWeeklyDigests(workspaceId: String): Flow<List<WeeklyDigestEntity>> = dao.observeWeeklyDigests(workspaceId)
    fun observeIntegrations(workspaceId: String): Flow<List<IntegrationEntity>> = dao.observeIntegrations(workspaceId)
    fun observeAiLogs(workspaceId: String): Flow<List<AiActivityLogEntity>> = dao.observeAiActivityLogs(workspaceId)
    fun observeAiLogsForMeeting(meetingId: String): Flow<List<AiActivityLogEntity>> = dao.observeAiLogsForMeeting(meetingId)

    // Auth & Workspaces
    suspend fun signInLocal(email: String, password: String): Result<UserProfileEntity> {
        val profile = dao.getProfileByEmail(email.trim().lowercase())
            ?: return Result.failure(IllegalArgumentException("No local workspace account found for $email"))
        if (profile.passwordHash != password.hashCode().toString()) {
            return Result.failure(IllegalArgumentException("Incorrect password for $email"))
        }
        prefs.setCurrentUser(profile.id)
        return Result.success(profile)
    }

    suspend fun signUpLocal(name: String, email: String, password: String, role: UserRole): Result<UserProfileEntity> {
        val normalizedEmail = email.trim().lowercase()
        if (name.isBlank() || !normalizedEmail.contains("@") || password.length < 4) {
            return Result.failure(IllegalArgumentException("Enter valid name, email, and password (4+ chars)."))
        }
        if (dao.getProfileByEmail(normalizedEmail) != null) {
            return Result.failure(IllegalArgumentException("An account with $normalizedEmail already exists."))
        }
        val initials = name.trim().split(" ").filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }.ifBlank { "MF" }
        val profile = UserProfileEntity(
            id = "usr_${UUID.randomUUID().toString().take(8)}",
            name = name.trim(),
            email = normalizedEmail,
            passwordHash = password.hashCode().toString(),
            role = role.name,
            avatarInitials = initials
        )
        dao.upsertProfile(profile)
        dao.upsertWorkspaceMembers(
            listOf(
                WorkspaceMemberEntity(
                    id = "wm_${UUID.randomUUID().toString().take(8)}",
                    workspaceId = "ws_core_product",
                    userId = profile.id,
                    displayName = profile.name,
                    email = profile.email,
                    role = role.name
                )
            )
        )
        prefs.setCurrentUser(profile.id)
        return Result.success(profile)
    }

    suspend fun resetLocalPassword(email: String, newPassword: String): Result<Unit> {
        val profile = dao.getProfileByEmail(email.trim().lowercase())
            ?: return Result.failure(IllegalArgumentException("Account not found for $email"))
        if (newPassword.length < 4) {
            return Result.failure(IllegalArgumentException("Password must be at least 4 characters."))
        }
        dao.upsertProfile(profile.copy(passwordHash = newPassword.hashCode().toString()))
        return Result.success(Unit)
    }

    suspend fun quickSignInDemoUser(userId: String) {
        prefs.setCurrentUser(userId)
    }

    suspend fun updateUserRole(userId: String, newRole: UserRole) {
        val profile = dao.getProfileById(userId) ?: return
        dao.upsertProfile(profile.copy(role = newRole.name))
    }

    suspend fun signOut() {
        prefs.setCurrentUser(null)
    }

    suspend fun createWorkspace(name: String, description: String, ownerId: String): String {
        val wsId = "ws_${UUID.randomUUID().toString().take(8)}"
        val workspace = WorkspaceEntity(
            id = wsId,
            name = name.trim(),
            description = description.trim().ifBlank { "Team Workspace" },
            ownerId = ownerId
        )
        dao.upsertWorkspace(workspace)
        seeder.seedIntegrations(wsId)
        prefs.setActiveWorkspace(wsId)
        return wsId
    }

    // Meeting Pipeline
    suspend fun processNewMeeting(
        input: MeetingPipelineInput,
        onProgress: (List<AgentStepState>) -> Unit
    ): Result<String> {
        return orchestrator.runMeetingPipeline(input, onProgress)
    }

    suspend fun updateMeetingSummary(
        meetingId: String,
        topics: String,
        discussions: String,
        conclusions: String
    ) {
        val existing = dao.getMeetingById(meetingId) ?: return
        dao.upsertMeeting(
            existing.copy(
                summaryTopics = topics,
                summaryDiscussions = discussions,
                summaryConclusions = conclusions
            )
        )
    }

    // Decisions
    suspend fun updateDecision(decision: DecisionEntity, updatedBy: String, reason: String) {
        val prev = dao.getDecisionById(decision.id)
        dao.upsertDecision(decision)
        if (prev != null && (prev.title != decision.title || prev.status != decision.status)) {
            dao.insertDecisionHistory(
                DecisionHistoryEntity(
                    id = "dh_${UUID.randomUUID().toString().take(8)}",
                    workspaceId = decision.workspaceId,
                    decisionId = decision.id,
                    previousValue = "${prev.title} (${prev.status})",
                    newValue = "${decision.title} (${decision.status})",
                    changedBy = updatedBy,
                    reason = reason,
                    meetingId = decision.meetingId
                )
            )
        }
    }

    // Conflict Resolution
    suspend fun resolveConflict(
        conflictId: String,
        action: String, // ACCEPT_NEW, KEEP_PREVIOUS, REQUEST_CLARIFICATION, MARK_RESOLVED
        actorName: String
    ) {
        val conflict = dao.getConflictById(conflictId) ?: return
        val prevDecision = dao.getDecisionById(conflict.previousDecisionId)

        when (action) {
            "ACCEPT_NEW" -> {
                dao.updateConflict(
                    conflict.copy(
                        status = ConflictStatus.RESOLVED.name,
                        resolutionAction = "Accepted New Proposal",
                        resolvedBy = actorName
                    )
                )
                if (prevDecision != null) {
                    dao.upsertDecision(prevDecision.copy(status = DecisionStatus.SUPERSEDED.name))
                    dao.insertDecisionHistory(
                        DecisionHistoryEntity(
                            id = "dh_${UUID.randomUUID().toString().take(8)}",
                            workspaceId = conflict.workspaceId,
                            decisionId = prevDecision.id,
                            previousValue = prevDecision.title,
                            newValue = conflict.newProposalText,
                            changedBy = actorName,
                            reason = "Conflict resolved: Accepted new proposal from ${conflict.meetingTitle}",
                            meetingId = conflict.meetingId
                        )
                    )
                }
                dao.insertMemories(
                    listOf(
                        MeetingMemoryEntity(
                            id = "mem_res_${UUID.randomUUID().toString().take(8)}",
                            workspaceId = conflict.workspaceId,
                            meetingId = conflict.meetingId,
                            meetingTitle = conflict.meetingTitle,
                            project = conflict.project,
                            memoryType = "CHANGE",
                            title = "Approved Change: ${conflict.newProposalText.take(60)}",
                            content = "Resolved conflict by $actorName: Superseded '${conflict.previousDecisionText}'.",
                            relatedPeopleCsv = actorName,
                            tagsCsv = "conflict-resolved,change",
                            memoryDate = "2026-11-21"
                        )
                    )
                )
            }
            "KEEP_PREVIOUS" -> {
                dao.updateConflict(
                    conflict.copy(
                        status = ConflictStatus.RESOLVED.name,
                        resolutionAction = "Kept Previous Canonical Decision",
                        resolvedBy = actorName
                    )
                )
            }
            "REQUEST_CLARIFICATION" -> {
                dao.updateConflict(
                    conflict.copy(
                        status = ConflictStatus.CLARIFICATION_REQUESTED.name,
                        resolutionAction = "Clarification Requested",
                        resolvedBy = actorName
                    )
                )
                dao.upsertTask(
                    TaskEntity(
                        id = "tsk_clar_${UUID.randomUUID().toString().take(8)}",
                        workspaceId = conflict.workspaceId,
                        meetingId = conflict.meetingId,
                        project = conflict.project,
                        title = "Clarify conflict: ${conflict.project}",
                        description = "Clarify between '${conflict.previousDecisionText}' and '${conflict.newProposalText}'.",
                        assignee = actorName,
                        deadline = "Next Sync",
                        priority = TaskPriority.HIGH.name,
                        dependencies = "Stakeholder confirmation",
                        status = TaskStatus.TODO.name,
                        syncStatus = "Internal Tracker"
                    )
                )
            }
            "MARK_RESOLVED" -> {
                dao.updateConflict(
                    conflict.copy(
                        status = ConflictStatus.RESOLVED.name,
                        resolutionAction = "Marked Resolved",
                        resolvedBy = actorName
                    )
                )
            }
        }

        dao.insertAiLog(
            AiActivityLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                workspaceId = conflict.workspaceId,
                meetingId = conflict.meetingId,
                agentName = "Conflict Detection Agent",
                operation = "Resolution Recorded",
                status = AgentStatus.COMPLETED.name,
                resultSummary = "$actorName executed $action on conflict ${conflict.id}"
            )
        )
    }

    // Approvals (Human-in-the-loop Execution)
    suspend fun decideApproval(
        approvalId: String,
        approve: Boolean,
        reviewerName: String,
        editedTitle: String? = null,
        editedDescription: String? = null
    ) {
        val item = dao.getApprovalById(approvalId) ?: return
        val updatedAction = editedTitle?.takeIf { it.isNotBlank() } ?: item.proposedAction
        val updatedDesc = editedDescription?.takeIf { it.isNotBlank() } ?: item.description

        if (approve) {
            val execResult = when (item.actionType) {
                "CREATE_TASK" -> {
                    val tasks = dao.getTasksSnapshot(item.workspaceId)
                    val linked = tasks.firstOrNull { it.id == item.payloadSummary }
                    if (linked != null) {
                        dao.upsertTask(linked.copy(syncStatus = "Approved (${item.destination})"))
                    }
                    "Executed: Task activated and logged in ${item.destination}."
                }
                "UPDATE_DECISION" -> {
                    val dec = dao.getDecisionById(item.payloadSummary)
                    if (dec != null) {
                        dao.upsertDecision(
                            dec.copy(
                                status = DecisionStatus.APPROVED.name,
                                approver = reviewerName
                            )
                        )
                        dao.insertDecisionHistory(
                            DecisionHistoryEntity(
                                id = "dh_${UUID.randomUUID().toString().take(8)}",
                                workspaceId = item.workspaceId,
                                decisionId = dec.id,
                                previousValue = "${dec.title} (PROPOSED)",
                                newValue = "${dec.title} (APPROVED)",
                                changedBy = reviewerName,
                                reason = "Ratified via Human Approval Center",
                                meetingId = dec.meetingId
                            )
                        )
                    }
                    "Executed: Decision ratified and stored in Meeting Memory."
                }
                else -> "Executed: Approved by $reviewerName."
            }

            dao.upsertApproval(
                item.copy(
                    proposedAction = updatedAction,
                    description = updatedDesc,
                    status = ApprovalStatus.APPROVED.name,
                    reviewedBy = reviewerName,
                    executionResult = execResult,
                    reviewedAt = System.currentTimeMillis()
                )
            )
            dao.insertAiLog(
                AiActivityLogEntity(
                    id = "log_${UUID.randomUUID().toString().take(8)}",
                    workspaceId = item.workspaceId,
                    meetingId = item.meetingId,
                    agentName = "Task Execution Agent",
                    operation = "Execute Approved Action",
                    status = AgentStatus.COMPLETED.name,
                    resultSummary = "$updatedAction -> $execResult"
                )
            )
        } else {
            dao.upsertApproval(
                item.copy(
                    proposedAction = updatedAction,
                    description = updatedDesc,
                    status = ApprovalStatus.REJECTED.name,
                    reviewedBy = reviewerName,
                    executionResult = "Rejected by $reviewerName — no external execution performed.",
                    reviewedAt = System.currentTimeMillis()
                )
            )
        }
    }

    // Tasks
    suspend fun saveTask(task: TaskEntity) {
        dao.upsertTask(task)
    }

    suspend fun deleteTask(taskId: String) {
        dao.deleteTaskById(taskId)
    }

    // Reminders & Real Local Notifications
    suspend fun runReminderAgentCheck(workspaceId: String): Int {
        val tasks = dao.getTasksSnapshot(workspaceId)
        val overdueOrDue = tasks.filter { it.status == TaskStatus.OVERDUE.name || it.status == TaskStatus.TODO.name }
        var sentCount = 0

        overdueOrDue.take(3).forEachIndexed { idx, task ->
            val title = if (task.status == TaskStatus.OVERDUE.name) {
                "Overdue Task: ${task.title}"
            } else {
                "Deadline Reminder: ${task.title}"
            }
            val msg = "Assigned to ${task.assignee} • Due: ${task.deadline} (${task.project})"
            val actuallySent = ReminderNotificationHelper.postNotification(
                context = context,
                id = 2000 + idx,
                title = title,
                message = msg
            )
            if (actuallySent) sentCount++
            dao.insertReminder(
                ReminderEntity(
                    id = "rem_${UUID.randomUUID().toString().take(8)}",
                    workspaceId = workspaceId,
                    taskId = task.id,
                    title = title,
                    message = msg,
                    dueLabel = task.deadline,
                    reminderType = if (task.status == TaskStatus.OVERDUE.name) "OVERDUE" else "DEADLINE",
                    isSent = actuallySent,
                    isRead = false
                )
            )
        }

        dao.insertAiLog(
            AiActivityLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                workspaceId = workspaceId,
                meetingId = "workspace",
                agentName = "Reminder Agent",
                operation = "Scan Deadlines & Dispatch",
                status = AgentStatus.COMPLETED.name,
                resultSummary = "Evaluated ${overdueOrDue.size} tasks; dispatched $sentCount system notification(s)."
            )
        )
        return sentCount
    }

    suspend fun markRemindersRead(workspaceId: String) {
        dao.markAllRemindersRead(workspaceId)
    }

    // Weekly Digest
    suspend fun generateDigest(workspaceId: String, memberFilter: String): WeeklyDigestEntity {
        val allTasks = dao.getTasksSnapshot(workspaceId)
        val filteredTasks = if (memberFilter == "All Team Members") {
            allTasks
        } else {
            allTasks.filter { it.assignee.contains(memberFilter, ignoreCase = true) }
        }
        val decisions = dao.getDecisionsSnapshot(workspaceId)
        val summary = aiRepository.generateWeeklyDigestSummary(
            memberName = memberFilter,
            tasks = filteredTasks,
            decisions = decisions,
            unresolvedConflictCount = 0
        )

        val digest = WeeklyDigestEntity(
            id = "dig_${UUID.randomUUID().toString().take(8)}",
            workspaceId = workspaceId,
            memberName = memberFilter,
            weekRange = "Nov 15 – Nov 21, 2026",
            assignedCount = filteredTasks.size,
            completedCount = filteredTasks.count { it.status == TaskStatus.COMPLETED.name },
            pendingCount = filteredTasks.count { it.status == TaskStatus.TODO.name || it.status == TaskStatus.IN_PROGRESS.name },
            overdueCount = filteredTasks.count { it.status == TaskStatus.OVERDUE.name },
            upcomingDeadlinesSummary = filteredTasks.filter { it.status != TaskStatus.COMPLETED.name }
                .take(4)
                .joinToString("\n") { "• ${it.title} (${it.assignee}) — ${it.deadline}" }
                .ifBlank { "No pending deadlines." },
            relevantDecisionsSummary = decisions.take(4)
                .joinToString("\n") { "• ${it.title} [${it.status}]" }
                .ifBlank { "No decisions logged yet." },
            meetingUpdatesSummary = "Compiled from ${decisions.map { it.meetingId }.distinct().size} active meeting(s) in workspace.",
            aiProgressSummary = summary,
            isSampleAi = !aiRepository.isLiveGeminiConnected
        )
        dao.upsertDigest(digest)
        dao.insertAiLog(
            AiActivityLogEntity(
                id = "log_${UUID.randomUUID().toString().take(8)}",
                workspaceId = workspaceId,
                meetingId = "digest",
                agentName = "Weekly Digest Agent",
                operation = "Generate Report ($memberFilter)",
                status = AgentStatus.COMPLETED.name,
                resultSummary = "Compiled digest for $memberFilter (${filteredTasks.size} tasks)"
            )
        )
        return digest
    }

    // Integrations (Honest Configuration)
    suspend fun configureIntegration(integration: IntegrationEntity, configDetail: String) {
        val trimmed = configDetail.trim()
        val updated = if (trimmed.isBlank()) {
            integration.copy(
                status = "DISCONNECTED",
                configDetail = "",
                lastCheckedAt = System.currentTimeMillis()
            )
        } else {
            integration.copy(
                status = "CONFIG_SAVED_PENDING_OAUTH",
                configDetail = trimmed,
                lastCheckedAt = System.currentTimeMillis()
            )
        }
        dao.updateIntegration(updated)
    }

    suspend fun disconnectIntegration(integration: IntegrationEntity) {
        dao.updateIntegration(
            integration.copy(
                status = "DISCONNECTED",
                configDetail = "",
                lastCheckedAt = System.currentTimeMillis()
            )
        )
    }
}
