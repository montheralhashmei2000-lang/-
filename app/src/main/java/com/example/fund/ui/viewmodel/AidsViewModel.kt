package com.example.fund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import kotlinx.coroutines.flow.StateFlow

class AidsViewModel : ViewModel() {

    private val coordinator = FundRepository.getInstance()
    private val aidRepo = coordinator.aidRepository
    private val memberRepo = coordinator.memberRepository
    private val settingsRepo = coordinator.settingsRepository
    private val userRepo = coordinator.userRepository

    val aidRequests: StateFlow<List<AidRequest>> = aidRepo.aidRequests
    val beneficiaries: StateFlow<List<Beneficiary>> = aidRepo.beneficiaries
    val periodicAids: StateFlow<List<PeriodicAid>> = aidRepo.periodicAids
    val donors: StateFlow<List<Donor>> = aidRepo.donors
    val campaigns: StateFlow<List<Campaign>> = aidRepo.campaigns
    val inKindItems: StateFlow<List<InKindItem>> = aidRepo.inKindItems
    val inKindMovements: StateFlow<List<InKindMovement>> = aidRepo.inKindMovements
    val members: StateFlow<List<Member>> = memberRepo.members
    val regions: StateFlow<List<RegionGroup>> = memberRepo.regions
    val fundInfo: StateFlow<FundInfo> = settingsRepo.fundInfo
    val appSettings: StateFlow<AppSettings> = settingsRepo.appSettings
    val currentUser: StateFlow<AppUser> = userRepo.currentUser

    fun addAidRequest(memberId: String, aidType: AidType, amountYer: Double, notes: String): AidRequest =
        aidRepo.addAidRequest(memberId, aidType, amountYer, notes)

    fun updateAidStatus(requestId: String, newStatus: AidStatus, approvedAmountYer: Double = 0.0) =
        aidRepo.updateAidStatus(requestId, newStatus, approvedAmountYer)

    fun addBeneficiary(b: Beneficiary) = aidRepo.addBeneficiary(b)
    fun addPeriodicAid(beneficiaryId: String, beneficiaryName: String, monthlyAmountYer: Double, sponsorshipType: String) =
        aidRepo.addPeriodicAid(beneficiaryId, beneficiaryName, monthlyAmountYer, sponsorshipType)

    fun disburseSinglePeriodicAid(id: String, month: String) = aidRepo.disburseSinglePeriodicAid(id, month)
    fun disbursePeriodicAidsBatch(month: String, ids: List<String>) = aidRepo.disbursePeriodicAidsBatch(month, ids)
    fun addDonor(donor: Donor) = aidRepo.addDonor(donor)
    fun recordDonorPledgePayment(donorId: String, amountOriginal: Double, currency: CurrencyCode, paymentMethod: PaymentMethod) =
        aidRepo.recordDonorPledgePayment(donorId, amountOriginal, currency, paymentMethod)

    fun addCampaign(campaign: Campaign) = aidRepo.addCampaign(campaign)
    fun donateToCampaign(campaignId: String, donorName: String, amountOriginal: Double, currency: CurrencyCode, paymentMethod: PaymentMethod) =
        aidRepo.donateToCampaign(campaignId, donorName, amountOriginal, currency, paymentMethod)

    fun disburseFromCampaign(campaignId: String, recipientName: String, amountYer: Double, description: String) =
        aidRepo.disburseFromCampaign(campaignId, recipientName, amountYer, description)

    fun addInKindItem(item: InKindItem) = aidRepo.addInKindItem(item)
    fun addInKindMovement(mov: InKindMovement) = aidRepo.addInKindMovement(mov)
}
