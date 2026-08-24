package com.example.data.repository

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.example.data.db.SavedMediaDao
import com.example.data.db.SavedMediaEntity
import com.example.data.model.SavedMediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class SavedMediaRepository(
    private val context: Context,
    private val savedMediaDao: SavedMediaDao
) {
    val allSaved: Flow<List<SavedMediaItem>> = savedMediaDao.getAllSaved().map { list ->
        list.map { it.toModel() }
    }

    val savedImages: Flow<List<SavedMediaItem>> = savedMediaDao.getSavedImages().map { list ->
        list.map { it.toModel() }
    }

    val savedVideos: Flow<List<SavedMediaItem>> = savedMediaDao.getSavedVideos().map { list ->
        list.map { it.toModel() }
    }

    val imagesCount: Flow<Int> = savedMediaDao.getSavedImagesCount()
    val videosCount: Flow<Int> = savedMediaDao.getSavedVideosCount()

    fun getVaultStorageDir(): File {
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val vaultDir = File(picturesDir, "WhatsApp Status Vault")
        if (!vaultDir.exists()) {
            vaultDir.mkdirs()
        }
        return vaultDir
    }

    suspend fun saveMediaFromStream(
        fileName: String,
        isVideo: Boolean,
        source: String,
        inputStreamProvider: () -> InputStream?
    ): Result<SavedMediaItem> = withContext(Dispatchers.IO) {
        try {
            val vaultDir = getVaultStorageDir()
            val targetFile = File(vaultDir, fileName)

            inputStreamProvider()?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return@withContext Result.failure(Exception("Failed to open source stream"))

            // Trigger MediaScanner so it appears in device Gallery immediately
            MediaScannerConnection.scanFile(
                context,
                arrayOf(targetFile.absolutePath),
                arrayOf(if (isVideo) "video/mp4" else "image/jpeg"),
                null
            )

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            val entity = SavedMediaEntity(
                uriString = contentUri.toString(),
                filePath = targetFile.absolutePath,
                fileName = fileName,
                isVideo = isVideo,
                sizeBytes = targetFile.length(),
                savedAt = System.currentTimeMillis(),
                originalSource = source
            )

            val insertedId = savedMediaDao.insert(entity)
            Result.success(entity.copy(id = insertedId).toModel())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isMediaSaved(fileName: String): Boolean = withContext(Dispatchers.IO) {
        savedMediaDao.isSaved(fileName)
    }

    suspend fun deleteSavedMedia(item: SavedMediaItem): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(item.filePath)
            if (file.exists()) {
                file.delete()
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
            }
            savedMediaDao.deleteById(item.id)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getAllSavedList(): List<SavedMediaItem> = withContext(Dispatchers.IO) {
        savedMediaDao.getAllSavedList().map { it.toModel() }
    }

    fun shareMedia(item: SavedMediaItem) {
        val file = File(item.filePath)
        val uri = if (file.exists()) {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } else {
            Uri.parse(item.uriString)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = if (item.isVideo) "video/*" else "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Media via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun SavedMediaEntity.toModel(): SavedMediaItem {
        return SavedMediaItem(
            id = id,
            uri = Uri.parse(uriString),
            uriString = uriString,
            filePath = filePath,
            fileName = fileName,
            isVideo = isVideo,
            size = sizeBytes,
            savedAt = savedAt,
            source = originalSource
        )
    }
}
