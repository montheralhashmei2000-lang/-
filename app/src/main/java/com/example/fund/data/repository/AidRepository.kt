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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.max

class AidRepository(
    private val dbScope: CoroutineScope,
    private val dbDispatcher: CoroutineDispatcher,
    private val accountingRepository: AccountingRepository,
    private val memberRepository: MemberRepository,
    private val userRepository: UserRepository
) {
    private val _aidRequests = MutableStateFlow(SeedData.aidRequests)
    val aidRequests: StateFlow<List<AidRequest>> = _aidRequests.asStateFlow()

    private val _beneficiaries = MutableStateFlow(SeedData.beneficiaries)
    val beneficiaries: StateFlow<List<Beneficiary>> = _beneficiaries.asStateFlow()

    private val _periodicAids = MutableStateFlow(SeedData.periodicAids)
    val periodicAids: StateFlow<List<PeriodicAid>> = _periodicAids.asStateFlow()

    private val _donors = MutableStateFlow(SeedData.donors)
    val donors: StateFlow<List<Donor>> = _donors.asStateFlow()

    private val _campaigns = MutableStateFlow(SeedData.campaigns)
    val campaigns: StateFlow<List<Campaign>> = _campaigns.asStateFlow()

    private val _inKindItems = MutableStateFlow(SeedData.inKindItems)
    val inKindItems: StateFlow<List<InKindItem>> = _inKindItems.asStateFlow()

    private val _inKindMovements = MutableStateFlow(SeedData.inKindMovements)
    val inKindMovements: StateFlow<List<InKindMovement>> = _inKindMovements.asStateFlow()

    private var db: FundDatabase? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    private fun nowStr() = dateFormat.format(Date())

    suspend fun attachAndHydrate(database: FundDatabase) {
        db = database
        val dbReqs = database.aidDao().observeAidRequests().first().map { it.toDomain() }
        if (dbReqs.isNotEmpty()) _aidRequests.value = dbReqs

        val dbBens = database.aidDao().observeBeneficiaries().first().map { it.toDomain() }
        if (dbBens.isNotEmpty()) _beneficiaries.value = dbBens

        val dbPeriodic = database.aidDao().observePeriodicAids().first().map { it.toDomain() }
        if (dbPeriodic.isNotEmpty()) _periodicAids.value = dbPeriodic

        val dbDonors = database.aidDao().observeDonors().first().map { it.toDomain() }
        if (dbDonors.isNotEmpty()) _donors.value = dbDonors

        val dbCampaigns = database.aidDao().observeCampaigns().first().map { it.toDomain() }
        if (dbCampaigns.isNotEmpty()) _campaigns.value = dbCampaigns

        val dbItems = database.aidDao().observeInKindItems().first().map { it.toDomain() }
        if (dbItems.isNotEmpty()) _inKindItems.value = dbItems

        val dbMovements = database.aidDao().observeInKindMovements().first().map { it.toDomain() }
        if (dbMovements.isNotEmpty()) _inKindMovements.value = dbMovements
    }

    fun addAidRequest(
        memberId: String,
        aidType: AidType,
        amountYer: Double,
        notes: String
    ): AidRequest {
        val member = memberRepository.members.value.find { it.id == memberId }
        val memberName = member?.fullName ?: "مستفيد خارجي"
        val maxSuffix = _aidRequests.value.mapNotNull { it.requestNo.substringAfterLast("-").toIntOrNull() }.maxOrNull() ?: 0
        val count = maxOf(maxSuffix, _aidRequests.value.size) + 1
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
        _aidRequests.value = listOf(req) + _aidRequests.value
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertAidRequest(AidRequestEntity.fromDomain(req, -count))
            }
        }
        userRepository.logAudit("طلب مساعدة", "تقديم طلب مساعدة $reqNo للأخ $memberName بمبلغ $amountYer ﷼")
        return req
    }

    fun updateAidStatus(requestId: String, newStatus: AidStatus, approvedAmountYer: Double = 0.0) {
        val req = _aidRequests.value.find { it.id == requestId } ?: return
        var voucherId: String? = req.voucherId

        if (newStatus == AidStatus.DISBURSED && req.status != AidStatus.DISBURSED) {
            val v = accountingRepository.issueVoucher(
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

        val updated = req.copy(
            status = newStatus,
            approvedAmountYer = if (approvedAmountYer > 0) approvedAmountYer else req.approvedAmountYer,
            decisionDate = nowStr(),
            voucherId = voucherId
        )
        _aidRequests.value = _aidRequests.value.map { if (it.id == requestId) updated else it }

        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertAidRequest(AidRequestEntity.fromDomain(updated))
            }
        }

        userRepository.logAudit("تحديث مساعدة", "تغيير حالة طلب المساعدة ${req.requestNo} إلى ${newStatus.title}")
    }

    fun addDonor(donor: Donor) {
        _donors.value = listOf(donor) + _donors.value
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertDonor(DonorEntity.fromDomain(donor))
            }
        }
        userRepository.logAudit("إضافة مانح", "إضافة الداعم: ${donor.name}")
    }

    fun recordDonorPledgePayment(
        donorId: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        paymentMethod: PaymentMethod
    ) {
        val donor = _donors.value.find { it.id == donorId } ?: return
        val amountYer = accountingRepository.convertToYer(amountOriginal, currency)
        val today = nowStr()

        val updated = donor.copy(
            totalDonatedYer = donor.totalDonatedYer + amountYer,
            pledgeFulfilledYer = donor.pledgeFulfilledYer + amountYer,
            lastDonationDate = today
        )
        _donors.value = _donors.value.map { if (it.id == donorId) updated else it }

        val accCode = when (paymentMethod) {
            PaymentMethod.CASH -> "1100"
            PaymentMethod.BANK_TRANSFER -> "1200"
            PaymentMethod.ELECTRONIC_WALLET -> "1300"
        }

        accountingRepository.issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = donor.name,
            amountOriginal = amountOriginal,
            currency = currency,
            paymentMethod = paymentMethod,
            accountCode = accCode,
            counterpartAccountCode = "4200",
            description = "تحصيل دفعة من التعهد المالي للداعم ${donor.name}"
        )

        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertDonor(DonorEntity.fromDomain(updated))
            }
        }
    }

    fun addCampaign(campaign: Campaign) {
        _campaigns.value = listOf(campaign) + _campaigns.value
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertCampaign(CampaignEntity.fromDomain(campaign))
            }
        }
        userRepository.logAudit("إضافة حملة", "إطلاق حملة خيرية: ${campaign.title}")
    }

    fun donateToCampaign(
        campaignId: String,
        donorName: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        paymentMethod: PaymentMethod
    ) {
        val c = _campaigns.value.find { it.id == campaignId } ?: return
        val amountYer = accountingRepository.convertToYer(amountOriginal, currency)
        val newCol = c.collectedAmountYer + amountYer
        val newStatus = if (newCol >= c.goalAmountYer) "مكتملة" else c.status
        val updated = c.copy(collectedAmountYer = newCol, status = newStatus)

        _campaigns.value = _campaigns.value.map { if (it.id == campaignId) updated else it }

        val accCode = when (paymentMethod) {
            PaymentMethod.CASH -> "1100"
            PaymentMethod.BANK_TRANSFER -> "1200"
            PaymentMethod.ELECTRONIC_WALLET -> "1300"
        }

        accountingRepository.issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = donorName.ifBlank { "فاعل خير - ${c.title}" },
            amountOriginal = amountOriginal,
            currency = currency,
            paymentMethod = paymentMethod,
            accountCode = accCode,
            counterpartAccountCode = "4300",
            description = "تبرع مخصص لحملة: ${c.title}"
        )

        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertCampaign(CampaignEntity.fromDomain(updated))
            }
        }
    }

    fun disburseFromCampaign(
        campaignId: String,
        recipientName: String,
        amountYer: Double,
        description: String
    ) {
        val c = _campaigns.value.find { it.id == campaignId } ?: return
        val updated = c.copy(spentAmountYer = c.spentAmountYer + amountYer)
        _campaigns.value = _campaigns.value.map { if (it.id == campaignId) updated else it }

        accountingRepository.issueVoucher(
            type = VoucherType.PAYMENT,
            partyName = recipientName.ifBlank { "مستفيدو حملة ${c.title}" },
            amountOriginal = amountYer,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5200",
            counterpartAccountCode = "1100",
            description = "صرف ميداني من حملة (${c.title}): $description"
        )

        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertCampaign(CampaignEntity.fromDomain(updated))
            }
        }
    }

    fun addBeneficiary(b: Beneficiary) {
        _beneficiaries.value = listOf(b) + _beneficiaries.value
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertBeneficiary(BeneficiaryEntity.fromDomain(b))
            }
        }
        userRepository.logAudit("إضافة مستفيد", "تسجيل أسرة مستفيدة: ${b.familyHeadName}")
    }

    fun addPeriodicAid(
        beneficiaryId: String,
        beneficiaryName: String,
        monthlyAmountYer: Double,
        sponsorshipType: String
    ) {
        val pa = PeriodicAid(
            beneficiaryId = beneficiaryId,
            beneficiaryName = beneficiaryName,
            monthlyAmountYer = monthlyAmountYer,
            sponsorshipType = sponsorshipType,
            status = "نشطة",
            lastDisbursedMonth = "جديد"
        )
        _periodicAids.value = listOf(pa) + _periodicAids.value
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertPeriodicAid(PeriodicAidEntity.fromDomain(pa))
            }
        }
        userRepository.logAudit("إضافة كفالة دورية", "جدولة كفالة شهرية للمستفيد $beneficiaryName بمبلغ $monthlyAmountYer ﷼")
    }

    fun disburseSinglePeriodicAid(id: String, month: String) {
        val pa = _periodicAids.value.find { it.id == id } ?: return
        val updated = pa.copy(lastDisbursedMonth = month)
        _periodicAids.value = _periodicAids.value.map { if (it.id == id) updated else it }

        accountingRepository.issueVoucher(
            type = VoucherType.PAYMENT,
            partyName = pa.beneficiaryName,
            amountOriginal = pa.monthlyAmountYer,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5200",
            counterpartAccountCode = "1100",
            description = "صرف كفالة شهرية فردية (${pa.sponsorshipType}) لشهر $month"
        )

        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertPeriodicAid(PeriodicAidEntity.fromDomain(updated))
            }
        }
    }

    fun disbursePeriodicAidsBatch(month: String, ids: List<String>) {
        var totalAmount = 0.0
        val updatedList = mutableListOf<PeriodicAid>()
        _periodicAids.value = _periodicAids.value.map { pa ->
            if (ids.contains(pa.id)) {
                totalAmount += pa.monthlyAmountYer
                pa.copy(lastDisbursedMonth = month).also { updatedList.add(it) }
            } else pa
        }

        if (totalAmount > 0.0) {
            accountingRepository.issueVoucher(
                type = VoucherType.PAYMENT,
                partyName = "كشف صرف الكفالات والمساعدات الدورية لعدد ${ids.size} أسرة",
                amountOriginal = totalAmount,
                currency = CurrencyCode.YER,
                paymentMethod = PaymentMethod.CASH,
                accountCode = "5200",
                counterpartAccountCode = "1100",
                description = "صرف المساعدات الدورية لشهر $month إجمالي $totalAmount ﷼"
            )
            db?.let { database ->
                dbScope.launch(dbDispatcher) {
                    database.aidDao().upsertPeriodicAids(updatedList.map { PeriodicAidEntity.fromDomain(it) })
                }
            }
        }
        userRepository.logAudit("صرف كفالات", "تم صرف دفعة كفالات لشهر $month بمبلغ إجمالي $totalAmount ﷼")
    }

    fun addInKindItem(item: InKindItem) {
        _inKindItems.value = listOf(item) + _inKindItems.value
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.aidDao().upsertInKindItem(InKindItemEntity.fromDomain(item))
            }
        }
        userRepository.logAudit("تعريف صنف عيني", "إضافة صنف مخزني جديد: ${item.name} (${item.code})")
    }

    fun addInKindMovement(mov: InKindMovement) {
        _inKindMovements.value = listOf(mov) + _inKindMovements.value
        var updatedItem: InKindItem? = null
        _inKindItems.value = _inKindItems.value.map { item ->
            if (item.id == mov.itemId) {
                val delta = if (mov.type == "توريد") mov.quantity else -mov.quantity
                item.copy(quantityOnHand = max(0, item.quantityOnHand + delta)).also { updatedItem = it }
            } else item
        }
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.withTransaction {
                    database.aidDao().upsertInKindMovement(InKindMovementEntity.fromDomain(mov))
                    updatedItem?.let {
                        database.aidDao().upsertInKindItem(InKindItemEntity.fromDomain(it))
                    }
                }
            }
        }
        userRepository.logAudit("حركة مخزنية عينية", "${mov.type} كمية ${mov.quantity} للصنف ${mov.itemName}")
    }

    fun applySnapshot(
        beneficiaries: List<Beneficiary>?,
        aidRequests: List<AidRequest>?,
        periodicAids: List<PeriodicAid>?,
        donors: List<Donor>?,
        campaigns: List<Campaign>?,
        inKindItems: List<InKindItem>?,
        inKindMovements: List<InKindMovement>?
    ) {
        beneficiaries?.let { _beneficiaries.value = it }
        aidRequests?.let { _aidRequests.value = it }
        periodicAids?.let { _periodicAids.value = it }
        donors?.let { _donors.value = it }
        campaigns?.let { _campaigns.value = it }
        inKindItems?.let { _inKindItems.value = it }
        inKindMovements?.let { _inKindMovements.value = it }
    }

    fun resetToSeed() {
        _aidRequests.value = SeedData.aidRequests
        _beneficiaries.value = SeedData.beneficiaries
        _periodicAids.value = SeedData.periodicAids
        _donors.value = SeedData.donors
        _campaigns.value = SeedData.campaigns
        _inKindItems.value = SeedData.inKindItems
        _inKindMovements.value = SeedData.inKindMovements
    }
}
