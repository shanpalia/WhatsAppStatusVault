package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.StatusVaultApplication
import com.example.data.model.DashboardStats
import com.example.data.model.NotificationItem
import com.example.data.model.SavedMediaItem
import com.example.data.model.StatusMediaItem
import com.example.data.model.ThemeMode
import com.example.data.repository.UpdateCheckResult
import com.example.service.WhatsAppNotificationListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.net.URLEncoder

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as StatusVaultApplication
    private val statusRepo = app.statusRepository
    private val savedMediaRepo = app.savedMediaRepository
    private val notificationRepo = app.notificationRepository
    private val settingsRepo = app.settingsRepository
    private val updateRepo = app.updateRepository
    private val reportGen = app.reportGenerator

    // --- Preferences & State ---
    val themeMode: StateFlow<ThemeMode> = settingsRepo.themeMode
    val isPinEnabled: StateFlow<Boolean> = settingsRepo.isPinEnabled
    val autoRefresh: StateFlow<Boolean> = settingsRepo.autoRefresh
    val notifyNewStatus: StateFlow<Boolean> = settingsRepo.notifyNewStatus
    val statusFolderUri: StateFlow<String?> = settingsRepo.statusFolderUri

    private val _isAppUnlocked = MutableStateFlow(!settingsRepo.isPinEnabled.value)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // --- Status Saver ---
    private val _isRefreshingStatuses = MutableStateFlow(false)
    val isRefreshingStatuses: StateFlow<Boolean> = _isRefreshingStatuses.asStateFlow()

    private val _scannedStatuses = MutableStateFlow<List<StatusMediaItem>>(emptyList())
    val scannedStatuses: StateFlow<List<StatusMediaItem>> = _scannedStatuses.asStateFlow()

    // --- Saved Media ---
    val savedImages: StateFlow<List<SavedMediaItem>> = savedMediaRepo.savedImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val savedVideos: StateFlow<List<SavedMediaItem>> = savedMediaRepo.savedVideos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavedMedia: StateFlow<List<SavedMediaItem>> = savedMediaRepo.allSaved
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Notification History ---
    val allNotifications: StateFlow<List<NotificationItem>> = notificationRepo.allNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val removedNotifications: StateFlow<List<NotificationItem>> = notificationRepo.removedNotifications
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchNotificationQuery = MutableStateFlow("")
    val searchNotificationQuery: StateFlow<String> = _searchNotificationQuery.asStateFlow()

    // --- Dashboard Stats ---
    val dashboardStats: StateFlow<DashboardStats> = combine(
        _scannedStatuses,
        savedMediaRepo.imagesCount,
        savedMediaRepo.videosCount,
        notificationRepo.totalCount,
        notificationRepo.removedCount
    ) { statuses, imgCount, vidCount, notifCount, removedCount ->
        DashboardStats(
            availableStatuses = statuses.size,
            savedImages = imgCount,
            savedVideos = vidCount,
            capturedNotifications = notifCount,
            removedNotifications = removedCount
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // --- Update Checker ---
    private val _updateState = MutableStateFlow<UpdateCheckResult>(UpdateCheckResult.Idle)
    val updateState: StateFlow<UpdateCheckResult> = _updateState.asStateFlow()

    // --- PDF Report ---
    private val _isGeneratingReport = MutableStateFlow(false)
    val isGeneratingReport: StateFlow<Boolean> = _isGeneratingReport.asStateFlow()

    private val _generatedReportFile = MutableStateFlow<File?>(null)
    val generatedReportFile: StateFlow<File?> = _generatedReportFile.asStateFlow()

    // --- Notification Access Check ---
    private val _isNotificationAccessGranted = MutableStateFlow(false)
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    init {
        checkNotificationAccess()
        if (settingsRepo.autoRefresh.value) {
            refreshStatuses()
        }
    }

    fun checkNotificationAccess() {
        _isNotificationAccessGranted.value = WhatsAppNotificationListener.isNotificationAccessGranted(app)
    }

    fun unlockApp(pin: String): Boolean {
        if (settingsRepo.verifyPin(pin)) {
            _isAppUnlocked.value = true
            return true
        }
        return false
    }

    fun lockApp() {
        if (settingsRepo.isPinEnabled.value) {
            _isAppUnlocked.value = false
        }
    }

    fun setPin(pin: String): Boolean {
        val success = settingsRepo.setPin(pin)
        if (success) {
            _isAppUnlocked.value = true
        }
        return success
    }

    fun disablePin(): Boolean {
        val success = settingsRepo.disablePin()
        if (success) {
            _isAppUnlocked.value = true
        }
        return success
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepo.setThemeMode(mode)
    }

    fun setAutoRefresh(enabled: Boolean) {
        settingsRepo.setAutoRefresh(enabled)
    }

    fun setNotifyNewStatus(enabled: Boolean) {
        settingsRepo.setNotifyNewStatus(enabled)
    }

    fun setStatusFolderUri(uriString: String?) {
        settingsRepo.setStatusFolderUri(uriString)
        refreshStatuses()
    }

    fun refreshStatuses() {
        viewModelScope.launch {
            _isRefreshingStatuses.value = true
            try {
                val results = statusRepo.scanStatuses()
                _scannedStatuses.value = results
            } catch (e: Exception) {
                // Handled gracefully
            } finally {
                _isRefreshingStatuses.value = false
            }
        }
    }

    fun saveStatus(item: StatusMediaItem, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = statusRepo.saveStatus(item)
            if (result.isSuccess) {
                // Update in-memory isSaved status
                _scannedStatuses.value = _scannedStatuses.value.map {
                    if (it.id == item.id) it.copy(isSaved = true) else it
                }
                onResult(true, "Saved to Gallery / Vault!")
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Failed to save status")
            }
        }
    }

    fun shareStatus(item: StatusMediaItem) {
        statusRepo.shareStatus(item)
    }

    fun deleteSavedMedia(item: SavedMediaItem, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = savedMediaRepo.deleteSavedMedia(item)
            // Update scanned statuses saved tag
            _scannedStatuses.value = _scannedStatuses.value.map {
                if (it.name == item.fileName) it.copy(isSaved = false) else it
            }
            onResult(success)
        }
    }

    fun shareSavedMedia(item: SavedMediaItem) {
        savedMediaRepo.shareMedia(item)
    }

    // --- Notifications ---
    fun setSearchNotificationQuery(query: String) {
        _searchNotificationQuery.value = query
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            notificationRepo.deleteNotification(id)
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            notificationRepo.clearAllNotifications()
        }
    }

    // --- WhatsApp Direct ---
    fun openWhatsAppDirect(
        countryCode: String,
        phone: String,
        message: String,
        onError: (String) -> Unit
    ) {
        val cleanCountryCode = countryCode.trim().replace("+", "").replace(" ", "")
        val cleanPhone = phone.trim().replace("+", "").replace(" ", "").replace("-", "")

        if (cleanPhone.isBlank()) {
            onError("Please enter a valid phone number")
            return
        }

        if (cleanPhone.length < 6) {
            onError("Phone number is too short")
            return
        }

        val fullNumber = if (cleanCountryCode.isNotEmpty() && !cleanPhone.startsWith(cleanCountryCode)) {
            "$cleanCountryCode$cleanPhone"
        } else {
            cleanPhone
        }

        try {
            val encodedMessage = if (message.isNotBlank()) URLEncoder.encode(message.trim(), "UTF-8") else ""
            val url = if (encodedMessage.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$fullNumber&text=$encodedMessage"
            } else {
                "https://api.whatsapp.com/send?phone=$fullNumber"
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            app.startActivity(intent)
        } catch (e: Exception) {
            onError("WhatsApp is not installed or unable to open deep link")
        }
    }

    // --- Reports ---
    fun generatePdfReport(selectedSenders: Set<String>, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            _isGeneratingReport.value = true
            val currentStats = dashboardStats.value
            val statuses = _scannedStatuses.value
            val saved = savedMediaRepo.getAllSavedList()
            val allNotifs = notificationRepo.getAllNotificationsList()
            val notifs = if (selectedSenders.isEmpty()) {
                emptyList()
            } else {
                allNotifs.filter { selectedSenders.contains(it.sender) }
            }

            val result = reportGen.generatePdfReport(currentStats, statuses, saved, notifs, selectedSenders)
            _isGeneratingReport.value = false

            if (result.isSuccess) {
                val file = result.getOrNull()
                _generatedReportFile.value = file
                onComplete(file)
            } else {
                onComplete(null)
            }
        }
    }

    fun sharePdfReport(file: File) {
        reportGen.sharePdf(file)
    }

    fun openPdfReport(file: File) {
        reportGen.openPdf(file)
    }

    // --- Updates ---
    fun checkForUpdates(currentVersionCode: Int, currentVersionName: String) {
        viewModelScope.launch {
            _updateState.value = UpdateCheckResult.Checking
            val result = updateRepo.checkForUpdates(currentVersionCode, currentVersionName)
            _updateState.value = result
        }
    }

    fun resetUpdateState() {
        _updateState.value = UpdateCheckResult.Idle
    }
}
