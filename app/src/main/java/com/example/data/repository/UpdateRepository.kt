package com.example.data.repository

import com.example.data.model.AppUpdateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

sealed class UpdateCheckResult {
    object Idle : UpdateCheckResult()
    object Checking : UpdateCheckResult()
    data class UpdateAvailable(val updateInfo: AppUpdateInfo) : UpdateCheckResult()
    data class UpToDate(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

class UpdateRepository {
    private val updateUrls = listOf(
        "https://shanpalia.github.io/WebsitePaliaAPK_V.2/version.json",
        "https://raw.githubusercontent.com/shanpalia/WebsitePaliaAPK_V.2/main/version.json",
        "https://raw.githubusercontent.com/shanpalia/WebsitePaliaAPK_V.2/master/version.json"
    )

    suspend fun checkForUpdates(
        currentVersionCode: Int,
        currentVersionName: String
    ): UpdateCheckResult = withContext(Dispatchers.IO) {
        var lastError = "Unable to connect to update server"

        for (baseUrl in updateUrls) {
            var connection: HttpURLConnection? = null
            try {
                val cacheBustedUrl = "$baseUrl?t=${System.currentTimeMillis()}"
                connection = (URL(cacheBustedUrl).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8000
                    readTimeout = 8000
                    useCaches = false
                    instanceFollowRedirects = true
                    setRequestProperty("Cache-Control", "no-cache, no-store")
                    setRequestProperty("Pragma", "no-cache")
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "WhatsAppStatusVault-Updater")
                }

                val responseCode = connection.responseCode

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    lastError = "HTTP $responseCode"
                    continue
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(response)

                // Missing/invalid versionCode must never be treated as "up to date".
                if (!json.has("versionCode")) {
                    lastError = "version.json is missing versionCode"
                    continue
                }

                val serverVersionCode = json.getInt("versionCode")
                val serverVersionName = json.optString("versionName", "")
                val apkUrl = json.optString("apkUrl", "")
                val forceUpdate = json.optBoolean("forceUpdate", false)

                if (serverVersionCode > currentVersionCode) {
                    val notesArray = json.optJSONArray("releaseNotes")
                    val notesList = mutableListOf<String>()
                    if (notesArray != null) {
                        for (i in 0 until notesArray.length()) {
                            notesList += notesArray.optString(i)
                        }
                    }

                    if (apkUrl.isBlank()) {
                        return@withContext UpdateCheckResult.Error(
                            "A new version is available, but apkUrl is missing."
                        )
                    }

                    return@withContext UpdateCheckResult.UpdateAvailable(
                        AppUpdateInfo(
                            versionCode = serverVersionCode,
                            versionName = serverVersionName,
                            apkUrl = apkUrl,
                            releaseNotes = notesList,
                            forceUpdate = forceUpdate
                        )
                    )
                }

                // A valid server response with same/lower version means up to date.
                return@withContext UpdateCheckResult.UpToDate(currentVersionName)
            } catch (e: Exception) {
                lastError = e.localizedMessage ?: "Update server request failed"
            } finally {
                connection?.disconnect()
            }
        }

        // Do not incorrectly show "up to date" when all endpoints failed.
        UpdateCheckResult.Error("Unable to check for updates ($lastError)")
    }
}
