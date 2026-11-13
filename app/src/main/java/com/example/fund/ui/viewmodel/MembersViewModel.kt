package com.example.fund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import kotlinx.coroutines.flow.StateFlow

class MembersViewModel : ViewModel() {

    private val coordinator = FundRepository.getInstance()
    private val memberRepo = coordinator.memberRepository
    private val settingsRepo = coordinator.settingsRepository
    private val userRepo = coordinator.userRepository

    val members: StateFlow<List<Member>> = memberRepo.members
    val regions: StateFlow<List<RegionGroup>> = memberRepo.regions
    val subscriptions: StateFlow<List<Subscription>> = memberRepo.subscriptions
    val appSettings: StateFlow<AppSettings> = settingsRepo.appSettings
    val fundInfo: StateFlow<FundInfo> = settingsRepo.fundInfo
    val currentUser: StateFlow<AppUser> = userRepo.currentUser

    fun addMember(member: Member) = memberRepo.addMember(member)
    fun updateMember(member: Member) = memberRepo.updateMember(member)
    fun toggleMemberStatus(memberId: String) = memberRepo.toggleMemberStatus(memberId)
    fun addVillageByRegionName(regionName: String, villageName: String) =
        memberRepo.addVillageByRegionName(regionName, villageName)

    fun addSubscription(
        memberId: String,
        amountOriginal: Double,
        currency: CurrencyCode,
        monthYear: String,
        paymentMethod: PaymentMethod,
        referenceNo: String,
        notes: String
    ): Subscription = memberRepo.addSubscription(
        memberId, amountOriginal, currency, monthYear, paymentMethod, referenceNo, notes
    )
}
