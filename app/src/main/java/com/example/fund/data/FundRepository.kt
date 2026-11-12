package com.example.fund.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.fund.model.*
import com.example.fund.util.TafqeetEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class FundRepository private constructor() {

    companion object {
        @Volatile
        private var instance: FundRepository? = null

        fun getInstance(): FundRepository =
            instance ?: synchronized(this) {
                instance ?: FundRepository().also { instance = it }
            }
    }

    private var prefs: SharedPreferences? = null
    private val repoScope = CoroutineScope(Dispatchers.Default)

    // App Preferences, Local Server & Session
    val currentUser = MutableStateFlow(SeedData.users[0])
    val isOnline = MutableStateFlow(true)
    val isDarkMode = MutableStateFlow(false)
    val fontScale = MutableStateFlow(1.0f) // 0.9f, 1.0f, 1.15f, 1.3f

    // Local Server & Synchronization Engine State
    val serverConfig = MutableStateFlow(LocalServerConfig())
    val autoSyncOnConnect = MutableStateFlow(true)
    val isSyncing = MutableStateFlow(false)
    val syncProgress = MutableStateFlow(0f)
    val syncStageMessage = MutableStateFlow("متصل ومزامن بالكامل مع الخادم المحلي")
    val lastSyncTimestamp = MutableStateFlow("2026-11-08 10:45")
    val syncHistory = MutableStateFlow(
        listOf(
            SyncBatchRecord(
                batchNo = "SYNC-2026-019",
                timestamp = "2026-11-08 10:45",
                operationsCount = 6,
                serverNode = "192.168.1.100:8443 (الخادم المحلي)",
                durationMs = 380L
            ),
            SyncBatchRecord(
                batchNo = "SYNC-2026-018",
                timestamp = "2026-11-07 18:20",
                operationsCount = 11,
                serverNode = "sync.social-fund.org (السحابة المركزية)",
                durationMs = 640L
            )
        )
    )

    // Core Entities & Configuration
    val fundInfo = MutableStateFlow(FundInfo())
    val regions = MutableStateFlow(SeedData.regions)
    val appSettings = MutableStateFlow(AppSettings())
    val printSettings = MutableStateFlow(PrintSettings())
    val accounts = MutableStateFlow(SeedData.accounts)
    val members = MutableStateFlow(SeedData.members)
    val subscriptions = MutableStateFlow(SeedData.subscriptions)
    val aidRequests = MutableStateFlow(SeedData.aidRequests)
    val vouchers = MutableStateFlow(SeedData.vouchers)
    val journalEntries = MutableStateFlow(SeedData.journalEntries)
    val budgets = MutableStateFlow(SeedData.budgets)
    val donors = MutableStateFlow(SeedData.donors)
    val campaigns = MutableStateFlow(SeedData.campaigns)
    val beneficiaries = MutableStateFlow(SeedData.beneficiaries)
    val periodicAids = MutableStateFlow(SeedData.periodicAids)
    val inKindItems = MutableStateFlow(SeedData.inKindItems)
    val inKindMovements = MutableStateFlow(SeedData.inKindMovements)
    val schedulerEvents = MutableStateFlow(SeedData.schedulerEvents)
    val internalMessages = MutableStateFlow(SeedData.internalMessages)
    val auditLogs = MutableStateFlow(SeedData.auditLogs)
    val users = MutableStateFlow(SeedData.users)
    val currencyRates = MutableStateFlow(SeedData.currencyRates.toMap())
    val syncQueue = MutableStateFlow<List<SyncQueueItem>>(emptyList())

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)

    private fun nowStr() = dateFormat.format(Date())
    private fun nowTimeStr() = timeFormat.format(Date())

    private fun computeShortSha256(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            bytes.take(4).joinToString("") { "%02X".format(it) }
        } catch (_: Exception) {
            UUID.randomUUID().toString().take(8).uppercase()
        }
    }

    // ------------------------------------------------------------------------
    // Persistent Local-First Storage Initialization
    // ------------------------------------------------------------------------
    fun initStorage(context: Context) {
        if (prefs != null) return
        val sp = context.getSharedPreferences("social_fund_v9_storage", Context.MODE_PRIVATE)
        prefs = sp

        isDarkMode.value = sp.getBoolean("is_dark_mode", false)
        fontScale.value = sp.getFloat("font_scale", 1.0f)
        autoSyncOnConnect.value = sp.getBoolean("auto_sync_on_connect", true)

        val savedServerName = sp.getString("server_name", null)
        val savedServerHost = sp.getString("server_host", null)
        if (!savedServerName.isNullOrBlank() && !savedServerHost.isNullOrBlank()) {
            serverConfig.value = serverConfig.value.copy(
                serverName = savedServerName,
                serverHost = savedServerHost
            )
        }

        val savedJson = sp.getString("full_database_json", null)
        if (!savedJson.isNullOrBlank()) {
            importDatabaseFromJson(savedJson, isSilentLoad = true)
        }
    }

    private fun saveToDisk() {
        val sp = prefs ?: return
        sp.edit {
            putBoolean("is_dark_mode", isDarkMode.value)
            putFloat("font_scale", fontScale.value)
            putBoolean("auto_sync_on_connect", autoSyncOnConnect.value)
            putString("server_name", serverConfig.value.serverName)
            putString("server_host", serverConfig.value.serverHost)
            putString("full_database_json", exportDatabaseJson())
        }
    }

    fun setDarkModePreference(enabled: Boolean) {
        isDarkMode.value = enabled
        saveToDisk()
    }

    fun setFontScalePreference(scale: Float) {
        fontScale.value = scale
        saveToDisk()
    }

    fun setAutoSyncOnConnect(enabled: Boolean) {
        autoSyncOnConnect.value = enabled
        saveToDisk()
        logAudit(
            "إعدادات المزامنة",
            if (enabled) "تفعيل المزامنة التلقائية الفورية عند الاتصال بالشبكة" else "إيقاف المزامنة التلقائية وتحويلها للوضع اليدوي"
        )
    }

    fun updateServerNode(name: String, host: String, latencyMs: Int = 8) {
        serverConfig.value = serverConfig.value.copy(
            serverName = name,
            serverHost = host,
            latencyMs = latencyMs,
            lastBackupTime = nowTimeStr()
        )
        saveToDisk()
        logAudit("تغيير الخادم المحلي", "الارتباط بالعقدة: $name ($host)")
    }

    fun getTotalLocalRecordsCount(): Int {
        return members.value.size +
                subscriptions.value.size +
                aidRequests.value.size +
                vouchers.value.size +
                journalEntries.value.size +
                accounts.value.size +
                budgets.value.size +
                donors.value.size +
                campaigns.value.size +
                beneficiaries.value.size +
                periodicAids.value.size +
                inKindItems.value.size +
                inKindMovements.value.size
    }

    fun resetToDefaultSeedData() {
        fundInfo.value = FundInfo()
        regions.value = SeedData.regions
        appSettings.value = AppSettings()
        printSettings.value = PrintSettings()
        accounts.value = SeedData.accounts
        members.value = SeedData.members
        subscriptions.value = SeedData.subscriptions
        aidRequests.value = SeedData.aidRequests
        vouchers.value = SeedData.vouchers
        journalEntries.value = SeedData.journalEntries
        budgets.value = SeedData.budgets
        donors.value = SeedData.donors
        campaigns.value = SeedData.campaigns
        beneficiaries.value = SeedData.beneficiaries
        periodicAids.value = SeedData.periodicAids
        inKindItems.value = SeedData.inKindItems
        inKindMovements.value = SeedData.inKindMovements
        schedulerEvents.value = SeedData.schedulerEvents
        internalMessages.value = SeedData.internalMessages
        auditLogs.value = SeedData.auditLogs
        users.value = SeedData.users
        currencyRates.value = SeedData.currencyRates.toMap()
        syncQueue.value = emptyList()
        logAudit("إعادة ضبط المصنع", "تمت استعادة البيانات الأولية الافتراضية للصندوق بنجاح")
        saveToDisk()
    }

    // ------------------------------------------------------------------------
    // Authentication, Audit & Smart Network Synchronization
    // ------------------------------------------------------------------------
    fun authenticateUser(usernameInput: String): AppUser {
        val clean = usernameInput.trim().lowercase()
        val matched = users.value.find { it.username.lowercase() == clean && it.isActive }
            ?: users.value.firstOrNull { it.isActive }
            ?: SeedData.users[0]

        val updatedUser = matched.copy(lastLogin = nowTimeStr())
        users.value = users.value.map { if (it.id == updatedUser.id) updatedUser else it }
        currentUser.value = updatedUser
        logAudit("تسجيل دخول آمن", "دخول المستخدم ${updatedUser.fullName} (${updatedUser.role.title}) عبر ${serverConfig.value.serverName}")
        return updatedUser
    }

    fun logAudit(action: String, description: String) {
        val user = currentUser.value
        val ts = nowTimeStr()
        if (appSettings.value.enableAuditTrail || action == "إعدادات النظام") {
            val log = AuditLog(
                timestamp = ts,
                userName = "${user.fullName} (${user.username})",
                userRole = user.role.title,
                actionType = action,
                description = description,
                ipOrDevice = "${serverConfig.value.serverHost} • ${if (isOnline.value) "متصل" else "محلي Offline"}"
            )
            auditLogs.value = listOf(log) + auditLogs.value
        }

        if (!isOnline.value && action != "انقطاع الاتصال" && action != "اتصال بالشبكة") {
            val hash = computeShortSha256("$ts|$action|$description")
            val sync = SyncQueueItem(
                timestamp = ts,
                entityName = action,
                action = "حفظ محلي بانتظار المزامنة",
                summary = description,
                txHash = hash,
                payloadBytes = (description.length * 14).coerceAtLeast(256)
            )
            syncQueue.value = listOf(sync) + syncQueue.value
            syncStageMessage.value = "وضع عدم الاتصال: ${syncQueue.value.size} عملية محفوظة محلياً في الطابور"
        }
        saveToDisk()
    }

    fun simulateOfflineFieldOperations() {
        val ts = nowTimeStr()
        val sampleItems = listOf(
            SyncQueueItem(
                timestamp = ts,
                entityName = "تحصيل اشتراك ميداني",
                action = "سند قبض محلي",
                summary = "تحصيل اشتراك ميداني بمبلغ 20,000 ﷼ (محفوظ محلياً)",
                txHash = computeShortSha256("$ts-SUB-1"),
                payloadBytes = 640
            ),
            SyncQueueItem(
                timestamp = ts,
                entityName = "طلب مساعدة علاجية",
                action = "تسجيل استمارة ميدانية",
                summary = "رفع استمارة بحث ميداني لحالة طارئة في القرى التابعة للصندوق",
                txHash = computeShortSha256("$ts-AID-2"),
                payloadBytes = 820
            ),
            SyncQueueItem(
                timestamp = ts,
                entityName = "صرف سلة غذائية",
                action = "حركة مستودع محلي",
                summary = "إذن صرف مستودعي رقم MOV-LOCAL لعدد 3 سلات غذائية",
                txHash = computeShortSha256("$ts-INV-3"),
                payloadBytes = 510
            )
        )
        isOnline.value = false
        syncQueue.value = sampleItems + syncQueue.value
        syncStageMessage.value = "تم تسجيل ${sampleItems.size} عمليات ميدانية في طابور الخادم المحلي (Offline)"
        saveToDisk()
    }

    fun syncNow() {
        val pendingList = syncQueue.value
        val count = pendingList.size
        if (count == 0) {
            lastSyncTimestamp.value = nowTimeStr()
            syncStageMessage.value = "جميع الجداول متطابقة 100% مع ${serverConfig.value.serverName}"
            return
        }

        // Immediate synchronous update for unit tests & deterministic state, plus animated stages for UI
        val ts = nowTimeStr()
        val batchNo = "SYNC-2026-%03d".format(syncHistory.value.size + 19)
        val newBatch = SyncBatchRecord(
            batchNo = batchNo,
            timestamp = ts,
            operationsCount = count,
            serverNode = "${serverConfig.value.serverHost} (${serverConfig.value.serverName})",
            durationMs = (count * 120L).coerceAtLeast(320L)
        )

        syncHistory.value = listOf(newBatch) + syncHistory.value
        syncQueue.value = emptyList()
        lastSyncTimestamp.value = ts
        serverConfig.value = serverConfig.value.copy(lastBackupTime = ts)

        logAudit(
            "مزامنة ناجحة ($batchNo)",
            "تمت مزامنة $count عملية محلية مع ${serverConfig.value.serverName} وتوثيق البصمة الرقمية SHA-256."
        )

        // Launch brief visual progress indicator for smooth UX
        repoScope.launch {
            isSyncing.value = true
            syncProgress.value = 0.25f
            syncStageMessage.value = "1/3 التحقق من البصمة الرقمية SHA-256 لعدد $count عملية..."
            delay(220)
            syncProgress.value = 0.65f
            syncStageMessage.value = "2/3 ترحيل القيود المزدوجة والسندات إلى ${serverConfig.value.serverHost}..."
            delay(220)
            syncProgress.value = 1.0f
            syncStageMessage.value = "3/3 اكتملت المزامنة بنجاح ($batchNo) وتحديث النسخة الاحتياطية ✔"
            delay(200)
            isSyncing.value = false
        }
    }

    fun toggleOnline() {
        val becomingOnline = !isOnline.value
        isOnline.value = becomingOnline
        logAudit(
            if (becomingOnline) "اتصال بالشبكة" else "انقطاع الاتصال",
            "تحويل حالة الشبكة إلى ${if (becomingOnline) "متصل بالخادم (${serverConfig.value.serverHost})" else "الوضع المحلي المستقل (Offline-First)"}"
        )

        if (becomingOnline) {
            if (syncQueue.value.isNotEmpty() && autoSyncOnConnect.value) {
                syncNow()
            } else if (syncQueue.value.isNotEmpty()) {
                syncStageMessage.value = "متصل بالشبكة: يوجد ${syncQueue.value.size} عملية جاهزة للمزامنة اليدوية"
            } else {
                syncStageMessage.value = "متصل ومزامن بالكامل مع ${serverConfig.value.serverName}"
            }
        } else {
            syncStageMessage.value = "النظام يعمل على المحرك المحلي (Offline) - الحفظ فوري وآمن"
        }
    }

    fun switchRole(role: UserRole) {
        val matched = users.value.find { it.role == role } ?: AppUser(
            username = role.name.lowercase(),
            fullName = "مستخدم (${role.title})",
            role = role
        )
        currentUser.value = matched
        logAudit("تبديل الدور", "تم تبديل دور المستخدم الحالي إلى ${role.title} (${matched.fullName})")
    }

    // ------------------------------------------------------------------------
    // Currency Conversion
    // ------------------------------------------------------------------------
    fun convertToYer(amountOriginal: Double, currency: CurrencyCode): Double {
        val rate = currencyRates.value[currency] ?: currency.defaultRate
        return amountOriginal * rate
    }

    fun updateCurrencyRate(currency: CurrencyCode, rate: Double) {
        val current = currencyRates.value.toMutableMap()
        current[currency] = rate
        currencyRates.value = current
        logAudit("تعديل سعر الصرف", "تم تعديل سعر صرف ${currency.title} إلى $rate ﷼")
    }

    // ------------------------------------------------------------------------
    // Double-Entry Engine & Voucher Operations
    // ------------------------------------------------------------------------
    private fun nextVoucherNo(type: VoucherType): String {
        val customPrefix = appSettings.value.voucherNoPrefix.ifBlank { "V" }
        val prefix = "$customPrefix-${Calendar.getInstance().get(Calendar.YEAR)}-"
        val count = vouchers.value.size + 1
        return "$prefix%03d".format(count)
    }

    private fun nextJournalNo(): String {
        val count = journalEntries.value.size + 1
        return "JE-%06d".format(count)
    }

    fun issueVoucher(
        type: VoucherType,
        partyName: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        paymentMethod: PaymentMethod,
        accountCode: String,
        counterpartAccountCode: String,
        description: String
    ): Voucher {
        val rate = currencyRates.value[currency] ?: currency.defaultRate
        val amountYer = amountOriginal * rate
        val words = TafqeetEngine.convertToWords(amountYer.toLong(), "ريال يمني")
        val voucherNo = nextVoucherNo(type)
        val journalNo = nextJournalNo()
        val today = nowStr()

        val acc = accounts.value.find { it.code == accountCode }
        val counterpartAcc = accounts.value.find { it.code == counterpartAccountCode }
        val accName = acc?.name ?: "الحساب المالي"
        val counterpartName = counterpartAcc?.name ?: "الحساب المقابل"

        val lines = if (type == VoucherType.RECEIPT) {
            listOf(
                JournalLine(accountCode, accName, amountYer, 0.0, "مدين - إيداع بموجب $voucherNo"),
                JournalLine(counterpartAccountCode, counterpartName, 0.0, amountYer, "دائن - $counterpartName")
            )
        } else {
            listOf(
                JournalLine(accountCode, accName, amountYer, 0.0, "مدين - $accName"),
                JournalLine(counterpartAccountCode, counterpartName, 0.0, amountYer, "دائن - صرف بموجب $voucherNo")
            )
        }

        val journalEntry = JournalEntry(
            entryNo = journalNo,
            date = today,
            description = "إثبات $voucherNo: $description",
            lines = lines,
            isPosted = true,
            originalVoucherNo = voucherNo
        )
        journalEntries.value = listOf(journalEntry) + journalEntries.value

        updateAccountBalance(accountCode, if (type == VoucherType.RECEIPT) amountYer else -amountYer)
        updateAccountBalance(counterpartAccountCode, if (type == VoucherType.RECEIPT) amountYer else -amountYer)

        val ps = printSettings.value
        val newVoucher = Voucher(
            voucherNo = voucherNo,
            type = type,
            status = VoucherStatus.POSTED,
            partyName = partyName,
            amountOriginal = amountOriginal,
            currency = currency,
            exchangeRate = rate,
            amountYer = amountYer,
            amountWords = words,
            paymentMethod = paymentMethod,
            accountCode = accountCode,
            accountName = accName,
            counterpartAccountCode = counterpartAccountCode,
            counterpartAccountName = counterpartName,
            description = description,
            date = today,
            linkedJournalEntryId = journalNo,
            receivedOrPaidBy = ps.sig1DefaultName.ifBlank { partyName },
            accountantName = ps.sig2DefaultName.ifBlank { "أمين الصندوق / المحاسب" },
            auditorName = ps.sig3DefaultName.ifBlank { "المراجع المالي" },
            managerName = ps.sig4DefaultName.ifBlank { "رئيس مجلس الإدارة" }
        )
        vouchers.value = listOf(newVoucher) + vouchers.value

        logAudit("إصدار ${type.title}", "إصدار $voucherNo للمكرم $partyName بمبلغ $amountYer ﷼ مع القيد $journalNo")
        return newVoucher
    }

    fun transferBetweenTreasuryAccounts(
        fromAccountCode: String,
        toAccountCode: String,
        amountYer: Double,
        notes: String
    ): Boolean {
        if (fromAccountCode == toAccountCode || amountYer <= 0) return false
        val fromAcc = accounts.value.find { it.code == fromAccountCode } ?: return false
        val toAcc = accounts.value.find { it.code == toAccountCode } ?: return false
        if (!appSettings.value.allowNegativeTreasury && fromAcc.balance < amountYer) return false

        val journalNo = nextJournalNo()
        val today = nowStr()
        val desc = notes.ifBlank { "تحويل سيولة داخلي من ${fromAcc.name} إلى ${toAcc.name}" }

        val entry = JournalEntry(
            entryNo = journalNo,
            date = today,
            description = desc,
            lines = listOf(
                JournalLine(toAccountCode, toAcc.name, amountYer, 0.0, "مدين - إيداع تحويل داخلي"),
                JournalLine(fromAccountCode, fromAcc.name, 0.0, amountYer, "دائن - سحب تحويل داخلي")
            ),
            isPosted = true
        )
        journalEntries.value = listOf(entry) + journalEntries.value
        updateAccountBalance(fromAccountCode, -amountYer)
        updateAccountBalance(toAccountCode, amountYer)

        logAudit("تحويل مالي داخلي", "تحويل مبلغ ${amountYer.toLong()} ﷼ من ${fromAcc.name} إلى ${toAcc.name} بموجب القيد $journalNo")
        return true
    }

    fun voidVoucher(voucherId: String) {
        val voucher = vouchers.value.find { it.id == voucherId } ?: return
        if (voucher.status == VoucherStatus.VOID) return

        vouchers.value = vouchers.value.map {
            if (it.id == voucherId) it.copy(status = VoucherStatus.VOID) else it
        }

        val revJournalNo = "${voucher.linkedJournalEntryId}-R"
        val lines = if (voucher.type == VoucherType.RECEIPT) {
            listOf(
                JournalLine(voucher.accountCode, voucher.accountName, 0.0, voucher.amountYer, "دائن - عكس إيداع السند الملغي ${voucher.voucherNo}"),
                JournalLine(voucher.counterpartAccountCode, voucher.counterpartAccountName, voucher.amountYer, 0.0, "مدين - عكس الإيراد للسند الملغي")
            )
        } else {
            listOf(
                JournalLine(voucher.accountCode, voucher.accountName, 0.0, voucher.amountYer, "دائن - عكس المصروف للسند الملغي ${voucher.voucherNo}"),
                JournalLine(voucher.counterpartAccountCode, voucher.counterpartAccountName, voucher.amountYer, 0.0, "مدين - استرداد النقدية للسند الملغي")
            )
        }

        val revEntry = JournalEntry(
            entryNo = revJournalNo,
            date = nowStr(),
            description = "قيد عكسي آلي لإلغاء السند ${voucher.voucherNo} (${voucher.description})",
            lines = lines,
            isPosted = true,
            isReversed = true,
            originalVoucherNo = voucher.voucherNo
        )
        journalEntries.value = listOf(revEntry) + journalEntries.value

        updateAccountBalance(voucher.accountCode, if (voucher.type == VoucherType.RECEIPT) -voucher.amountYer else voucher.amountYer)
        updateAccountBalance(voucher.counterpartAccountCode, if (voucher.type == VoucherType.RECEIPT) -voucher.amountYer else voucher.amountYer)

        logAudit("إلغاء سند", "تم إلغاء السند ${voucher.voucherNo} بنجاح وتوليد قيد عكسي $revJournalNo")
    }

    private fun updateAccountBalance(code: String, delta: Double) {
        accounts.value = accounts.value.map {
            if (it.code == code) it.copy(balance = it.balance + delta) else it
        }
    }

    fun addSubAccount(code: String, name: String, type: AccountType, parentCode: String, openingBalance: Double = 0.0): Boolean {
        if (accounts.value.any { it.code == code }) return false
        val isDebitNormal = type == AccountType.ASSET || type == AccountType.EXPENSE
        val newAcc = Account(
            code = code,
            name = name,
            type = type,
            isDebitNormal = isDebitNormal,
            balance = openingBalance,
            parentCode = parentCode
        )
        accounts.value = (accounts.value + newAcc).sortedBy { it.code }
        logAudit("إضافة حساب محاسبي", "إضافة حساب فرعي ($code - $name) تحت ($parentCode)")
        return true
    }

    fun createManualJournalEntry(lines: List<JournalLine>, description: String): Boolean {
        val totalDebit = lines.sumOf { it.debit }
        val totalCredit = lines.sumOf { it.credit }
        if (appSettings.value.enforceBalancedJournal && Math.abs(totalDebit - totalCredit) > 0.01) {
            return false
        }

        val entryNo = nextJournalNo()
        val entry = JournalEntry(
            entryNo = entryNo,
            date = nowStr(),
            description = description,
            lines = lines,
            isPosted = true
        )
        journalEntries.value = listOf(entry) + journalEntries.value

        lines.forEach { line ->
            val delta = line.debit - line.credit
            updateAccountBalance(line.accountCode, delta)
        }

        logAudit("قيد تسوية يدوي", "إنشاء قيد يومية متوازن $entryNo بمبلغ $totalDebit ﷼")
        return true
    }

    // ------------------------------------------------------------------------
    // Member Operations
    // ------------------------------------------------------------------------
    fun addMember(member: Member) {
        members.value = listOf(member) + members.value
        logAudit("إضافة عضو", "تسجيل العضو الجديد: ${member.fullName} برقم ${member.memberNo}")
    }

    fun updateMember(member: Member) {
        members.value = members.value.map { if (it.id == member.id) member else it }
        logAudit("تعديل عضو", "تحديث بيانات العضو: ${member.fullName} (${member.memberNo})")
    }

    fun toggleMemberStatus(memberId: String) {
        val m = members.value.find { it.id == memberId } ?: return
        val newStatus = if (m.status == "نشط") "معلق" else "نشط"
        members.value = members.value.map { if (it.id == memberId) it.copy(status = newStatus) else it }
        logAudit("تغيير حالة عضو", "تغيير حالة عضوية ${m.fullName} إلى $newStatus")
    }

    // ------------------------------------------------------------------------
    // Subscriptions
    // ------------------------------------------------------------------------
    fun addSubscription(
        memberId: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        monthYear: String,
        paymentMethod: PaymentMethod,
        referenceNo: String,
        notes: String
    ): Subscription {
        val member = members.value.find { it.id == memberId }!!
        val rate = currencyRates.value[currency] ?: currency.defaultRate
        val amountYer = amountOriginal * rate
        val receiptNo = "SUB-${Calendar.getInstance().get(Calendar.YEAR)}-%03d".format(subscriptions.value.size + 1)
        val today = nowStr()

        val sub = Subscription(
            receiptNo = receiptNo,
            memberId = memberId,
            memberName = member.fullName,
            amountOriginal = amountOriginal,
            currency = currency,
            exchangeRate = rate,
            amountYer = amountYer,
            monthYear = monthYear,
            paymentMethod = paymentMethod,
            referenceNo = referenceNo,
            date = today,
            notes = notes
        )
        subscriptions.value = listOf(sub) + subscriptions.value

        members.value = members.value.map {
            if (it.id == memberId) {
                val newDue = Math.max(0.0, it.dueBalanceYer - amountYer)
                it.copy(
                    totalPaidYer = it.totalPaidYer + amountYer,
                    dueBalanceYer = newDue
                )
            } else it
        }

        val treasuryAccountCode = when (paymentMethod) {
            PaymentMethod.CASH -> "1100"
            PaymentMethod.BANK_TRANSFER -> "1200"
            PaymentMethod.ELECTRONIC_WALLET -> "1300"
        }
        issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = member.fullName,
            amountOriginal = amountOriginal,
            currency = currency,
            paymentMethod = paymentMethod,
            accountCode = treasuryAccountCode,
            counterpartAccountCode = "4100",
            description = "تحصيل اشتراك $monthYear للعضو ${member.fullName} ($receiptNo)"
        )

        return sub
    }

    // ------------------------------------------------------------------------
    // Aids Operations
    // ------------------------------------------------------------------------
    fun addAidRequest(
        memberId: String,
        aidType: AidType,
        amountYer: Double,
        notes: String
    ): AidRequest {
        val member = members.value.find { it.id == memberId }
        val memberName = member?.fullName ?: "مستفيد خارجي"
        val count = aidRequests.value.size + 1
        val reqNo = "REQ-${Calendar.getInstance().get(Calendar.YEAR)}-%03d".format(count)

        val req = AidRequest(
            requestNo = reqNo,
            memberId = memberId,
            memberName = memberName,
            aidType = aidType,
            requestedAmountYer = amountYer,
            approvedAmountYer = 0.0,
            status = AidStatus.IN_REVIEW,
            requestDate = nowStr(),
            notes = notes
        )
        aidRequests.value = listOf(req) + aidRequests.value
        logAudit("طلب مساعدة", "تقديم طلب مساعدة $reqNo للأخ $memberName بمبلغ $amountYer ﷼")
        return req
    }

    fun updateAidStatus(requestId: String, newStatus: AidStatus, approvedAmountYer: Double = 0.0) {
        val req = aidRequests.value.find { it.id == requestId } ?: return
        var voucherId: String? = req.voucherId

        if (newStatus == AidStatus.DISBURSED && req.status != AidStatus.DISBURSED) {
            val v = issueVoucher(
                type = VoucherType.PAYMENT,
                partyName = req.memberName,
                amountOriginal = if (approvedAmountYer > 0) approvedAmountYer else req.requestedAmountYer,
                currency = CurrencyCode.YER,
                paymentMethod = PaymentMethod.CASH,
                accountCode = "5100",
                counterpartAccountCode = "1100",
                description = "صرف مساعدة ${req.aidType.title} بموجب الطلب ${req.requestNo}"
            )
            voucherId = v.voucherNo
        }

        aidRequests.value = aidRequests.value.map {
            if (it.id == requestId) {
                it.copy(
                    status = newStatus,
                    approvedAmountYer = if (approvedAmountYer > 0) approvedAmountYer else it.approvedAmountYer,
                    decisionDate = nowStr(),
                    voucherId = voucherId
                )
            } else it
        }

        logAudit("تحديث مساعدة", "تغيير حالة طلب المساعدة ${req.requestNo} إلى ${newStatus.title}")
    }

    // ------------------------------------------------------------------------
    // Other Module Adders & Actions
    // ------------------------------------------------------------------------
    fun addBudget(category: String, accountCode: String, budgetedAmount: Double, month: String) {
        val b = Budget(
            category = category,
            accountCode = accountCode,
            budgetedAmount = budgetedAmount,
            actualSpent = 0.0,
            month = month
        )
        budgets.value = budgets.value + b
        logAudit("إضافة موازنة", "إضافة موازنة لبند $category بمبلغ $budgetedAmount ﷼")
    }

    fun updateBudgetAmount(budgetId: String, newAmount: Double) {
        budgets.value = budgets.value.map {
            if (it.id == budgetId) it.copy(budgetedAmount = newAmount) else it
        }
        logAudit("تحديث موازنة", "تعديل السقف التقديري لبند الموازنة إلى $newAmount ﷼")
    }

    fun addDonor(donor: Donor) {
        donors.value = listOf(donor) + donors.value
        logAudit("إضافة مانح", "إضافة الداعم: ${donor.name}")
    }

    fun recordDonorPledgePayment(donorId: String, amountOriginal: Double, currency: CurrencyCode, paymentMethod: PaymentMethod) {
        val donor = donors.value.find { it.id == donorId } ?: return
        val amountYer = convertToYer(amountOriginal, currency)
        val today = nowStr()

        donors.value = donors.value.map {
            if (it.id == donorId) {
                it.copy(
                    totalDonatedYer = it.totalDonatedYer + amountYer,
                    pledgeFulfilledYer = it.pledgeFulfilledYer + amountYer,
                    lastDonationDate = today
                )
            } else it
        }

        val accCode = when (paymentMethod) {
            PaymentMethod.CASH -> "1100"
            PaymentMethod.BANK_TRANSFER -> "1200"
            PaymentMethod.ELECTRONIC_WALLET -> "1300"
        }

        issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = donor.name,
            amountOriginal = amountOriginal,
            currency = currency,
            paymentMethod = paymentMethod,
            accountCode = accCode,
            counterpartAccountCode = "4200",
            description = "تحصيل دفعة من التعهد المالي للداعم ${donor.name}"
        )
    }

    fun addCampaign(campaign: Campaign) {
        campaigns.value = listOf(campaign) + campaigns.value
        logAudit("إضافة حملة", "إطلاق حملة خيرية: ${campaign.title}")
    }

    fun donateToCampaign(campaignId: String, donorName: String, amountOriginal: Double, currency: CurrencyCode, paymentMethod: PaymentMethod) {
        val c = campaigns.value.find { it.id == campaignId } ?: return
        val amountYer = convertToYer(amountOriginal, currency)

        campaigns.value = campaigns.value.map {
            if (it.id == campaignId) {
                val newCol = it.collectedAmountYer + amountYer
                val newStatus = if (newCol >= it.goalAmountYer) "مكتملة" else it.status
                it.copy(collectedAmountYer = newCol, status = newStatus)
            } else it
        }

        val accCode = when (paymentMethod) {
            PaymentMethod.CASH -> "1100"
            PaymentMethod.BANK_TRANSFER -> "1200"
            PaymentMethod.ELECTRONIC_WALLET -> "1300"
        }

        issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = donorName.ifBlank { "فاعل خير - ${c.title}" },
            amountOriginal = amountOriginal,
            currency = currency,
            paymentMethod = paymentMethod,
            accountCode = accCode,
            counterpartAccountCode = "4300",
            description = "تبرع مخصص لحملة: ${c.title}"
        )
    }

    fun disburseFromCampaign(campaignId: String, recipientName: String, amountYer: Double, description: String) {
        val c = campaigns.value.find { it.id == campaignId } ?: return
        campaigns.value = campaigns.value.map {
            if (it.id == campaignId) {
                it.copy(spentAmountYer = it.spentAmountYer + amountYer)
            } else it
        }

        issueVoucher(
            type = VoucherType.PAYMENT,
            partyName = recipientName.ifBlank { "مستفيدو حملة ${c.title}" },
            amountOriginal = amountYer,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5200",
            counterpartAccountCode = "1100",
            description = "صرف ميداني من حملة (${c.title}): $description"
        )
    }

    fun addBeneficiary(b: Beneficiary) {
        beneficiaries.value = listOf(b) + beneficiaries.value
        logAudit("إضافة مستفيد", "تسجيل أسرة مستفيدة: ${b.familyHeadName}")
    }

    fun addPeriodicAid(beneficiaryId: String, beneficiaryName: String, monthlyAmountYer: Double, sponsorshipType: String) {
        val pa = PeriodicAid(
            beneficiaryId = beneficiaryId,
            beneficiaryName = beneficiaryName,
            monthlyAmountYer = monthlyAmountYer,
            sponsorshipType = sponsorshipType,
            status = "نشطة",
            lastDisbursedMonth = "جديد"
        )
        periodicAids.value = listOf(pa) + periodicAids.value
        logAudit("إضافة كفالة دورية", "جدولة كفالة شهرية للمستفيد $beneficiaryName بمبلغ $monthlyAmountYer ﷼")
    }

    fun disburseSinglePeriodicAid(id: String, month: String) {
        val pa = periodicAids.value.find { it.id == id } ?: return
        periodicAids.value = periodicAids.value.map {
            if (it.id == id) it.copy(lastDisbursedMonth = month) else it
        }
        issueVoucher(
            type = VoucherType.PAYMENT,
            partyName = pa.beneficiaryName,
            amountOriginal = pa.monthlyAmountYer,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5200",
            counterpartAccountCode = "1100",
            description = "صرف كفالة شهرية فردية (${pa.sponsorshipType}) لشهر $month"
        )
    }

    fun disbursePeriodicAidsBatch(month: String, ids: List<String>) {
        var totalAmount = 0.0
        val updated = periodicAids.value.map { pa ->
            if (ids.contains(pa.id)) {
                totalAmount += pa.monthlyAmountYer
                pa.copy(lastDisbursedMonth = month)
            } else pa
        }
        periodicAids.value = updated

        if (totalAmount > 0) {
            issueVoucher(
                type = VoucherType.PAYMENT,
                partyName = "كشف صرف الكفالات والمساعدات الدورية لعدد ${ids.size} أسرة",
                amountOriginal = totalAmount,
                currency = CurrencyCode.YER,
                paymentMethod = PaymentMethod.CASH,
                accountCode = "5200",
                counterpartAccountCode = "1100",
                description = "صرف المساعدات الدورية لشهر $month إجمالي $totalAmount ﷼"
            )
        }
        logAudit("صرف كفالات", "تم صرف دفعة كفالات لشهر $month بمبلغ إجمالي $totalAmount ﷼")
    }

    fun addInKindItem(item: InKindItem) {
        inKindItems.value = listOf(item) + inKindItems.value
        logAudit("تعريف صنف عيني", "إضافة صنف مخزني جديد: ${item.name} (${item.code})")
    }

    fun addInKindMovement(mov: InKindMovement) {
        inKindMovements.value = listOf(mov) + inKindMovements.value
        inKindItems.value = inKindItems.value.map { item ->
            if (item.id == mov.itemId) {
                val delta = if (mov.type == "توريد") mov.quantity else -mov.quantity
                item.copy(quantityOnHand = Math.max(0, item.quantityOnHand + delta))
            } else item
        }
        logAudit("حركة مخزنية عينية", "${mov.type} كمية ${mov.quantity} للصنف ${mov.itemName}")
    }

    fun addSchedulerEvent(event: SchedulerEvent) {
        schedulerEvents.value = listOf(event) + schedulerEvents.value
        logAudit("إضافة موعد", "جدولة فعالية: ${event.title}")
    }

    fun sendMessage(subject: String, body: String, recipientRole: String) {
        val user = currentUser.value
        val msg = InternalMessage(
            senderName = user.fullName,
            senderRole = user.role.title,
            recipientRole = recipientRole,
            subject = subject,
            body = body,
            timestamp = nowTimeStr(),
            isRead = false
        )
        internalMessages.value = listOf(msg) + internalMessages.value
        logAudit("إرسال رسالة", "إرسال رسالة داخلية: $subject إلى $recipientRole")
    }

    fun markMessageRead(id: String) {
        internalMessages.value = internalMessages.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        saveToDisk()
    }

    fun addUser(username: String, fullName: String, role: UserRole) {
        val u = AppUser(
            username = username,
            fullName = fullName,
            role = role,
            isActive = true,
            lastLogin = nowStr()
        )
        users.value = users.value + u
        logAudit("إضافة مستخدم", "إنشاء حساب موظف جديد: $fullName بدور ${role.title}")
    }

    fun toggleUserActive(userId: String) {
        users.value = users.value.map {
            if (it.id == userId) it.copy(isActive = !it.isActive) else it
        }
        logAudit("تحديث حالة موظف", "تغيير حالة تفعيل المستخدم رقم $userId")
    }

    fun updateFundInfo(info: FundInfo) {
        fundInfo.value = info
        logAudit("تحديث بيانات الصندوق", "تم حفظ وتحديث البيانات والترخيص الرسمي للصندوق")
    }

    fun updateFundLogo(logoBase64: String) {
        fundInfo.value = fundInfo.value.copy(customLogoBase64 = logoBase64)
        logAudit(
            "تحديث شعار الصندوق",
            if (logoBase64.isBlank()) "استعادة الشعار الافتراضي للصندوق" else "تعيين شعار مخصص جديد للصندوق"
        )
    }

    fun updateAppSettings(settings: AppSettings) {
        appSettings.value = settings
        logAudit("إعدادات النظام", "تحديث الإعدادات التشغيلية والمالية الشاملة لجميع الشاشات")
    }

    fun updatePrintSettings(settings: PrintSettings) {
        printSettings.value = settings
        logAudit("إعدادات الطباعة والسندات", "تحديث قوالب الترويسة والتذييل والخطوط وحقول التوقيعات")
    }

    fun resetPrintSettingsToDefault() {
        printSettings.value = PrintSettings()
        logAudit("إعدادات الطباعة والسندات", "استعادة الإعدادات الافتراضية لقوالب الطباعة والسندات")
    }

    // ------------------------------------------------------------------------
    // Regions & Villages Management (1 Region -> Multiple Villages)
    // ------------------------------------------------------------------------
    fun addRegion(regionName: String): Boolean {
        val trimmed = regionName.trim()
        if (trimmed.isBlank() || regions.value.any { it.name == trimmed }) return false
        regions.value = regions.value + RegionGroup(name = trimmed, villages = emptyList())
        logAudit("إضافة منطقة", "إضافة منطقة إدارية جديدة: $trimmed")
        return true
    }

    fun deleteRegion(regionId: String) {
        if (regions.value.size <= 1) return
        val target = regions.value.find { it.id == regionId } ?: return
        regions.value = regions.value.filterNot { it.id == regionId }
        logAudit("حذف منطقة", "حذف المنطقة: ${target.name}")
    }

    fun addVillageToRegion(regionId: String, villageName: String): Boolean {
        val trimmed = villageName.trim()
        if (trimmed.isBlank()) return false
        var added = false
        regions.value = regions.value.map { r ->
            if (r.id == regionId && !r.villages.contains(trimmed)) {
                added = true
                r.copy(villages = r.villages + trimmed)
            } else r
        }
        if (added) {
            val rName = regions.value.find { it.id == regionId }?.name ?: ""
            logAudit("إضافة قرية", "إضافة قرية ($trimmed) ضمن نطاق ($rName)")
        }
        return added
    }

    fun addVillageByRegionName(regionName: String, villageName: String) {
        val rName = regionName.trim()
        val vName = villageName.trim()
        if (rName.isBlank()) return
        val existing = regions.value.find { it.name == rName }
        if (existing == null) {
            val list = if (vName.isNotBlank()) listOf(vName) else emptyList()
            regions.value = regions.value + RegionGroup(name = rName, villages = list)
            logAudit("إضافة منطقة وقرية", "إنشاء منطقة ($rName)${if (vName.isNotBlank()) " مع القرية ($vName)" else ""}")
        } else if (vName.isNotBlank() && !existing.villages.contains(vName)) {
            addVillageToRegion(existing.id, vName)
        }
    }

    fun removeVillageFromRegion(regionId: String, villageName: String) {
        regions.value = regions.value.map { r ->
            if (r.id == regionId) r.copy(villages = r.villages.filterNot { it == villageName }) else r
        }
        logAudit("حذف قرية", "إزالة القرية: $villageName")
    }

    // ------------------------------------------------------------------------
    // Full JSON Serialization & Deserialization (Including Sync Queue)
    // ------------------------------------------------------------------------
    fun exportDatabaseJson(): String {
        val root = JSONObject()
        root.put("version", "9.0")
        root.put("exportDate", nowTimeStr())

        // FundInfo
        val fi = fundInfo.value
        root.put("fundInfo", JSONObject().apply {
            put("name", fi.name)
            put("subtitle", fi.subtitle)
            put("licenseNo", fi.licenseNo)
            put("address", fi.address)
            put("phone", fi.phone)
            put("email", fi.email)
            put("bankAccountKareemi", fi.bankAccountKareemi)
            put("bankAccountCAC", fi.bankAccountCAC)
            put("maxAidCeilingYer", fi.maxAidCeilingYer)
            put("fiscalYear", fi.fiscalYear)
            put("customLogoBase64", fi.customLogoBase64)
        })

        // Regions & Villages
        val regionsArr = JSONArray()
        regions.value.forEach { r ->
            regionsArr.put(JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("villages", JSONArray(r.villages))
            })
        }
        root.put("regions", regionsArr)

        // AppSettings
        val st = appSettings.value
        root.put("appSettings", JSONObject().apply {
            put("monthlySubscriptionFeeYer", st.monthlySubscriptionFeeYer)
            put("autoSuspendMonths", st.autoSuspendMonths)
            put("requireNationalId", st.requireNationalId)
            put("memberNoPrefix", st.memberNoPrefix)
            put("requireReviewerApproval", st.requireReviewerApproval)
            put("periodicAidDisburseDay", st.periodicAidDisburseDay)
            put("lowStockThreshold", st.lowStockThreshold)
            put("defaultWarehouseName", st.defaultWarehouseName)
            put("voucherNoPrefix", st.voucherNoPrefix)
            put("defaultTreasuryAccountCode", st.defaultTreasuryAccountCode)
            put("allowNegativeTreasury", st.allowNegativeTreasury)
            put("enforceBalancedJournal", st.enforceBalancedJournal)
            put("budgetAlertPercent", st.budgetAlertPercent)
            put("autoPrintVoucher", st.autoPrintVoucher)
            put("allowAnonymousDonors", st.allowAnonymousDonors)
            put("defaultEventLocation", st.defaultEventLocation)
            put("strictRolePermissions", st.strictRolePermissions)
            put("enableAuditTrail", st.enableAuditTrail)
        })

        // PrintSettings
        val ps = printSettings.value
        root.put("printSettings", JSONObject().apply {
            put("showCountryHeader", ps.showCountryHeader)
            put("countryHeaderText", ps.countryHeaderText)
            put("showDepartmentHeader", ps.showDepartmentHeader)
            put("departmentHeaderText", ps.departmentHeaderText)
            put("showLogoInPrint", ps.showLogoInPrint)
            put("showLicenseAndAddress", ps.showLicenseAndAddress)
            put("receiptVoucherTitle", ps.receiptVoucherTitle)
            put("paymentVoucherTitle", ps.paymentVoucherTitle)
            put("journalVoucherTitle", ps.journalVoucherTitle)
            put("financialStatementSubtitle", ps.financialStatementSubtitle)
            put("analyticalReportSubtitle", ps.analyticalReportSubtitle)
            put("receiptPartyLabel", ps.receiptPartyLabel)
            put("paymentPartyLabel", ps.paymentPartyLabel)
            put("amountWordsLabel", ps.amountWordsLabel)
            put("paymentMethodLabel", ps.paymentMethodLabel)
            put("accountLabel", ps.accountLabel)
            put("descriptionLabel", ps.descriptionLabel)
            put("fontFamily", ps.fontFamily.name)
            put("headerFontSizeSp", ps.headerFontSizeSp)
            put("bodyFontSizeSp", ps.bodyFontSizeSp)
            put("signatureFontSizeSp", ps.signatureFontSizeSp)
            put("borderStyle", ps.borderStyle.name)
            put("paperSize", ps.paperSize.name)
            put("accentColorHex", ps.accentColorHex)
            put("showSignaturesSection", ps.showSignaturesSection)
            put("sig1Enabled", ps.sig1Enabled)
            put("sig1Title", ps.sig1Title)
            put("sig1DefaultName", ps.sig1DefaultName)
            put("sig2Enabled", ps.sig2Enabled)
            put("sig2Title", ps.sig2Title)
            put("sig2DefaultName", ps.sig2DefaultName)
            put("sig3Enabled", ps.sig3Enabled)
            put("sig3Title", ps.sig3Title)
            put("sig3DefaultName", ps.sig3DefaultName)
            put("sig4Enabled", ps.sig4Enabled)
            put("sig4Title", ps.sig4Title)
            put("sig4DefaultName", ps.sig4DefaultName)
            put("showSignatureLine", ps.showSignatureLine)
            put("showOfficialStampBox", ps.showOfficialStampBox)
            put("officialStampLabel", ps.officialStampLabel)
            put("showQrVerification", ps.showQrVerification)
            put("qrCaptionText", ps.qrCaptionText)
            put("showJournalReference", ps.showJournalReference)
            put("showCurrencyDetails", ps.showCurrencyDetails)
            put("showFundContactInFooter", ps.showFundContactInFooter)
            put("footerNote", ps.footerNote)
            put("footerSubNote", ps.footerSubNote)
        })

        // Members
        val membersArr = JSONArray()
        members.value.forEach { m ->
            membersArr.put(JSONObject().apply {
                put("id", m.id)
                put("memberNo", m.memberNo)
                put("fullName", m.fullName)
                put("phone", m.phone)
                put("nationalId", m.nationalId)
                put("joinDate", m.joinDate)
                put("status", m.status)
                put("totalPaidYer", m.totalPaidYer)
                put("dueBalanceYer", m.dueBalanceYer)
                put("job", m.job)
                put("region", m.region)
                put("village", m.village)
                put("notes", m.notes)
            })
        }
        root.put("members", membersArr)

        // Beneficiaries
        val benArr = JSONArray()
        beneficiaries.value.forEach { b ->
            benArr.put(JSONObject().apply {
                put("id", b.id)
                put("familyHeadName", b.familyHeadName)
                put("nationalId", b.nationalId)
                put("phone", b.phone)
                put("membersCount", b.membersCount)
                put("monthlyIncomeYer", b.monthlyIncomeYer)
                put("povertyTier", b.povertyTier.name)
                put("region", b.region)
                put("village", b.village)
                put("address", b.address)
                put("aidHistoryCount", b.aidHistoryCount)
            })
        }
        root.put("beneficiaries", benArr)

        // Accounts
        val accountsArr = JSONArray()
        accounts.value.forEach { a ->
            accountsArr.put(JSONObject().apply {
                put("code", a.code)
                put("name", a.name)
                put("type", a.type.name)
                put("isDebitNormal", a.isDebitNormal)
                put("balance", a.balance)
                put("parentCode", a.parentCode ?: "")
            })
        }
        root.put("accounts", accountsArr)

        // Vouchers
        val vouchersArr = JSONArray()
        vouchers.value.forEach { v ->
            vouchersArr.put(JSONObject().apply {
                put("id", v.id)
                put("voucherNo", v.voucherNo)
                put("type", v.type.name)
                put("status", v.status.name)
                put("partyName", v.partyName)
                put("amountOriginal", v.amountOriginal)
                put("currency", v.currency.name)
                put("exchangeRate", v.exchangeRate)
                put("amountYer", v.amountYer)
                put("amountWords", v.amountWords)
                put("paymentMethod", v.paymentMethod.name)
                put("accountCode", v.accountCode)
                put("accountName", v.accountName)
                put("counterpartAccountCode", v.counterpartAccountCode)
                put("counterpartAccountName", v.counterpartAccountName)
                put("description", v.description)
                put("date", v.date)
                put("linkedJournalEntryId", v.linkedJournalEntryId)
                put("receivedOrPaidBy", v.receivedOrPaidBy)
            })
        }
        root.put("vouchers", vouchersArr)

        // Pending Sync Queue
        val syncArr = JSONArray()
        syncQueue.value.forEach { sq ->
            syncArr.put(JSONObject().apply {
                put("id", sq.id)
                put("timestamp", sq.timestamp)
                put("entityName", sq.entityName)
                put("action", sq.action)
                put("summary", sq.summary)
                put("txHash", sq.txHash)
                put("payloadBytes", sq.payloadBytes)
            })
        }
        root.put("syncQueue", syncArr)

        return root.toString(2)
    }

    fun importDatabaseFromJson(json: String, isSilentLoad: Boolean = false): Boolean {
        return try {
            val root = JSONObject(json)

            root.optJSONObject("fundInfo")?.let { fi ->
                fundInfo.value = FundInfo(
                    name = fi.optString("name", fundInfo.value.name),
                    subtitle = fi.optString("subtitle", fundInfo.value.subtitle),
                    licenseNo = fi.optString("licenseNo", fundInfo.value.licenseNo),
                    address = fi.optString("address", fundInfo.value.address),
                    phone = fi.optString("phone", fundInfo.value.phone),
                    email = fi.optString("email", fundInfo.value.email),
                    bankAccountKareemi = fi.optString("bankAccountKareemi", fundInfo.value.bankAccountKareemi),
                    bankAccountCAC = fi.optString("bankAccountCAC", fundInfo.value.bankAccountCAC),
                    maxAidCeilingYer = fi.optDouble("maxAidCeilingYer", fundInfo.value.maxAidCeilingYer),
                    fiscalYear = fi.optString("fiscalYear", fundInfo.value.fiscalYear),
                    customLogoBase64 = fi.optString("customLogoBase64", fundInfo.value.customLogoBase64)
                )
            }

            root.optJSONArray("regions")?.let { arr ->
                val list = mutableListOf<RegionGroup>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val vArr = o.optJSONArray("villages")
                    val villages = mutableListOf<String>()
                    if (vArr != null) {
                        for (j in 0 until vArr.length()) {
                            val v = vArr.optString(j)
                            if (v.isNotBlank()) villages.add(v)
                        }
                    }
                    list.add(RegionGroup(id = o.optString("id"), name = o.optString("name"), villages = villages))
                }
                if (list.isNotEmpty()) regions.value = list
            }

            root.optJSONObject("appSettings")?.let { st ->
                val cur = appSettings.value
                appSettings.value = AppSettings(
                    monthlySubscriptionFeeYer = st.optDouble("monthlySubscriptionFeeYer", cur.monthlySubscriptionFeeYer),
                    autoSuspendMonths = st.optInt("autoSuspendMonths", cur.autoSuspendMonths),
                    requireNationalId = st.optBoolean("requireNationalId", cur.requireNationalId),
                    memberNoPrefix = st.optString("memberNoPrefix", cur.memberNoPrefix),
                    requireReviewerApproval = st.optBoolean("requireReviewerApproval", cur.requireReviewerApproval),
                    periodicAidDisburseDay = st.optInt("periodicAidDisburseDay", cur.periodicAidDisburseDay),
                    lowStockThreshold = st.optInt("lowStockThreshold", cur.lowStockThreshold),
                    defaultWarehouseName = st.optString("defaultWarehouseName", cur.defaultWarehouseName),
                    voucherNoPrefix = st.optString("voucherNoPrefix", cur.voucherNoPrefix),
                    defaultTreasuryAccountCode = st.optString("defaultTreasuryAccountCode", cur.defaultTreasuryAccountCode),
                    allowNegativeTreasury = st.optBoolean("allowNegativeTreasury", cur.allowNegativeTreasury),
                    enforceBalancedJournal = st.optBoolean("enforceBalancedJournal", cur.enforceBalancedJournal),
                    budgetAlertPercent = st.optInt("budgetAlertPercent", cur.budgetAlertPercent),
                    autoPrintVoucher = st.optBoolean("autoPrintVoucher", cur.autoPrintVoucher),
                    allowAnonymousDonors = st.optBoolean("allowAnonymousDonors", cur.allowAnonymousDonors),
                    defaultEventLocation = st.optString("defaultEventLocation", cur.defaultEventLocation),
                    strictRolePermissions = st.optBoolean("strictRolePermissions", cur.strictRolePermissions),
                    enableAuditTrail = st.optBoolean("enableAuditTrail", cur.enableAuditTrail)
                )
            }

            root.optJSONObject("printSettings")?.let { psObj ->
                val cur = printSettings.value
                printSettings.value = PrintSettings(
                    showCountryHeader = psObj.optBoolean("showCountryHeader", cur.showCountryHeader),
                    countryHeaderText = psObj.optString("countryHeaderText", cur.countryHeaderText),
                    showDepartmentHeader = psObj.optBoolean("showDepartmentHeader", cur.showDepartmentHeader),
                    departmentHeaderText = psObj.optString("departmentHeaderText", cur.departmentHeaderText),
                    showLogoInPrint = psObj.optBoolean("showLogoInPrint", cur.showLogoInPrint),
                    showLicenseAndAddress = psObj.optBoolean("showLicenseAndAddress", cur.showLicenseAndAddress),
                    receiptVoucherTitle = psObj.optString("receiptVoucherTitle", cur.receiptVoucherTitle),
                    paymentVoucherTitle = psObj.optString("paymentVoucherTitle", cur.paymentVoucherTitle),
                    journalVoucherTitle = psObj.optString("journalVoucherTitle", cur.journalVoucherTitle),
                    financialStatementSubtitle = psObj.optString("financialStatementSubtitle", cur.financialStatementSubtitle),
                    analyticalReportSubtitle = psObj.optString("analyticalReportSubtitle", cur.analyticalReportSubtitle),
                    receiptPartyLabel = psObj.optString("receiptPartyLabel", cur.receiptPartyLabel),
                    paymentPartyLabel = psObj.optString("paymentPartyLabel", cur.paymentPartyLabel),
                    amountWordsLabel = psObj.optString("amountWordsLabel", cur.amountWordsLabel),
                    paymentMethodLabel = psObj.optString("paymentMethodLabel", cur.paymentMethodLabel),
                    accountLabel = psObj.optString("accountLabel", cur.accountLabel),
                    descriptionLabel = psObj.optString("descriptionLabel", cur.descriptionLabel),
                    fontFamily = runCatching { PrintFontOption.valueOf(psObj.optString("fontFamily")) }.getOrDefault(cur.fontFamily),
                    headerFontSizeSp = psObj.optInt("headerFontSizeSp", cur.headerFontSizeSp),
                    bodyFontSizeSp = psObj.optInt("bodyFontSizeSp", cur.bodyFontSizeSp),
                    signatureFontSizeSp = psObj.optInt("signatureFontSizeSp", cur.signatureFontSizeSp),
                    borderStyle = runCatching { PrintBorderStyle.valueOf(psObj.optString("borderStyle")) }.getOrDefault(cur.borderStyle),
                    paperSize = runCatching { PrintPaperSize.valueOf(psObj.optString("paperSize")) }.getOrDefault(cur.paperSize),
                    accentColorHex = psObj.optString("accentColorHex", cur.accentColorHex),
                    showSignaturesSection = psObj.optBoolean("showSignaturesSection", cur.showSignaturesSection),
                    sig1Enabled = psObj.optBoolean("sig1Enabled", cur.sig1Enabled),
                    sig1Title = psObj.optString("sig1Title", cur.sig1Title),
                    sig1DefaultName = psObj.optString("sig1DefaultName", cur.sig1DefaultName),
                    sig2Enabled = psObj.optBoolean("sig2Enabled", cur.sig2Enabled),
                    sig2Title = psObj.optString("sig2Title", cur.sig2Title),
                    sig2DefaultName = psObj.optString("sig2DefaultName", cur.sig2DefaultName),
                    sig3Enabled = psObj.optBoolean("sig3Enabled", cur.sig3Enabled),
                    sig3Title = psObj.optString("sig3Title", cur.sig3Title),
                    sig3DefaultName = psObj.optString("sig3DefaultName", cur.sig3DefaultName),
                    sig4Enabled = psObj.optBoolean("sig4Enabled", cur.sig4Enabled),
                    sig4Title = psObj.optString("sig4Title", cur.sig4Title),
                    sig4DefaultName = psObj.optString("sig4DefaultName", cur.sig4DefaultName),
                    showSignatureLine = psObj.optBoolean("showSignatureLine", cur.showSignatureLine),
                    showOfficialStampBox = psObj.optBoolean("showOfficialStampBox", cur.showOfficialStampBox),
                    officialStampLabel = psObj.optString("officialStampLabel", cur.officialStampLabel),
                    showQrVerification = psObj.optBoolean("showQrVerification", cur.showQrVerification),
                    qrCaptionText = psObj.optString("qrCaptionText", cur.qrCaptionText),
                    showJournalReference = psObj.optBoolean("showJournalReference", cur.showJournalReference),
                    showCurrencyDetails = psObj.optBoolean("showCurrencyDetails", cur.showCurrencyDetails),
                    showFundContactInFooter = psObj.optBoolean("showFundContactInFooter", cur.showFundContactInFooter),
                    footerNote = psObj.optString("footerNote", cur.footerNote),
                    footerSubNote = psObj.optString("footerSubNote", cur.footerSubNote)
                )
            }

            root.optJSONArray("members")?.let { arr ->
                val list = mutableListOf<Member>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val legacy = o.optString("villageOrArea")
                    val regionVal = o.optString("region").ifBlank {
                        legacy.substringBefore(" - ", "المنطقة الوسطى").trim()
                    }
                    val villageVal = o.optString("village").ifBlank {
                        if (legacy.contains(" - ")) legacy.substringAfter(" - ").trim() else "القرية المركزية"
                    }
                    list.add(
                        Member(
                            id = o.optString("id"),
                            memberNo = o.optString("memberNo"),
                            fullName = o.optString("fullName"),
                            phone = o.optString("phone"),
                            nationalId = o.optString("nationalId"),
                            joinDate = o.optString("joinDate"),
                            status = o.optString("status", "نشط"),
                            totalPaidYer = o.optDouble("totalPaidYer", 0.0),
                            dueBalanceYer = o.optDouble("dueBalanceYer", 0.0),
                            job = o.optString("job"),
                            region = regionVal,
                            village = villageVal,
                            notes = o.optString("notes")
                        )
                    )
                }
                if (list.isNotEmpty()) members.value = list
            }

            root.optJSONArray("beneficiaries")?.let { arr ->
                val list = mutableListOf<Beneficiary>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        Beneficiary(
                            id = o.optString("id"),
                            familyHeadName = o.optString("familyHeadName"),
                            nationalId = o.optString("nationalId"),
                            phone = o.optString("phone"),
                            membersCount = o.optInt("membersCount", 1),
                            monthlyIncomeYer = o.optDouble("monthlyIncomeYer", 0.0),
                            povertyTier = runCatching { PovertyTier.valueOf(o.optString("povertyTier")) }.getOrDefault(PovertyTier.MEDIUM),
                            region = o.optString("region", "المنطقة الوسطى"),
                            village = o.optString("village", "القرية المركزية"),
                            address = o.optString("address"),
                            aidHistoryCount = o.optInt("aidHistoryCount", 0)
                        )
                    )
                }
                if (list.isNotEmpty()) beneficiaries.value = list
            }

            root.optJSONArray("accounts")?.let { arr ->
                val list = mutableListOf<Account>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val pCode = o.optString("parentCode")
                    list.add(
                        Account(
                            code = o.optString("code"),
                            name = o.optString("name"),
                            type = runCatching { AccountType.valueOf(o.optString("type")) }.getOrDefault(AccountType.ASSET),
                            isDebitNormal = o.optBoolean("isDebitNormal", true),
                            balance = o.optDouble("balance", 0.0),
                            parentCode = if (pCode.isNullOrEmpty()) null else pCode
                        )
                    )
                }
                if (list.isNotEmpty()) accounts.value = list
            }

            root.optJSONArray("vouchers")?.let { arr ->
                val list = mutableListOf<Voucher>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        Voucher(
                            id = o.optString("id"),
                            voucherNo = o.optString("voucherNo"),
                            type = runCatching { VoucherType.valueOf(o.optString("type")) }.getOrDefault(VoucherType.RECEIPT),
                            status = runCatching { VoucherStatus.valueOf(o.optString("status")) }.getOrDefault(VoucherStatus.POSTED),
                            partyName = o.optString("partyName"),
                            amountOriginal = o.optDouble("amountOriginal", 0.0),
                            currency = runCatching { CurrencyCode.valueOf(o.optString("currency")) }.getOrDefault(CurrencyCode.YER),
                            exchangeRate = o.optDouble("exchangeRate", 1.0),
                            amountYer = o.optDouble("amountYer", 0.0),
                            amountWords = o.optString("amountWords"),
                            paymentMethod = runCatching { PaymentMethod.valueOf(o.optString("paymentMethod")) }.getOrDefault(PaymentMethod.CASH),
                            accountCode = o.optString("accountCode"),
                            accountName = o.optString("accountName"),
                            counterpartAccountCode = o.optString("counterpartAccountCode"),
                            counterpartAccountName = o.optString("counterpartAccountName"),
                            description = o.optString("description"),
                            date = o.optString("date"),
                            linkedJournalEntryId = o.optString("linkedJournalEntryId"),
                            receivedOrPaidBy = o.optString("receivedOrPaidBy")
                        )
                    )
                }
                if (list.isNotEmpty()) vouchers.value = list
            }

            root.optJSONArray("syncQueue")?.let { arr ->
                val list = mutableListOf<SyncQueueItem>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                        SyncQueueItem(
                            id = o.optString("id"),
                            timestamp = o.optString("timestamp"),
                            entityName = o.optString("entityName"),
                            action = o.optString("action"),
                            summary = o.optString("summary"),
                            txHash = o.optString("txHash", "A9F401C2"),
                            payloadBytes = o.optInt("payloadBytes", 512)
                        )
                    )
                }
                syncQueue.value = list
            }

            if (!isSilentLoad) {
                logAudit("استيراد بيانات", "تم استيراد نسخة احتياطية بنجاح بتاريخ ${root.optString("exportDate")}")
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}
