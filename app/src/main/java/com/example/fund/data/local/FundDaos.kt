package com.example.fund.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountingDao {
    @Query("SELECT * FROM accounts ORDER BY code ASC")
    fun observeAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts ORDER BY code ASC")
    suspend fun getAllAccounts(): List<AccountEntity>

    @Query("SELECT * FROM accounts WHERE code = :code LIMIT 1")
    suspend fun getAccountByCode(code: String): AccountEntity?

    @Upsert
    suspend fun upsertAccount(account: AccountEntity)

    @Upsert
    suspend fun upsertAccounts(accounts: List<AccountEntity>)

    @Query("UPDATE accounts SET balance = balance + :delta WHERE code = :code")
    suspend fun adjustAccountBalance(code: String, delta: Double)

    @Query("DELETE FROM accounts")
    suspend fun clearAccounts()

    @Query("SELECT * FROM vouchers ORDER BY sortOrder ASC, date DESC")
    fun observeVouchers(): Flow<List<VoucherEntity>>

    @Query("SELECT * FROM vouchers")
    suspend fun getAllVouchers(): List<VoucherEntity>

    @Upsert
    suspend fun upsertVoucher(voucher: VoucherEntity)

    @Upsert
    suspend fun upsertVouchers(vouchers: List<VoucherEntity>)

    @Query("DELETE FROM vouchers")
    suspend fun clearVouchers()

    @Query("SELECT * FROM journal_entries ORDER BY sortOrder ASC, date DESC")
    fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries")
    suspend fun getAllJournalEntries(): List<JournalEntryEntity>

    @Upsert
    suspend fun upsertJournalEntry(entry: JournalEntryEntity)

    @Upsert
    suspend fun upsertJournalEntries(entries: List<JournalEntryEntity>)

    @Query("DELETE FROM journal_entries")
    suspend fun clearJournalEntries()

    @Query("SELECT * FROM budgets ORDER BY accountCode ASC")
    fun observeBudgets(): Flow<List<BudgetEntity>>

    @Upsert
    suspend fun upsertBudget(budget: BudgetEntity)

    @Upsert
    suspend fun upsertBudgets(budgets: List<BudgetEntity>)

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    @Query("SELECT * FROM currency_rates")
    fun observeCurrencyRates(): Flow<List<CurrencyRateEntity>>

    @Upsert
    suspend fun upsertCurrencyRate(rate: CurrencyRateEntity)

    @Upsert
    suspend fun upsertCurrencyRates(rates: List<CurrencyRateEntity>)

    @Query("DELETE FROM currency_rates")
    suspend fun clearCurrencyRates()
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM region_groups")
    fun observeRegions(): Flow<List<RegionGroupEntity>>

    @Upsert
    suspend fun upsertRegion(region: RegionGroupEntity)

    @Upsert
    suspend fun upsertRegions(regions: List<RegionGroupEntity>)

    @Query("DELETE FROM region_groups WHERE id = :regionId")
    suspend fun deleteRegionById(regionId: String)

    @Query("DELETE FROM region_groups")
    suspend fun clearRegions()

    @Query("SELECT * FROM members ORDER BY sortOrder ASC, memberNo ASC")
    fun observeMembers(): Flow<List<MemberEntity>>

    @Upsert
    suspend fun upsertMember(member: MemberEntity)

    @Upsert
    suspend fun upsertMembers(members: List<MemberEntity>)

    @Query("DELETE FROM members")
    suspend fun clearMembers()

    @Query("SELECT * FROM subscriptions ORDER BY sortOrder ASC, date DESC")
    fun observeSubscriptions(): Flow<List<SubscriptionEntity>>

    @Upsert
    suspend fun upsertSubscription(subscription: SubscriptionEntity)

    @Upsert
    suspend fun upsertSubscriptions(subscriptions: List<SubscriptionEntity>)

    @Query("DELETE FROM subscriptions")
    suspend fun clearSubscriptions()
}

@Dao
interface AidDao {
    @Query("SELECT * FROM beneficiaries")
    fun observeBeneficiaries(): Flow<List<BeneficiaryEntity>>

    @Upsert
    suspend fun upsertBeneficiary(beneficiary: BeneficiaryEntity)

    @Upsert
    suspend fun upsertBeneficiaries(beneficiaries: List<BeneficiaryEntity>)

    @Query("DELETE FROM beneficiaries")
    suspend fun clearBeneficiaries()

    @Query("SELECT * FROM aid_requests ORDER BY sortOrder ASC, requestDate DESC")
    fun observeAidRequests(): Flow<List<AidRequestEntity>>

    @Upsert
    suspend fun upsertAidRequest(request: AidRequestEntity)

    @Upsert
    suspend fun upsertAidRequests(requests: List<AidRequestEntity>)

    @Query("DELETE FROM aid_requests")
    suspend fun clearAidRequests()

    @Query("SELECT * FROM periodic_aids")
    fun observePeriodicAids(): Flow<List<PeriodicAidEntity>>

    @Upsert
    suspend fun upsertPeriodicAid(aid: PeriodicAidEntity)

    @Upsert
    suspend fun upsertPeriodicAids(aids: List<PeriodicAidEntity>)

    @Query("DELETE FROM periodic_aids")
    suspend fun clearPeriodicAids()

    @Query("SELECT * FROM donors")
    fun observeDonors(): Flow<List<DonorEntity>>

    @Upsert
    suspend fun upsertDonor(donor: DonorEntity)

    @Upsert
    suspend fun upsertDonors(donors: List<DonorEntity>)

    @Query("DELETE FROM donors")
    suspend fun clearDonors()

    @Query("SELECT * FROM campaigns")
    fun observeCampaigns(): Flow<List<CampaignEntity>>

    @Upsert
    suspend fun upsertCampaign(campaign: CampaignEntity)

    @Upsert
    suspend fun upsertCampaigns(campaigns: List<CampaignEntity>)

    @Query("DELETE FROM campaigns")
    suspend fun clearCampaigns()

    @Query("SELECT * FROM in_kind_items")
    fun observeInKindItems(): Flow<List<InKindItemEntity>>

    @Upsert
    suspend fun upsertInKindItem(item: InKindItemEntity)

    @Upsert
    suspend fun upsertInKindItems(items: List<InKindItemEntity>)

    @Query("DELETE FROM in_kind_items")
    suspend fun clearInKindItems()

    @Query("SELECT * FROM in_kind_movements ORDER BY date DESC")
    fun observeInKindMovements(): Flow<List<InKindMovementEntity>>

    @Upsert
    suspend fun upsertInKindMovement(movement: InKindMovementEntity)

    @Upsert
    suspend fun upsertInKindMovements(movements: List<InKindMovementEntity>)

    @Query("DELETE FROM in_kind_movements")
    suspend fun clearInKindMovements()
}

@Dao
interface UserDao {
    @Query("SELECT * FROM app_users")
    fun observeUsers(): Flow<List<AppUserEntity>>

    @Query("SELECT * FROM app_users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): AppUserEntity?

    @Upsert
    suspend fun upsertUser(user: AppUserEntity)

    @Upsert
    suspend fun upsertUsers(users: List<AppUserEntity>)

    @Query("DELETE FROM app_users")
    suspend fun clearUsers()

    @Query("SELECT * FROM audit_logs ORDER BY sortOrder DESC")
    fun observeAuditLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AuditLogEntity>)

    @Query("DELETE FROM audit_logs")
    suspend fun clearAuditLogs()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM fund_info WHERE id = 1")
    fun observeFundInfo(): Flow<FundInfoEntity?>

    @Query("SELECT * FROM fund_info WHERE id = 1")
    suspend fun getFundInfo(): FundInfoEntity?

    @Upsert
    suspend fun upsertFundInfo(info: FundInfoEntity)

    @Query("SELECT * FROM app_settings WHERE id = 1")
    fun observeAppSettings(): Flow<AppSettingsEntity?>

    @Query("SELECT * FROM app_settings WHERE id = 1")
    suspend fun getAppSettings(): AppSettingsEntity?

    @Upsert
    suspend fun upsertAppSettings(settings: AppSettingsEntity)

    @Query("SELECT * FROM print_settings WHERE id = 1")
    fun observePrintSettings(): Flow<PrintSettingsEntity?>

    @Query("SELECT * FROM print_settings WHERE id = 1")
    suspend fun getPrintSettings(): PrintSettingsEntity?

    @Upsert
    suspend fun upsertPrintSettings(settings: PrintSettingsEntity)

    @Query("SELECT * FROM scheduler_events")
    fun observeSchedulerEvents(): Flow<List<SchedulerEventEntity>>

    @Upsert
    suspend fun upsertSchedulerEvent(event: SchedulerEventEntity)

    @Upsert
    suspend fun upsertSchedulerEvents(events: List<SchedulerEventEntity>)

    @Query("DELETE FROM scheduler_events")
    suspend fun clearSchedulerEvents()

    @Query("SELECT * FROM internal_messages")
    fun observeMessages(): Flow<List<InternalMessageEntity>>

    @Upsert
    suspend fun upsertMessage(message: InternalMessageEntity)

    @Upsert
    suspend fun upsertMessages(messages: List<InternalMessageEntity>)

    @Query("DELETE FROM internal_messages")
    suspend fun clearMessages()
}

@Dao
interface SyncDao {
    @Query("SELECT * FROM sync_queue")
    fun observeSyncQueue(): Flow<List<SyncQueueItemEntity>>

    @Upsert
    suspend fun upsertSyncQueueItem(item: SyncQueueItemEntity)

    @Upsert
    suspend fun upsertSyncQueueItems(items: List<SyncQueueItemEntity>)

    @Query("DELETE FROM sync_queue")
    suspend fun clearSyncQueue()

    @Query("SELECT * FROM sync_batches")
    fun observeSyncBatches(): Flow<List<SyncBatchRecordEntity>>

    @Upsert
    suspend fun upsertSyncBatch(batch: SyncBatchRecordEntity)

    @Upsert
    suspend fun upsertSyncBatches(batches: List<SyncBatchRecordEntity>)

    @Query("DELETE FROM sync_batches")
    suspend fun clearSyncBatches()

    @Query("SELECT * FROM local_server_config WHERE id = 1")
    fun observeServerConfig(): Flow<LocalServerConfigEntity?>

    @Upsert
    suspend fun upsertServerConfig(config: LocalServerConfigEntity)
}
