package com.example.fund.data

import android.content.Context
import com.example.fund.data.local.FundBackupCodec
import com.example.fund.data.local.FundDatabase
import com.example.fund.data.local.LegacyMigrationHelper
import com.example.fund.data.repository.*
import com.example.fund.model.*
import com.example.fund.security.EncryptedSessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Unified coordinator facade over the 6 specialized MVVM repositories backed by
 * Room SQLite ([FundDatabase]) and [EncryptedSessionManager].
 */
class FundRepository private constructor() {

    companion object {
        @Volatile
        private var instance: FundRepository? = null

        fun getInstance(): FundRepository =
            instance ?: synchronized(this) {
                instance ?: FundRepository().also { instance = it }
            }
    }

    private val dbDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val userRepository = UserRepository(repoScope, dbDispatcher)
    val settingsRepository = SettingsRepository(repoScope, dbDispatcher, userRepository)
    val syncRepository = SyncRepository(repoScope, dbDispatcher, userRepository)
    val accountingRepository = AccountingRepository(repoScope, dbDispatcher, settingsRepository, userRepository)
    val memberRepository = MemberRepository(repoScope, dbDispatcher, accountingRepository, userRepository)
    val aidRepository = AidRepository(repoScope, dbDispatcher, accountingRepository, memberRepository, userRepository)

    private var database: FundDatabase? = null
    private var sessionManager: EncryptedSessionManager? = null
    @Volatile
    private var storageInitialized = false

    init {
        userRepository.configureHooks(
            settingsProvider = { settingsRepository.appSettings.value },
            serverConfigProvider = { syncRepository.serverConfig.value },
            isOnlineProvider = { syncRepository.isOnline.value },
            onOfflineAuditHook = { action, desc -> syncRepository.enqueueOfflineOperation(action, desc) }
        )
        syncRepository.setBackupJsonProvider { exportDatabaseJson() }
    }

    // StateFlow projections delegated directly to specialized repositories
    val currentUser: StateFlow<AppUser> get() = userRepository.currentUser
    val isLoggedIn: StateFlow<Boolean> get() = userRepository.isLoggedIn
    val users: StateFlow<List<AppUser>> get() = userRepository.users
    val auditLogs: StateFlow<List<AuditLog>> get() = userRepository.auditLogs

    val fundInfo: StateFlow<FundInfo> get() = settingsRepository.fundInfo
    val appSettings: StateFlow<AppSettings> get() = settingsRepository.appSettings
    val printSettings: StateFlow<PrintSettings> get() = settingsRepository.printSettings
    val schedulerEvents: StateFlow<List<SchedulerEvent>> get() = settingsRepository.schedulerEvents
    val internalMessages: StateFlow<List<InternalMessage>> get() = settingsRepository.internalMessages
    val isDarkMode: StateFlow<Boolean> get() = settingsRepository.isDarkMode
    val fontScale: StateFlow<Float> get() = settingsRepository.fontScale
    val appFontFamily: StateFlow<AppFontOption> get() = settingsRepository.appFontFamily
    val screenFormat: StateFlow<DisplayFormatConfig> get() = settingsRepository.screenFormat
    val printFormat: StateFlow<DisplayFormatConfig> get() = settingsRepository.printFormat

    val isOnline: StateFlow<Boolean> get() = syncRepository.isOnline
    val serverConfig: StateFlow<LocalServerConfig> get() = syncRepository.serverConfig
    val autoSyncOnConnect: StateFlow<Boolean> get() = syncRepository.autoSyncOnConnect
    val isSyncing: StateFlow<Boolean> get() = syncRepository.isSyncing
    val syncProgress: StateFlow<Float> get() = syncRepository.syncProgress
    val syncStageMessage: StateFlow<String> get() = syncRepository.syncStageMessage
    val lastSyncTimestamp: StateFlow<String> get() = syncRepository.lastSyncTimestamp
    val syncHistory: StateFlow<List<SyncBatchRecord>> get() = syncRepository.syncHistory
    val syncQueue: StateFlow<List<SyncQueueItem>> get() = syncRepository.syncQueue

    val accounts: StateFlow<List<Account>> get() = accountingRepository.accounts
    val vouchers: StateFlow<List<Voucher>> get() = accountingRepository.vouchers
    val journalEntries: StateFlow<List<JournalEntry>> get() = accountingRepository.journalEntries
    val budgets: StateFlow<List<Budget>> get() = accountingRepository.budgets
    val currencyRates: StateFlow<Map<CurrencyCode, Double>> get() = accountingRepository.currencyRates

    val regions: StateFlow<List<RegionGroup>> get() = memberRepository.regions
    val members: StateFlow<List<Member>> get() = memberRepository.members
    val subscriptions: StateFlow<List<Subscription>> get() = memberRepository.subscriptions

    val aidRequests: StateFlow<List<AidRequest>> get() = aidRepository.aidRequests
    val beneficiaries: StateFlow<List<Beneficiary>> get() = aidRepository.beneficiaries
    val periodicAids: StateFlow<List<PeriodicAid>> get() = aidRepository.periodicAids
    val donors: StateFlow<List<Donor>> get() = aidRepository.donors
    val campaigns: StateFlow<List<Campaign>> get() = aidRepository.campaigns
    val inKindItems: StateFlow<List<InKindItem>> get() = aidRepository.inKindItems
    val inKindMovements: StateFlow<List<InKindMovement>> get() = aidRepository.inKindMovements

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH)
    private fun nowTimeStr() = timeFormat.format(Date())

    // ------------------------------------------------------------------------
    // Room SQLite + EncryptedSessionManager Initialization & One-Time Migration
    // ------------------------------------------------------------------------
    fun initStorage(context: Context) {
        if (storageInitialized) return
        synchronized(this) {
            if (storageInitialized) return
            val appCtx = context.applicationContext
            val db = FundDatabase.getInstance(appCtx)
            val session = EncryptedSessionManager(appCtx)
            database = db
            sessionManager = session

            runBlocking(Dispatchers.IO) {
                LegacyMigrationHelper.migrateAndSeedIfNeeded(appCtx, db, session)
                userRepository.attachAndHydrate(db, session)
                settingsRepository.attachAndHydrate(db, session)
                syncRepository.attachAndHydrate(db, session)
                accountingRepository.attachAndHydrate(db)
                memberRepository.attachAndHydrate(db)
                aidRepository.attachAndHydrate(db)
            }
            storageInitialized = true
        }
    }

    fun getDatabaseOrNull(): FundDatabase? = database
    fun getSessionManagerOrNull(): EncryptedSessionManager? = sessionManager

    fun setDarkModePreference(enabled: Boolean) = settingsRepository.setDarkModePreference(enabled)
    fun setFontScalePreference(scale: Float) = settingsRepository.setFontScalePreference(scale)
    fun setAppFontPreference(fontOption: AppFontOption) = settingsRepository.setAppFontPreference(fontOption)
    fun setAutoSyncOnConnect(enabled: Boolean) = syncRepository.setAutoSyncOnConnect(enabled)
    fun updateServerNode(name: String, host: String, latencyMs: Int = 8) =
        syncRepository.updateServerNode(name, host, latencyMs)

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
        settingsRepository.resetToSeed()
        memberRepository.resetToSeed()
        aidRepository.resetToSeed()
        accountingRepository.resetToSeed()
        userRepository.resetToSeed()
        syncRepository.resetToSeed()
        userRepository.logAudit("إعادة ضبط المصنع", "تمت استعادة البيانات الأولية الافتراضية للصندوق بنجاح")

        val db = database
        val session = sessionManager
        if (db != null && session != null) {
            repoScope.launch(dbDispatcher) {
                LegacyMigrationHelper.seedDefaultDatabase(db, session)
            }
        }
    }

    // Authentication & Audit
    fun authenticateWithPassword(usernameInput: String, passwordInput: String): AuthResult =
        userRepository.authenticateWithPassword(usernameInput, passwordInput)

    fun authenticateUser(usernameInput: String): AppUser =
        userRepository.authenticateUser(usernameInput)

    fun logout() = userRepository.logout()
    fun logAudit(action: String, description: String) = userRepository.logAudit(action, description)
    fun simulateOfflineFieldOperations() = syncRepository.simulateOfflineFieldOperations()
    fun syncNow() = syncRepository.syncNow()
    fun toggleOnline() = syncRepository.toggleOnline()
    fun switchRole(role: UserRole) = userRepository.switchRole(role)

    // Currency
    fun convertToYer(amountOriginal: Double, currency: CurrencyCode): Double =
        accountingRepository.convertToYer(amountOriginal, currency)

    fun updateCurrencyRate(currency: CurrencyCode, rate: Double) =
        accountingRepository.updateCurrencyRate(currency, rate)

    // Accounting & Vouchers
    fun issueVoucher(
        type: VoucherType,
        partyName: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        paymentMethod: PaymentMethod,
        accountCode: String,
        counterpartAccountCode: String,
        description: String
    ): Voucher = accountingRepository.issueVoucher(
        type, partyName, amountOriginal, currency, paymentMethod, accountCode, counterpartAccountCode, description
    )

    fun transferBetweenTreasuryAccounts(
        fromAccountCode: String,
        toAccountCode: String,
        amountYer: Double,
        notes: String
    ): Boolean = accountingRepository.transferBetweenTreasuryAccounts(fromAccountCode, toAccountCode, amountYer, notes)

    fun voidVoucher(voucherId: String) = accountingRepository.voidVoucher(voucherId)

    fun addSubAccount(
        code: String,
        name: String,
        type: AccountType,
        parentCode: String,
        openingBalance: Double = 0.0
    ): Boolean = accountingRepository.addSubAccount(code, name, type, parentCode, openingBalance)

    fun createManualJournalEntry(lines: List<JournalLine>, description: String): Boolean =
        accountingRepository.createManualJournalEntry(lines, description)

    fun addBudget(category: String, accountCode: String, budgetedAmount: Double, month: String) =
        accountingRepository.addBudget(category, accountCode, budgetedAmount, month)

    fun updateBudgetAmount(budgetId: String, newAmount: Double) =
        accountingRepository.updateBudgetAmount(budgetId, newAmount)

    // Members & Regions
    fun addMember(member: Member) = memberRepository.addMember(member)
    fun updateMember(member: Member) = memberRepository.updateMember(member)
    fun toggleMemberStatus(memberId: String) = memberRepository.toggleMemberStatus(memberId)
    fun addSubscription(
        memberId: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        monthYear: String,
        paymentMethod: PaymentMethod,
        referenceNo: String,
        notes: String
    ): Subscription = memberRepository.addSubscription(
        memberId, amountOriginal, currency, monthYear, paymentMethod, referenceNo, notes
    )

    fun addRegion(regionName: String): Boolean = memberRepository.addRegion(regionName)
    fun deleteRegion(regionId: String) = memberRepository.deleteRegion(regionId)
    fun addVillageToRegion(regionId: String, villageName: String): Boolean =
        memberRepository.addVillageToRegion(regionId, villageName)

    fun addVillageByRegionName(regionName: String, villageName: String) =
        memberRepository.addVillageByRegionName(regionName, villageName)

    fun removeVillageFromRegion(regionId: String, villageName: String) =
        memberRepository.removeVillageFromRegion(regionId, villageName)

    // Aids, Donors, Campaigns, Beneficiaries, In-Kind
    fun addAidRequest(memberId: String, aidType: AidType, amountYer: Double, notes: String): AidRequest =
        aidRepository.addAidRequest(memberId, aidType, amountYer, notes)

    fun updateAidStatus(requestId: String, newStatus: AidStatus, approvedAmountYer: Double = 0.0) =
        aidRepository.updateAidStatus(requestId, newStatus, approvedAmountYer)

    fun addDonor(donor: Donor) = aidRepository.addDonor(donor)

    fun recordDonorPledgePayment(
        donorId: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        paymentMethod: PaymentMethod
    ) = aidRepository.recordDonorPledgePayment(donorId, amountOriginal, currency, paymentMethod)

    fun addCampaign(campaign: Campaign) = aidRepository.addCampaign(campaign)

    fun donateToCampaign(
        campaignId: String,
        donorName: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        paymentMethod: PaymentMethod
    ) = aidRepository.donateToCampaign(campaignId, donorName, amountOriginal, currency, paymentMethod)

    fun disburseFromCampaign(
        campaignId: String,
        recipientName: String,
        amountYer: Double,
        description: String
    ) = aidRepository.disburseFromCampaign(campaignId, recipientName, amountYer, description)

    fun addBeneficiary(b: Beneficiary) = aidRepository.addBeneficiary(b)

    fun addPeriodicAid(
        beneficiaryId: String,
        beneficiaryName: String,
        monthlyAmountYer: Double,
        sponsorshipType: String
    ) = aidRepository.addPeriodicAid(beneficiaryId, beneficiaryName, monthlyAmountYer, sponsorshipType)

    fun disburseSinglePeriodicAid(id: String, month: String) = aidRepository.disburseSinglePeriodicAid(id, month)

    fun disbursePeriodicAidsBatch(month: String, ids: List<String>) =
        aidRepository.disbursePeriodicAidsBatch(month, ids)

    fun addInKindItem(item: InKindItem) = aidRepository.addInKindItem(item)
    fun addInKindMovement(mov: InKindMovement) = aidRepository.addInKindMovement(mov)

    // Settings, Scheduler, Messages, Users
    fun addSchedulerEvent(event: SchedulerEvent) = settingsRepository.addSchedulerEvent(event)
    fun sendMessage(subject: String, body: String, recipientRole: String) =
        settingsRepository.sendMessage(subject, body, recipientRole)

    fun markMessageRead(id: String) = settingsRepository.markMessageRead(id)

    fun addUser(
        username: String,
        fullName: String,
        role: UserRole,
        rawPassword: String = SeedData.defaultDemoPassword
    ) = userRepository.addUser(username, fullName, role, rawPassword)

    fun toggleUserActive(userId: String) = userRepository.toggleUserActive(userId)

    fun updateFundInfo(info: FundInfo) = settingsRepository.updateFundInfo(info)
    fun updateFundLogo(logoBase64: String) = settingsRepository.updateFundLogo(logoBase64)
    fun updateAppSettings(settings: AppSettings) = settingsRepository.updateAppSettings(settings)
    fun updatePrintSettings(settings: PrintSettings) = settingsRepository.updatePrintSettings(settings)
    fun resetPrintSettingsToDefault() = settingsRepository.resetPrintSettingsToDefault()

    // Full JSON Backup Export & Import (Room-backed)
    fun exportDatabaseJson(): String {
        return FundBackupCodec.exportToJson(
            exportDate = nowTimeStr(),
            fundInfo = fundInfo.value,
            regions = regions.value,
            appSettings = appSettings.value,
            printSettings = printSettings.value,
            members = members.value,
            beneficiaries = beneficiaries.value,
            accounts = accounts.value,
            vouchers = vouchers.value,
            journalEntries = journalEntries.value,
            subscriptions = subscriptions.value,
            aidRequests = aidRequests.value,
            budgets = budgets.value,
            donors = donors.value,
            campaigns = campaigns.value,
            periodicAids = periodicAids.value,
            inKindItems = inKindItems.value,
            inKindMovements = inKindMovements.value,
            schedulerEvents = schedulerEvents.value,
            internalMessages = internalMessages.value,
            currencyRates = currencyRates.value,
            syncQueue = syncQueue.value
        )
    }

    fun importDatabaseFromJson(json: String, isSilentLoad: Boolean = false): Boolean {
        val parsed = FundBackupCodec.parseFromJson(
            json = json,
            fallbackFundInfo = fundInfo.value,
            fallbackAppSettings = appSettings.value,
            fallbackPrintSettings = printSettings.value
        ) ?: return false

        settingsRepository.applySnapshot(
            parsed.fundInfo,
            parsed.appSettings,
            parsed.printSettings,
            parsed.schedulerEvents,
            parsed.internalMessages
        )
        memberRepository.applySnapshot(parsed.regions, parsed.members, parsed.subscriptions)
        aidRepository.applySnapshot(
            parsed.beneficiaries,
            parsed.aidRequests,
            parsed.periodicAids,
            parsed.donors,
            parsed.campaigns,
            parsed.inKindItems,
            parsed.inKindMovements
        )
        accountingRepository.applySnapshot(
            parsed.accounts,
            parsed.vouchers,
            parsed.journalEntries,
            parsed.budgets,
            parsed.currencyRates
        )
        syncRepository.applySyncQueueSnapshot(parsed.syncQueue)

        database?.let { db ->
            repoScope.launch(dbDispatcher) {
                LegacyMigrationHelper.writeSnapshotToDatabase(db, parsed, clearFirst = true)
            }
        }

        if (!isSilentLoad) {
            logAudit("استيراد بيانات", "تم استيراد نسخة احتياطية بنجاح بتاريخ ${parsed.exportDate}")
        }
        return true
    }
}
