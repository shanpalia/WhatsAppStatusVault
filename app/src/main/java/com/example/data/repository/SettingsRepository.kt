package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("vault_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(getSavedThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _isPinEnabled = MutableStateFlow(prefs.getBoolean(KEY_PIN_ENABLED, false))
    val isPinEnabled: StateFlow<Boolean> = _isPinEnabled.asStateFlow()

    private val _autoRefresh = MutableStateFlow(prefs.getBoolean(KEY_AUTO_REFRESH, true))
    val autoRefresh: StateFlow<Boolean> = _autoRefresh.asStateFlow()

    private val _notifyNewStatus = MutableStateFlow(prefs.getBoolean(KEY_NOTIFY_STATUS, true))
    val notifyNewStatus: StateFlow<Boolean> = _notifyNewStatus.asStateFlow()

    private val _statusFolderUri = MutableStateFlow(prefs.getString(KEY_STATUS_URI, null))
    val statusFolderUri: StateFlow<String?> = _statusFolderUri.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    private fun getSavedThemeMode(): ThemeMode {
        val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return try {
            ThemeMode.valueOf(name)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    fun setAutoRefresh(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REFRESH, enabled).apply()
        _autoRefresh.value = enabled
    }

    fun setNotifyNewStatus(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_STATUS, enabled).apply()
        _notifyNewStatus.value = enabled
    }

    fun setStatusFolderUri(uriString: String?) {
        prefs.edit().putString(KEY_STATUS_URI, uriString).apply()
        _statusFolderUri.value = uriString
    }

    fun setPin(pin: String): Boolean {
        if (pin.length in 4..6 && pin.all { it.isDigit() }) {
            val hash = hashPin(pin)
            prefs.edit()
                .putString(KEY_PIN_HASH, hash)
                .putBoolean(KEY_PIN_ENABLED, true)
                .apply()
            _isPinEnabled.value = true
            return true
        }
        return false
    }

    fun disablePin(): Boolean {
        prefs.edit()
            .remove(KEY_PIN_HASH)
            .putBoolean(KEY_PIN_ENABLED, false)
            .apply()
        _isPinEnabled.value = false
        return true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return hashPin(pin) == storedHash
    }

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_PIN_ENABLED = "key_pin_enabled"
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_AUTO_REFRESH = "key_auto_refresh"
        private const val KEY_NOTIFY_STATUS = "key_notify_status"
        private const val KEY_STATUS_URI = "key_status_folder_uri"
    }
}
