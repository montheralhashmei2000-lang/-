package com.example.fund.data.local

import android.content.Context
import androidx.core.content.edit
import androidx.room.withTransaction
import com.example.fund.data.SeedData
import com.example.fund.model.AppSettings
import com.example.fund.model.FundInfo
import com.example.fund.model.LocalServerConfig
import com.example.fund.model.PrintSettings
import com.example.fund.security.EncryptedSessionManager

/**
 * Performs a safe one-time migration from the legacy SharedPreferences JSON blob
 * ("social_fund_v9_storage" -> "full_database_json") into Room SQLite + EncryptedSessionManager,
 * and seeds initial records when Room starts empty.
 */
object LegacyMigrationHelper {

    const val LEGACY_PREFS_NAME = "social_fund_v9_storage"
    const val LEGACY_JSON_KEY = "full_database_json"

    suspend fun migrateAndSeedIfNeeded(
        context: Context,
        db: FundDatabase,
        sessionManager: EncryptedSessionManager
    ) {
        val legacyPrefs = context.applicationContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
        val legacyJson = legacyPrefs.getString(LEGACY_JSON_KEY, null)

        // 1. Migrate legacy UI/Server preferences if present and not yet migrated
        if (!legacyPrefs.getBoolean("migrated_prefs_to_encrypted", false)) {
            if (legacyPrefs.contains("is_dark_mode")) {
                sessionManager.isDarkMode = legacyPrefs.getBoolean("is_dark_mode", false)
            }
            if (legacyPrefs.contains("font_scale")) {
                sessionManager.fontScale = legacyPrefs.getFloat("font_scale", 1.0f)
            }
            if (legacyPrefs.contains("auto_sync_on_connect")) {
                sessionManager.autoSyncOnConnect = legacyPrefs.getBoolean("auto_sync_on_connect", true)
            }
            val oldServerName = legacyPrefs.getString("server_name", null)
            val oldServerHost = legacyPrefs.getString("server_host", null)
            if (!oldServerName.isNullOrBlank() && !oldServerHost.isNullOrBlank()) {
                sessionManager.serverName = oldServerName
                sessionManager.serverHost = oldServerHost
            }
            legacyPrefs.edit { putBoolean("migrated_prefs_to_encrypted", true) }
        }

        // 2. Migrate legacy JSON blob into Room inside a single atomic transaction.
        // Never remove LEGACY_JSON_KEY or mark migration completed if parsing or DB commit fails.
        if (!legacyJson.isNullOrBlank()) {
            val parsed = FundBackupCodec.parseFromJson(legacyJson) ?: return
            val committed = runCatching {
                writeSnapshotToDatabase(db, parsed, clearFirst = true)
            }.isSuccess
            if (committed) {
                legacyPrefs.edit {
                    remove(LEGACY_JSON_KEY)
                    putBoolean("migrated_to_room_v1", true)
                }
            }
            return
        }

        // 3. Seed Room with initial data if tables are empty
        if (db.accountingDao().getAllAccounts().isEmpty()) {
            seedDefaultDatabase(db, sessionManager)
        }
    }

    suspend fun writeSnapshotToDatabase(
        db: FundDatabase,
        snapshot: ParsedDatabaseSnapshot,
        clearFirst: Boolean = true
    ) {
        val accountingDao = db.accountingDao()
        val memberDao = db.memberDao()
        val aidDao = db.aidDao()
        val userDao = db.userDao()
        val settingsDao = db.settingsDao()
        val syncDao = db.syncDao()

        db.withTransaction {
            if (clearFirst) {
                // Delete child FK tables before parent tables
                aidDao.clearInKindMovements()
                aidDao.clearInKindItems()
                aidDao.clearPeriodicAids()
                aidDao.clearAidRequests()
                aidDao.clearBeneficiaries()
                aidDao.clearDonors()
                aidDao.clearCampaigns()
                memberDao.clearSubscriptions()
                memberDao.clearMembers()
                memberDao.clearRegions()
                accountingDao.clearVouchers()
                accountingDao.clearJournalEntries()
                accountingDao.clearBudgets()
                accountingDao.clearAccounts()
                accountingDao.clearCurrencyRates()
                settingsDao.clearSchedulerEvents()
                settingsDao.clearMessages()
                syncDao.clearSyncQueue()
            }

            // Preserve custom/updated users when present in snapshot
            val users = snapshot.users?.takeIf { it.isNotEmpty() } ?: SeedData.users
            if (clearFirst) {
                userDao.clearUsers()
            }
            userDao.upsertUsers(users.map { AppUserEntity.fromDomain(it) })

            val fi = snapshot.fundInfo ?: FundInfo()
            val st = snapshot.appSettings ?: AppSettings()
            val ps = snapshot.printSettings ?: PrintSettings()
            settingsDao.upsertFundInfo(FundInfoEntity.fromDomain(fi))
            settingsDao.upsertAppSettings(AppSettingsEntity.fromDomain(st))
            settingsDao.upsertPrintSettings(PrintSettingsEntity.fromDomain(ps))

            val regions = snapshot.regions ?: SeedData.regions
            memberDao.upsertRegions(regions.map { RegionGroupEntity.fromDomain(it) })

            val accounts = snapshot.accounts ?: SeedData.accounts
            accountingDao.upsertAccounts(accounts.map { AccountEntity.fromDomain(it) })
            val accountCodes = accounts.map { it.code }.toSet()

            val rates = snapshot.currencyRates ?: SeedData.currencyRates
            accountingDao.upsertCurrencyRates(rates.map { (k, v) -> CurrencyRateEntity(k, v) })

            val members = snapshot.members ?: SeedData.members
            memberDao.upsertMembers(members.mapIndexed { idx, m -> MemberEntity.fromDomain(m, idx) })
            val memberIds = members.map { it.id }.toSet()

            val subs = snapshot.subscriptions ?: SeedData.subscriptions
            memberDao.upsertSubscriptions(
                subs.filter { memberIds.contains(it.memberId) }
                    .mapIndexed { idx, s -> SubscriptionEntity.fromDomain(s, idx) }
            )

            val beneficiaries = snapshot.beneficiaries ?: SeedData.beneficiaries
            aidDao.upsertBeneficiaries(beneficiaries.map { BeneficiaryEntity.fromDomain(it) })
            val benIds = beneficiaries.map { it.id }.toSet()

            val periodic = snapshot.periodicAids ?: SeedData.periodicAids
            aidDao.upsertPeriodicAids(
                periodic.filter { benIds.contains(it.beneficiaryId) }
                    .map { PeriodicAidEntity.fromDomain(it) }
            )

            val aidReqs = snapshot.aidRequests ?: SeedData.aidRequests
            aidDao.upsertAidRequests(
                aidReqs.filter { memberIds.contains(it.memberId) }
                    .mapIndexed { idx, r -> AidRequestEntity.fromDomain(r, idx) }
            )

            val vouchers = snapshot.vouchers ?: SeedData.vouchers
            accountingDao.upsertVouchers(
                vouchers.filter { accountCodes.contains(it.accountCode) && accountCodes.contains(it.counterpartAccountCode) }
                    .mapIndexed { idx, v -> VoucherEntity.fromDomain(v, idx) }
            )

            val journals = snapshot.journalEntries ?: SeedData.journalEntries
            accountingDao.upsertJournalEntries(journals.mapIndexed { idx, j -> JournalEntryEntity.fromDomain(j, idx) })

            val budgets = snapshot.budgets ?: SeedData.budgets
            accountingDao.upsertBudgets(
                budgets.filter { accountCodes.contains(it.accountCode) }
                    .map { BudgetEntity.fromDomain(it) }
            )

            val donors = snapshot.donors ?: SeedData.donors
            aidDao.upsertDonors(donors.map { DonorEntity.fromDomain(it) })

            val campaigns = snapshot.campaigns ?: SeedData.campaigns
            aidDao.upsertCampaigns(campaigns.map { CampaignEntity.fromDomain(it) })

            val inKindItems = snapshot.inKindItems ?: SeedData.inKindItems
            aidDao.upsertInKindItems(inKindItems.map { InKindItemEntity.fromDomain(it) })
            val itemIds = inKindItems.map { it.id }.toSet()

            val movements = snapshot.inKindMovements ?: SeedData.inKindMovements
            aidDao.upsertInKindMovements(
                movements.filter { itemIds.contains(it.itemId) }
                    .map { InKindMovementEntity.fromDomain(it) }
            )

            val events = snapshot.schedulerEvents ?: SeedData.schedulerEvents
            settingsDao.upsertSchedulerEvents(events.map { SchedulerEventEntity.fromDomain(it) })

            val messages = snapshot.internalMessages ?: SeedData.internalMessages
            settingsDao.upsertMessages(messages.map { InternalMessageEntity.fromDomain(it) })

            // Append-only audit logs insertion
            val auditLogs = snapshot.auditLogs ?: SeedData.auditLogs
            userDao.insertAuditLogs(auditLogs.mapIndexed { idx, a ->
                AuditLogEntity.fromDomain(a, System.currentTimeMillis() - idx * 1000L)
            })

            val syncQueue = snapshot.syncQueue ?: emptyList()
            syncDao.upsertSyncQueueItems(syncQueue.map { SyncQueueItemEntity.fromDomain(it) })

            snapshot.syncBatches?.let { batches ->
                syncDao.upsertSyncBatches(batches.map { SyncBatchRecordEntity.fromDomain(it) })
            }
            snapshot.serverConfig?.let { sc ->
                syncDao.upsertServerConfig(LocalServerConfigEntity.fromDomain(sc))
            }
        }
    }

    suspend fun seedDefaultDatabase(db: FundDatabase, sessionManager: EncryptedSessionManager) {
        val defaultSnapshot = ParsedDatabaseSnapshot(
            fundInfo = FundInfo(),
            regions = SeedData.regions,
            appSettings = AppSettings(),
            printSettings = PrintSettings(),
            members = SeedData.members,
            beneficiaries = SeedData.beneficiaries,
            accounts = SeedData.accounts,
            vouchers = SeedData.vouchers,
            journalEntries = SeedData.journalEntries,
            subscriptions = SeedData.subscriptions,
            aidRequests = SeedData.aidRequests,
            budgets = SeedData.budgets,
            donors = SeedData.donors,
            campaigns = SeedData.campaigns,
            periodicAids = SeedData.periodicAids,
            inKindItems = SeedData.inKindItems,
            inKindMovements = SeedData.inKindMovements,
            schedulerEvents = SeedData.schedulerEvents,
            internalMessages = SeedData.internalMessages,
            currencyRates = SeedData.currencyRates,
            syncQueue = emptyList(),
            users = SeedData.users,
            auditLogs = SeedData.auditLogs
        )
        writeSnapshotToDatabase(db, defaultSnapshot, clearFirst = true)
        db.syncDao().upsertServerConfig(
            LocalServerConfigEntity.fromDomain(
                LocalServerConfig(
                    serverName = sessionManager.serverName,
                    serverHost = sessionManager.serverHost
                )
            )
        )
    }
}
