package com.example.fund.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.fund.model.*

@Entity(tableName = "region_groups")
data class RegionGroupEntity(
    @PrimaryKey val id: String,
    val name: String,
    val villages: List<String>
) {
    fun toDomain() = RegionGroup(id = id, name = name, villages = villages)
    companion object {
        fun fromDomain(d: RegionGroup) = RegionGroupEntity(d.id, d.name, d.villages)
    }
}

@Entity(
    tableName = "members",
    indices = [Index(value = ["memberNo"]), Index(value = ["region"])]
)
data class MemberEntity(
    @PrimaryKey val id: String,
    val memberNo: String,
    val fullName: String,
    val phone: String,
    val nationalId: String,
    val joinDate: String,
    val status: String,
    val totalPaidYer: Double,
    val dueBalanceYer: Double,
    val job: String,
    val region: String,
    val village: String,
    val notes: String,
    val sortOrder: Int = 0
) {
    fun toDomain() = Member(
        id, memberNo, fullName, phone, nationalId, joinDate, status,
        totalPaidYer, dueBalanceYer, job, region, village, notes
    )
    companion object {
        fun fromDomain(d: Member, order: Int = 0) = MemberEntity(
            d.id, d.memberNo, d.fullName, d.phone, d.nationalId, d.joinDate, d.status,
            d.totalPaidYer, d.dueBalanceYer, d.job, d.region, d.village, d.notes, order
        )
    }
}

@Entity(
    tableName = "subscriptions",
    indices = [Index(value = ["memberId"]), Index(value = ["receiptNo"])],
    foreignKeys = [
        ForeignKey(
            entity = MemberEntity::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val receiptNo: String,
    val memberId: String,
    val memberName: String,
    val amountOriginal: Double,
    val currency: CurrencyCode,
    val exchangeRate: Double,
    val amountYer: Double,
    val monthYear: String,
    val paymentMethod: PaymentMethod,
    val referenceNo: String,
    val date: String,
    val notes: String,
    val sortOrder: Int = 0
) {
    fun toDomain() = Subscription(
        id, receiptNo, memberId, memberName, amountOriginal, currency, exchangeRate,
        amountYer, monthYear, paymentMethod, referenceNo, date, notes
    )
    companion object {
        fun fromDomain(d: Subscription, order: Int = 0) = SubscriptionEntity(
            d.id, d.receiptNo, d.memberId, d.memberName, d.amountOriginal, d.currency,
            d.exchangeRate, d.amountYer, d.monthYear, d.paymentMethod, d.referenceNo, d.date, d.notes, order
        )
    }
}

@Entity(
    tableName = "aid_requests",
    indices = [Index(value = ["memberId"]), Index(value = ["status"])]
)
data class AidRequestEntity(
    @PrimaryKey val id: String,
    val requestNo: String,
    val memberId: String,
    val memberName: String,
    val aidType: AidType,
    val requestedAmountYer: Double,
    val approvedAmountYer: Double,
    val status: AidStatus,
    val requestDate: String,
    val decisionDate: String,
    val notes: String,
    val voucherId: String?,
    val sortOrder: Int = 0
) {
    fun toDomain() = AidRequest(
        id, requestNo, memberId, memberName, aidType, requestedAmountYer, approvedAmountYer,
        status, requestDate, decisionDate, notes, voucherId
    )
    companion object {
        fun fromDomain(d: AidRequest, order: Int = 0) = AidRequestEntity(
            d.id, d.requestNo, d.memberId, d.memberName, d.aidType, d.requestedAmountYer,
            d.approvedAmountYer, d.status, d.requestDate, d.decisionDate, d.notes, d.voucherId, order
        )
    }
}

@Entity(
    tableName = "vouchers",
    indices = [Index(value = ["voucherNo"]), Index(value = ["linkedJournalEntryId"])]
)
data class VoucherEntity(
    @PrimaryKey val id: String,
    val voucherNo: String,
    val type: VoucherType,
    val status: VoucherStatus,
    val partyName: String,
    val amountOriginal: Double,
    val currency: CurrencyCode,
    val exchangeRate: Double,
    val amountYer: Double,
    val amountWords: String,
    val paymentMethod: PaymentMethod,
    val accountCode: String,
    val accountName: String,
    val counterpartAccountCode: String,
    val counterpartAccountName: String,
    val description: String,
    val date: String,
    val linkedJournalEntryId: String,
    val receivedOrPaidBy: String,
    val accountantName: String,
    val auditorName: String,
    val managerName: String,
    val sortOrder: Int = 0
) {
    fun toDomain() = Voucher(
        id, voucherNo, type, status, partyName, amountOriginal, currency, exchangeRate,
        amountYer, amountWords, paymentMethod, accountCode, accountName, counterpartAccountCode,
        counterpartAccountName, description, date, linkedJournalEntryId, receivedOrPaidBy,
        accountantName, auditorName, managerName
    )
    companion object {
        fun fromDomain(d: Voucher, order: Int = 0) = VoucherEntity(
            d.id, d.voucherNo, d.type, d.status, d.partyName, d.amountOriginal, d.currency,
            d.exchangeRate, d.amountYer, d.amountWords, d.paymentMethod, d.accountCode,
            d.accountName, d.counterpartAccountCode, d.counterpartAccountName, d.description,
            d.date, d.linkedJournalEntryId, d.receivedOrPaidBy, d.accountantName,
            d.auditorName, d.managerName, order
        )
    }
}

@Entity(
    tableName = "journal_entries",
    indices = [Index(value = ["entryNo"]), Index(value = ["originalVoucherNo"])]
)
data class JournalEntryEntity(
    @PrimaryKey val id: String,
    val entryNo: String,
    val date: String,
    val description: String,
    val lines: List<JournalLine>,
    val isPosted: Boolean,
    val isReversed: Boolean,
    val originalVoucherNo: String?,
    val sortOrder: Int = 0
) {
    fun toDomain() = JournalEntry(id, entryNo, date, description, lines, isPosted, isReversed, originalVoucherNo)
    companion object {
        fun fromDomain(d: JournalEntry, order: Int = 0) = JournalEntryEntity(
            d.id, d.entryNo, d.date, d.description, d.lines, d.isPosted, d.isReversed, d.originalVoucherNo, order
        )
    }
}

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val code: String,
    val name: String,
    val type: AccountType,
    val isDebitNormal: Boolean,
    val balance: Double,
    val parentCode: String?
) {
    fun toDomain() = Account(code, name, type, isDebitNormal, balance, parentCode)
    companion object {
        fun fromDomain(d: Account) = AccountEntity(d.code, d.name, d.type, d.isDebitNormal, d.balance, d.parentCode)
    }
}

@Entity(tableName = "budgets", indices = [Index(value = ["accountCode"])])
data class BudgetEntity(
    @PrimaryKey val id: String,
    val category: String,
    val accountCode: String,
    val budgetedAmount: Double,
    val actualSpent: Double,
    val month: String
) {
    fun toDomain() = Budget(id, category, accountCode, budgetedAmount, actualSpent, month)
    companion object {
        fun fromDomain(d: Budget) = BudgetEntity(d.id, d.category, d.accountCode, d.budgetedAmount, d.actualSpent, d.month)
    }
}

@Entity(tableName = "donors")
data class DonorEntity(
    @PrimaryKey val id: String,
    val name: String,
    val phone: String,
    val type: String,
    val totalDonatedYer: Double,
    val pledgeAmountYer: Double,
    val pledgeFulfilledYer: Double,
    val lastDonationDate: String
) {
    fun toDomain() = Donor(id, name, phone, type, totalDonatedYer, pledgeAmountYer, pledgeFulfilledYer, lastDonationDate)
    companion object {
        fun fromDomain(d: Donor) = DonorEntity(d.id, d.name, d.phone, d.type, d.totalDonatedYer, d.pledgeAmountYer, d.pledgeFulfilledYer, d.lastDonationDate)
    }
}

@Entity(tableName = "campaigns")
data class CampaignEntity(
    @PrimaryKey val id: String,
    val title: String,
    val goalAmountYer: Double,
    val collectedAmountYer: Double,
    val spentAmountYer: Double,
    val status: String,
    val description: String,
    val targetCategory: String
) {
    fun toDomain() = Campaign(id, title, goalAmountYer, collectedAmountYer, spentAmountYer, status, description, targetCategory)
    companion object {
        fun fromDomain(d: Campaign) = CampaignEntity(d.id, d.title, d.goalAmountYer, d.collectedAmountYer, d.spentAmountYer, d.status, d.description, d.targetCategory)
    }
}

@Entity(tableName = "beneficiaries", indices = [Index(value = ["region"])])
data class BeneficiaryEntity(
    @PrimaryKey val id: String,
    val familyHeadName: String,
    val nationalId: String,
    val phone: String,
    val membersCount: Int,
    val monthlyIncomeYer: Double,
    val povertyTier: PovertyTier,
    val region: String,
    val village: String,
    val address: String,
    val aidHistoryCount: Int
) {
    fun toDomain() = Beneficiary(id, familyHeadName, nationalId, phone, membersCount, monthlyIncomeYer, povertyTier, region, village, address, aidHistoryCount)
    companion object {
        fun fromDomain(d: Beneficiary) = BeneficiaryEntity(d.id, d.familyHeadName, d.nationalId, d.phone, d.membersCount, d.monthlyIncomeYer, d.povertyTier, d.region, d.village, d.address, d.aidHistoryCount)
    }
}

@Entity(
    tableName = "periodic_aids",
    indices = [Index(value = ["beneficiaryId"])],
    foreignKeys = [
        ForeignKey(
            entity = BeneficiaryEntity::class,
            parentColumns = ["id"],
            childColumns = ["beneficiaryId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class PeriodicAidEntity(
    @PrimaryKey val id: String,
    val beneficiaryId: String,
    val beneficiaryName: String,
    val monthlyAmountYer: Double,
    val sponsorshipType: String,
    val status: String,
    val lastDisbursedMonth: String
) {
    fun toDomain() = PeriodicAid(id, beneficiaryId, beneficiaryName, monthlyAmountYer, sponsorshipType, status, lastDisbursedMonth)
    companion object {
        fun fromDomain(d: PeriodicAid) = PeriodicAidEntity(d.id, d.beneficiaryId, d.beneficiaryName, d.monthlyAmountYer, d.sponsorshipType, d.status, d.lastDisbursedMonth)
    }
}

@Entity(tableName = "in_kind_items", indices = [Index(value = ["code"])])
data class InKindItemEntity(
    @PrimaryKey val id: String,
    val code: String,
    val name: String,
    val category: String,
    val unit: String,
    val quantityOnHand: Int,
    val reorderLevel: Int,
    val unitCostYer: Double,
    val location: String
) {
    fun toDomain() = InKindItem(id, code, name, category, unit, quantityOnHand, reorderLevel, unitCostYer, location)
    companion object {
        fun fromDomain(d: InKindItem) = InKindItemEntity(d.id, d.code, d.name, d.category, d.unit, d.quantityOnHand, d.reorderLevel, d.unitCostYer, d.location)
    }
}

@Entity(
    tableName = "in_kind_movements",
    indices = [Index(value = ["itemId"])],
    foreignKeys = [
        ForeignKey(
            entity = InKindItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class InKindMovementEntity(
    @PrimaryKey val id: String,
    val movementNo: String,
    val type: String,
    val itemId: String,
    val itemName: String,
    val quantity: Int,
    val partyName: String,
    val date: String,
    val notes: String
) {
    fun toDomain() = InKindMovement(id, movementNo, type, itemId, itemName, quantity, partyName, date, notes)
    companion object {
        fun fromDomain(d: InKindMovement) = InKindMovementEntity(d.id, d.movementNo, d.type, d.itemId, d.itemName, d.quantity, d.partyName, d.date, d.notes)
    }
}

@Entity(tableName = "scheduler_events")
data class SchedulerEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val type: String,
    val colorHex: String,
    val notes: String
) {
    fun toDomain() = SchedulerEvent(id, title, date, time, location, type, colorHex, notes)
    companion object {
        fun fromDomain(d: SchedulerEvent) = SchedulerEventEntity(d.id, d.title, d.date, d.time, d.location, d.type, d.colorHex, d.notes)
    }
}

@Entity(tableName = "internal_messages")
data class InternalMessageEntity(
    @PrimaryKey val id: String,
    val senderName: String,
    val senderRole: String,
    val recipientRole: String,
    val subject: String,
    val body: String,
    val timestamp: String,
    val isRead: Boolean
) {
    fun toDomain() = InternalMessage(id, senderName, senderRole, recipientRole, subject, body, timestamp, isRead)
    companion object {
        fun fromDomain(d: InternalMessage) = InternalMessageEntity(d.id, d.senderName, d.senderRole, d.recipientRole, d.subject, d.body, d.timestamp, d.isRead)
    }
}

@Entity(tableName = "audit_logs", indices = [Index(value = ["timestamp"])])
data class AuditLogEntity(
    @PrimaryKey val id: String,
    val timestamp: String,
    val userName: String,
    val userRole: String,
    val actionType: String,
    val description: String,
    val ipOrDevice: String,
    val sortOrder: Long = System.currentTimeMillis()
) {
    fun toDomain() = AuditLog(id, timestamp, userName, userRole, actionType, description, ipOrDevice)
    companion object {
        fun fromDomain(d: AuditLog, order: Long = System.currentTimeMillis()) = AuditLogEntity(
            d.id, d.timestamp, d.userName, d.userRole, d.actionType, d.description, d.ipOrDevice, order
        )
    }
}

@Entity(tableName = "app_users", indices = [Index(value = ["username"], unique = true)])
data class AppUserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val fullName: String,
    val role: UserRole,
    val isActive: Boolean,
    val lastLogin: String,
    val passwordHash: String
) {
    fun toDomain() = AppUser(id, username, fullName, role, isActive, lastLogin, passwordHash)
    companion object {
        fun fromDomain(d: AppUser) = AppUserEntity(d.id, d.username, d.fullName, d.role, d.isActive, d.lastLogin, d.passwordHash)
    }
}

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey val currencyCode: CurrencyCode,
    val rate: Double
)

@Entity(tableName = "fund_info")
data class FundInfoEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val subtitle: String,
    val licenseNo: String,
    val address: String,
    val phone: String,
    val email: String,
    val bankAccountKareemi: String,
    val bankAccountCAC: String,
    val maxAidCeilingYer: Double,
    val fiscalYear: String,
    val customLogoBase64: String
) {
    fun toDomain() = FundInfo(name, subtitle, licenseNo, address, phone, email, bankAccountKareemi, bankAccountCAC, maxAidCeilingYer, fiscalYear, customLogoBase64)
    companion object {
        fun fromDomain(d: FundInfo) = FundInfoEntity(1, d.name, d.subtitle, d.licenseNo, d.address, d.phone, d.email, d.bankAccountKareemi, d.bankAccountCAC, d.maxAidCeilingYer, d.fiscalYear, d.customLogoBase64)
    }
}

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val monthlySubscriptionFeeYer: Double,
    val autoSuspendMonths: Int,
    val requireNationalId: Boolean,
    val memberNoPrefix: String,
    val requireReviewerApproval: Boolean,
    val periodicAidDisburseDay: Int,
    val lowStockThreshold: Int,
    val defaultWarehouseName: String,
    val voucherNoPrefix: String,
    val defaultTreasuryAccountCode: String,
    val allowNegativeTreasury: Boolean,
    val enforceBalancedJournal: Boolean,
    val budgetAlertPercent: Int,
    val autoPrintVoucher: Boolean,
    val allowAnonymousDonors: Boolean,
    val defaultEventLocation: String,
    val strictRolePermissions: Boolean,
    val enableAuditTrail: Boolean,
    val screenFormat: DisplayFormatConfig = DisplayFormatConfig()
) {
    fun toDomain() = AppSettings(
        monthlySubscriptionFeeYer, autoSuspendMonths, requireNationalId, memberNoPrefix,
        requireReviewerApproval, periodicAidDisburseDay, lowStockThreshold, defaultWarehouseName,
        voucherNoPrefix, defaultTreasuryAccountCode, allowNegativeTreasury, enforceBalancedJournal,
        budgetAlertPercent, autoPrintVoucher, allowAnonymousDonors, defaultEventLocation,
        strictRolePermissions, enableAuditTrail, screenFormat
    )
    companion object {
        fun fromDomain(d: AppSettings) = AppSettingsEntity(
            1, d.monthlySubscriptionFeeYer, d.autoSuspendMonths, d.requireNationalId, d.memberNoPrefix,
            d.requireReviewerApproval, d.periodicAidDisburseDay, d.lowStockThreshold, d.defaultWarehouseName,
            d.voucherNoPrefix, d.defaultTreasuryAccountCode, d.allowNegativeTreasury, d.enforceBalancedJournal,
            d.budgetAlertPercent, d.autoPrintVoucher, d.allowAnonymousDonors, d.defaultEventLocation,
            d.strictRolePermissions, d.enableAuditTrail, d.screenFormat
        )
    }
}

@Entity(tableName = "print_settings")
data class PrintSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val showCountryHeader: Boolean,
    val countryHeaderText: String,
    val showDepartmentHeader: Boolean,
    val departmentHeaderText: String,
    val showLogoInPrint: Boolean,
    val showLicenseAndAddress: Boolean,
    val receiptVoucherTitle: String,
    val paymentVoucherTitle: String,
    val journalVoucherTitle: String,
    val financialStatementSubtitle: String,
    val analyticalReportSubtitle: String,
    val receiptPartyLabel: String,
    val paymentPartyLabel: String,
    val amountWordsLabel: String,
    val paymentMethodLabel: String,
    val accountLabel: String,
    val descriptionLabel: String,
    val fontFamily: PrintFontOption,
    val headerFontSizeSp: Int,
    val bodyFontSizeSp: Int,
    val signatureFontSizeSp: Int,
    val borderStyle: PrintBorderStyle,
    val paperSize: PrintPaperSize,
    val accentColorHex: String,
    val showSignaturesSection: Boolean,
    val sig1Enabled: Boolean,
    val sig1Title: String,
    val sig1DefaultName: String,
    val sig2Enabled: Boolean,
    val sig2Title: String,
    val sig2DefaultName: String,
    val sig3Enabled: Boolean,
    val sig3Title: String,
    val sig3DefaultName: String,
    val sig4Enabled: Boolean,
    val sig4Title: String,
    val sig4DefaultName: String,
    val showSignatureLine: Boolean,
    val showOfficialStampBox: Boolean,
    val officialStampLabel: String,
    val showQrVerification: Boolean,
    val qrCaptionText: String,
    val showJournalReference: Boolean,
    val showCurrencyDetails: Boolean,
    val showFundContactInFooter: Boolean,
    val footerNote: String,
    val footerSubNote: String,
    val screenFormat: DisplayFormatConfig = DisplayFormatConfig(),
    val printFormat: DisplayFormatConfig = DisplayFormatConfig()
) {
    fun toDomain() = PrintSettings(
        showCountryHeader, countryHeaderText, showDepartmentHeader, departmentHeaderText,
        showLogoInPrint, showLicenseAndAddress, receiptVoucherTitle, paymentVoucherTitle,
        journalVoucherTitle, financialStatementSubtitle, analyticalReportSubtitle,
        receiptPartyLabel, paymentPartyLabel, amountWordsLabel, paymentMethodLabel,
        accountLabel, descriptionLabel, fontFamily, headerFontSizeSp, bodyFontSizeSp,
        signatureFontSizeSp, borderStyle, paperSize, accentColorHex, showSignaturesSection,
        sig1Enabled, sig1Title, sig1DefaultName, sig2Enabled, sig2Title, sig2DefaultName,
        sig3Enabled, sig3Title, sig3DefaultName, sig4Enabled, sig4Title, sig4DefaultName,
        showSignatureLine, showOfficialStampBox, officialStampLabel, showQrVerification,
        qrCaptionText, showJournalReference, showCurrencyDetails, showFundContactInFooter,
        footerNote, footerSubNote, screenFormat, printFormat
    )
    companion object {
        fun fromDomain(d: PrintSettings) = PrintSettingsEntity(
            1, d.showCountryHeader, d.countryHeaderText, d.showDepartmentHeader, d.departmentHeaderText,
            d.showLogoInPrint, d.showLicenseAndAddress, d.receiptVoucherTitle, d.paymentVoucherTitle,
            d.journalVoucherTitle, d.financialStatementSubtitle, d.analyticalReportSubtitle,
            d.receiptPartyLabel, d.paymentPartyLabel, d.amountWordsLabel, d.paymentMethodLabel,
            d.accountLabel, d.descriptionLabel, d.fontFamily, d.headerFontSizeSp, d.bodyFontSizeSp,
            d.signatureFontSizeSp, d.borderStyle, d.paperSize, d.accentColorHex, d.showSignaturesSection,
            d.sig1Enabled, d.sig1Title, d.sig1DefaultName, d.sig2Enabled, d.sig2Title, d.sig2DefaultName,
            d.sig3Enabled, d.sig3Title, d.sig3DefaultName, d.sig4Enabled, d.sig4Title, d.sig4DefaultName,
            d.showSignatureLine, d.showOfficialStampBox, d.officialStampLabel, d.showQrVerification,
            d.qrCaptionText, d.showJournalReference, d.showCurrencyDetails, d.showFundContactInFooter,
            d.footerNote, d.footerSubNote, d.screenFormat, d.printFormat
        )
    }
}

@Entity(tableName = "sync_queue")
data class SyncQueueItemEntity(
    @PrimaryKey val id: String,
    val timestamp: String,
    val entityName: String,
    val action: String,
    val summary: String,
    val txHash: String,
    val payloadBytes: Int
) {
    fun toDomain() = SyncQueueItem(id, timestamp, entityName, action, summary, txHash, payloadBytes)
    companion object {
        fun fromDomain(d: SyncQueueItem) = SyncQueueItemEntity(d.id, d.timestamp, d.entityName, d.action, d.summary, d.txHash, d.payloadBytes)
    }
}

@Entity(tableName = "sync_batches")
data class SyncBatchRecordEntity(
    @PrimaryKey val id: String,
    val batchNo: String,
    val timestamp: String,
    val operationsCount: Int,
    val serverNode: String,
    val durationMs: Long,
    val status: String
) {
    fun toDomain() = SyncBatchRecord(id, batchNo, timestamp, operationsCount, serverNode, durationMs, status)
    companion object {
        fun fromDomain(d: SyncBatchRecord) = SyncBatchRecordEntity(d.id, d.batchNo, d.timestamp, d.operationsCount, d.serverNode, d.durationMs, d.status)
    }
}

@Entity(tableName = "local_server_config")
data class LocalServerConfigEntity(
    @PrimaryKey val id: Int = 1,
    val serverName: String,
    val serverHost: String,
    val protocol: String,
    val latencyMs: Int,
    val lastBackupTime: String
) {
    fun toDomain() = LocalServerConfig(serverName, serverHost, protocol, latencyMs, lastBackupTime)
    companion object {
        fun fromDomain(d: LocalServerConfig) = LocalServerConfigEntity(1, d.serverName, d.serverHost, d.protocol, d.latencyMs, d.lastBackupTime)
    }
}
