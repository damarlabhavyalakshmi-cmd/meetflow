package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetFlowDao {

    // Profiles & Workspaces
    @Query("SELECT * FROM profiles ORDER BY createdAt ASC")
    fun observeProfiles(): Flow<List<UserProfileEntity>>

    @Query("SELECT * FROM profiles WHERE email = :email LIMIT 1")
    suspend fun getProfileByEmail(email: String): UserProfileEntity?

    @Query("SELECT * FROM profiles WHERE id = :id LIMIT 1")
    suspend fun getProfileById(id: String): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM workspaces ORDER BY createdAt ASC")
    fun observeWorkspaces(): Flow<List<WorkspaceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkspace(workspace: WorkspaceEntity)

    @Query("SELECT * FROM workspace_members WHERE workspaceId = :workspaceId")
    fun observeWorkspaceMembers(workspaceId: String): Flow<List<WorkspaceMemberEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWorkspaceMembers(members: List<WorkspaceMemberEntity>)

    // Meetings & Transcripts
    @Query("SELECT * FROM meetings WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun observeMeetings(workspaceId: String): Flow<List<MeetingEntity>>

    @Query("SELECT * FROM meetings WHERE id = :meetingId LIMIT 1")
    fun observeMeeting(meetingId: String): Flow<MeetingEntity?>

    @Query("SELECT * FROM meetings WHERE id = :meetingId LIMIT 1")
    suspend fun getMeetingById(meetingId: String): MeetingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMeeting(meeting: MeetingEntity)

    @Query("SELECT * FROM transcript_segments WHERE meetingId = :meetingId ORDER BY sequenceOrder ASC")
    fun observeTranscriptSegments(meetingId: String): Flow<List<TranscriptSegmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscriptSegments(segments: List<TranscriptSegmentEntity>)

    // Decisions & History
    @Query("SELECT * FROM decisions WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun observeDecisions(workspaceId: String): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decisions WHERE meetingId = :meetingId ORDER BY createdAt DESC")
    fun observeDecisionsForMeeting(meetingId: String): Flow<List<DecisionEntity>>

    @Query("SELECT * FROM decisions WHERE workspaceId = :workspaceId")
    suspend fun getDecisionsSnapshot(workspaceId: String): List<DecisionEntity>

    @Query("SELECT * FROM decisions WHERE id = :decisionId LIMIT 1")
    suspend fun getDecisionById(decisionId: String): DecisionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDecision(decision: DecisionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecisions(decisions: List<DecisionEntity>)

    @Query("SELECT * FROM decision_history WHERE workspaceId = :workspaceId ORDER BY changedAt DESC")
    fun observeDecisionHistory(workspaceId: String): Flow<List<DecisionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDecisionHistory(history: DecisionHistoryEntity)

    // Meeting Memories
    @Query("SELECT * FROM meeting_memories WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun observeMemories(workspaceId: String): Flow<List<MeetingMemoryEntity>>

    @Query("SELECT * FROM meeting_memories WHERE workspaceId = :workspaceId")
    suspend fun getMemoriesSnapshot(workspaceId: String): List<MeetingMemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemories(memories: List<MeetingMemoryEntity>)

    // Conflicts
    @Query("SELECT * FROM conflicts WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun observeConflicts(workspaceId: String): Flow<List<ConflictEntity>>

    @Query("SELECT * FROM conflicts WHERE meetingId = :meetingId ORDER BY createdAt DESC")
    fun observeConflictsForMeeting(meetingId: String): Flow<List<ConflictEntity>>

    @Query("SELECT * FROM conflicts WHERE id = :conflictId LIMIT 1")
    suspend fun getConflictById(conflictId: String): ConflictEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConflicts(conflicts: List<ConflictEntity>)

    @Update
    suspend fun updateConflict(conflict: ConflictEntity)

    // Tasks
    @Query("SELECT * FROM tasks WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun observeTasks(workspaceId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE meetingId = :meetingId ORDER BY createdAt DESC")
    fun observeTasksForMeeting(meetingId: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE workspaceId = :workspaceId")
    suspend fun getTasksSnapshot(workspaceId: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    // Approvals
    @Query("SELECT * FROM approvals WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun observeApprovals(workspaceId: String): Flow<List<ApprovalEntity>>

    @Query("SELECT * FROM approvals WHERE id = :approvalId LIMIT 1")
    suspend fun getApprovalById(approvalId: String): ApprovalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertApproval(approval: ApprovalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApprovals(approvals: List<ApprovalEntity>)

    // Reminders & Notifications
    @Query("SELECT * FROM reminders WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun observeReminders(workspaceId: String): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(reminders: List<ReminderEntity>)

    @Query("UPDATE reminders SET isRead = 1 WHERE workspaceId = :workspaceId")
    suspend fun markAllRemindersRead(workspaceId: String)

    // Weekly Digests
    @Query("SELECT * FROM weekly_digests WHERE workspaceId = :workspaceId ORDER BY createdAt DESC")
    fun observeWeeklyDigests(workspaceId: String): Flow<List<WeeklyDigestEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDigest(digest: WeeklyDigestEntity)

    // Integrations
    @Query("SELECT * FROM integrations WHERE workspaceId = :workspaceId ORDER BY provider ASC")
    fun observeIntegrations(workspaceId: String): Flow<List<IntegrationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIntegrations(integrations: List<IntegrationEntity>)

    @Update
    suspend fun updateIntegration(integration: IntegrationEntity)

    // AI Activity Logs
    @Query("SELECT * FROM ai_activity_logs WHERE workspaceId = :workspaceId ORDER BY timestamp DESC")
    fun observeAiActivityLogs(workspaceId: String): Flow<List<AiActivityLogEntity>>

    @Query("SELECT * FROM ai_activity_logs WHERE meetingId = :meetingId ORDER BY timestamp ASC")
    fun observeAiLogsForMeeting(meetingId: String): Flow<List<AiActivityLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiLog(log: AiActivityLogEntity)

    // Reset Workspace Data (for Hackathon Demo Reset)
    @Query("DELETE FROM meetings WHERE workspaceId = :workspaceId")
    suspend fun clearMeetings(workspaceId: String)

    @Query("DELETE FROM transcript_segments WHERE workspaceId = :workspaceId")
    suspend fun clearTranscripts(workspaceId: String)

    @Query("DELETE FROM decisions WHERE workspaceId = :workspaceId")
    suspend fun clearDecisions(workspaceId: String)

    @Query("DELETE FROM decision_history WHERE workspaceId = :workspaceId")
    suspend fun clearDecisionHistory(workspaceId: String)

    @Query("DELETE FROM meeting_memories WHERE workspaceId = :workspaceId")
    suspend fun clearMemories(workspaceId: String)

    @Query("DELETE FROM conflicts WHERE workspaceId = :workspaceId")
    suspend fun clearConflicts(workspaceId: String)

    @Query("DELETE FROM tasks WHERE workspaceId = :workspaceId")
    suspend fun clearTasks(workspaceId: String)

    @Query("DELETE FROM approvals WHERE workspaceId = :workspaceId")
    suspend fun clearApprovals(workspaceId: String)

    @Query("DELETE FROM reminders WHERE workspaceId = :workspaceId")
    suspend fun clearReminders(workspaceId: String)

    @Query("DELETE FROM weekly_digests WHERE workspaceId = :workspaceId")
    suspend fun clearDigests(workspaceId: String)

    @Query("DELETE FROM ai_activity_logs WHERE workspaceId = :workspaceId")
    suspend fun clearAiLogs(workspaceId: String)
}
