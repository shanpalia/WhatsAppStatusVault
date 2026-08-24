package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import com.example.data.db.AppDatabase
import com.example.data.repository.NotificationRepository
import com.example.data.repository.ReportGenerator
import com.example.data.repository.SavedMediaRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.StatusRepository
import com.example.data.repository.UpdateRepository

class StatusVaultApplication : Application(), ImageLoaderFactory {

    val database by lazy { AppDatabase.getDatabase(this) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val savedMediaRepository by lazy { SavedMediaRepository(this, database.savedMediaDao()) }
    val statusRepository by lazy {
        StatusRepository(this, database.statusDao(), savedMediaRepository, settingsRepository)
    }
    val notificationRepository by lazy { NotificationRepository(database.notificationDao()) }
    val updateRepository by lazy { UpdateRepository() }
    val reportGenerator by lazy { ReportGenerator(this) }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .crossfade(true)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: StatusVaultApplication
            private set
    }
}
