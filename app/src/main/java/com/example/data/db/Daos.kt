package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StatusDao {
    @Query("SELECT * FROM detected_statuses ORDER BY detectedAt DESC")
    fun getAllDetected(): Flow<List<DetectedStatusEntity>>

    @Query("SELECT id FROM detected_statuses")
    suspend fun getAllKnownIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(statuses: List<DetectedStatusEntity>): List<Long>

    @Query("DELETE FROM detected_statuses WHERE id NOT IN (:currentIds)")
    suspend fun cleanOldStatuses(currentIds: List<String>)

    @Query("SELECT COUNT(*) FROM detected_statuses")
    fun getDetectedCount(): Flow<Int>
}

@Dao
interface SavedMediaDao {
    @Query("SELECT * FROM saved_media ORDER BY savedAt DESC")
    fun getAllSaved(): Flow<List<SavedMediaEntity>>

    @Query("SELECT * FROM saved_media WHERE isVideo = 0 ORDER BY savedAt DESC")
    fun getSavedImages(): Flow<List<SavedMediaEntity>>

    @Query("SELECT * FROM saved_media WHERE isVideo = 1 ORDER BY savedAt DESC")
    fun getSavedVideos(): Flow<List<SavedMediaEntity>>

    @Query("SELECT COUNT(*) FROM saved_media WHERE isVideo = 0")
    fun getSavedImagesCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM saved_media WHERE isVideo = 1")
    fun getSavedVideosCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(savedMedia: SavedMediaEntity): Long

    @Query("DELETE FROM saved_media WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM saved_media WHERE filePath = :filePath")
    suspend fun deleteByPath(filePath: String)

    @Query("SELECT EXISTS(SELECT 1 FROM saved_media WHERE fileName = :fileName)")
    suspend fun isSaved(fileName: String): Boolean

    @Query("SELECT * FROM saved_media")
    suspend fun getAllSavedList(): List<SavedMediaEntity>
}

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationHistoryEntity>>

    @Query("SELECT * FROM notification_history WHERE isRemoved = 1 ORDER BY timestamp DESC")
    fun getRemovedNotifications(): Flow<List<NotificationHistoryEntity>>

    @Query("SELECT * FROM notification_history WHERE (sender LIKE '%' || :query || '%' OR messageText LIKE '%' || :query || '%') ORDER BY timestamp DESC")
    fun searchNotifications(query: String): Flow<List<NotificationHistoryEntity>>

    @Query("SELECT COUNT(*) FROM notification_history")
    fun getTotalNotificationsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM notification_history WHERE isRemoved = 1")
    fun getRemovedNotificationsCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(notification: NotificationHistoryEntity): Long

    @Query("UPDATE notification_history SET isRemoved = 1, removedTimestamp = :removedTime WHERE notificationKey = :key AND isRemoved = 0")
    suspend fun markRemoved(key: String, removedTime: Long = System.currentTimeMillis())

    @Query("UPDATE notification_history SET isRemoved = 1, removedTimestamp = :removedTime WHERE id = (SELECT id FROM notification_history WHERE packageSource = :packageSource AND sender = :sender AND isRemoved = 0 ORDER BY timestamp DESC LIMIT 1)")
    suspend fun markLatestFromSenderRemoved(packageSource: String, sender: String, removedTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM notification_history WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM notification_history")
    suspend fun clearAll()

    @Query("SELECT * FROM notification_history ORDER BY timestamp DESC LIMIT 500")
    suspend fun getAllNotificationsList(): List<NotificationHistoryEntity>
}
