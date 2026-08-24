package com.example.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "detected_statuses")
data class DetectedStatusEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val isVideo: Boolean,
    val sizeBytes: Long,
    val dateModified: Long,
    val detectedAt: Long = System.currentTimeMillis(),
    val packageSource: String
)

@Entity(
    tableName = "saved_media",
    indices = [Index(value = ["filePath"], unique = true)]
)
data class SavedMediaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uriString: String,
    val filePath: String,
    val fileName: String,
    val isVideo: Boolean,
    val sizeBytes: Long,
    val savedAt: Long = System.currentTimeMillis(),
    val originalSource: String
)

@Entity(
    tableName = "notification_history",
    indices = [Index(value = ["notificationKey", "timestamp"])]
)
data class NotificationHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val notificationKey: String,
    val packageSource: String,
    val sender: String,
    val messageText: String,
    val timestamp: Long,
    val isRemoved: Boolean = false,
    val removedTimestamp: Long? = null,
    val isGroup: Boolean = false,
    val tag: String? = null
)
