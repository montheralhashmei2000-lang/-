package com.example.fund.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        RegionGroupEntity::class,
        MemberEntity::class,
        SubscriptionEntity::class,
        AidRequestEntity::class,
        VoucherEntity::class,
        JournalEntryEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        DonorEntity::class,
        CampaignEntity::class,
        BeneficiaryEntity::class,
        PeriodicAidEntity::class,
        InKindItemEntity::class,
        InKindMovementEntity::class,
        SchedulerEventEntity::class,
        InternalMessageEntity::class,
        AuditLogEntity::class,
        AppUserEntity::class,
        CurrencyRateEntity::class,
        FundInfoEntity::class,
        AppSettingsEntity::class,
        PrintSettingsEntity::class,
        SyncQueueItemEntity::class,
        SyncBatchRecordEntity::class,
        LocalServerConfigEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(FundTypeConverters::class)
abstract class FundDatabase : RoomDatabase() {

    abstract fun accountingDao(): AccountingDao
    abstract fun memberDao(): MemberDao
    abstract fun aidDao(): AidDao
    abstract fun userDao(): UserDao
    abstract fun settingsDao(): SettingsDao
    abstract fun syncDao(): SyncDao

    companion object {
        private const val DB_NAME = "social_fund_v9.db"

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE app_settings ADD COLUMN screenFormat TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE print_settings ADD COLUMN printFormat TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE print_settings ADD COLUMN screenFormat TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Add per-user PIN hash & mandatory password change flag to app_users
                db.execSQL("ALTER TABLE app_users ADD COLUMN quickPinHash TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE app_users ADD COLUMN mustChangePassword INTEGER NOT NULL DEFAULT 0")

                // 2. Rebuild members with unique index on memberNo
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `members_new` (
                        `id` TEXT NOT NULL,
                        `memberNo` TEXT NOT NULL,
                        `fullName` TEXT NOT NULL,
                        `phone` TEXT NOT NULL,
                        `nationalId` TEXT NOT NULL,
                        `joinDate` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `totalPaidYer` REAL NOT NULL,
                        `dueBalanceYer` REAL NOT NULL,
                        `job` TEXT NOT NULL,
                        `region` TEXT NOT NULL,
                        `village` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT OR IGNORE INTO `members_new` SELECT `id`, `memberNo`, `fullName`, `phone`, `nationalId`, `joinDate`, `status`, `totalPaidYer`, `dueBalanceYer`, `job`, `region`, `village`, `notes`, `sortOrder` FROM `members`")
                db.execSQL("DROP TABLE `members`")
                db.execSQL("ALTER TABLE `members_new` RENAME TO `members`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_members_memberNo` ON `members` (`memberNo`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_members_region` ON `members` (`region`)")

                // 3. Rebuild subscriptions with unique index on receiptNo and FK to members
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `subscriptions_new` (
                        `id` TEXT NOT NULL,
                        `receiptNo` TEXT NOT NULL,
                        `memberId` TEXT NOT NULL,
                        `memberName` TEXT NOT NULL,
                        `amountOriginal` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `exchangeRate` REAL NOT NULL,
                        `amountYer` REAL NOT NULL,
                        `monthYear` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `referenceNo` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`memberId`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT OR IGNORE INTO `subscriptions_new` SELECT `id`, `receiptNo`, `memberId`, `memberName`, `amountOriginal`, `currency`, `exchangeRate`, `amountYer`, `monthYear`, `paymentMethod`, `referenceNo`, `date`, `notes`, `sortOrder` FROM `subscriptions` WHERE `memberId` IN (SELECT `id` FROM `members`)")
                db.execSQL("DROP TABLE `subscriptions`")
                db.execSQL("ALTER TABLE `subscriptions_new` RENAME TO `subscriptions`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_subscriptions_memberId` ON `subscriptions` (`memberId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_subscriptions_receiptNo` ON `subscriptions` (`receiptNo`)")

                // 4. Rebuild aid_requests with unique index on requestNo and FK to members
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `aid_requests_new` (
                        `id` TEXT NOT NULL,
                        `requestNo` TEXT NOT NULL,
                        `memberId` TEXT NOT NULL,
                        `memberName` TEXT NOT NULL,
                        `aidType` TEXT NOT NULL,
                        `requestedAmountYer` REAL NOT NULL,
                        `approvedAmountYer` REAL NOT NULL,
                        `status` TEXT NOT NULL,
                        `requestDate` TEXT NOT NULL,
                        `decisionDate` TEXT NOT NULL,
                        `notes` TEXT NOT NULL,
                        `voucherId` TEXT,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`memberId`) REFERENCES `members`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT OR IGNORE INTO `aid_requests_new` SELECT `id`, `requestNo`, `memberId`, `memberName`, `aidType`, `requestedAmountYer`, `approvedAmountYer`, `status`, `requestDate`, `decisionDate`, `notes`, `voucherId`, `sortOrder` FROM `aid_requests` WHERE `memberId` IN (SELECT `id` FROM `members`)")
                db.execSQL("DROP TABLE `aid_requests`")
                db.execSQL("ALTER TABLE `aid_requests_new` RENAME TO `aid_requests`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_aid_requests_memberId` ON `aid_requests` (`memberId`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_aid_requests_requestNo` ON `aid_requests` (`requestNo`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_aid_requests_status` ON `aid_requests` (`status`)")

                // 5. Rebuild vouchers with unique index on voucherNo and FKs to accounts
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `vouchers_new` (
                        `id` TEXT NOT NULL,
                        `voucherNo` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `partyName` TEXT NOT NULL,
                        `amountOriginal` REAL NOT NULL,
                        `currency` TEXT NOT NULL,
                        `exchangeRate` REAL NOT NULL,
                        `amountYer` REAL NOT NULL,
                        `amountWords` TEXT NOT NULL,
                        `paymentMethod` TEXT NOT NULL,
                        `accountCode` TEXT NOT NULL,
                        `accountName` TEXT NOT NULL,
                        `counterpartAccountCode` TEXT NOT NULL,
                        `counterpartAccountName` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `linkedJournalEntryId` TEXT NOT NULL,
                        `receivedOrPaidBy` TEXT NOT NULL,
                        `accountantName` TEXT NOT NULL,
                        `auditorName` TEXT NOT NULL,
                        `managerName` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`accountCode`) REFERENCES `accounts`(`code`) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(`counterpartAccountCode`) REFERENCES `accounts`(`code`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT OR IGNORE INTO `vouchers_new` SELECT `id`, `voucherNo`, `type`, `status`, `partyName`, `amountOriginal`, `currency`, `exchangeRate`, `amountYer`, `amountWords`, `paymentMethod`, `accountCode`, `accountName`, `counterpartAccountCode`, `counterpartAccountName`, `description`, `date`, `linkedJournalEntryId`, `receivedOrPaidBy`, `accountantName`, `auditorName`, `managerName`, `sortOrder` FROM `vouchers` WHERE `accountCode` IN (SELECT `code` FROM `accounts`) AND `counterpartAccountCode` IN (SELECT `code` FROM `accounts`)")
                db.execSQL("DROP TABLE `vouchers`")
                db.execSQL("ALTER TABLE `vouchers_new` RENAME TO `vouchers`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_vouchers_voucherNo` ON `vouchers` (`voucherNo`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vouchers_linkedJournalEntryId` ON `vouchers` (`linkedJournalEntryId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vouchers_accountCode` ON `vouchers` (`accountCode`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vouchers_counterpartAccountCode` ON `vouchers` (`counterpartAccountCode`)")

                // 6. Rebuild journal_entries with unique index on entryNo
                db.execSQL("DROP INDEX IF EXISTS `index_journal_entries_entryNo`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_journal_entries_entryNo` ON `journal_entries` (`entryNo`)")

                // 7. Rebuild budgets with FK to accounts
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budgets_new` (
                        `id` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `accountCode` TEXT NOT NULL,
                        `budgetedAmount` REAL NOT NULL,
                        `actualSpent` REAL NOT NULL,
                        `month` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`accountCode`) REFERENCES `accounts`(`code`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT OR IGNORE INTO `budgets_new` SELECT `id`, `category`, `accountCode`, `budgetedAmount`, `actualSpent`, `month` FROM `budgets` WHERE `accountCode` IN (SELECT `code` FROM `accounts`)")
                db.execSQL("DROP TABLE `budgets`")
                db.execSQL("ALTER TABLE `budgets_new` RENAME TO `budgets`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_budgets_accountCode` ON `budgets` (`accountCode`)")

                // 8. Unique indices for in_kind_items(code) and in_kind_movements(movementNo)
                db.execSQL("DROP INDEX IF EXISTS `index_in_kind_items_code`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_in_kind_items_code` ON `in_kind_items` (`code`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_in_kind_movements_movementNo` ON `in_kind_movements` (`movementNo`)")
            }
        }

        @Volatile
        private var instance: FundDatabase? = null

        fun getInstance(context: Context): FundDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FundDatabase::class.java,
                    DB_NAME
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { instance = it }
            }
        }

        fun buildInMemory(context: Context): FundDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                FundDatabase::class.java
            ).allowMainThreadQueries().build()
        }
    }
}
