package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.daos.*
import com.example.data.local.entities.*

@Database(
    entities = [
        ConversationEntity::class,
        MessageEntity::class,
        MemoryEntity::class,
        ReminderEntity::class,
        StudyTaskEntity::class,
        DocumentEntity::class,
        ProjectEntity::class,
        VoiceProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class EvaDatabase : RoomDatabase() {
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun memoryDao(): MemoryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun documentDao(): DocumentDao
    abstract fun projectDao(): ProjectDao
    abstract fun voiceProfileDao(): VoiceProfileDao

    companion object {
        @Volatile
        private var INSTANCE: EvaDatabase? = null

        fun getDatabase(context: Context): EvaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EvaDatabase::class.java,
                    "eva_ai_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
