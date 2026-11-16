package com.example.fund.security

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.util.concurrent.ConcurrentHashMap

/**
 * Encrypted local store for session credentials, active user state, and UI/server preferences.
 * Uses AndroidX EncryptedSharedPreferences backed by AndroidKeyStore AES-256-GCM.
 * Never falls back to unencrypted SharedPreferences XML on disk; if hardware KeyStore is
 * unavailable (e.g., JVM Robolectric or damaged KeyStore), uses volatile RAM-only AES-256-GCM storage.
 */
class EncryptedSessionManager(context: Context) {

    private val storageBacking: StorageBacking = createEncryptedBacking(context.applicationContext)
    private val prefs: SharedPreferences = storageBacking.prefs
    val isEncryptedStorageActive: Boolean = storageBacking.isHardwareEncrypted

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
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit { putBoolean(KEY_IS_LOGGED_IN, value) }

    var hasCompletedInitialLogin: Boolean
        get() = prefs.getBoolean(KEY_HAS_INITIAL_LOGIN, false)
        set(value) = prefs.edit { putBoolean(KEY_HAS_INITIAL_LOGIN, value) }

    var lastAuthenticatedUserId: String?
        get() = prefs.getString(KEY_LAST_AUTH_USER_ID, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_LAST_AUTH_USER_ID) else putString(KEY_LAST_AUTH_USER_ID, value)
        }

    var lastAuthenticatedUsername: String?
        get() = prefs.getString(KEY_LAST_AUTH_USERNAME, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_LAST_AUTH_USERNAME) else putString(KEY_LAST_AUTH_USERNAME, value)
        }

    /**
     * Scoped quick PIN hash lookup/mutation bound to the currently active or last authenticated userId.
     */
    var quickPinHash: String?
        get() {
            val uid = activeUserId ?: lastAuthenticatedUserId
            return if (!uid.isNullOrBlank()) getQuickPinHashForUser(uid) else prefs.getString(KEY_QUICK_PIN_HASH, null)
        }
        set(value) {
            val uid = activeUserId ?: lastAuthenticatedUserId
            if (!uid.isNullOrBlank()) {
                setQuickPinHashForUser(uid, value)
            } else {
                prefs.edit {
                    if (value == null) remove(KEY_QUICK_PIN_HASH) else putString(KEY_QUICK_PIN_HASH, value)
                }
            }
        }

    fun getQuickPinHashForUser(userId: String): String? {
        if (userId.isBlank()) return null
        return prefs.getString("$KEY_QUICK_PIN_PREFIX$userId", null)
    }

    fun setQuickPinHashForUser(userId: String, hash: String?) {
        if (userId.isBlank()) return
        prefs.edit {
            if (hash == null) remove("$KEY_QUICK_PIN_PREFIX$userId")
            else putString("$KEY_QUICK_PIN_PREFIX$userId", hash)
        }
    }

    fun clearQuickPinForUser(userId: String) {
        if (userId.isBlank()) return
        prefs.edit { remove("$KEY_QUICK_PIN_PREFIX$userId") }
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
        get() = prefs.getString(KEY_LAST_SYNC, "لم تتم المزامنة بعد") ?: "لم تتم المزامنة بعد"
        set(value) = prefs.edit { putString(KEY_LAST_SYNC, value) }

    var serverProfilesJson: String?
        get() = prefs.getString(KEY_SERVER_PROFILES_JSON, null)
        set(value) = prefs.edit {
            if (value.isNullOrBlank()) remove(KEY_SERVER_PROFILES_JSON) else putString(KEY_SERVER_PROFILES_JSON, value)
        }

    fun getServerAuthToken(host: String): String? {
        val norm = normalizeHostKey(host)
        if (norm.isBlank()) return null
        return prefs.getString("$KEY_SERVER_TOKEN_PREFIX$norm", null)?.takeIf { it.isNotBlank() }
    }

    fun setServerAuthToken(host: String, token: String?) {
        val norm = normalizeHostKey(host)
        if (norm.isBlank()) return
        prefs.edit {
            val clean = token?.trim().orEmpty()
            if (clean.isEmpty()) remove("$KEY_SERVER_TOKEN_PREFIX$norm")
            else putString("$KEY_SERVER_TOKEN_PREFIX$norm", clean)
        }
    }

    private fun normalizeHostKey(host: String): String =
        host.trim().lowercase().removePrefix("https://").removePrefix("http://").trimEnd('/')

    var syncEnvelopeKeyBase64: String?
        get() = prefs.getString(KEY_SYNC_ENVELOPE_KEY, null)
        set(value) = prefs.edit {
            if (value == null) remove(KEY_SYNC_ENVELOPE_KEY) else putString(KEY_SYNC_ENVELOPE_KEY, value)
        }

    fun getOrCreateSyncEnvelopeKey(): ByteArray {
        val existingB64 = syncEnvelopeKeyBase64
        if (!existingB64.isNullOrBlank()) {
            val decoded = runCatching { android.util.Base64.decode(existingB64, android.util.Base64.NO_WRAP) }.getOrNull()
            if (decoded != null && decoded.size == 32) return decoded
        }
        val fresh = SecurityUtils.generateRandomKeyBytes(32)
        syncEnvelopeKeyBase64 = android.util.Base64.encodeToString(fresh, android.util.Base64.NO_WRAP)
        return fresh
    }

    fun saveActiveSession(userId: String, username: String, loggedIn: Boolean = true) {
        prefs.edit {
            putString(KEY_ACTIVE_USER_ID, userId)
            putString(KEY_ACTIVE_USERNAME, username)
            putString(KEY_LAST_AUTH_USER_ID, userId)
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
        val uid = activeUserId ?: lastAuthenticatedUserId
        prefs.edit {
            remove(KEY_QUICK_PIN_HASH)
            if (!uid.isNullOrBlank()) remove("$KEY_QUICK_PIN_PREFIX$uid")
        }
    }

    private data class StorageBacking(
        val prefs: SharedPreferences,
        val isHardwareEncrypted: Boolean
    )

    /**
     * Volatile RAM-only SharedPreferences sealed with an ephemeral in-process AES-256-GCM key.
     * Never writes plaintext secrets to disk if EncryptedSharedPreferences initialization fails.
     */
    private class VolatileEncryptedMemoryPreferences : SharedPreferences {
        private val store = ConcurrentHashMap<String, String>()
        private val listeners = ConcurrentHashMap.newKeySet<SharedPreferences.OnSharedPreferenceChangeListener>()

        private fun seal(raw: String): String = SecurityUtils.encryptAesGcm(raw, ephemeralRamKey)
        private fun unseal(token: String?): String? = token?.let { SecurityUtils.decryptAesGcm(it, ephemeralRamKey) }

        override fun getAll(): MutableMap<String, *> {
            return store.mapValues { (_, v) -> unseal(v) ?: "" }.toMutableMap()
        }

        override fun getString(key: String?, defValue: String?): String? {
            if (key == null) return defValue
            return unseal(store[key]) ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = defValues

        override fun getInt(key: String?, defValue: Int): Int {
            return getString(key, null)?.toIntOrNull() ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            return getString(key, null)?.toLongOrNull() ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            return getString(key, null)?.toFloatOrNull() ?: defValue
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            return when (getString(key, null)) {
                "true" -> true
                "false" -> false
                else -> defValue
            }
        }

        override fun contains(key: String?): Boolean = key != null && store.containsKey(key)

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val updates = LinkedHashMap<String, String?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor {
                if (key != null) updates[key] = value
                return this
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = this

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
                if (key != null) updates[key] = value.toString()
                return this
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
                if (key != null) updates[key] = value.toString()
                return this
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
                if (key != null) updates[key] = value.toString()
                return this
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
                if (key != null) updates[key] = value.toString()
                return this
            }

            override fun remove(key: String?): SharedPreferences.Editor {
                if (key != null) updates[key] = null
                return this
            }

            override fun clear(): SharedPreferences.Editor {
                clearAll = true
                return this
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearAll) store.clear()
                val changedKeys = ArrayList<String>(updates.size)
                updates.forEach { (k, v) ->
                    if (v == null) store.remove(k) else store[k] = seal(v)
                    changedKeys.add(k)
                }
                changedKeys.forEach { k ->
                    listeners.forEach { l -> l.onSharedPreferenceChanged(this@VolatileEncryptedMemoryPreferences, k) }
                }
            }
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            listener?.let { listeners.add(it) }
        }

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {
            listener?.let { listeners.remove(it) }
        }
    }

    companion object {
        private const val ENCRYPTED_PREFS_NAME = "social_fund_v9_encrypted_session"

        private const val KEY_ACTIVE_USER_ID = "active_user_id"
        private const val KEY_ACTIVE_USERNAME = "active_username"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_HAS_INITIAL_LOGIN = "has_completed_initial_login"
        private const val KEY_LAST_AUTH_USER_ID = "last_authenticated_user_id"
        private const val KEY_LAST_AUTH_USERNAME = "last_authenticated_username"
        private const val KEY_QUICK_PIN_HASH = "quick_pin_hash"
        private const val KEY_QUICK_PIN_PREFIX = "quick_pin_hash_"
        private const val KEY_DARK_MODE = "is_dark_mode"
        private const val KEY_FONT_SCALE = "font_scale"
        private const val KEY_APP_FONT = "app_font_family"
        private const val KEY_AUTO_SYNC = "auto_sync_on_connect"
        private const val KEY_SERVER_NAME = "server_name"
        private const val KEY_SERVER_HOST = "server_host"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_SYNC_ENVELOPE_KEY = "sync_envelope_aes_key_b64"
        private const val KEY_SERVER_PROFILES_JSON = "server_profiles_json_v1"
        private const val KEY_SERVER_TOKEN_PREFIX = "server_bearer_token_"

        private const val DEFAULT_SERVER_NAME = "الخزنة المحلية المدمجة (Local Vault)"
        private const val DEFAULT_SERVER_HOST = "127.0.0.1:9000"

        private val ephemeralRamKey: ByteArray by lazy { SecurityUtils.generateRandomKeyBytes(32) }
        private val volatileRamPrefs: VolatileEncryptedMemoryPreferences by lazy {
            VolatileEncryptedMemoryPreferences()
        }

        private fun createEncryptedBacking(context: Context): StorageBacking {
            return try {
                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
                val encrypted = EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
                StorageBacking(encrypted, isHardwareEncrypted = true)
            } catch (_: Exception) {
                // Never persist plaintext SharedPreferences to disk; use volatile AES-GCM RAM backing only
                StorageBacking(volatileRamPrefs, isHardwareEncrypted = false)
            }
        }
    }
}
