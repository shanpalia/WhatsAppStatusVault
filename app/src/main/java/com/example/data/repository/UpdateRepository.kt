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
    private val updateUrl = "https://shanpalia.github.io/WebsitePaliaAPK_V.2/version.json"

    suspend fun checkForUpdates(currentVersionCode: Int, currentVersionName: String): UpdateCheckResult = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            // Cache busting query parameter
            val cacheBustedUrl = "$updateUrl?t=${System.currentTimeMillis()}"
            val url = URL(cacheBustedUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Accept", "application/json")
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext UpdateCheckResult.Error("Server returned HTTP $responseCode (${connection.responseMessage})")
            }

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            val json = JSONObject(response.toString())
            val serverVersionCode = json.optInt("versionCode", 1)
            val serverVersionName = json.optString("versionName", "1.0")
            val apkUrl = json.optString("apkUrl", "")
            val forceUpdate = json.optBoolean("forceUpdate", false)
            val notesArray = json.optJSONArray("releaseNotes")
            val notesList = mutableListOf<String>()
            if (notesArray != null) {
                for (i in 0 until notesArray.length()) {
                    notesList.add(notesArray.getString(i))
                }
            }

            val updateInfo = AppUpdateInfo(
                versionCode = serverVersionCode,
                versionName = serverVersionName,
                apkUrl = apkUrl,
                releaseNotes = notesList,
                forceUpdate = forceUpdate
            )

            if (serverVersionCode > currentVersionCode) {
                UpdateCheckResult.UpdateAvailable(updateInfo)
            } else {
                UpdateCheckResult.UpToDate(currentVersionName)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.localizedMessage ?: "Unable to connect to update server")
        } finally {
            connection?.disconnect()
        }
    }
}
