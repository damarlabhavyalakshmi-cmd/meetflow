package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.AgentStepState
import com.example.ai.MeetingPipelineInput
import com.example.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class MeetFlowUiState(
    val isReady: Boolean = false,
    val session: SessionState = SessionState(),
    val currentUser: UserProfileEntity? = null,
    val allProfiles: List<UserProfileEntity> = emptyList(),
    val workspaces: List<WorkspaceEntity> = emptyList(),
    val activeWorkspace: WorkspaceEntity? = null,
    val members: List<WorkspaceMemberEntity> = emptyList(),
    val meetings: List<MeetingEntity> = emptyList(),
    val decisions: List<DecisionEntity> = emptyList(),
    val decisionHistory: List<DecisionHistoryEntity> = emptyList(),
    val memories: List<MeetingMemoryEntity> = emptyList(),
    val conflicts: List<ConflictEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val approvals: List<ApprovalEntity> = emptyList(),
    val reminders: List<ReminderEntity> = emptyList(),
    val digests: List<WeeklyDigestEntity> = emptyList(),
    val integrations: List<IntegrationEntity> = emptyList(),
    val aiLogs: List<AiActivityLogEntity> = emptyList(),
    val pipelineSteps: List<AgentStepState> = emptyList(),
    val isProcessingMeeting: Boolean = false,
    val bannerMessage: String? = null,
    val authError: String? = null
) {
    val userRole: UserRole
        get() = UserRole.entries.firstOrNull { it.name == currentUser?.role } ?: UserRole.TEAM_MEMBER

    val canApproveAndReset: Boolean
        get() = userRole == UserRole.ADMIN || userRole == UserRole.MANAGER
}

@OptIn(ExperimentalCoroutinesApi::class)
class MeetFlowViewModel(application: Application) : AndroidViewModel(application) {
    val repository = MeetFlowRepository(application)

    private val _pipelineSteps = MutableStateFlow<List<AgentStepState>>(emptyList())
    private val _isProcessing = MutableStateFlow(false)
    private val _bannerMessage = MutableStateFlow<String?>(null)
    private val _authError = MutableStateFlow<String?>(null)
    private val _isReady = MutableStateFlow(false)

    val aiNoticeMessage: String get() = repository.aiRepository.aiNoticeMessage

    init {
        viewModelScope.launch {
            val currentSession = repository.prefs.sessionFlow.first()
            repository.ensureInitialized(currentSession.demoSeeded)
            _isReady.value = true
        }
    }

    private val sessionFlow = repository.prefs.sessionFlow

    private val coreFlows = combine(
        _isReady,
        sessionFlow,
        repository.observeProfiles(),
        repository.observeWorkspaces()
    ) { ready, session, profiles, workspaces ->
        Quad(ready, session, profiles, workspaces)
    }

    private val workspaceDataFlow = sessionFlow.flatMapLatest { session ->
        val wsId = session.activeWorkspaceId
        val group1 = combine(
            repository.observeMembers(wsId),
            repository.observeMeetings(wsId),
            repository.observeDecisions(wsId),
            repository.observeDecisionHistory(wsId),
            repository.observeMemories(wsId)
        ) { members, meetings, decisions, history, memories ->
            WsGroup1(members, meetings, decisions, history, memories)
        }
        val group2 = combine(
            repository.observeConflicts(wsId),
            repository.observeTasks(wsId),
            repository.observeApprovals(wsId),
            repository.observeReminders(wsId),
            repository.observeWeeklyDigests(wsId)
        ) { conflicts, tasks, approvals, reminders, digests ->
            WsGroup2(conflicts, tasks, approvals, reminders, digests)
        }
        val group3 = combine(
            repository.observeIntegrations(wsId),
            repository.observeAiLogs(wsId)
        ) { integrations, logs ->
            integrations to logs
        }
        combine(group1, group2, group3) { g1, g2, g3 ->
            Triple(g1, g2, g3)
        }
    }

    val uiState: StateFlow<MeetFlowUiState> = combine(
        coreFlows,
        workspaceDataFlow,
        _pipelineSteps,
        _isProcessing,
        combine(_bannerMessage, _authError) { b, a -> b to a }
    ) { core, wsData, steps, processing, msgs ->
        val (ready, session, profiles, workspaces) = core
        val currentUser = profiles.firstOrNull { it.id == session.currentUserId }
        val activeWorkspace = workspaces.firstOrNull { it.id == session.activeWorkspaceId } ?: workspaces.firstOrNull()
        val (g1, g2, g3) = wsData
        MeetFlowUiState(
            isReady = ready,
            session = session,
            currentUser = currentUser,
            allProfiles = profiles,
            workspaces = workspaces,
            activeWorkspace = activeWorkspace,
            members = g1.members,
            meetings = g1.meetings,
            decisions = g1.decisions,
            decisionHistory = g1.history,
            memories = g1.memories,
            conflicts = g2.conflicts,
            tasks = g2.tasks,
            approvals = g2.approvals,
            reminders = g2.reminders,
            digests = g2.digests,
            integrations = g3.first,
            aiLogs = g3.second,
            pipelineSteps = steps,
            isProcessingMeeting = processing,
            bannerMessage = msgs.first,
            authError = msgs.second
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MeetFlowUiState()
    )

    fun observeMeeting(meetingId: String) = repository.observeMeeting(meetingId)
    fun observeTranscript(meetingId: String) = repository.observeTranscriptSegments(meetingId)
    fun observeMeetingDecisions(meetingId: String) = repository.observeDecisionsForMeeting(meetingId)
    fun observeMeetingTasks(meetingId: String) = repository.observeTasksForMeeting(meetingId)
    fun observeMeetingConflicts(meetingId: String) = repository.observeConflictsForMeeting(meetingId)
    fun observeMeetingLogs(meetingId: String) = repository.observeAiLogsForMeeting(meetingId)

    fun clearBanner() { _bannerMessage.value = null }
    fun clearAuthError() { _authError.value = null }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.signInLocal(email, password).onFailure {
                _authError.value = it.message ?: "Sign-in failed"
            }
        }
    }

    fun signUp(name: String, email: String, password: String, role: UserRole) {
        viewModelScope.launch {
            _authError.value = null
            repository.signUpLocal(name, email, password, role).onFailure {
                _authError.value = it.message ?: "Sign-up failed"
            }
        }
    }

    fun resetPassword(email: String, newPassword: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.resetLocalPassword(email, newPassword).onSuccess {
                _bannerMessage.value = "Password updated for $email."
            }.onFailure {
                _authError.value = it.message ?: "Could not reset password"
            }
        }
    }

    fun quickDemoSignIn(userId: String) {
        viewModelScope.launch {
            _authError.value = null
            repository.quickSignInDemoUser(userId)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
        }
    }

    fun switchRole(role: UserRole) {
        val uid = uiState.value.currentUser?.id ?: return
        viewModelScope.launch {
            repository.updateUserRole(uid, role)
            _bannerMessage.value = "Switched role to ${role.displayName}"
        }
    }

    fun switchWorkspace(workspaceId: String) {
        viewModelScope.launch {
            repository.prefs.setActiveWorkspace(workspaceId)
        }
    }

    fun createWorkspace(name: String, description: String) {
        val uid = uiState.value.currentUser?.id ?: return
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createWorkspace(name, description, uid)
            _bannerMessage.value = "Workspace '$name' created"
        }
    }

    fun setDarkMode(override: Boolean?) {
        viewModelScope.launch {
            repository.prefs.setDarkMode(override)
        }
    }

    fun resetDemoData() {
        val wsId = uiState.value.session.activeWorkspaceId
        viewModelScope.launch {
            repository.resetDemoData(wsId)
            _bannerMessage.value = "Hackathon Demo Data reset cleanly (3 meetings, conflict & approvals restored)."
        }
    }

    fun uploadAndAnalyzeMeeting(
        title: String,
        date: String,
        project: String,
        description: String,
        participantsCsv: String,
        sourceType: String,
        sourceFileName: String,
        rawTranscript: String,
        onComplete: (String) -> Unit
    ) {
        if (_isProcessing.value) return
        val wsId = uiState.value.session.activeWorkspaceId
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.processNewMeeting(
                MeetingPipelineInput(
                    workspaceId = wsId,
                    title = title.trim(),
                    meetingDate = date.trim(),
                    project = project.trim().ifBlank { "Website Launch" },
                    description = description.trim(),
                    participantsCsv = participantsCsv.trim().ifBlank { uiState.value.currentUser?.name ?: "Alex Rivera" },
                    sourceType = sourceType,
                    sourceFileName = sourceFileName,
                    rawTranscriptText = rawTranscript
                )
            ) { steps ->
                _pipelineSteps.value = steps
            }
            _isProcessing.value = false
            result.onSuccess { meetingId ->
                _bannerMessage.value = "7-Agent Pipeline completed for '$title'."
                onComplete(meetingId)
            }.onFailure {
                _bannerMessage.value = "Pipeline error: ${it.localizedMessage}"
            }
        }
    }

    fun clearPipelineSteps() {
        _pipelineSteps.value = emptyList()
    }

    fun updateMeetingSummary(meetingId: String, topics: String, discussions: String, conclusions: String) {
        viewModelScope.launch {
            repository.updateMeetingSummary(meetingId, topics, discussions, conclusions)
            _bannerMessage.value = "Meeting summary updated."
        }
    }

    fun updateDecision(decision: DecisionEntity, reason: String) {
        val actor = uiState.value.currentUser?.name ?: "Workspace User"
        viewModelScope.launch {
            repository.updateDecision(decision, actor, reason)
            _bannerMessage.value = "Decision updated and logged in history."
        }
    }

    fun resolveConflict(conflictId: String, action: String) {
        val actor = uiState.value.currentUser?.name ?: "Workspace User"
        viewModelScope.launch {
            repository.resolveConflict(conflictId, action, actor)
            _bannerMessage.value = "Conflict updated ($action)."
        }
    }

    fun decideApproval(
        approvalId: String,
        approve: Boolean,
        editedTitle: String? = null,
        editedDesc: String? = null
    ) {
        val actor = uiState.value.currentUser?.name ?: "Reviewer"
        viewModelScope.launch {
            repository.decideApproval(approvalId, approve, actor, editedTitle, editedDesc)
            _bannerMessage.value = if (approve) "Approved and executed." else "Proposal rejected."
        }
    }

    fun saveTask(
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
    ) {
        val wsId = uiState.value.session.activeWorkspaceId
        viewModelScope.launch {
            repository.saveTask(
                TaskEntity(
                    id = existingId ?: "tsk_${UUID.randomUUID().toString().take(8)}",
                    workspaceId = wsId,
                    meetingId = meetingId.ifBlank { "manual" },
                    project = project.ifBlank { "General" },
                    title = title.trim(),
                    description = description.trim(),
                    assignee = assignee.trim().ifBlank { "Unassigned" },
                    deadline = deadline.trim().ifBlank { "TBD" },
                    priority = priority,
                    dependencies = dependencies.trim().ifBlank { "None" },
                    status = status,
                    syncStatus = "Internal Tracker"
                )
            )
            _bannerMessage.value = if (existingId == null) "Task created." else "Task updated."
        }
    }

    fun updateTaskStatus(task: TaskEntity, newStatus: TaskStatus) {
        viewModelScope.launch {
            repository.saveTask(task.copy(status = newStatus.name))
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            _bannerMessage.value = "Task deleted."
        }
    }

    fun runReminderAgentCheck() {
        val wsId = uiState.value.session.activeWorkspaceId
        viewModelScope.launch {
            val sent = repository.runReminderAgentCheck(wsId)
            _bannerMessage.value = if (sent > 0) {
                "Reminder Agent dispatched $sent Android notification(s)."
            } else {
                "Reminder Agent logged alerts in-app (grant Notification permission for system alerts)."
            }
        }
    }

    fun markRemindersRead() {
        val wsId = uiState.value.session.activeWorkspaceId
        viewModelScope.launch {
            repository.markRemindersRead(wsId)
        }
    }

    fun generateWeeklyDigest(memberName: String) {
        val wsId = uiState.value.session.activeWorkspaceId
        viewModelScope.launch {
            repository.generateDigest(wsId, memberName)
            _bannerMessage.value = "Weekly Digest generated for $memberName."
        }
    }

    fun saveIntegrationConfig(integration: IntegrationEntity, detail: String) {
        viewModelScope.launch {
            repository.configureIntegration(integration, detail)
            _bannerMessage.value = "${integration.provider} credentials saved locally (OAuth verification required for live sync)."
        }
    }

    fun disconnectIntegration(integration: IntegrationEntity) {
        viewModelScope.launch {
            repository.disconnectIntegration(integration)
            _bannerMessage.value = "${integration.provider} disconnected."
        }
    }
}

private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private data class WsGroup1(
    val members: List<WorkspaceMemberEntity>,
    val meetings: List<MeetingEntity>,
    val decisions: List<DecisionEntity>,
    val history: List<DecisionHistoryEntity>,
    val memories: List<MeetingMemoryEntity>
)
private data class WsGroup2(
    val conflicts: List<ConflictEntity>,
    val tasks: List<TaskEntity>,
    val approvals: List<ApprovalEntity>,
    val reminders: List<ReminderEntity>,
    val digests: List<WeeklyDigestEntity>
)
