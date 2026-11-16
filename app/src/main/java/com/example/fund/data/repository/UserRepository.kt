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
import java.util.concurrent.ConcurrentHashMap

sealed class AuthResult {
    data class Success(val user: AppUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class UserRepository(
    private val dbScope: CoroutineScope,
    private val dbDispatcher: CoroutineDispatcher
) {
    companion object {
        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L
    }

    private val _users = MutableStateFlow(SeedData.users)
    val users: StateFlow<List<AppUser>> = _users.asStateFlow()

    private val _currentUser = MutableStateFlow(SeedData.users[0])
    val currentUser: StateFlow<AppUser> = _currentUser.asStateFlow()

    private val _auditLogs = MutableStateFlow(SeedData.auditLogs)
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    // Secure default: never auto-authenticate on fresh install
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _hasCompletedInitialLogin = MutableStateFlow(false)
    val hasCompletedInitialLogin: StateFlow<Boolean> = _hasCompletedInitialLogin.asStateFlow()

    private val _hasQuickPin = MutableStateFlow(false)
    val hasQuickPin: StateFlow<Boolean> = _hasQuickPin.asStateFlow()

    private val _requiresPasswordChange = MutableStateFlow(false)
    val requiresPasswordChange: StateFlow<Boolean> = _requiresPasswordChange.asStateFlow()

    private val failedAttempts = ConcurrentHashMap<String, Int>()
    private val lockoutUntilMs = ConcurrentHashMap<String, Long>()

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
        val isDbInitialized = database.settingsDao().getFundInfo() != null || dbUsers.isNotEmpty()
        if (isDbInitialized) {
            _auditLogs.value = dbLogs
        }

        val savedUserId = session.activeUserId ?: session.lastAuthenticatedUserId
        val savedUsername = session.activeUsername ?: session.lastAuthenticatedUsername
        val restored = _users.value.firstOrNull {
            (!savedUserId.isNullOrBlank() && it.id == savedUserId) ||
                (!savedUsername.isNullOrBlank() && it.username.equals(savedUsername, ignoreCase = true))
        }
        val canRestoreLoggedIn = session.isLoggedIn && restored != null && restored.isActive
        if (restored != null && restored.isActive) {
            _currentUser.value = restored
        }
        _isLoggedIn.value = canRestoreLoggedIn
        _hasCompletedInitialLogin.value = session.hasCompletedInitialLogin && restored != null && restored.isActive
        _requiresPasswordChange.value = canRestoreLoggedIn && (restored?.mustChangePassword == true)
        syncQuickPinAvailability(restored ?: _currentUser.value)
    }

    private fun syncQuickPinAvailability(user: AppUser) {
        val sessionPin = sessionManager?.getQuickPinHashForUser(user.id)
        _hasQuickPin.value = user.quickPinHash.isNotBlank() || !sessionPin.isNullOrBlank()
    }

    private fun checkLockout(usernameKey: String): AuthResult.Error? {
        val until = lockoutUntilMs[usernameKey] ?: return null
        val now = System.currentTimeMillis()
        if (now < until) {
            val remainingSec = ((until - now) / 1000L).coerceAtLeast(1L)
            return AuthResult.Error("الحساب مقفل مؤقتاً بعد $MAX_FAILED_ATTEMPTS محاولات فاشلة؛ يرجى الانتظار $remainingSec ثانية.")
        }
        lockoutUntilMs.remove(usernameKey)
        failedAttempts.remove(usernameKey)
        return null
    }

    private fun recordFailedAttempt(usernameKey: String): AuthResult.Error {
        val nextCount = (failedAttempts[usernameKey] ?: 0) + 1
        failedAttempts[usernameKey] = nextCount
        if (nextCount >= MAX_FAILED_ATTEMPTS) {
            lockoutUntilMs[usernameKey] = System.currentTimeMillis() + LOCKOUT_DURATION_MS
            logAudit("قفل أمني للحساب", "تم قفل الحساب ($usernameKey) مؤقتاً بعد $MAX_FAILED_ATTEMPTS محاولات دخول فاشلة.")
            return AuthResult.Error("تم تجاوز الحد الأقصى ($MAX_FAILED_ATTEMPTS محاولات فاشلة)؛ تم قفل الحساب مؤقتاً.")
        }
        val remaining = MAX_FAILED_ATTEMPTS - nextCount
        return AuthResult.Error("بيانات الاعتماد غير صحيحة. المحاولات المتبقية قبل القفل المؤقت: $remaining")
    }

    private fun clearFailedAttempts(usernameKey: String) {
        failedAttempts.remove(usernameKey)
        lockoutUntilMs.remove(usernameKey)
    }

    /**
     * Authenticates using username + password verified via PBKDF2WithHmacSHA256.
     */
    fun authenticateWithPassword(usernameInput: String, passwordInput: String): AuthResult {
        val clean = usernameInput.trim().lowercase(Locale.ENGLISH)
        if (clean.isBlank() || passwordInput.isBlank()) {
            return AuthResult.Error("يرجى إدخال اسم المستخدم وكلمة المرور.")
        }

        checkLockout(clean)?.let { return it }

        val matched = _users.value.find { it.username.lowercase(Locale.ENGLISH) == clean }
            ?: run {
                logAudit("محاولة دخول فاشلة", "محاولة دخول باسم مستخدم غير مسجل: $clean")
                return recordFailedAttempt(clean)
            }

        if (!matched.isActive) {
            logAudit("حساب موقوف", "محاولة دخول لحساب موقوف: ${matched.username}")
            return AuthResult.Error("هذا الحساب موقوف حالياً، يرجى مراجعة مدير النظام.")
        }

        if (!SecurityUtils.verifyPassword(passwordInput, matched.passwordHash)) {
            logAudit("فشل كلمة المرور", "كلمة مرور غير صحيحة للحساب ${matched.username}")
            return recordFailedAttempt(clean)
        }

        clearFailedAttempts(clean)
        return AuthResult.Success(completeLogin(matched, "تسجيل دخول آمن (PBKDF2)"))
    }

    /**
     * Switches active user account only after valid password re-authentication.
     */
    fun switchAccountWithPassword(targetUsername: String, passwordInput: String): AuthResult {
        val res = authenticateWithPassword(targetUsername, passwordInput)
        if (res is AuthResult.Success) {
            logAudit("تبديل حساب موثق", "تم التحقق من كلمة المرور والتبديل إلى ${res.user.fullName} (${res.user.role.title})")
        }
        return res
    }

    /**
     * Enforces mandatory change of default password ("123456") or user-initiated password rotation.
     */
    fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        val user = _currentUser.value
        if (!SecurityUtils.verifyPassword(currentPassword, user.passwordHash)) {
            return AuthResult.Error("كلمة المرور الحالية غير صحيحة.")
        }
        val cleanNew = newPassword.trim()
        if (cleanNew.length < 6) {
            return AuthResult.Error("يجب أن تتكون كلمة المرور الجديدة من 6 أحرف/أرقام على الأقل.")
        }
        if (cleanNew == SeedData.defaultDemoPassword || cleanNew == currentPassword) {
            return AuthResult.Error("يجب اختيار كلمة مرور جديدة تختلف عن كلمة المرور الافتراضية والحالية.")
        }

        val newHash = SecurityUtils.hashPassword(cleanNew)
        val updated = user.copy(
            passwordHash = newHash,
            mustChangePassword = false
        )
        _users.value = _users.value.map { if (it.id == updated.id) updated else it }
        _currentUser.value = updated
        _requiresPasswordChange.value = false

        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.userDao().upsertUser(AppUserEntity.fromDomain(updated))
            }
        }
        logAudit("تغيير كلمة المرور", "تم تحديث كلمة المرور وتشفيرها بـ PBKDF2 (120k) للمستخدم ${updated.username}")
        return AuthResult.Success(updated)
    }

    /**
     * Binds a PBKDF2-hashed Quick PIN strictly to the currently authenticated user's ID.
     */
    fun setQuickPin(pin: String): Boolean {
        if (!_isLoggedIn.value && !_hasCompletedInitialLogin.value) return false
        val cleanPin = pin.trim()
        if (cleanPin.length !in 4..6 || !cleanPin.all { it.isDigit() }) {
            return false
        }
        val current = _currentUser.value
        val hashed = SecurityUtils.hashPassword(cleanPin)
        val updated = current.copy(quickPinHash = hashed)

        _users.value = _users.value.map { if (it.id == updated.id) updated else it }
        _currentUser.value = updated
        sessionManager?.setQuickPinHashForUser(updated.id, hashed)
        _hasQuickPin.value = true

        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.userDao().upsertUser(AppUserEntity.fromDomain(updated))
            }
        }
        logAudit("إعداد الدخول السريع", "تم ربط رمز PIN المشفر بالحساب ${updated.username} (${updated.id})")
        return true
    }

    /**
     * Resolves bound user strictly from the previously authenticated session; prevents username swapping.
     */
    fun getBoundQuickAuthUser(): AppUser? {
        if (!_hasCompletedInitialLogin.value) return null
        val boundId = sessionManager?.lastAuthenticatedUserId
        val boundUsername = sessionManager?.lastAuthenticatedUsername
        return _users.value.firstOrNull {
            (!boundId.isNullOrBlank() && it.id == boundId) ||
                (!boundUsername.isNullOrBlank() && it.username.equals(boundUsername, ignoreCase = true))
        }?.takeIf { it.isActive } ?: _currentUser.value.takeIf { it.isActive }
    }

    fun authenticateWithQuickPin(usernameInput: String, pin: String): AuthResult {
        if (!_hasCompletedInitialLogin.value) {
            return AuthResult.Error("يرجى تسجيل الدخول بكلمة المرور لمرة واحدة أولاً لتفعيل الدخول السريع.")
        }
        val boundUser = getBoundQuickAuthUser()
            ?: return AuthResult.Error("لا يوجد حساب نشط مرتبط بالدخول السريع.")

        val requestedName = usernameInput.trim()
        if (requestedName.isNotBlank() && !requestedName.equals(boundUser.username, ignoreCase = true)) {
            logAudit("محاولة تبديل مستخدم PIN", "رفض محاولة تغيير اسم المستخدم من ${boundUser.username} إلى $requestedName أثناء دخول PIN")
            return AuthResult.Error("رمز PIN مقيد بالحساب (@${boundUser.username}) فقط؛ استخدم كلمة المرور لتبديل الحساب.")
        }

        val lockKey = boundUser.username.lowercase(Locale.ENGLISH)
        checkLockout(lockKey)?.let { return it }

        val storedHash = boundUser.quickPinHash.ifBlank {
            sessionManager?.getQuickPinHashForUser(boundUser.id).orEmpty()
        }
        if (storedHash.isBlank()) {
            return AuthResult.Error("لم يتم تعيين رمز دخول سريع (PIN) لهذا الحساب بعد.")
        }
        val cleanPin = pin.trim()
        if (!SecurityUtils.verifyPassword(cleanPin, storedHash)) {
            logAudit("فشل رمز PIN", "إدخال رمز PIN سريع غير صحيح للحساب ${boundUser.username}")
            return recordFailedAttempt(lockKey)
        }

        clearFailedAttempts(lockKey)
        return AuthResult.Success(completeLogin(boundUser, "دخول سريع بالرمز (PIN)"))
    }

    /**
     * Authenticates strictly for [userId] after AndroidKeyStore CryptoObject verification succeeds.
     */
    fun authenticateWithBiometrics(userId: String, cryptoVerified: Boolean): AuthResult {
        if (!_hasCompletedInitialLogin.value) {
            return AuthResult.Error("يرجى تسجيل الدخول بكلمة المرور لمرة واحدة أولاً لتفعيل البصمة.")
        }
        if (!cryptoVerified || userId.isBlank()) {
            return AuthResult.Error("فشل التحقق التشفيري من مفتاح البصمة (AndroidKeyStore CryptoObject).")
        }
        val boundUser = getBoundQuickAuthUser()
            ?: return AuthResult.Error("لا يوجد حساب نشط مرتبط بالبصمة الحيوية.")
        if (boundUser.id != userId) {
            return AuthResult.Error("البصمة مرتبطة بحساب آخر (@${boundUser.username}) ولا يمكن استخدامها لهذا الحساب.")
        }
        val lockKey = boundUser.username.lowercase(Locale.ENGLISH)
        checkLockout(lockKey)?.let { return it }

        clearFailedAttempts(lockKey)
        return AuthResult.Success(completeLogin(boundUser, "مصادقة حيوية محمية بـ AndroidKeyStore"))
    }

    private fun completeLogin(matched: AppUser, auditAction: String): AppUser {
        val updatedUser = matched.copy(lastLogin = nowTimeStr())
        _users.value = _users.value.map { if (it.id == updatedUser.id) updatedUser else it }
        _currentUser.value = updatedUser
        _isLoggedIn.value = true
        _hasCompletedInitialLogin.value = true
        _requiresPasswordChange.value = updatedUser.mustChangePassword
        sessionManager?.saveActiveSession(updatedUser.id, updatedUser.username, loggedIn = true)
        syncQuickPinAvailability(updatedUser)

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
        _requiresPasswordChange.value = false
        sessionManager?.clearSession()
        logAudit("تسجيل خروج", "تسجيل خروج آمن للمستخدم ${user.fullName}")
    }

    /**
     * Centralized RBAC guard executed at the repository layer whenever [strictRolePermissions] is enabled.
     */
    fun requirePermission(vararg allowedRoles: UserRole, actionDescription: String) {
        if (!settingsProvider().strictRolePermissions) return
        val user = _currentUser.value
        if (!user.isActive) {
            logAudit("رفض حساب موقوف", "محاولة تنفيذ ($actionDescription) من حساب موقوف: ${user.username}")
            throw SecurityException("الحساب الحالي موقوف ولا يملك صلاحية التنفيذ.")
        }
        if (user.role !in allowedRoles) {
            logAudit(
                "رفض صلاحية (RBAC)",
                "محاولة غير مصرح بها لتنفيذ ($actionDescription) بدور (${user.role.title})"
            )
            throw SecurityException("صلاحية غير كافية (${user.role.title}) لتنفيذ: $actionDescription")
        }
    }

    fun addUser(
        username: String,
        fullName: String,
        role: UserRole,
        rawPassword: String = SeedData.defaultDemoPassword
    ): AppUser {
        requirePermission(UserRole.ADMIN, actionDescription = "إضافة مستخدم جديد")
        val cleanUsername = username.trim()
        require(cleanUsername.isNotBlank() && fullName.trim().isNotBlank()) { "اسم المستخدم والاسم الكامل مطلوبان." }
        require(_users.value.none { it.username.equals(cleanUsername, ignoreCase = true) }) {
            "اسم المستخدم ($cleanUsername) مسجل مسبقاً."
        }
        val effectivePw = rawPassword.ifBlank { SeedData.defaultDemoPassword }
        val hashed = SecurityUtils.hashPassword(effectivePw)
        val u = AppUser(
            username = cleanUsername,
            fullName = fullName.trim(),
            role = role,
            isActive = true,
            lastLogin = nowTimeStr(),
            passwordHash = hashed,
            quickPinHash = "",
            mustChangePassword = effectivePw == SeedData.defaultDemoPassword
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
        requirePermission(UserRole.ADMIN, actionDescription = "تعديل حالة تفعيل مستخدم")
        require(userId != _currentUser.value.id) { "لا يمكن إيقاف الحساب النشط حالياً." }
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

        val isSyncableDataMutation = action !in setOf(
            "انقطاع الاتصال",
            "اتصال بالشبكة",
            "تعذر الاتصال بالخادم",
            "تغيير خادم المزامنة",
            "تغيير الخادم المحلي",
            "إعدادات المزامنة",
            "إعادة ضبط المصنع",
            "تسجيل دخول آمن (PBKDF2)",
            "دخول سريع بالرمز (PIN)",
            "مصادقة حيوية محمية بـ AndroidKeyStore",
            "تسجيل خروج",
            "محاولة دخول فاشلة",
            "فشل كلمة المرور",
            "فشل رمز PIN",
            "قفل أمني للحساب",
            "حساب موقوف",
            "رفض صلاحية (RBAC)",
            "رفض حساب موقوف",
            "محاولة تبديل مستخدم PIN",
            "تصدير PDF",
            "معاينة طباعة"
        ) && !action.startsWith("فشل مزامنة") && !action.startsWith("مزامنة ناجحة")

        if (!online && isSyncableDataMutation) {
            onOfflineAuditHook?.invoke(action, description)
        }
    }

    fun applyUsersAndAuditSnapshot(users: List<AppUser>?, auditLogs: List<AuditLog>?) {
        users?.let { restoredUsers ->
            _users.value = restoredUsers
            val refreshedCurrent = restoredUsers.find { it.id == _currentUser.value.id }
                ?: restoredUsers.firstOrNull { it.isActive }
            if (refreshedCurrent != null) {
                _currentUser.value = refreshedCurrent
                _requiresPasswordChange.value = _isLoggedIn.value && refreshedCurrent.mustChangePassword
                syncQuickPinAvailability(refreshedCurrent)
            }
        }
        auditLogs?.let { _auditLogs.value = it }
    }

    fun resetToSeed() {
        _users.value = SeedData.users
        _currentUser.value = SeedData.users[0]
        _auditLogs.value = SeedData.auditLogs
        _isLoggedIn.value = false
        _hasCompletedInitialLogin.value = false
        _hasQuickPin.value = false
        _requiresPasswordChange.value = false
        failedAttempts.clear()
        lockoutUntilMs.clear()
        sessionManager?.clearSession()
        sessionManager?.clearQuickPin()
    }
}
