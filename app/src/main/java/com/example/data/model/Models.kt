package com.example.data.model

import android.net.Uri

data class StatusMediaItem(
    val id: String,
    val uri: Uri,
    val uriString: String,
    val path: String,
    val name: String,
    val isVideo: Boolean,
    val size: Long,
    val dateModified: Long,
    val packageSource: String, // "com.whatsapp" or "com.whatsapp.w4b"
    val isSaved: Boolean = false
)

data class SavedMediaItem(
    val id: Long = 0,
    val uri: Uri,
    val uriString: String,
    val filePath: String,
    val fileName: String,
    val isVideo: Boolean,
    val size: Long,
    val savedAt: Long,
    val source: String
)

data class NotificationItem(
    val id: Long = 0,
    val key: String,
    val packageSource: String,
    val sender: String,
    val messageText: String,
    val timestamp: Long,
    val isRemoved: Boolean = false,
    val removedTimestamp: Long? = null,
    val isGroup: Boolean = false,
    val tag: String? = null
)

data class AppUpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: List<String>,
    val forceUpdate: Boolean
)

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class DashboardStats(
    val availableStatuses: Int = 0,
    val availableImages: Int = 0,
    val availableVideos: Int = 0,
    val savedImages: Int = 0,
    val savedVideos: Int = 0,
    val capturedNotifications: Int = 0,
    val removedNotifications: Int = 0
)
