package com.example.util

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Debug
import android.util.Log
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

object SecurityGuard {

    private const val TAG = "SecurityGuard"
    private var activeSessionToken: String? = null
    private var sessionCreationTime: Long = 0L

    fun generateNewSessionToken(): String {
        val token = UUID.randomUUID().toString() + "-" + System.currentTimeMillis()
        activeSessionToken = token
        sessionCreationTime = System.currentTimeMillis()
        return token
    }

    fun isSessionValid(token: String?): Boolean {
        if (token.isNullOrBlank() || activeSessionToken == null) return false
        return token == activeSessionToken
    }

    fun invalidateSession() {
        activeSessionToken = null
        sessionCreationTime = 0L
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = cm.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = cm.activeNetworkInfo
            @Suppress("DEPRECATION")
            networkInfo != null && networkInfo.isConnected
        }
    }

    fun isDebuggerAttached(): Boolean {
        return Debug.isDebuggerConnected() || Debug.waitingForDebugger()
    }

    fun isHookFrameworkPresent(): Boolean {
        val suspectClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "com.elderdrivers.riru.Riru",
            "me.weishu.epic.art.Epic",
            "com.saurik.substrate.MS"
        )
        for (className in suspectClasses) {
            try {
                Class.forName(className)
                return true
            } catch (_: ClassNotFoundException) {}
        }
        return false
    }

    suspend fun verifyRemoteLicense(
        context: Context,
        userKey: String
    ): AuthResult = withContext(Dispatchers.IO) {
        if (userKey.isBlank()) {
            return@withContext AuthResult.KeyNotFound
        }
        val authManager = AuthManager(context)
        return@withContext authManager.authenticate(userKey)
    }
}
