package com.example.data.repository

import com.example.data.db.NotificationDao
import com.example.data.db.NotificationHistoryEntity
import com.example.data.model.NotificationItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class NotificationRepository(private val notificationDao: NotificationDao) {

    val allNotifications: Flow<List<NotificationItem>> =
        notificationDao.getAllNotifications().map { list ->
            list.map { it.toModel() }
        }

    val removedNotifications: Flow<List<NotificationItem>> =
        notificationDao.getRemovedNotifications().map { list ->
            list.map { it.toModel() }
        }

    val totalCount: Flow<Int> = notificationDao.getTotalNotificationsCount()
    val removedCount: Flow<Int> = notificationDao.getRemovedNotificationsCount()

    fun searchNotifications(query: String): Flow<List<NotificationItem>> {
        return notificationDao.searchNotifications(query).map { list ->
            list.map { it.toModel() }
        }
    }

    suspend fun saveNotification(
        key: String,
        packageSource: String,
        sender: String,
        messageText: String,
        timestamp: Long,
        isGroup: Boolean = false,
        tag: String? = null
    ): Long = withContext(Dispatchers.IO) {
        if (sender.isBlank() && messageText.isBlank()) return@withContext -1L

        val entity = NotificationHistoryEntity(
            notificationKey = key,
            packageSource = packageSource,
            sender = sender.ifBlank { "WhatsApp Contact" },
            messageText = messageText.ifBlank { "Media / Voice / Message" },
            timestamp = timestamp,
            isRemoved = false,
            isGroup = isGroup,
            tag = tag
        )
        notificationDao.insert(entity)
    }

    suspend fun markNotificationRemoved(key: String) = withContext(Dispatchers.IO) {
        notificationDao.markRemoved(key, System.currentTimeMillis())
    }

    suspend fun deleteNotification(id: Long) = withContext(Dispatchers.IO) {
        notificationDao.deleteById(id)
    }

    suspend fun clearAllNotifications() = withContext(Dispatchers.IO) {
        notificationDao.clearAll()
    }

    suspend fun getAllNotificationsList(): List<NotificationItem> = withContext(Dispatchers.IO) {
        notificationDao.getAllNotificationsList().map { it.toModel() }
    }

    private fun NotificationHistoryEntity.toModel(): NotificationItem {
        return NotificationItem(
            id = id,
            key = notificationKey,
            packageSource = packageSource,
            sender = sender,
            messageText = messageText,
            timestamp = timestamp,
            isRemoved = isRemoved,
            removedTimestamp = removedTimestamp,
            isGroup = isGroup,
            tag = tag
        )
    }
}
