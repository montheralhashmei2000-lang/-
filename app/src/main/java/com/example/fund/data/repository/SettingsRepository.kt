package com.example.fund.data.repository

import com.example.fund.data.SeedData
import com.example.fund.data.local.*
import com.example.fund.model.*
import com.example.fund.security.EncryptedSessionManager
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

class SettingsRepository(
    private val dbScope: CoroutineScope,
    private val dbDispatcher: CoroutineDispatcher,
    private val userRepository: UserRepository
) {
    private val _fundInfo = MutableStateFlow(FundInfo())
    val fundInfo: StateFlow<FundInfo> = _fundInfo.asStateFlow()

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _printSettings = MutableStateFlow(PrintSettings())
    val printSettings: StateFlow<PrintSettings> = _printSettings.asStateFlow()

    private val _schedulerEvents = MutableStateFlow(SeedData.schedulerEvents)
    val schedulerEvents: StateFlow<List<SchedulerEvent>> = _schedulerEvents.asStateFlow()

    private val _internalMessages = MutableStateFlow(SeedData.internalMessages)
    val internalMessages: StateFlow<List<InternalMessage>> = _internalMessages.asStateFlow()

    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _fontScale = MutableStateFlow(1.0f)
    val fontScale: StateFlow<Float> = _fontScale.asStateFlow()

    private var db: FundDatabase? = null
    private var sessionManager: EncryptedSessionManager? = null

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
    private fun nowTimeStr() = timeFormat.format(Date())

    suspend fun attachAndHydrate(database: FundDatabase, session: EncryptedSessionManager) {
        db = database
        sessionManager = session

        _isDarkMode.value = session.isDarkMode
        _fontScale.value = session.fontScale

        database.settingsDao().getFundInfo()?.toDomain()?.let { _fundInfo.value = it }
        database.settingsDao().getAppSettings()?.toDomain()?.let { _appSettings.value = it }
        database.settingsDao().getPrintSettings()?.toDomain()?.let { _printSettings.value = it }

        val dbEvents = database.settingsDao().observeSchedulerEvents().first().map { it.toDomain() }
        if (dbEvents.isNotEmpty()) _schedulerEvents.value = dbEvents

        val dbMessages = database.settingsDao().observeMessages().first().map { it.toDomain() }
        if (dbMessages.isNotEmpty()) _internalMessages.value = dbMessages
    }

    fun setDarkModePreference(enabled: Boolean) {
        _isDarkMode.value = enabled
        sessionManager?.isDarkMode = enabled
    }

    fun setFontScalePreference(scale: Float) {
        _fontScale.value = scale
        sessionManager?.fontScale = scale
    }

    fun updateFundInfo(info: FundInfo) {
        _fundInfo.value = info
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.settingsDao().upsertFundInfo(FundInfoEntity.fromDomain(info))
            }
        }
        userRepository.logAudit("تحديث بيانات الصندوق", "تم حفظ وتحديث البيانات والترخيص الرسمي للصندوق")
    }

    fun updateFundLogo(logoBase64: String) {
        val updated = _fundInfo.value.copy(customLogoBase64 = logoBase64)
        _fundInfo.value = updated
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.settingsDao().upsertFundInfo(FundInfoEntity.fromDomain(updated))
            }
        }
        userRepository.logAudit(
            "تحديث شعار الصندوق",
            if (logoBase64.isBlank()) "استعادة الشعار الافتراضي للصندوق" else "تعيين شعار مخصص جديد للصندوق"
        )
    }

    fun updateAppSettings(settings: AppSettings) {
        _appSettings.value = settings
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.settingsDao().upsertAppSettings(AppSettingsEntity.fromDomain(settings))
            }
        }
        userRepository.logAudit("إعدادات النظام", "تحديث الإعدادات التشغيلية والمالية الشاملة لجميع الشاشات")
    }

    fun updatePrintSettings(settings: PrintSettings) {
        _printSettings.value = settings
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.settingsDao().upsertPrintSettings(PrintSettingsEntity.fromDomain(settings))
            }
        }
        userRepository.logAudit("إعدادات الطباعة والسندات", "تحديث قوالب الترويسة والتذييل والخطوط وحقول التوقيعات")
    }

    fun resetPrintSettingsToDefault() {
        val defaultPs = PrintSettings()
        _printSettings.value = defaultPs
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.settingsDao().upsertPrintSettings(PrintSettingsEntity.fromDomain(defaultPs))
            }
        }
        userRepository.logAudit("إعدادات الطباعة والسندات", "استعادة الإعدادات الافتراضية لقوالب الطباعة والسندات")
    }

    fun addSchedulerEvent(event: SchedulerEvent) {
        _schedulerEvents.value = listOf(event) + _schedulerEvents.value
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.settingsDao().upsertSchedulerEvent(SchedulerEventEntity.fromDomain(event))
            }
        }
        userRepository.logAudit("إضافة موعد", "جدولة فعالية: ${event.title}")
    }

    fun sendMessage(subject: String, body: String, recipientRole: String) {
        val user = userRepository.currentUser.value
        val msg = InternalMessage(
            senderName = user.fullName,
            senderRole = user.role.title,
            recipientRole = recipientRole,
            subject = subject,
            body = body,
            timestamp = nowTimeStr(),
            isRead = false
        )
        _internalMessages.value = listOf(msg) + _internalMessages.value
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.settingsDao().upsertMessage(InternalMessageEntity.fromDomain(msg))
            }
        }
        userRepository.logAudit("إرسال رسالة", "إرسال رسالة داخلية: $subject إلى $recipientRole")
    }

    fun markMessageRead(id: String) {
        var updatedMsg: InternalMessage? = null
        _internalMessages.value = _internalMessages.value.map {
            if (it.id == id) it.copy(isRead = true).also { m -> updatedMsg = m } else it
        }
        updatedMsg?.let { msg ->
            db?.let { database ->
                dbScope.launch(dbDispatcher) {
                    database.settingsDao().upsertMessage(InternalMessageEntity.fromDomain(msg))
                }
            }
        }
    }

    fun applySnapshot(
        fundInfo: FundInfo?,
        appSettings: AppSettings?,
        printSettings: PrintSettings?,
        schedulerEvents: List<SchedulerEvent>?,
        internalMessages: List<InternalMessage>?
    ) {
        fundInfo?.let { _fundInfo.value = it }
        appSettings?.let { _appSettings.value = it }
        printSettings?.let { _printSettings.value = it }
        schedulerEvents?.let { _schedulerEvents.value = it }
        internalMessages?.let { _internalMessages.value = it }
    }

    fun resetToSeed() {
        _fundInfo.value = FundInfo()
        _appSettings.value = AppSettings()
        _printSettings.value = PrintSettings()
        _schedulerEvents.value = SeedData.schedulerEvents
        _internalMessages.value = SeedData.internalMessages
    }
}
