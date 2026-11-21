package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole(val displayName: String) {
    ADMIN("Admin"),
    MANAGER("Manager"),
    TEAM_MEMBER("Team Member")
}

enum class DecisionStatus(val label: String) {
    PROPOSED("Proposed"),
    APPROVED("Approved"),
    REJECTED("Rejected"),
    SUPERSEDED("Superseded")
}

enum class TaskStatus(val label: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    OVERDUE("Overdue")
}

enum class TaskPriority(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low")
}

enum class ConflictStatus(val label: String) {
    UNRESOLVED("Unresolved"),
    CLARIFICATION_REQUESTED("Clarification Requested"),
    RESOLVED("Resolved")
}

enum class ApprovalStatus(val label: String) {
    PENDING("Pending"),
    APPROVED("Approved"),
    REJECTED("Rejected")
}

enum class AgentStatus(val label: String) {
    RUNNING("Running"),
    COMPLETED("Completed"),
    FAILED("Failed")
}

@Entity(tableName = "profiles")
data class UserProfileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: String = UserRole.ADMIN.name,
    val avatarInitials: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workspaces")
data class WorkspaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val ownerId: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "workspace_members")
data class WorkspaceMemberEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val userId: String,
    val displayName: String,
    val email: String,
    val role: String
)

@Entity(tableName = "meetings")
data class MeetingEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val title: String,
    val meetingDate: String,
    val project: String,
    val description: String,
    val participantsCsv: String,
    val sourceType: String,
    val sourceFileName: String,
    val processingStatus: String,
    val summaryTopics: String,
    val summaryDiscussions: String,
    val summaryConclusions: String,
    val unresolvedIssuesSerialized: String,
    val importantQuotesSerialized: String,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "transcript_segments")
data class TranscriptSegmentEntity(
    @PrimaryKey val id: String,
    val meetingId: String,
    val workspaceId: String,
    val speaker: String,
    val timestampLabel: String,
    val text: String,
    val sequenceOrder: Int
)

@Entity(tableName = "decisions")
data class DecisionEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val meetingId: String,
    val meetingTitle: String,
    val project: String,
    val topicKey: String,
    val title: String,
    val description: String,
    val proposer: String,
    val approver: String,
    val decisionDate: String,
    val status: String,
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "decision_history")
data class DecisionHistoryEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val decisionId: String,
    val previousValue: String,
    val newValue: String,
    val changedBy: String,
    val reason: String,
    val meetingId: String,
    val changedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "meeting_memories")
data class MeetingMemoryEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val meetingId: String,
    val meetingTitle: String,
    val project: String,
    val memoryType: String,
    val title: String,
    val content: String,
    val relatedPeopleCsv: String,
    val tagsCsv: String,
    val memoryDate: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "conflicts")
data class ConflictEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val meetingId: String,
    val meetingTitle: String,
    val project: String,
    val previousDecisionId: String,
    val previousDecisionText: String,
    val newProposalText: String,
    val reason: String,
    val aiRecommendation: String,
    val status: String = ConflictStatus.UNRESOLVED.name,
    val resolutionAction: String = "",
    val resolvedBy: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val meetingId: String,
    val project: String,
    val title: String,
    val description: String,
    val assignee: String,
    val deadline: String,
    val priority: String,
    val dependencies: String,
    val status: String,
    val syncStatus: String = "Internal Tracker",
    val isDemo: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "approvals")
data class ApprovalEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val meetingId: String,
    val meetingTitle: String,
    val proposedAction: String,
    val description: String,
    val responsiblePerson: String,
    val destination: String,
    val actionType: String,
    val payloadSummary: String,
    val status: String = ApprovalStatus.PENDING.name,
    val reviewedBy: String = "",
    val executionResult: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val reviewedAt: Long = 0L
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val taskId: String,
    val title: String,
    val message: String,
    val dueLabel: String,
    val reminderType: String,
    val isSent: Boolean = false,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "weekly_digests")
data class WeeklyDigestEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val memberName: String,
    val weekRange: String,
    val assignedCount: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val overdueCount: Int,
    val upcomingDeadlinesSummary: String,
    val relevantDecisionsSummary: String,
    val meetingUpdatesSummary: String,
    val aiProgressSummary: String,
    val isSampleAi: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "integrations")
data class IntegrationEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val provider: String,
    val category: String,
    val description: String,
    val status: String,
    val configDetail: String,
    val requiredNote: String,
    val lastCheckedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_activity_logs")
data class AiActivityLogEntity(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val meetingId: String,
    val agentName: String,
    val operation: String,
    val status: String,
    val resultSummary: String,
    val errorMessage: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
