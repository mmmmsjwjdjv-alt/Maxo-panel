package com.example.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Debug
import com.example.data.auth.AuthManager
import com.example.data.auth.AuthResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.net.InetAddress
import java.net.Socket
import java.security.MessageDigest
import java.util.UUID

object SecurityGuard {

    private const val TAG = "SecurityGuard"
    private var activeSessionToken: String? = null
    private var sessionCreationTime: Long = 0L

    fun generateNewSessionToken(): String {
        val raw = UUID.randomUUID().toString() + "-" + System.currentTimeMillis()
        val hash = sha256(raw)
        activeSessionToken = hash
        sessionCreationTime = System.currentTimeMillis()
        return hash
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
        // 1. Check suspect reverse engineering and injection classes
        val suspectClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "com.elderdrivers.riru.Riru",
            "me.weishu.epic.art.Epic",
            "com.saurik.substrate.MS",
            "com.topjohnwu.magisk",
            "org.lsposed.lspd",
            "io.github.vvb2060.magisk",
            "com.swift.sandhook.SandHook"
        )
        for (className in suspectClasses) {
            try {
                Class.forName(className)
                return true
            } catch (_: ClassNotFoundException) {}
            catch (_: Throwable) {}
        }

        // 2. Scan memory maps for Frida or Xposed agent injection
        try {
            val mapsFile = File("/proc/self/maps")
            if (mapsFile.exists() && mapsFile.canRead()) {
                BufferedReader(FileReader(mapsFile)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val lower = line?.lowercase() ?: ""
                        if (lower.contains("frida-agent") ||
                            lower.contains("frida-gadget") ||
                            lower.contains("xposedbridge") ||
                            lower.contains("libgadget.so")
                        ) {
                            return true
                        }
                    }
                }
            }
        } catch (_: Exception) {}

        return false
    }

    fun isDeviceRooted(): Boolean {
        // 1. Check Build Tags for test keys
        val buildTags = Build.TAGS
        if (buildTags != null && buildTags.contains("test-keys")) {
            return true
        }

        // 2. Check existence of su binaries across standard locations
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }

        return false
    }

    fun isSecurityCompromised(): Boolean {
        return isDebuggerAttached() || isHookFrameworkPresent() || isDeviceRooted()
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

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
