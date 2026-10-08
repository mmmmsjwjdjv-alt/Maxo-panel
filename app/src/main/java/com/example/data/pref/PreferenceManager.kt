package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_FILE = "app_data"
        private const val KEY_LOGGED_IN = "logged_in"
        private const val KEY_SAVED_KEY = "saved_key"
        private const val KEY_SAVE_PASSWORD_ENABLED = "save_password_enabled"
        private const val KEY_USER_KEY = "user_key"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_EXPIRY_DATE = "expiry_date"
        private const val KEY_ALLOW_OFFLINE = "allow_offline"
        private const val KEY_OVERLAY_ACTIVE = "overlay_active"

        @Volatile
        private var instance: PreferenceManager? = null

        fun getInstance(context: Context): PreferenceManager {
            return instance ?: synchronized(this) {
                instance ?: PreferenceManager(context).also { instance = it }
            }
        }
    }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    var savedKey: String
        get() = prefs.getString(KEY_SAVED_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_SAVED_KEY, value).apply()

    var isSavePasswordEnabled: Boolean
        get() = prefs.getBoolean(KEY_SAVE_PASSWORD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_SAVE_PASSWORD_ENABLED, value).apply()

    var userKey: String
        get() = prefs.getString(KEY_USER_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USER_KEY, value).apply()

    var deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, "") ?: ""
        set(value) = prefs.edit().putString(KEY_DEVICE_ID, value).apply()

    var expiryDate: String
        get() = prefs.getString(KEY_EXPIRY_DATE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_EXPIRY_DATE, value).apply()

    var allowOffline: Boolean
        get() = prefs.getBoolean(KEY_ALLOW_OFFLINE, false)
        set(value) = prefs.edit().putBoolean(KEY_ALLOW_OFFLINE, value).apply()

    var isOverlayActive: Boolean
        get() = prefs.getBoolean(KEY_OVERLAY_ACTIVE, false)
        set(value) = prefs.edit().putBoolean(KEY_OVERLAY_ACTIVE, value).apply()

    fun saveAuthSuccess(
        key: String,
        devId: String,
        expiry: String,
        allowOff: Boolean
    ) {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, true)
            .putString(KEY_USER_KEY, key)
            .putString(KEY_DEVICE_ID, devId)
            .putString(KEY_EXPIRY_DATE, expiry)
            .putBoolean(KEY_ALLOW_OFFLINE, allowOff)
            .apply()
    }

    fun logout() {
        prefs.edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .apply()
    }
}
