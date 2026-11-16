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

class AidRepository(
    private val dbScope: CoroutineScope,
    private val dbDispatcher: CoroutineDispatcher,
    private val accountingRepository: AccountingRepository,
    private val memberRepository: MemberRepository,
    private val settingsRepository: SettingsRepository,
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
        val dbBens = database.aidDao().observeBeneficiaries().first().map { it.toDomain() }
        val dbPeriodic = database.aidDao().observePeriodicAids().first().map { it.toDomain() }
        val dbDonors = database.aidDao().observeDonors().first().map { it.toDomain() }
        val dbCampaigns = database.aidDao().observeCampaigns().first().map { it.toDomain() }
        val dbItems = database.aidDao().observeInKindItems().first().map { it.toDomain() }
        val dbMovements = database.aidDao().observeInKindMovements().first().map { it.toDomain() }

        val isDbInitialized = database.settingsDao().getFundInfo() != null ||
            database.accountingDao().getAllAccounts().isNotEmpty()
        if (isDbInitialized) {
            _aidRequests.value = dbReqs
            _beneficiaries.value = dbBens
            _periodicAids.value = dbPeriodic
            _donors.value = dbDonors
            _campaigns.value = dbCampaigns
            _inKindItems.value = dbItems
            _inKindMovements.value = dbMovements
        }
    }

    fun addAidRequest(
        memberId: String,
        aidType: AidType,
        amountYer: Double,
        notes: String
    ): AidRequest {
        userRepository.requirePermission(
            UserRole.ADMIN,
            UserRole.ACCOUNTANT,
            UserRole.REVIEWER,
            actionDescription = "تقديم طلب مساعدة"
        )
        AccountingValidator.requirePositiveFiniteAmount(amountYer, "مبلغ المساعدة المطلوب")

        val ceiling = settingsRepository.fundInfo.value.maxAidCeilingYer
        if (ceiling > 0.0) {
            require(amountYer <= ceiling) {
                "المبلغ المطلوب (${amountYer.toLong()} ﷼) يتجاوز السقف الأعلى المعتمد للمساعدات (${ceiling.toLong()} ﷼)."
            }
        }

        val member = memberRepository.members.value.find { it.id == memberId }
            ?: throw IllegalArgumentException("يجب ربط طلب المساعدة بعضو مسجل في الصندوق ($memberId).")

        val maxSuffix = _aidRequests.value.mapNotNull { it.requestNo.substringAfterLast("-").toIntOrNull() }.maxOrNull() ?: 0
        val count = maxOf(maxSuffix, _aidRequests.value.size) + 1
        val reqNo = String.format(Locale.US, "REQ-%d-%03d", Calendar.getInstance().get(Calendar.YEAR), count)

        val req = AidRequest(
            requestNo = reqNo,
            memberId = member.id,
            memberName = member.fullName,
            aidType = aidType,
            requestedAmountYer = amountYer,
            approvedAmountYer = 0.0,
            status = AidStatus.IN_REVIEW,
            requestDate = nowStr(),
            notes = notes
        )

        db?.let { database ->
            runBlocking(dbDispatcher) {
                database.withTransaction {
                    database.aidDao().upsertAidRequest(AidRequestEntity.fromDomain(req, -count))
                }
            }
        }

        _aidRequests.value = listOf(req) + _aidRequests.value
        userRepository.logAudit("طلب مساعدة", "تقديم طلب مساعدة $reqNo للأخ ${member.fullName} بمبلغ $amountYer ﷼")
        return req
    }

    fun updateAidStatus(requestId: String, newStatus: AidStatus, approvedAmountYer: Double = 0.0) {
        val req = _aidRequests.value.find { it.id == requestId }
            ?: throw IllegalArgumentException("طلب المساعدة غير موجود ($requestId).")

        when (newStatus) {
            AidStatus.APPROVED, AidStatus.REJECTED -> {
                userRepository.requirePermission(
                    UserRole.ADMIN,
                    UserRole.REVIEWER,
                    actionDescription = "مراجعة واعتماد طلب المساعدة"
                )
                check(req.status != AidStatus.DISBURSED) {
                    "لا يمكن تعديل طلب مساعدة تم صرفه مسبقاً."
                }
            }
            AidStatus.DISBURSED -> {
                userRepository.requirePermission(
                    UserRole.ADMIN,
                    UserRole.ACCOUNTANT,
                    actionDescription = "صرف طلب المساعدة"
                )
                check(req.status != AidStatus.DISBURSED && req.voucherId.isNullOrBlank()) {
                    "تم صرف طلب المساعدة (${req.requestNo}) مسبقاً بالسند (${req.voucherId}); يمنع تكرار الصرف."
                }
                if (settingsRepository.appSettings.value.requireReviewerApproval) {
                    check(req.status == AidStatus.APPROVED) {
                        "يشترط اعتماد المراجع المالي أولاً قبل صرف المساعدة (${req.requestNo})."
                    }
                }
            }
            AidStatus.IN_REVIEW -> {
                userRepository.requirePermission(
                    UserRole.ADMIN,
                    UserRole.REVIEWER,
                    actionDescription = "إعادة الطلب للمراجعة"
                )
                check(req.status != AidStatus.DISBURSED) {
                    "لا يمكن إرجاع طلب مصروف للمراجعة."
                }
            }
        }

        val ceiling = settingsRepository.fundInfo.value.maxAidCeilingYer
        val resolvedAmount = when (newStatus) {
            AidStatus.APPROVED -> {
                val target = if (approvedAmountYer > 0.0) approvedAmountYer else req.requestedAmountYer
                AccountingValidator.requirePositiveFiniteAmount(target, "المبلغ المعتمد للمساعدة")
                require(target <= req.requestedAmountYer) {
                    "المبلغ المعتمد ($target) لا يمكن أن يتجاوز المبلغ المطلوب (${req.requestedAmountYer})."
                }
                if (ceiling > 0.0) {
                    require(target <= ceiling) {
                        "المبلغ المعتمد (${target.toLong()} ﷼) يتجاوز السقف الأعلى للمساعدات (${ceiling.toLong()} ﷼)."
                    }
                }
                target
            }
            AidStatus.DISBURSED -> {
                val target = when {
                    approvedAmountYer > 0.0 -> approvedAmountYer
                    req.approvedAmountYer > 0.0 -> req.approvedAmountYer
                    else -> req.requestedAmountYer
                }
                AccountingValidator.requirePositiveFiniteAmount(target, "المبلغ المصروف للمساعدة")
                val cap = if (req.approvedAmountYer > 0.0) minOf(req.requestedAmountYer, req.approvedAmountYer) else req.requestedAmountYer
                require(target <= cap) {
                    "المبلغ المصروف ($target) يتجاوز الحد المعتمد/المطلوب ($cap)."
                }
                if (ceiling > 0.0) {
                    require(target <= ceiling) {
                        "المبلغ المصروف (${target.toLong()} ﷼) يتجاوز السقف الأعلى للمساعدات (${ceiling.toLong()} ﷼)."
                    }
                }
                target
            }
            else -> req.approvedAmountYer
        }

        var preparedVoucher: PreparedVoucherPosting? = null
        if (newStatus == AidStatus.DISBURSED) {
            preparedVoucher = accountingRepository.prepareVoucherPosting(
                type = VoucherType.PAYMENT,
                partyName = req.memberName,
                amountOriginal = resolvedAmount,
                currency = CurrencyCode.YER,
                paymentMethod = PaymentMethod.CASH,
                accountCode = "5100",
                counterpartAccountCode = resolveDefaultCashTreasuryCode(),
                description = "صرف مساعدة ${req.aidType.title} بموجب الطلب ${req.requestNo}"
            )
        }

        var updated = req.copy(
            status = newStatus,
            approvedAmountYer = resolvedAmount,
            decisionDate = nowStr(),
            voucherId = preparedVoucher?.voucher?.voucherNo ?: req.voucherId
        )

        db?.let { database ->
            val committed = runBlocking(dbDispatcher) {
                database.withTransaction {
                    val dbReq = database.aidDao().getAidRequestById(requestId)?.toDomain()
                    if (dbReq != null && newStatus == AidStatus.DISBURSED) {
                        check(dbReq.status != AidStatus.DISBURSED && dbReq.voucherId.isNullOrBlank()) {
                            "تم صرف هذا الطلب مسبقاً في قاعدة البيانات."
                        }
                    }
                    val committedVoucher = preparedVoucher?.let {
                        accountingRepository.persistVoucherInTransaction(database, it)
                    }
                    val finalReq = updated.copy(
                        voucherId = committedVoucher?.voucher?.voucherNo ?: updated.voucherId
                    )
                    database.aidDao().upsertAidRequest(AidRequestEntity.fromDomain(finalReq))
                    finalReq to committedVoucher
                }
            }
            updated = committed.first
            preparedVoucher = committed.second
        }

        _aidRequests.value = _aidRequests.value.map { if (it.id == requestId) updated else it }
        preparedVoucher?.let { accountingRepository.commitVoucherInMemory(it) }
        userRepository.logAudit("تحديث مساعدة", "تغيير حالة طلب المساعدة ${req.requestNo} إلى ${newStatus.title}")
    }

    fun addDonor(donor: Donor) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إضافة مانح")
        AccountingValidator.requireNonNegativeFiniteAmount(donor.totalDonatedYer, "إجمالي التبرعات")
        AccountingValidator.requireNonNegativeFiniteAmount(donor.pledgeAmountYer, "مبلغ التعهد")
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
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "تحصيل دفعة مانح")
        AccountingValidator.requirePositiveFiniteAmount(amountOriginal, "مبلغ التبرع")
        val donor = _donors.value.find { it.id == donorId } ?: return
        val amountYer = accountingRepository.convertToYer(amountOriginal, currency)
        val today = nowStr()

        val accCode = when (paymentMethod) {
            PaymentMethod.CASH -> "1100"
            PaymentMethod.BANK_TRANSFER -> "1200"
            PaymentMethod.ELECTRONIC_WALLET -> "1300"
        }

        var preparedVoucher = accountingRepository.prepareVoucherPosting(
            type = VoucherType.RECEIPT,
            partyName = donor.name,
            amountOriginal = amountOriginal,
            currency = currency,
            paymentMethod = paymentMethod,
            accountCode = accCode,
            counterpartAccountCode = "4200",
            description = "تحصيل دفعة من التعهد المالي للداعم ${donor.name}"
        )

        val updated = donor.copy(
            totalDonatedYer = donor.totalDonatedYer + amountYer,
            pledgeFulfilledYer = donor.pledgeFulfilledYer + amountYer,
            lastDonationDate = today
        )

        db?.let { database ->
            preparedVoucher = runBlocking(dbDispatcher) {
                database.withTransaction {
                    val committedV = accountingRepository.persistVoucherInTransaction(database, preparedVoucher)
                    database.aidDao().upsertDonor(DonorEntity.fromDomain(updated))
                    committedV
                }
            }
        }

        _donors.value = _donors.value.map { if (it.id == donorId) updated else it }
        accountingRepository.commitVoucherInMemory(preparedVoucher)
    }

    fun addCampaign(campaign: Campaign) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "إطلاق حملة خيرية")
        AccountingValidator.requirePositiveFiniteAmount(campaign.goalAmountYer, "هدف الحملة")
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
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "تسجيل تبرع لحملة")
        if (!settingsRepository.appSettings.value.allowAnonymousDonors && donorName.isBlank()) {
            throw IllegalArgumentException("تم تعطيل التبرعات المجهولة في إعدادات الصندوق؛ يرجى إدخال اسم المتبرع.")
        }
        AccountingValidator.requirePositiveFiniteAmount(amountOriginal, "مبلغ التبرع")
        val c = _campaigns.value.find { it.id == campaignId } ?: return
        val amountYer = accountingRepository.convertToYer(amountOriginal, currency)
        val newCol = c.collectedAmountYer + amountYer
        val newStatus = if (newCol >= c.goalAmountYer) "مكتملة" else c.status
        val updated = c.copy(collectedAmountYer = newCol, status = newStatus)

        val accCode = when (paymentMethod) {
            PaymentMethod.CASH -> resolveDefaultCashTreasuryCode()
            PaymentMethod.BANK_TRANSFER -> "1200"
            PaymentMethod.ELECTRONIC_WALLET -> "1300"
        }

        var preparedVoucher = accountingRepository.prepareVoucherPosting(
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
            preparedVoucher = runBlocking(dbDispatcher) {
                database.withTransaction {
                    val committedV = accountingRepository.persistVoucherInTransaction(database, preparedVoucher)
                    database.aidDao().upsertCampaign(CampaignEntity.fromDomain(updated))
                    committedV
                }
            }
        }

        _campaigns.value = _campaigns.value.map { if (it.id == campaignId) updated else it }
        accountingRepository.commitVoucherInMemory(preparedVoucher)
    }

    fun disburseFromCampaign(
        campaignId: String,
        recipientName: String,
        amountYer: Double,
        description: String
    ) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "صرف من حملة خيرية")
        AccountingValidator.requirePositiveFiniteAmount(amountYer, "مبلغ الصرف من الحملة")
        val c = _campaigns.value.find { it.id == campaignId }
            ?: throw IllegalArgumentException("لم يتم العثور على الحملة المطلوبة.")
        require(c.status != "مغلقة" && c.status != "ملغاة") {
            "لا يمكن الصرف من حملة (${c.title}) لأن حالتها: ${c.status}."
        }
        val availableBalance = kotlin.math.max(0.0, c.collectedAmountYer - c.spentAmountYer)
        check(amountYer <= availableBalance + AccountingValidator.BALANCE_TOLERANCE) {
            "مبلغ الصرف المطلوب (${amountYer.toLong()} ﷼) يتجاوز المتاح في رصيد الحملة (${availableBalance.toLong()} ﷼)."
        }
        val updated = c.copy(spentAmountYer = c.spentAmountYer + amountYer)

        var preparedVoucher = accountingRepository.prepareVoucherPosting(
            type = VoucherType.PAYMENT,
            partyName = recipientName.ifBlank { "مستفيدو حملة ${c.title}" },
            amountOriginal = amountYer,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5200",
            counterpartAccountCode = resolveDefaultCashTreasuryCode(),
            description = "صرف ميداني من حملة (${c.title}): $description"
        )

        db?.let { database ->
            preparedVoucher = runBlocking(dbDispatcher) {
                database.withTransaction {
                    val committedV = accountingRepository.persistVoucherInTransaction(database, preparedVoucher)
                    database.aidDao().upsertCampaign(CampaignEntity.fromDomain(updated))
                    committedV
                }
            }
        }

        _campaigns.value = _campaigns.value.map { if (it.id == campaignId) updated else it }
        accountingRepository.commitVoucherInMemory(preparedVoucher)
    }

    fun addBeneficiary(b: Beneficiary) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, UserRole.REVIEWER, actionDescription = "إضافة مستفيد")
        AccountingValidator.requireNonNegativeFiniteAmount(b.monthlyIncomeYer, "الدخل الشهري")
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
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "جدولة كفالة دورية")
        AccountingValidator.requirePositiveFiniteAmount(monthlyAmountYer, "مبلغ الكفالة الشهرية")
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
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "صرف كفالة دورية")
        val pa = _periodicAids.value.find { it.id == id }
            ?: throw IllegalArgumentException("سجل الكفالة الدورية غير موجود.")
        check(pa.status == "نشطة") { "الكفالة (${pa.beneficiaryName}) موقوفة حالياً ولا يمكن صرفها." }
        check(pa.lastDisbursedMonth != month) { "تم صرف كفالة (${pa.beneficiaryName}) لشهر $month مسبقاً؛ يمنع تكرار الصرف." }
        AccountingValidator.requirePositiveFiniteAmount(pa.monthlyAmountYer, "مبلغ الكفالة")
        val updated = pa.copy(lastDisbursedMonth = month)

        var preparedVoucher = accountingRepository.prepareVoucherPosting(
            type = VoucherType.PAYMENT,
            partyName = pa.beneficiaryName,
            amountOriginal = pa.monthlyAmountYer,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5200",
            counterpartAccountCode = resolveDefaultCashTreasuryCode(),
            description = "صرف كفالة شهرية فردية (${pa.sponsorshipType}) لشهر $month"
        )

        db?.let { database ->
            preparedVoucher = runBlocking(dbDispatcher) {
                database.withTransaction {
                    val committedV = accountingRepository.persistVoucherInTransaction(database, preparedVoucher)
                    database.aidDao().upsertPeriodicAid(PeriodicAidEntity.fromDomain(updated))
                    committedV
                }
            }
        }

        _periodicAids.value = _periodicAids.value.map { if (it.id == id) updated else it }
        accountingRepository.commitVoucherInMemory(preparedVoucher)
    }

    fun disbursePeriodicAidsBatch(month: String, ids: List<String>) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "صرف دفعة كفالات")
        val targets = _periodicAids.value.filter {
            ids.contains(it.id) && it.status == "نشطة" && it.lastDisbursedMonth != month
        }
        check(targets.isNotEmpty()) { "جميع الكفالات المحددة مصروفة مسبقاً لشهر $month أو غير نشطة." }
        val totalAmount = targets.sumOf { it.monthlyAmountYer }
        AccountingValidator.requirePositiveFiniteAmount(totalAmount, "إجمالي دفعة الكفالات")

        val updatedList = targets.map { it.copy(lastDisbursedMonth = month) }
        var preparedVoucher = accountingRepository.prepareVoucherPosting(
            type = VoucherType.PAYMENT,
            partyName = "كشف صرف الكفالات والمساعدات الدورية لعدد ${targets.size} أسرة",
            amountOriginal = totalAmount,
            currency = CurrencyCode.YER,
            paymentMethod = PaymentMethod.CASH,
            accountCode = "5200",
            counterpartAccountCode = resolveDefaultCashTreasuryCode(),
            description = "صرف المساعدات الدورية لشهر $month إجمالي $totalAmount ﷼"
        )

        db?.let { database ->
            preparedVoucher = runBlocking(dbDispatcher) {
                database.withTransaction {
                    val committedV = accountingRepository.persistVoucherInTransaction(database, preparedVoucher)
                    database.aidDao().upsertPeriodicAids(updatedList.map { PeriodicAidEntity.fromDomain(it) })
                    committedV
                }
            }
        }

        val updatedMap = updatedList.associateBy { it.id }
        _periodicAids.value = _periodicAids.value.map { updatedMap[it.id] ?: it }
        accountingRepository.commitVoucherInMemory(preparedVoucher)
        userRepository.logAudit("صرف كفالات", "تم صرف دفعة كفالات لشهر $month بمبلغ إجمالي $totalAmount ﷼")
    }

    private fun resolveDefaultCashTreasuryCode(): String {
        val preferred = settingsRepository.appSettings.value.defaultTreasuryAccountCode.trim()
        val accounts = accountingRepository.accounts.value
        val match = accounts.find { it.code == preferred && it.type == AccountType.ASSET }
        val isLeaf = match != null && accounts.none { it.parentCode == match.code }
        return if (isLeaf) preferred else "1100"
    }

    internal data class AidVoidRollbackState(
        val restoredAidRequest: AidRequest? = null,
        val restoredDonor: Donor? = null,
        val restoredCampaign: Campaign? = null
    )

    internal suspend fun rollbackAidEntitiesInTransaction(database: FundDatabase?, voucher: Voucher): AidVoidRollbackState {
        val aidReq = _aidRequests.value.find {
            it.voucherId == voucher.id ||
                it.voucherId == voucher.voucherNo ||
                (voucher.type == VoucherType.PAYMENT && voucher.description.contains(it.requestNo))
        }?.let { existing ->
            val restoredStatus = if (settingsRepository.appSettings.value.requireReviewerApproval) {
                AidStatus.APPROVED
            } else {
                AidStatus.IN_REVIEW
            }
            existing.copy(status = restoredStatus, voucherId = null)
        }
        if (aidReq != null && database != null) {
            database.aidDao().upsertAidRequest(AidRequestEntity.fromDomain(aidReq))
        }

        val donor = if (voucher.type == VoucherType.RECEIPT &&
            (voucher.counterpartAccountCode == "4200" || voucher.description.contains("للداعم"))
        ) {
            _donors.value.find { it.name == voucher.partyName }?.let { d ->
                d.copy(
                    totalDonatedYer = kotlin.math.max(0.0, d.totalDonatedYer - voucher.amountYer),
                    pledgeFulfilledYer = kotlin.math.max(0.0, d.pledgeFulfilledYer - voucher.amountYer)
                )
            }
        } else null
        if (donor != null && database != null) {
            database.aidDao().upsertDonor(DonorEntity.fromDomain(donor))
        }

        val campaign = when {
            voucher.type == VoucherType.RECEIPT &&
                (voucher.counterpartAccountCode == "4300" || voucher.description.contains("لحملة:")) -> {
                _campaigns.value.find { voucher.description.contains(it.title) }?.let { c ->
                    val newCol = kotlin.math.max(0.0, c.collectedAmountYer - voucher.amountYer)
                    val nextStatus = if (c.status == "مكتملة" && newCol < c.goalAmountYer) "نشطة" else c.status
                    c.copy(collectedAmountYer = newCol, status = nextStatus)
                }
            }
            voucher.type == VoucherType.PAYMENT && voucher.description.contains("من حملة (") -> {
                _campaigns.value.find { voucher.description.contains("(${it.title})") }?.let { c ->
                    c.copy(spentAmountYer = kotlin.math.max(0.0, c.spentAmountYer - voucher.amountYer))
                }
            }
            else -> null
        }
        if (campaign != null && database != null) {
            database.aidDao().upsertCampaign(CampaignEntity.fromDomain(campaign))
        }

        return AidVoidRollbackState(aidReq, donor, campaign)
    }

    internal fun commitAidVoidInMemory(state: AidVoidRollbackState) {
        state.restoredAidRequest?.let { req ->
            _aidRequests.value = _aidRequests.value.map { if (it.id == req.id) req else it }
        }
        state.restoredDonor?.let { donor ->
            _donors.value = _donors.value.map { if (it.id == donor.id) donor else it }
        }
        state.restoredCampaign?.let { campaign ->
            _campaigns.value = _campaigns.value.map { if (it.id == campaign.id) campaign else it }
        }
    }

    fun addInKindItem(item: InKindItem) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "تعريف صنف عيني")
        require(item.quantityOnHand >= 0) { "الرصيد الافتتاحي لا يمكن أن يكون سالباً." }
        AccountingValidator.requireNonNegativeFiniteAmount(item.unitCostYer, "تكلفة الوحدة")
        db?.let { database ->
            runBlocking(dbDispatcher) {
                database.aidDao().upsertInKindItem(InKindItemEntity.fromDomain(item))
            }
        }
        _inKindItems.value = listOf(item) + _inKindItems.value
        userRepository.logAudit("تعريف صنف عيني", "إضافة صنف مخزني جديد: ${item.name} (${item.code})")
    }

    /**
     * Atomically validates stock availability inside a Room `@Transaction` without clamping to 0.
     * Rejects issue movements exceeding [InKindItem.quantityOnHand].
     */
    fun addInKindMovement(mov: InKindMovement) {
        userRepository.requirePermission(UserRole.ADMIN, UserRole.ACCOUNTANT, actionDescription = "تسجيل حركة مخزن عيني")
        require(mov.quantity > 0) { "الكمية في حركة المستودع يجب أن تكون أكبر من صفر." }

        val currentItem = _inKindItems.value.find { it.id == mov.itemId }
            ?: throw IllegalArgumentException("الصنف العيني المطلوب (${mov.itemName}) غير موجود في المستودع.")

        val isSupply = mov.type == "توريد"
        if (!isSupply) {
            check(currentItem.quantityOnHand >= mov.quantity) {
                "الكمية المطلوبة (${mov.quantity}) تتجاوز الرصيد المتاح بالمستودع (${currentItem.quantityOnHand})."
            }
        }

        var updatedItem = currentItem.copy(
            quantityOnHand = if (isSupply) currentItem.quantityOnHand + mov.quantity else currentItem.quantityOnHand - mov.quantity
        )

        db?.let { database ->
            updatedItem = runBlocking(dbDispatcher) {
                database.withTransaction {
                    val dbItem = database.aidDao().getInKindItemById(mov.itemId)?.toDomain() ?: currentItem
                    if (!isSupply) {
                        check(dbItem.quantityOnHand >= mov.quantity) {
                            "الكمية المطلوبة (${mov.quantity}) تتجاوز الرصيد المتاح في قاعدة البيانات (${dbItem.quantityOnHand})."
                        }
                    }
                    val computed = dbItem.copy(
                        quantityOnHand = if (isSupply) dbItem.quantityOnHand + mov.quantity else dbItem.quantityOnHand - mov.quantity
                    )
                    database.aidDao().upsertInKindMovement(InKindMovementEntity.fromDomain(mov))
                    database.aidDao().upsertInKindItem(InKindItemEntity.fromDomain(computed))
                    computed
                }
            }
        }

        _inKindMovements.value = listOf(mov) + _inKindMovements.value
        _inKindItems.value = _inKindItems.value.map { if (it.id == mov.itemId) updatedItem else it }
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
