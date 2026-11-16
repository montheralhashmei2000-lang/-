package com.example.fund.data.repository

import androidx.room.withTransaction
import com.example.fund.data.SeedData
import com.example.fund.data.local.*
import com.example.fund.model.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class MemberRepository(
    private val dbScope: CoroutineScope,
    private val dbDispatcher: CoroutineDispatcher,
    private val accountingRepository: AccountingRepository,
    private val settingsRepository: SettingsRepository,
    private val userRepository: UserRepository
) {
    private val _regions = MutableStateFlow(SeedData.regions)
    val regions: StateFlow<List<RegionGroup>> = _regions.asStateFlow()

    private val _members = MutableStateFlow(SeedData.members)
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _subscriptions = MutableStateFlow(SeedData.subscriptions)
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions.asStateFlow()

    private val subscriptionDueReducedById = java.util.concurrent.ConcurrentHashMap<String, Double>()
    private var db: FundDatabase? = null
    private var extraRegionUsageCounter: (String) -> Int = { 0 }

    fun setExtraRegionUsageCounter(counter: (String) -> Int) {
        extraRegionUsageCounter = counter
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    private fun nowStr() = dateFormat.format(Date())

    suspend fun attachAndHydrate(database: FundDatabase) {
        db = database
        val dbRegions = database.memberDao().observeRegions().first().map { it.toDomain() }
        val dbMembers = database.memberDao().observeMembers().first().map { it.toDomain() }
        val dbSubs = database.memberDao().observeSubscriptions().first().map { it.toDomain() }

        val isDbInitialized = database.settingsDao().getFundInfo() != null ||
            database.accountingDao().getAllAccounts().isNotEmpty()
        if (isDbInitialized) {
            _regions.value = dbRegions
            _members.value = dbMembers
            _subscriptions.value = dbSubs
        }
    }

    private fun validateMemberIdentity(member: Member, excludeId: String? = null) {
        require(member.fullName.isNotBlank() && member.memberNo.isNotBlank()) { "الاسم ورقم العضوية مطلوبان." }
        val cleanMemberNo = member.memberNo.trim()
        require(_members.value.none { it.id != excludeId && it.memberNo.equals(cleanMemberNo, ignoreCase = true) }) {
            "رقم العضوية ($cleanMemberNo) مسجل لعضو آخر مسبقاً."
        }
        val cleanNationalId = member.nationalId.trim()
        if (settingsRepository.appSettings.value.requireNationalId) {
            require(cleanNationalId.isNotBlank()) {
                "رقم الهوية الوطنية مطلوب حسب إعدادات الصندوق."
            }
        }
        if (cleanNationalId.isNotBlank()) {
            require(_members.value.none { it.id != excludeId && it.nationalId.trim().equals(cleanNationalId, ignoreCase = true) }) {
                "رقم الهوية الوطنية ($cleanNationalId) مسجل لعضو آخر مسبقاً."
            }
        }
    }

    fun addMember(member: Member) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إضافة عضو جديد")
        validateMemberIdentity(member)
        AccountingValidator.requireNonNegativeFiniteAmount(member.totalPaidYer, "إجمالي المدفوعات")
        AccountingValidator.requireNonNegativeFiniteAmount(member.dueBalanceYer, "الرصيد المستحق")

        val count = _members.value.size + 1
        db?.let { database ->
            runBlocking(dbDispatcher) {
                database.memberDao().upsertMember(MemberEntity.fromDomain(member, -count))
            }
        }
        _members.value = listOf(member) + _members.value
        userRepository.logAudit("إضافة عضو", "تسجيل العضو الجديد: ${member.fullName} برقم ${member.memberNo}")
    }

    fun updateMember(member: Member) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "تعديل بيانات عضو")
        validateMemberIdentity(member, excludeId = member.id)
        AccountingValidator.requireNonNegativeFiniteAmount(member.totalPaidYer, "إجمالي المدفوعات")
        AccountingValidator.requireNonNegativeFiniteAmount(member.dueBalanceYer, "الرصيد المستحق")

        db?.let { database ->
            runBlocking(dbDispatcher) {
                database.memberDao().upsertMember(MemberEntity.fromDomain(member))
            }
        }
        _members.value = _members.value.map { if (it.id == member.id) member else it }
        userRepository.logAudit("تعديل عضو", "تحديث بيانات العضو: ${member.fullName} (${member.memberNo})")
    }

    fun toggleMemberStatus(memberId: String) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "تغيير حالة عضو")
        val m = _members.value.find { it.id == memberId } ?: return
        val newStatus = if (m.status == "نشط") "معلق" else "نشط"
        val updated = m.copy(status = newStatus)
        db?.let { database ->
            runBlocking(dbDispatcher) {
                database.memberDao().upsertMember(MemberEntity.fromDomain(updated))
            }
        }
        _members.value = _members.value.map { if (it.id == memberId) updated else it }
        userRepository.logAudit("تغيير حالة عضو", "تغيير حالة عضوية ${m.fullName} إلى $newStatus")
    }

    private fun nextReceiptNo(offset: Int = 0): String {
        val maxSuffix = _subscriptions.value.mapNotNull { it.receiptNo.substringAfterLast("-").toIntOrNull() }.maxOrNull() ?: 0
        val nextNum = maxOf(maxSuffix, _subscriptions.value.size) + 1 + offset
        return String.format(Locale.US, "SUB-%d-%03d", Calendar.getInstance().get(Calendar.YEAR), nextNum)
    }

    /**
     * Atomically records a subscription payment, updates member totals, and posts the corresponding
     * receipt voucher + double-entry journal entry inside ONE Room `@Transaction` before mutating
     * in-memory StateFlows.
     */
    fun addSubscription(
        memberId: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        monthYear: String,
        paymentMethod: PaymentMethod,
        referenceNo: String,
        notes: String
    ): Subscription {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "تحصيل اشتراك")
        AccountingValidator.requirePositiveFiniteAmount(amountOriginal, "مبلغ الاشتراك")

        val member = requireNotNull(_members.value.find { it.id == memberId }) {
            "لم يتم العثور على العضو المطلوب ($memberId)"
        }
        val rate = accountingRepository.currencyRates.value[currency] ?: currency.defaultRate
        AccountingValidator.requirePositiveFiniteAmount(rate, "سعر الصرف")
        val amountYer = AccountingValidator.roundMoney(amountOriginal * rate)
        AccountingValidator.requirePositiveFiniteAmount(amountYer, "مبلغ الاشتراك بالريال")

        var receiptNo = nextReceiptNo()
        val today = nowStr()

        var sub = Subscription(
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

        var dueReducedAmount = minOf(member.dueBalanceYer, amountYer)
        val updatedMember = member.copy(
            totalPaidYer = member.totalPaidYer + amountYer,
            dueBalanceYer = max(0.0, member.dueBalanceYer - amountYer)
        )

        val treasuryAccountCode = when (paymentMethod) {
            PaymentMethod.CASH -> "1100"
            PaymentMethod.BANK_TRANSFER -> "1200"
            PaymentMethod.ELECTRONIC_WALLET -> "1300"
        }

        // Validate and prepare accounting posting BEFORE committing anything
        var preparedPosting = accountingRepository.prepareVoucherPosting(
            type = VoucherType.RECEIPT,
            partyName = member.fullName,
            amountOriginal = amountOriginal,
            currency = currency,
            paymentMethod = paymentMethod,
            accountCode = treasuryAccountCode,
            counterpartAccountCode = "4100",
            description = "تحصيل اشتراك $monthYear للعضو ${member.fullName} ($receiptNo)"
        )

        // Wait for DB transaction and keep committedMember so StateFlow mirrors SQLite exactly
        var committedMemberSnapshot = updatedMember
        db?.let { database ->
            val subCount = _subscriptions.value.size + 1
            val committedPair = runBlocking(dbDispatcher) {
                database.withTransaction {
                    val persistedMember = database.memberDao().getMemberById(memberId)
                        ?: throw IllegalArgumentException("لم يتم العثور على العضو المطلوب في قاعدة البيانات ($memberId)")
                    val existingSubCount = database.memberDao().countSubscriptionsByReceiptNo(sub.receiptNo)
                    if (existingSubCount > 0) {
                        var offset = 1
                        while (database.memberDao().countSubscriptionsByReceiptNo(nextReceiptNo(offset)) > 0) {
                            offset++
                        }
                        receiptNo = nextReceiptNo(offset)
                        sub = sub.copy(receiptNo = receiptNo)
                    }
                    val dbMember = persistedMember.toDomain()
                    dueReducedAmount = minOf(dbMember.dueBalanceYer, amountYer)
                    val committedMember = dbMember.copy(
                        totalPaidYer = dbMember.totalPaidYer + amountYer,
                        dueBalanceYer = max(0.0, dbMember.dueBalanceYer - amountYer)
                    )
                    database.memberDao().upsertMember(MemberEntity.fromDomain(committedMember))
                    database.memberDao().upsertSubscription(SubscriptionEntity.fromDomain(sub, -subCount))
                    val committedVoucher = accountingRepository.persistVoucherInTransaction(database, preparedPosting)
                    Triple(sub, committedMember, committedVoucher)
                }
            }
            sub = committedPair.first
            committedMemberSnapshot = committedPair.second
            preparedPosting = committedPair.third
        }

        // Only update in-memory StateFlows AFTER SQLite @Transaction has succeeded
        subscriptionDueReducedById[sub.id] = dueReducedAmount
        _subscriptions.value = listOf(sub) + _subscriptions.value
        _members.value = _members.value.map { if (it.id == memberId) committedMemberSnapshot else it }
        accountingRepository.commitVoucherInMemory(preparedPosting)
        userRepository.logAudit("تحصيل اشتراك", "تحصيل اشتراك (${sub.receiptNo}) للعضو ${member.fullName} بمبلغ $amountYer ﷼")

        return sub
    }

    /**
     * Automatically suspends active members whose overdue balance meets or exceeds
     * [thresholdMonths] * [monthlyFeeYer]. Returns the count of newly suspended members.
     */
    fun autoSuspendOverdueMembers(thresholdMonths: Int, monthlyFeeYer: Double): Int {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "التعليق التلقائي للمتأخرين")
        if (thresholdMonths <= 0 || monthlyFeeYer <= 0.0 || !monthlyFeeYer.isFinite()) return 0
        val cutoffYer = thresholdMonths * monthlyFeeYer
        val toSuspend = _members.value.filter { it.status == "نشط" && it.dueBalanceYer >= cutoffYer }
        if (toSuspend.isEmpty()) return 0

        val updatedMap = toSuspend.associate { it.id to it.copy(status = "معلق") }
        db?.let { database ->
            runBlocking(dbDispatcher) {
                database.withTransaction {
                    database.memberDao().upsertMembers(updatedMap.values.map { MemberEntity.fromDomain(it) })
                }
            }
        }
        _members.value = _members.value.map { updatedMap[it.id] ?: it }
        userRepository.logAudit(
            "تعليق تلقائي للمتأخرين",
            "تم تعليق عضوية ${updatedMap.size} عضو لتجاوزهم حد التأخر ($thresholdMonths أشهر / ${cutoffYer.toLong()} ﷼)"
        )
        return updatedMap.size
    }

    internal suspend fun rollbackSubscriptionInTransaction(database: FundDatabase, voucher: Voucher): Pair<Subscription, Member>? {
        val matchedSub = findSubscriptionForVoucher(voucher) ?: return null
        val dbMember = database.memberDao().getMemberById(matchedSub.memberId)?.toDomain()
            ?: _members.value.find { it.id == matchedSub.memberId }
            ?: return null
        val dueRestore = subscriptionDueReducedById.remove(matchedSub.id) ?: 0.0
        val restoredMember = dbMember.copy(
            totalPaidYer = max(0.0, dbMember.totalPaidYer - matchedSub.amountYer),
            dueBalanceYer = dbMember.dueBalanceYer + dueRestore
        )
        database.memberDao().deleteSubscriptionById(matchedSub.id)
        database.memberDao().upsertMember(MemberEntity.fromDomain(restoredMember))
        return matchedSub to restoredMember
    }

    internal fun findSubscriptionForVoucher(voucher: Voucher): Subscription? {
        if (voucher.type != VoucherType.RECEIPT) return null
        val subReceiptRegex = Regex("""SUB-\d{4}-\d+""")
        val matchedReceiptNo = subReceiptRegex.find(voucher.description)?.value
        if (matchedReceiptNo != null) {
            _subscriptions.value.find { it.receiptNo == matchedReceiptNo }?.let { return it }
        }
        if (voucher.counterpartAccountCode == "4100" && voucher.description.contains("اشتراك")) {
            return _subscriptions.value.firstOrNull { sub ->
                sub.memberName == voucher.partyName &&
                    kotlin.math.abs(sub.amountYer - voucher.amountYer) < 0.01 &&
                    sub.date == voucher.date
            }
        }
        return null
    }

    internal fun commitSubscriptionVoidInMemory(sub: Subscription, restoredMember: Member?) {
        val dueRestore = subscriptionDueReducedById.remove(sub.id) ?: 0.0
        _subscriptions.value = _subscriptions.value.filterNot { it.id == sub.id }
        val resolvedMember = restoredMember ?: _members.value.find { it.id == sub.memberId }?.let { m ->
            m.copy(
                totalPaidYer = max(0.0, m.totalPaidYer - sub.amountYer),
                dueBalanceYer = m.dueBalanceYer + dueRestore
            )
        }
        if (resolvedMember != null) {
            _members.value = _members.value.map { if (it.id == resolvedMember.id) resolvedMember else it }
        }
    }

    // Regions & Villages Management
    fun addRegion(regionName: String): Boolean {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إضافة منطقة")
        val trimmed = regionName.trim()
        if (trimmed.isBlank() || _regions.value.any { it.name == trimmed }) return false
        val newRegion = RegionGroup(name = trimmed, villages = emptyList())
        _regions.value = _regions.value + newRegion
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.memberDao().upsertRegion(RegionGroupEntity.fromDomain(newRegion))
            }
        }
        userRepository.logAudit("إضافة منطقة", "إضافة منطقة إدارية جديدة: $trimmed")
        return true
    }

    fun deleteRegion(regionId: String) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "حذف منطقة")
        check(_regions.value.size > 1) { "لا يمكن حذف آخر منطقة إدارية في الصندوق." }
        val target = _regions.value.find { it.id == regionId } ?: return
        val linkedMembers = _members.value.count { it.region == target.name }
        val linkedBeneficiaries = extraRegionUsageCounter(target.name)
        check(linkedMembers == 0 && linkedBeneficiaries == 0) {
            "لا يمكن حذف منطقة (${target.name}) لوجود $linkedMembers عضو و $linkedBeneficiaries أسرة مستفيدة مرتبطة بها."
        }
        _regions.value = _regions.value.filterNot { it.id == regionId }
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.memberDao().deleteRegionById(regionId)
            }
        }
        userRepository.logAudit("حذف منطقة", "حذف المنطقة: ${target.name}")
    }

    fun addVillageToRegion(regionId: String, villageName: String): Boolean {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إضافة قرية")
        val trimmed = villageName.trim()
        if (trimmed.isBlank()) return false
        var added = false
        var updatedRegion: RegionGroup? = null
        _regions.value = _regions.value.map { r ->
            if (r.id == regionId && !r.villages.contains(trimmed)) {
                added = true
                r.copy(villages = r.villages + trimmed).also { updatedRegion = it }
            } else r
        }
        if (added) {
            updatedRegion?.let { ur ->
                db?.let { database ->
                    dbScope.launch(dbDispatcher) {
                        database.memberDao().upsertRegion(RegionGroupEntity.fromDomain(ur))
                    }
                }
            }
            val rName = _regions.value.find { it.id == regionId }?.name ?: ""
            userRepository.logAudit("إضافة قرية", "إضافة قرية ($trimmed) ضمن نطاق ($rName)")
        }
        return added
    }

    fun addVillageByRegionName(regionName: String, villageName: String) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إضافة قرية لمنطقة")
        val rName = regionName.trim()
        val vName = villageName.trim()
        if (rName.isBlank()) return
        val existing = _regions.value.find { it.name == rName }
        if (existing == null) {
            val list = if (vName.isNotBlank()) listOf(vName) else emptyList()
            val newRegion = RegionGroup(name = rName, villages = list)
            _regions.value = _regions.value + newRegion
            db?.let { database ->
                dbScope.launch(dbDispatcher) {
                    database.memberDao().upsertRegion(RegionGroupEntity.fromDomain(newRegion))
                }
            }
            userRepository.logAudit("إضافة منطقة وقرية", "إنشاء منطقة ($rName)${if (vName.isNotBlank()) " مع القرية ($vName)" else ""}")
        } else if (vName.isNotBlank() && !existing.villages.contains(vName)) {
            addVillageToRegion(existing.id, vName)
        }
    }

    fun removeVillageFromRegion(regionId: String, villageName: String) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "حذف قرية")
        var updatedRegion: RegionGroup? = null
        _regions.value = _regions.value.map { r ->
            if (r.id == regionId) r.copy(villages = r.villages.filterNot { it == villageName }).also { updatedRegion = it } else r
        }
        updatedRegion?.let { ur ->
            db?.let { database ->
                dbScope.launch(dbDispatcher) {
                    database.memberDao().upsertRegion(RegionGroupEntity.fromDomain(ur))
                }
            }
        }
        userRepository.logAudit("حذف قرية", "إزالة القرية: $villageName")
    }

    fun applySnapshot(
        regions: List<RegionGroup>?,
        members: List<Member>?,
        subscriptions: List<Subscription>?
    ) {
        regions?.let { _regions.value = it }
        members?.let { _members.value = it }
        subscriptions?.let { _subscriptions.value = it }
    }

    fun resetToSeed() {
        _regions.value = SeedData.regions
        _members.value = SeedData.members
        _subscriptions.value = SeedData.subscriptions
    }
}
