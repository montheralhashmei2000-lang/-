package com.example.fund.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

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
    version = 1,
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

        @Volatile
        private var instance: FundDatabase? = null

        fun getInstance(context: Context): FundDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    FundDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
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
