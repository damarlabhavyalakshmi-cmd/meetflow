package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        WorkspaceEntity::class,
        WorkspaceMemberEntity::class,
        MeetingEntity::class,
        TranscriptSegmentEntity::class,
        DecisionEntity::class,
        DecisionHistoryEntity::class,
        MeetingMemoryEntity::class,
        ConflictEntity::class,
        TaskEntity::class,
        ApprovalEntity::class,
        ReminderEntity::class,
        WeeklyDigestEntity::class,
        IntegrationEntity::class,
        AiActivityLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MeetFlowDatabase : RoomDatabase() {
    abstract fun dao(): MeetFlowDao

    companion object {
        @Volatile
        private var INSTANCE: MeetFlowDatabase? = null

        fun getInstance(context: Context): MeetFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MeetFlowDatabase::class.java,
                    "meetflow_ai.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
