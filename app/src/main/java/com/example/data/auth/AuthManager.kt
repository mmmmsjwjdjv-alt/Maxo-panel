package com.example.data.auth

import android.content.Context
import android.provider.Settings
import com.example.data.model.UserRecord
import com.example.data.pref.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class AuthResult {
    data class Success(val userRecord: UserRecord) : AuthResult()
    data object KeyNotFound : AuthResult()
    data object DeviceMismatch : AuthResult()
    data object Expired : AuthResult()
    data class NetworkError(val message: String) : AuthResult()
}

class AuthManager(private val context: Context) {

    private val prefManager = PreferenceManager.getInstance(context)

    companion object {
        private const val USER_DATABASE_URL =
            "https://raw.githubusercontent.com/mmmmsjwjdjv-alt/Maxo/refs/heads/main/User.json"
        private const val TIMEOUT_MS = 10000
    }

    fun getDeviceAndroidId(): String {
        return Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: "UNKNOWN_DEVICE_ID"
    }

    suspend fun authenticate(enteredKey: String): AuthResult = withContext(Dispatchers.IO) {
        val currentDeviceId = getDeviceAndroidId()

        try {
            val url = URL(USER_DATABASE_URL)
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doInput = true
            }

            val responseCode = conn.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                return@withContext checkOfflineFallback(enteredKey, currentDeviceId, "HTTP $responseCode")
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                sb.append(line)
            }
            reader.close()
            conn.disconnect()

            val jsonArray = JSONArray(sb.toString())

            var keyExists = false
            var deviceMatches = false
            var expired = false
            var matchedRecord: UserRecord? = null

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val jsonKey = obj.optString("key", "")
                val jsonDeviceId = obj.optString("device_id", "")

                if (jsonKey == enteredKey) {
                    keyExists = true

                    if (jsonDeviceId == currentDeviceId) {
                        deviceMatches = true

                        val allowOffline = obj.optBoolean("Allowoffline", false)
                        val expiryStr = obj.optString("expirydate", "")

                        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
                        val expiryDate = try {
                            sdf.parse(expiryStr)
                        } catch (e: Exception) {
                            null
                        }

                        val now = Date()
                        if (expiryDate != null && now.after(expiryDate)) {
                            expired = true
                        }

                        matchedRecord = UserRecord(
                            key = jsonKey,
                            deviceId = jsonDeviceId,
                            expiryDate = expiryStr,
                            allowOffline = allowOffline
                        )
                    }
                    break
                }
            }

            when {
                !keyExists -> AuthResult.KeyNotFound
                !deviceMatches -> AuthResult.DeviceMismatch
                expired -> AuthResult.Expired
                matchedRecord != null -> {
                    prefManager.saveAuthSuccess(
                        key = matchedRecord.key,
                        devId = matchedRecord.deviceId,
                        expiry = matchedRecord.expiryDate,
                        allowOff = matchedRecord.allowOffline
                    )
                    AuthResult.Success(matchedRecord)
                }
                else -> AuthResult.KeyNotFound
            }
        } catch (e: Exception) {
            checkOfflineFallback(enteredKey, currentDeviceId, e.localizedMessage ?: "Network error")
        }
    }

    private fun checkOfflineFallback(
        enteredKey: String,
        deviceId: String,
        errorMsg: String
    ): AuthResult {
        // If offline and previously cached with Allowoffline == true
        if (prefManager.allowOffline &&
            prefManager.userKey == enteredKey &&
            prefManager.deviceId == deviceId
        ) {
            val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.US)
            val expiryDate = try {
                sdf.parse(prefManager.expiryDate)
            } catch (e: Exception) {
                null
            }
            val now = Date()
            if (expiryDate != null && now.after(expiryDate)) {
                return AuthResult.Expired
            }
            return AuthResult.Success(
                UserRecord(
                    key = prefManager.userKey,
                    deviceId = prefManager.deviceId,
                    expiryDate = prefManager.expiryDate,
                    allowOffline = true
                )
            )
        }
        return AuthResult.NetworkError(errorMsg)
    }
}
