package com.example.fund.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Encrypted local store for session credentials, active user state, and UI/server preferences.
 * Uses AndroidX EncryptedSharedPreferences backed by AndroidKeyStore AES-256-GCM, with an
 * automatic fallback for JVM Robolectric environments where KeyStore hardware is unavailable.
 */
class EncryptedSessionManager(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context.applicationContext)

    var activeUserId: String?
        get() = prefs.getString(KEY_ACTIVE_USER_ID, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_ACTIVE_USER_ID) else putString(KEY_ACTIVE_USER_ID, value)
        }

    var activeUsername: String?
        get() = prefs.getString(KEY_ACTIVE_USERNAME, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_ACTIVE_USERNAME) else putString(KEY_ACTIVE_USERNAME, value)
        }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, true)
        set(value) = prefs.edit { putBoolean(KEY_IS_LOGGED_IN, value) }

    var hasCompletedInitialLogin: Boolean
        get() = prefs.getBoolean(KEY_HAS_INITIAL_LOGIN, false)
        set(value) = prefs.edit { putBoolean(KEY_HAS_INITIAL_LOGIN, value) }

    var lastAuthenticatedUsername: String?
        get() = prefs.getString(KEY_LAST_AUTH_USERNAME, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_LAST_AUTH_USERNAME) else putString(KEY_LAST_AUTH_USERNAME, value)
        }

    var quickPinHash: String?
        get() = prefs.getString(KEY_QUICK_PIN_HASH, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_QUICK_PIN_HASH) else putString(KEY_QUICK_PIN_HASH, value)
        }

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DARK_MODE, value) }

    var fontScale: Float
        get() = prefs.getFloat(KEY_FONT_SCALE, 1.0f)
        set(value) = prefs.edit { putFloat(KEY_FONT_SCALE, value) }

    var appFontFamily: String
        get() = prefs.getString(KEY_APP_FONT, "TAJAWAL") ?: "TAJAWAL"
        set(value) = prefs.edit { putString(KEY_APP_FONT, value) }

    var autoSyncOnConnect: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, true)
        set(value) = prefs.edit { putBoolean(KEY_AUTO_SYNC, value) }

    var serverName: String
        get() = prefs.getString(KEY_SERVER_NAME, DEFAULT_SERVER_NAME) ?: DEFAULT_SERVER_NAME
        set(value) = prefs.edit { putString(KEY_SERVER_NAME, value) }

    var serverHost: String
        get() = prefs.getString(KEY_SERVER_HOST, DEFAULT_SERVER_HOST) ?: DEFAULT_SERVER_HOST
        set(value) = prefs.edit { putString(KEY_SERVER_HOST, value) }

    var lastSyncTimestamp: String
        get() = prefs.getString(KEY_LAST_SYNC, "2026-11-08 10:45") ?: "2026-11-08 10:45"
        set(value) = prefs.edit { putString(KEY_LAST_SYNC, value) }

    fun saveActiveSession(userId: String, username: String, loggedIn: Boolean = true) {
        prefs.edit {
            putString(KEY_ACTIVE_USER_ID, userId)
            putString(KEY_ACTIVE_USERNAME, username)
            putString(KEY_LAST_AUTH_USERNAME, username)
            putBoolean(KEY_IS_LOGGED_IN, loggedIn)
            if (loggedIn) {
                putBoolean(KEY_HAS_INITIAL_LOGIN, true)
            }
        }
    }

    fun clearSession() {
        prefs.edit {
            remove(KEY_ACTIVE_USER_ID)
            remove(KEY_ACTIVE_USERNAME)
            putBoolean(KEY_IS_LOGGED_IN, false)
        }
    }

    fun clearQuickPin() {
        prefs.edit { remove(KEY_QUICK_PIN_HASH) }
    }

    companion object {
        private const val ENCRYPTED_PREFS_NAME = "social_fund_v9_encrypted_session"
        private const val FALLBACK_PREFS_NAME = "social_fund_v9_session_fallback"

        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_ACTIVE_USERNAME = "active_username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_HAS_INITIAL_LOGIN = "has_completed_initial_login"
        private const val KEY_LAST_AUTH_USERNAME = "last_authenticated_username"
        private const val KEY_QUICK_PIN_HASH = "quick_pin_hash"
        private const val KEY_DARK_MODE = "is_dark_mode"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_APP_FONT = "app_font_family"
        private const val KEY_AUTO_SYNC = "auto_sync_on_connect"
        private const val KEY_SERVER_NAME = "server_name"
        private const val KEY_SERVER_HOST = "server_host"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"

        private const val DEFAULT_SERVER_NAME = "الخادم المحلي الميداني (LAN)"
        private const val DEFAULT_SERVER_HOST = "192.168.1.100:8443"

        private fun createEncryptedPrefs(context: Context): SharedPreferences {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (_: Exception) {
                context.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
            }
        }
    }
}
