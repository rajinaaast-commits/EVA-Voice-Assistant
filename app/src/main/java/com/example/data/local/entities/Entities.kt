package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversations")
data class ConversationEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val conversationId: String,
    val sender: String, // "user" or "eva"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val providerUsed: String = "Gemini", // "Gemini" or "OmniRoute"
    val toolCallInfo: String? = null,
    val attachmentPath: String? = null
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val category: String = "General",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val timestamp: Long,
    val isCompleted: Boolean = false,
    val isRecurring: Boolean = false,
    val recurrencePattern: String? = null
)

@Entity(tableName = "study_tasks")
data class StudyTaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String,
    val task: String,
    val isCompleted: Boolean = false,
    val dueDate: Long = System.currentTimeMillis() + 86400000,
    val notes: String = ""
)

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val content: String,
    val format: String = "TXT", // TXT, PDF, DOC
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val htmlCode: String = "<!DOCTYPE html>\n<html>\n<head>\n<title>EVA Site</title>\n</head>\n<body>\n<h1>Hello from EVA AI</h1>\n</body>\n</html>",
    val cssCode: String = "body { background: #0A0F24; color: #fff; font-family: sans-serif; }",
    val jsCode: String = "console.log('EVA Powered Website');",
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "voice_profiles")
data class VoiceProfileEntity(
    @PrimaryKey
    val id: String = "default_user",
    val userName: String = "User",
    val sampleCount: Int = 0,
    val averagePitchHz: Float = 140f,
    val energyProfileJson: String = "[]",
    val isTrained: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
