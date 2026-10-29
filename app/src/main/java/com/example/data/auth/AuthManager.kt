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
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.HttpsURLConnection

sealed class AuthResult {
    data class Success(val userRecord: UserRecord) : AuthResult()
    data object KeyNotFound : AuthResult()
    data object DeviceMismatch : AuthResult()
    data object Expired : AuthResult()
    data object AccountBanned : AuthResult()
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
            val conn = (url.openConnection() as HttpsURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "RedJohn-SecurityClient/3.0")
                setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                setRequestProperty("Pragma", "no-cache")
                setRequestProperty("Accept", "application/json")
                doInput = true
            }

            val responseCode = conn.responseCode
            if (responseCode != HttpsURLConnection.HTTP_OK) {
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

            val rawJson = sb.toString().trim()
            if (rawJson.isEmpty()) {
                return@withContext checkOfflineFallback(enteredKey, currentDeviceId, "Empty server response")
            }

            val jsonArray = JSONArray(rawJson)

            var keyExists = false
            var deviceMatches = false
            var expired = false
            var isBanned = false
            var matchedRecord: UserRecord? = null

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val jsonKey = obj.optString("key", "").trim()
                val jsonDeviceId = obj.optString("device_id", "").trim()

                if (jsonKey.equals(enteredKey.trim(), ignoreCase = false)) {
                    keyExists = true

                    // Check explicit ban/revocation flags in GitHub record
                    val status = obj.optString("status", "").lowercase(Locale.US)
                    val bannedFlag = obj.optBoolean("banned", false) || obj.optBoolean("is_banned", false)
                    if (status == "banned" || status == "inactive" || status == "revoked" || bannedFlag) {
                        isBanned = true
                        break
                    }

                    if (jsonDeviceId.equals(currentDeviceId.trim(), ignoreCase = true)) {
                        deviceMatches = true

                        val allowOffline = obj.optBoolean("Allowoffline", false) ||
                                obj.optString("Allowoffline", "false").equals("true", ignoreCase = true)
                        val expiryStr = obj.optString("expirydate", "").trim()

                        // Parse Expiration Date with multiple date format support
                        if (!isPermanentExpiry(expiryStr)) {
                            val parsedDate = parseExpiryDate(expiryStr)
                            val now = Date()
                            if (parsedDate != null && now.after(parsedDate)) {
                                expired = true
                            }
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
                isBanned -> AuthResult.AccountBanned
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

    private fun isPermanentExpiry(expiryStr: String): Boolean {
        val lower = expiryStr.lowercase(Locale.US)
        return lower.contains("permanent") ||
                lower.contains("unlimited") ||
                lower.contains("forever") ||
                lower.contains("9999") ||
                lower.contains("2099")
    }

    private fun parseExpiryDate(dateStr: String): Date? {
        val formats = arrayOf(
            "dd-MM-yyyy",
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "yyyy/MM/dd",
            "dd.MM.yyyy"
        )
        for (pattern in formats) {
            try {
                val sdf = SimpleDateFormat(pattern, Locale.US)
                sdf.isLenient = false
                val date = sdf.parse(dateStr)
                if (date != null) return date
            } catch (_: Exception) {}
        }
        return null
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
            if (!isPermanentExpiry(prefManager.expiryDate)) {
                val parsedDate = parseExpiryDate(prefManager.expiryDate)
                val now = Date()
                if (parsedDate != null && now.after(parsedDate)) {
                    return AuthResult.Expired
                }
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
