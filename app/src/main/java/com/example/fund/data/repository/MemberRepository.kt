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

class MemberRepository(
    private val dbScope: CoroutineScope,
    private val dbDispatcher: CoroutineDispatcher,
    private val accountingRepository: AccountingRepository,
    private val userRepository: UserRepository
) {
    private val _regions = MutableStateFlow(SeedData.regions)
    val regions: StateFlow<List<RegionGroup>> = _regions.asStateFlow()

    private val _members = MutableStateFlow(SeedData.members)
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    private val _subscriptions = MutableStateFlow(SeedData.subscriptions)
    val subscriptions: StateFlow<List<Subscription>> = _subscriptions.asStateFlow()

    private var db: FundDatabase? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
    private fun nowStr() = dateFormat.format(Date())

    suspend fun attachAndHydrate(database: FundDatabase) {
        db = database
        val dbRegions = database.memberDao().observeRegions().first().map { it.toDomain() }
        if (dbRegions.isNotEmpty()) _regions.value = dbRegions

        val dbMembers = database.memberDao().observeMembers().first().map { it.toDomain() }
        if (dbMembers.isNotEmpty()) _members.value = dbMembers

        val dbSubs = database.memberDao().observeSubscriptions().first().map { it.toDomain() }
        if (dbSubs.isNotEmpty()) _subscriptions.value = dbSubs
    }

    fun addMember(member: Member) {
        _members.value = listOf(member) + _members.value
        val count = _members.value.size
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.memberDao().upsertMember(MemberEntity.fromDomain(member, -count))
            }
        }
        userRepository.logAudit("إضافة عضو", "تسجيل العضو الجديد: ${member.fullName} برقم ${member.memberNo}")
    }

    fun updateMember(member: Member) {
        _members.value = _members.value.map { if (it.id == member.id) member else it }
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.memberDao().upsertMember(MemberEntity.fromDomain(member))
            }
        }
        userRepository.logAudit("تعديل عضو", "تحديث بيانات العضو: ${member.fullName} (${member.memberNo})")
    }

    fun toggleMemberStatus(memberId: String) {
        val m = _members.value.find { it.id == memberId } ?: return
        val newStatus = if (m.status == "نشط") "معلق" else "نشط"
        val updated = m.copy(status = newStatus)
        _members.value = _members.value.map { if (it.id == memberId) updated else it }
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.memberDao().upsertMember(MemberEntity.fromDomain(updated))
            }
        }
        userRepository.logAudit("تغيير حالة عضو", "تغيير حالة عضوية ${m.fullName} إلى $newStatus")
    }

    fun addSubscription(
        memberId: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        monthYear: String,
        paymentMethod: PaymentMethod,
        referenceNo: String,
        notes: String
    ): Subscription {
        val member = requireNotNull(_members.value.find { it.id == memberId }) {
            "لم يتم العثور على العضو المطلوب ($memberId)"
        }
        val rate = accountingRepository.currencyRates.value[currency] ?: currency.defaultRate
        val amountYer = amountOriginal * rate
        val maxSuffix = _subscriptions.value.mapNotNull { it.receiptNo.substringAfterLast("-").toIntOrNull() }.maxOrNull() ?: 0
        val nextNum = maxOf(maxSuffix, _subscriptions.value.size) + 1
        val receiptNo = "SUB-${Calendar.getInstance().get(Calendar.YEAR)}-%03d".format(nextNum)
        val today = nowStr()

        val sub = Subscription(
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
        _subscriptions.value = listOf(sub) + _subscriptions.value

        val updatedMember = member.copy(
            totalPaidYer = member.totalPaidYer + amountYer,
            dueBalanceYer = max(0.0, member.dueBalanceYer - amountYer)
        )
        _members.value = _members.value.map { if (it.id == memberId) updatedMember else it }

        val treasuryAccountCode = when (paymentMethod) {
            PaymentMethod.CASH -> "1100"
            PaymentMethod.BANK_TRANSFER -> "1200"
            PaymentMethod.ELECTRONIC_WALLET -> "1300"
        }

        accountingRepository.issueVoucher(
            type = VoucherType.RECEIPT,
            partyName = member.fullName,
            amountOriginal = amountOriginal,
            currency = currency,
            paymentMethod = paymentMethod,
            accountCode = treasuryAccountCode,
            counterpartAccountCode = "4100",
            description = "تحصيل اشتراك $monthYear للعضو ${member.fullName} ($receiptNo)"
        )

        db?.let { database ->
            val subCount = _subscriptions.value.size
            dbScope.launch(dbDispatcher) {
                database.withTransaction {
                    database.memberDao().upsertMember(MemberEntity.fromDomain(updatedMember))
                    database.memberDao().upsertSubscription(SubscriptionEntity.fromDomain(sub, -subCount))
                }
            }
        }

        return sub
    }

    // Regions & Villages Management (1 Region -> Multiple Villages)
    fun addRegion(regionName: String): Boolean {
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
        if (_regions.value.size <= 1) return
        val target = _regions.value.find { it.id == regionId } ?: return
        _regions.value = _regions.value.filterNot { it.id == regionId }
        db?.let { database ->
            dbScope.launch(dbDispatcher) {
                database.memberDao().deleteRegionById(regionId)
            }
        }
        userRepository.logAudit("حذف منطقة", "حذف المنطقة: ${target.name}")
    }

    fun addVillageToRegion(regionId: String, villageName: String): Boolean {
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
