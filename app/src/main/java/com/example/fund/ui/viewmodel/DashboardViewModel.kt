package com.example.fund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import kotlinx.coroutines.flow.StateFlow

class DashboardViewModel : ViewModel() {

    private val coordinator = FundRepository.getInstance()
    private val accountingRepo = coordinator.accountingRepository
    private val memberRepo = coordinator.memberRepository
    private val aidRepo = coordinator.aidRepository
    private val syncRepo = coordinator.syncRepository
    private val userRepo = coordinator.userRepository

    val accounts: StateFlow<List<Account>> = accountingRepo.accounts
    val members: StateFlow<List<Member>> = memberRepo.members
    val aidRequests: StateFlow<List<AidRequest>> = aidRepo.aidRequests
    val vouchers: StateFlow<List<Voucher>> = accountingRepo.vouchers
    val budgets: StateFlow<List<Budget>> = accountingRepo.budgets
    val campaigns: StateFlow<List<Campaign>> = aidRepo.campaigns
    val currencyRates: StateFlow<Map<CurrencyCode, Double>> = accountingRepo.currencyRates
    val isOnline: StateFlow<Boolean> = syncRepo.isOnline
    val syncQueue: StateFlow<List<SyncQueueItem>> = syncRepo.syncQueue
    val serverConfig: StateFlow<LocalServerConfig> = syncRepo.serverConfig
    val currentUser: StateFlow<AppUser> = userRepo.currentUser
}
