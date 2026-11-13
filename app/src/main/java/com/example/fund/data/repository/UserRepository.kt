package com.example.fund.data.repository

import com.example.fund.data.SeedData
import com.example.fund.data.local.AppUserEntity
import com.example.fund.data.local.AuditLogEntity
import com.example.fund.data.local.FundDatabase
import com.example.fund.model.AppSettings
import com.example.fund.model.AppUser
import com.example.fund.model.AuditLog
import com.example.fund.model.LocalServerConfig
import com.example.fund.model.UserRole
import com.example.fund.security.EncryptedSessionManager
import com.example.fund.security.SecurityUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class AuthResult {
    data class Success(val user: AppUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class UserRepository(
    private val dbScope: CoroutineScope,
    private val dbDispatcher: CoroutineDispatcher
) {
    private val _users = MutableStateFlow(SeedData.users)
    val users: StateFlow<List<AppUser>> = _users.asStateFlow()

    private val _currentUser = MutableStateFlow(SeedData.users[0])
    val currentUser: StateFlow<AppUser> = _currentUser.asStateFlow()

    private val _auditLogs = MutableStateFlow(SeedData.auditLogs)
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(true)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _hasCompletedInitialLogin = MutableStateFlow(false)
    val hasCompletedInitialLogin: StateFlow<Boolean> = _hasCompletedInitialLogin.asStateFlow()

    private val _hasQuickPin = MutableStateFlow(false)
    val hasQuickPin: StateFlow<Boolean> = _hasQuickPin.asStateFlow()

    private var inMemoryQuickPinHash: String? = null

    private var db: FundDatabase? = null
    private var sessionManager: EncryptedSessionManager? = null
    private var settingsProvider: () -> AppSettings = { AppSettings() }
    private var serverConfigProvider: () -> LocalServerConfig = { LocalServerConfig() }
    private var isOnlineProvider: () -> Boolean = { true }
    private var onOfflineAuditHook: ((action: String, description: String) -> Unit)? = null

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
    private fun nowTimeStr() = timeFormat.format(Date())

    fun configureHooks(
        settingsProvider: () -> AppSettings,
        serverConfigProvider: () -> LocalServerConfig,
        isOnlineProvider: () -> Boolean,
        onOfflineAuditHook: (String, String) -> Unit
    ) {
        this.settingsProvider = settingsProvider
        this.serverConfigProvider = serverConfigProvider
        this.isOnlineProvider = isOnlineProvider
        this.onOfflineAuditHook = onOfflineAuditHook
    }

    suspend fun attachAndHydrate(database: FundDatabase, session: EncryptedSessionManager) {
        db = database
        sessionManager = session

        val dbUsers = database.userDao().observeUsers().first().map { it.toDomain() }
        if (dbUsers.isNotEmpty()) {
            _users.value = dbUsers
        }

        val dbLogs = database.userDao().observeAuditLogs().first().map { it.toDomain() }
        if (dbLogs.isNotEmpty()) {
            _auditLogs.value = dbLogs
        }

        val savedUsername = session.activeUsername ?: session.lastAuthenticatedUsername
        val savedUserId = session.activeUserId
        val restored = _users.value.firstOrNull {
            (savedUserId != null && it.id == savedUserId) ||
                (savedUsername != null && it.username.equals(savedUsername, ignoreCase = true))
        }
        if (restored != null && restored.isActive) {
            _currentUser.value = restored
        }
        _isLoggedIn.value = session.isLoggedIn
        _hasCompletedInitialLogin.value = session.hasCompletedInitialLogin
        inMemoryQuickPinHash = session.quickPinHash
        _hasQuickPin.value = !inMemoryQuickPinHash.isNullOrBlank()
    }

    /**
     * Authenticates using username + password verified via PBKDF2WithHmacSHA256.
     */
    fun authenticateWithPassword(usernameInput: String, passwordInput: String): AuthResult {
        val clean = usernameInput.trim().lowercase()
        if (clean.isBlank() || passwordInput.isBlank()) {
            return AuthResult.Error("يرجى إدخال اسم المستخدم وكلمة المرور.")
        }

        val matched = _users.value.find { it.username.lowercase() == clean }
            ?: run {
                logAudit("محاولة دخول فاشلة", "محاولة دخول باسم مستخدم غير مسجل: $clean")
                return AuthResult.Error("اسم المستخدم غير موجود في سجل موظفي الصندوق.")
            }

        if (!matched.isActive) {
            logAudit("حساب موقوف", "محاولة دخول لحساب موقوف: ${matched.username}")
            return AuthResult.Error("هذا الحساب موقوف حالياً، يرجى مراجعة مدير النظام.")
        }

        if (!SecurityUtils.verifyPassword(passwordInput, matched.passwordHash)) {
            logAudit("فشل كلمة المرور", "كلمة مرور غير صحيحة للحساب ${matched.username}")
            return AuthResult.Error("كلمة المرور غير صحيحة. (للحسابات التجريبية الافتراضية: 123456)")
        }

        return AuthResult.Success(completeLogin(matched, "تسجيل دخول آمن (PBKDF2)"))
    }

    /**
     * Authenticates after OTP / Biometric verification or internal demo profile selection.
     */
    fun authenticateUser(usernameInput: String, methodLabel: String = "تسجيل دخول آمن"): AppUser {
        val clean = usernameInput.trim().lowercase()
        val matched = _users.value.find { it.username.lowercase() == clean && it.isActive }
            ?: _users.value.firstOrNull { it.isActive }
            ?: SeedData.users[0]
        return completeLogin(matched, methodLabel)
    }

    fun setQuickPin(pin: String): Boolean {
        val cleanPin = pin.trim()
        if (cleanPin.length !in 4..6 || !cleanPin.all { it.isDigit() }) {
            return false
        }
        val hashed = SecurityUtils.hashPassword(cleanPin)
        inMemoryQuickPinHash = hashed
        sessionManager?.quickPinHash = hashed
        _hasQuickPin.value = true
        logAudit("إعداد الدخول السريع", "تم تعيين رمز الدخول السريع (PIN) المشفر للحساب ${_currentUser.value.username}")
        return true
    }

    fun authenticateWithQuickPin(usernameInput: String, pin: String): AuthResult {
        if (!_hasCompletedInitialLogin.value) {
            return AuthResult.Error("يرجى تسجيل الدخول بكلمة المرور لمرة واحدة أولاً لتفعيل الدخول السريع.")
        }
        val cleanPin = pin.trim()
        val storedHash = inMemoryQuickPinHash ?: sessionManager?.quickPinHash
        if (storedHash.isNullOrBlank()) {
            return AuthResult.Error("لم يتم تعيين رمز دخول سريع (PIN) بعد.")
        }
        if (!SecurityUtils.verifyPassword(cleanPin, storedHash)) {
            logAudit("فشل رمز PIN", "إدخال رمز PIN سريع غير صحيح للحساب $usernameInput")
            return AuthResult.Error("رمز الدخول السريع (PIN) غير صحيح.")
        }
        val targetUsername = usernameInput.trim().ifBlank {
            sessionManager?.lastAuthenticatedUsername ?: _currentUser.value.username
        }
        val matched = _users.value.find { it.username.equals(targetUsername, ignoreCase = true) && it.isActive }
            ?: _users.value.firstOrNull { it.isActive }
            ?: return AuthResult.Error("لم يتم العثور على حساب نشط مطابق.")
        return AuthResult.Success(completeLogin(matched, "دخول سريع بالرمز (PIN)"))
    }

    private fun completeLogin(matched: AppUser, auditAction: String): AppUser {
        val updatedUser = matched.copy(lastLogin = nowTimeStr())
        _users.value = _users.value.map { if (it.id == updatedUser.id) updatedUser else it }
        _currentUser.value = updatedUser
        _isLoggedIn.value = true
        _hasCompletedInitialLogin.value = true
        sessionManager?.saveActiveSession(updatedUser.id, updatedUser.username, loggedIn = true)

        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.userDao().upsertUser(AppUserEntity.fromDomain(updatedUser))
            }
        }
        logAudit(
            auditAction,
            "دخول المستخدم ${updatedUser.fullName} (${updatedUser.role.title}) عبر ${serverConfigProvider().serverName}"
        )
        return updatedUser
    }

    fun logout() {
        val user = _currentUser.value
        _isLoggedIn.value = false
        sessionManager?.clearSession()
        logAudit("تسجيل خروج", "تسجيل خروج آمن للمستخدم ${user.fullName}")
    }

    fun switchRole(role: UserRole) {
        val matched = _users.value.find { it.role == role } ?: AppUser(
            username = role.name.lowercase(),
            fullName = "مستخدم (${role.title})",
            role = role,
            passwordHash = SecurityUtils.hashPassword(SeedData.defaultDemoPassword)
        )
        _currentUser.value = matched
        sessionManager?.saveActiveSession(matched.id, matched.username, loggedIn = true)
        logAudit("تبديل الدور", "تم تبديل دور المستخدم الحالي إلى ${role.title} (${matched.fullName})")
    }

    fun addUser(
        username: String,
        fullName: String,
        role: UserRole,
        rawPassword: String = SeedData.defaultDemoPassword
    ): AppUser {
        val hashed = SecurityUtils.hashPassword(rawPassword.ifBlank { SeedData.defaultDemoPassword })
        val u = AppUser(
            username = username.trim(),
            fullName = fullName.trim(),
            role = role,
            isActive = true,
            lastLogin = nowTimeStr(),
            passwordHash = hashed
        )
        _users.value = _users.value + u
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.userDao().upsertUser(AppUserEntity.fromDomain(u))
            }
        }
        logAudit("إضافة مستخدم", "إنشاء حساب موظف جديد: $fullName بدور ${role.title}")
        return u
    }

    fun toggleUserActive(userId: String) {
        var updated: AppUser? = null
        _users.value = _users.value.map {
            if (it.id == userId) {
                it.copy(isActive = !it.isActive).also { u -> updated = u }
            } else it
        }
        updated?.let { u ->
            db?.let { database ->
                dbScope.launch(dbDispatcher) {
                    database.userDao().upsertUser(AppUserEntity.fromDomain(u))
                }
            }
        }
        logAudit("تحديث حالة موظف", "تغيير حالة تفعيل المستخدم رقم $userId")
    }

    fun logAudit(action: String, description: String) {
        val user = _currentUser.value
        val ts = nowTimeStr()
        val settings = settingsProvider()
        val server = serverConfigProvider()
        val online = isOnlineProvider()

        if (settings.enableAuditTrail || action == "إعدادات النظام") {
            val log = AuditLog(
                timestamp = ts,
                userName = "${user.fullName} (${user.username})",
                userRole = user.role.title,
                actionType = action,
                description = description,
                ipOrDevice = "${server.serverHost} • ${if (online) "متصل" else "محلي Offline"}"
            )
            _auditLogs.value = listOf(log) + _auditLogs.value
            db?.let { database ->
                val order = System.currentTimeMillis()
                dbScope.launch(dbDispatcher) {
                    database.userDao().insertAuditLog(AuditLogEntity.fromDomain(log, order))
                }
            }
        }

        if (!online && action != "انقطاع الاتصال" && action != "اتصال بالشبكة") {
            onOfflineAuditHook?.invoke(action, description)
        }
    }

    fun resetToSeed() {
        _users.value = SeedData.users
        _currentUser.value = SeedData.users[0]
        _auditLogs.value = SeedData.auditLogs
        _hasCompletedInitialLogin.value = false
        _hasQuickPin.value = false
        inMemoryQuickPinHash = null
        sessionManager?.clearQuickPin()
    }
}
