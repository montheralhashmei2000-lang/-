package com.example.fund.model

import java.util.UUID

enum class UserRole(val title: String) {
    ADMIN("مدير النظام"),
    ACCOUNTANT("محاسب"),
    REVIEWER("مراجع"),
    VIEWER("مراقب")
}

enum class AidType(val title: String) {
    MEDICAL("مرضية"),
    MARRIAGE("زواج"),
    EMERGENCY("طارئة"),
    EDUCATION("تعليمية"),
    DEATH("وفاة")
}

enum class AidStatus(val title: String) {
    IN_REVIEW("قيد المراجعة"),
    APPROVED("معتمدة"),
    DISBURSED("مصروفة"),
    REJECTED("مرفوضة")
}

enum class VoucherType(val title: String) {
    RECEIPT("سند قبض"),
    PAYMENT("سند صرف")
}

enum class VoucherStatus(val title: String) {
    POSTED("معتمد"),
    VOID("ملغي")
}

enum class PaymentMethod(val title: String) {
    CASH("نقداً"),
    BANK_TRANSFER("تحويل بنكي"),
    ELECTRONIC_WALLET("محفظة إلكترونية")
}

enum class CurrencyCode(val symbol: String, val title: String, val defaultRate: Double) {
    YER("﷼", "الريال اليمني", 1.0),
    USD("$", "الدولار الأمريكي", 2530.0),
    SAR("ر.س", "الريال السعودي", 665.0)
}

enum class AccountType(val title: String) {
    ASSET("الأصول"),
    LIABILITY("الالتزامات"),
    EQUITY("حقوق الملكية"),
    REVENUE("الإيرادات"),
    EXPENSE("المصروفات")
}

enum class PovertyTier(val title: String) {
    EXTREME("شديد الاحتياج"),
    HIGH("احتياج مرتفع"),
    MEDIUM("احتياج متوسط"),
    LOW("احتياج خفيف")
}

data class Member(
    val id: String = UUID.randomUUID().toString(),
    val memberNo: String,
    val fullName: String,
    val phone: String,
    val nationalId: String,
    val joinDate: String,
    val status: String = "نشط", // نشط / معلق
    val totalPaidYer: Double = 0.0,
    val dueBalanceYer: Double = 0.0,
    val job: String = "",
    val villageOrArea: String = "الحبيلين",
    val notes: String = ""
)

data class Subscription(
    val id: String = UUID.randomUUID().toString(),
    val receiptNo: String,
    val memberId: String,
    val memberName: String,
    val amountOriginal: Double,
    val currency: CurrencyCode = CurrencyCode.YER,
    val exchangeRate: Double = 1.0,
    val amountYer: Double,
    val monthYear: String,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val referenceNo: String = "",
    val date: String,
    val notes: String = ""
)

data class AidRequest(
    val id: String = UUID.randomUUID().toString(),
    val requestNo: String,
    val memberId: String,
    val memberName: String,
    val aidType: AidType,
    val requestedAmountYer: Double,
    val approvedAmountYer: Double,
    val status: AidStatus = AidStatus.IN_REVIEW,
    val requestDate: String,
    val decisionDate: String = "",
    val notes: String = "",
    val voucherId: String? = null
)

data class Voucher(
    val id: String = UUID.randomUUID().toString(),
    val voucherNo: String,
    val type: VoucherType,
    val status: VoucherStatus = VoucherStatus.POSTED,
    val partyName: String,
    val amountOriginal: Double,
    val currency: CurrencyCode = CurrencyCode.YER,
    val exchangeRate: Double = 1.0,
    val amountYer: Double,
    val amountWords: String,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val accountCode: String,
    val accountName: String,
    val counterpartAccountCode: String,
    val counterpartAccountName: String,
    val description: String,
    val date: String,
    val linkedJournalEntryId: String = "",
    val receivedOrPaidBy: String = "",
    val accountantName: String = "أمين الصندوق / المحاسب",
    val auditorName: String = "المراجع المالي",
    val managerName: String = "رئيس مجلس الإدارة"
)

data class JournalLine(
    val accountCode: String,
    val accountName: String,
    val debit: Double,
    val credit: Double,
    val description: String = ""
)

data class JournalEntry(
    val id: String = UUID.randomUUID().toString(),
    val entryNo: String,
    val date: String,
    val description: String,
    val lines: List<JournalLine>,
    val isPosted: Boolean = true,
    val isReversed: Boolean = false,
    val originalVoucherNo: String? = null
)

data class Account(
    val code: String,
    val name: String,
    val type: AccountType,
    val isDebitNormal: Boolean,
    val balance: Double = 0.0,
    val parentCode: String? = null
)

data class Budget(
    val id: String = UUID.randomUUID().toString(),
    val category: String,
    val accountCode: String,
    val budgetedAmount: Double,
    val actualSpent: Double,
    val month: String
)

data class Donor(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val type: String = "مغترب / فردي",
    val totalDonatedYer: Double = 0.0,
    val pledgeAmountYer: Double = 0.0,
    val pledgeFulfilledYer: Double = 0.0,
    val lastDonationDate: String = ""
)

data class Campaign(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val goalAmountYer: Double,
    val collectedAmountYer: Double = 0.0,
    val spentAmountYer: Double = 0.0,
    val status: String = "نشطة",
    val description: String = "",
    val targetCategory: String = "إغاثة عاجلة"
)

data class Beneficiary(
    val id: String = UUID.randomUUID().toString(),
    val familyHeadName: String,
    val nationalId: String,
    val phone: String,
    val membersCount: Int,
    val monthlyIncomeYer: Double,
    val povertyTier: PovertyTier,
    val villageOrArea: String = "الحبيلين",
    val address: String = "",
    val aidHistoryCount: Int = 0
)

data class PeriodicAid(
    val id: String = UUID.randomUUID().toString(),
    val beneficiaryId: String,
    val beneficiaryName: String,
    val monthlyAmountYer: Double,
    val sponsorshipType: String = "كفالة أيتام",
    val status: String = "نشطة",
    val lastDisbursedMonth: String = "2026-10"
)

data class InKindItem(
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val name: String,
    val category: String,
    val unit: String,
    val quantityOnHand: Int,
    val reorderLevel: Int = 10,
    val unitCostYer: Double = 0.0,
    val location: String = "مستودع الحبيلين الرئيسي"
)

data class InKindMovement(
    val id: String = UUID.randomUUID().toString(),
    val movementNo: String,
    val type: String, // توريد / صرف وتوزيع
    val itemId: String,
    val itemName: String,
    val quantity: Int,
    val partyName: String,
    val date: String,
    val notes: String = ""
)

data class SchedulerEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val type: String,
    val colorHex: String = "#2E7D32",
    val notes: String = ""
)

data class InternalMessage(
    val id: String = UUID.randomUUID().toString(),
    val senderName: String,
    val senderRole: String,
    val recipientRole: String,
    val subject: String,
    val body: String,
    val timestamp: String,
    val isRead: Boolean = false
)

data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val userName: String,
    val userRole: String,
    val actionType: String,
    val description: String,
    val ipOrDevice: String = "نظام محلي آمن - الحبيلين"
)

data class AppUser(
    val id: String = UUID.randomUUID().toString(),
    val username: String,
    val fullName: String,
    val role: UserRole,
    val isActive: Boolean = true,
    val lastLogin: String = "2026-11-08"
)

data class FundInfo(
    val name: String = "صندوق الحبيلين الاجتماعي التنموي",
    val subtitle: String = "الإصدار 9.0 - نظام مالي ورقابي متكامل",
    val licenseNo: String = "ترخيص وزارة الشؤون الاجتماعية رقم: 442/2018",
    val address: String = "الجمهورية اليمنية - محافظة لحج - مديرية ردفان - الحبيلين",
    val phone: String = "+967 770 123 456",
    val email: String = "info@alhubaylin-fund.org",
    val bankAccountKareemi: String = "بنك الكريمي - حساب رقم: 300455821",
    val bankAccountCAC: String = "بنك التسليف التعاوني CAC - حساب رقم: 102948201",
    val maxAidCeilingYer: Double = 500000.0,
    val fiscalYear: String = "2026"
)

data class SyncQueueItem(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: String,
    val entityName: String,
    val action: String,
    val summary: String,
    val txHash: String = UUID.randomUUID().toString().take(8).uppercase(),
    val payloadBytes: Int = 512
)

data class SyncBatchRecord(
    val id: String = UUID.randomUUID().toString(),
    val batchNo: String,
    val timestamp: String,
    val operationsCount: Int,
    val serverNode: String,
    val durationMs: Long = 420L,
    val status: String = "مكتملة بنجاح ✔"
)

data class LocalServerConfig(
    val serverName: String = "الخادم المحلي الميداني - الحبيلين (LAN)",
    val serverHost: String = "192.168.1.100:8443",
    val protocol: String = "TLS 1.3 / SQLite-JSON Engine",
    val latencyMs: Int = 8,
    val lastBackupTime: String = "2026-11-08 10:30"
)
