package com.example.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.example.data.db.DetectedStatusEntity
import com.example.data.db.StatusDao
import com.example.data.model.StatusMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import java.io.File

class StatusRepository(
    private val context: Context,
    private val statusDao: StatusDao,
    private val savedMediaRepository: SavedMediaRepository,
    private val settingsRepository: SettingsRepository
) {
    val detectedStatusesCount: Flow<Int> = statusDao.getDetectedCount()

    val detectedStatuses: Flow<List<StatusMediaItem>> = statusDao.getAllDetected().mapLatest { list ->
        list.map { entity ->
            val isSaved = savedMediaRepository.isMediaSaved(entity.fileName)
            StatusMediaItem(
                id = entity.id,
                uri = Uri.parse(entity.id),
                uriString = entity.id,
                path = entity.id,
                name = entity.fileName,
                isVideo = entity.isVideo,
                size = entity.sizeBytes,
                dateModified = entity.dateModified,
                packageSource = entity.packageSource,
                isSaved = isSaved
            )
        }
    }

    suspend fun scanStatuses(userUriString: String? = null): List<StatusMediaItem> = withContext(Dispatchers.IO) {
        val foundItems = mutableListOf<StatusMediaItem>()
        val savedUri = userUriString ?: settingsRepository.statusFolderUri.value

        // 1. Legacy SAF URI fallback (kept only for existing installations).
        if (!savedUri.isNullOrBlank()) {
            try {
                val treeUri = Uri.parse(savedUri)
                val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                if (pickedDir != null && pickedDir.exists() && pickedDir.isDirectory) {
                    val files = pickedDir.listFiles()
                    for (doc in files) {
                        val name = doc.name ?: continue
                        if (name.startsWith(".nomedia") || name.startsWith(".")) continue
                        val isVideo = name.endsWith(".mp4", ignoreCase = true) || name.endsWith(".mkv", ignoreCase = true) || (doc.type?.startsWith("video/") == true)
                        val isImage = name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true) || name.endsWith(".png", ignoreCase = true) || name.endsWith(".webp", ignoreCase = true) || name.endsWith(".webp", ignoreCase = true) || (doc.type?.startsWith("image/") == true)
                        
                        if (isImage || isVideo) {
                            val isSaved = savedMediaRepository.isMediaSaved(name)
                            foundItems.add(
                                StatusMediaItem(
                                    id = doc.uri.toString(),
                                    uri = doc.uri,
                                    uriString = doc.uri.toString(),
                                    path = doc.uri.toString(),
                                    name = name,
                                    isVideo = isVideo,
                                    size = doc.length(),
                                    dateModified = doc.lastModified(),
                                    packageSource = if (savedUri.contains("w4b", ignoreCase = true)) "com.whatsapp.w4b" else "com.whatsapp",
                                    isSaved = isSaved
                                )
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                // Saf access exception handled gracefully
            }
        }

        // 2. Scan WhatsApp status directories directly. With MANAGE_EXTERNAL_STORAGE
        // enabled this does not require the user to pick .Statuses manually.
        val possiblePaths = listOf(
            // Modern Android (Android/media)
            Pair(File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp/WhatsApp/Media/.Statuses"), "com.whatsapp"),
            Pair(File(Environment.getExternalStorageDirectory(), "Android/media/com.whatsapp.w4b/WhatsApp Business/Media/.Statuses"), "com.whatsapp.w4b"),
            // Legacy paths
            Pair(File(Environment.getExternalStorageDirectory(), "WhatsApp/Media/.Statuses"), "com.whatsapp"),
            Pair(File(Environment.getExternalStorageDirectory(), "WhatsApp Business/Media/.Statuses"), "com.whatsapp.w4b")
        )

        for ((folder, pkg) in possiblePaths) {
            try {
                if (folder.exists() && folder.isDirectory && folder.canRead()) {
                    val files = folder.listFiles() ?: continue
                    for (file in files) {
                        if (file.isDirectory || file.name.startsWith(".nomedia") || file.name.startsWith(".")) continue
                        val name = file.name
                        val isVideo = name.endsWith(".mp4", ignoreCase = true) || name.endsWith(".mkv", ignoreCase = true)
                        val isImage = name.endsWith(".jpg", ignoreCase = true) || name.endsWith(".jpeg", ignoreCase = true) || name.endsWith(".png", ignoreCase = true) || name.endsWith(".webp", ignoreCase = true)

                        if (isImage || isVideo) {
                            val uri = Uri.fromFile(file)
                            val isSaved = savedMediaRepository.isMediaSaved(name)
                            // Avoid duplicate by name if already added
                            if (foundItems.none { it.name == name }) {
                                foundItems.add(
                                    StatusMediaItem(
                                        id = uri.toString(),
                                        uri = uri,
                                        uriString = uri.toString(),
                                        path = file.absolutePath,
                                        name = name,
                                        isVideo = isVideo,
                                        size = file.length(),
                                        dateModified = file.lastModified(),
                                        packageSource = pkg,
                                        isSaved = isSaved
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore inaccessible paths
            }
        }

        // Sync with Room DB
        val entities = foundItems.map { item ->
            DetectedStatusEntity(
                id = item.id,
                fileName = item.name,
                isVideo = item.isVideo,
                sizeBytes = item.size,
                dateModified = item.dateModified,
                packageSource = item.packageSource
            )
        }

        val knownIds = statusDao.getAllKnownIds().toSet()
        val newEntities = entities.filter { !knownIds.contains(it.id) }

        if (entities.isNotEmpty()) {
            statusDao.insertAll(entities)
            statusDao.cleanOldStatuses(entities.map { it.id })
        } else {
            statusDao.cleanOldStatuses(emptyList())
        }

        foundItems.sortedByDescending { it.dateModified }
    }

    suspend fun saveStatus(item: StatusMediaItem): Result<Boolean> = withContext(Dispatchers.IO) {
        val result = savedMediaRepository.saveMediaFromStream(
            fileName = item.name,
            isVideo = item.isVideo,
            source = item.packageSource
        ) {
            if (item.uriString.startsWith("content://")) {
                context.contentResolver.openInputStream(Uri.parse(item.uriString))
            } else {
                val file = File(item.path)
                if (file.exists()) file.inputStream() else context.contentResolver.openInputStream(item.uri)
            }
        }

        if (result.isSuccess) {
            Result.success(true)
        } else {
            Result.failure(result.exceptionOrNull() ?: Exception("Save failed"))
        }
    }

    fun shareStatus(item: StatusMediaItem) {
        try {
            val uri = if (item.uriString.startsWith("content://")) {
                Uri.parse(item.uriString)
            } else {
                val file = File(item.path)
                if (file.exists()) {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                } else {
                    item.uri
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = if (item.isVideo) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Share Status via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            // Share error handled
        }
    }
}
