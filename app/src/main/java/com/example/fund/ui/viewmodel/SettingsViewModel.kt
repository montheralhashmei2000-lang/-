package com.example.fund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fund.data.FundRepository
import com.example.fund.model.*
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {

    private val coordinator = FundRepository.getInstance()
    private val settingsRepo = coordinator.settingsRepository
    private val memberRepo = coordinator.memberRepository
    private val accountingRepo = coordinator.accountingRepository
    private val aidRepo = coordinator.aidRepository
    private val userRepo = coordinator.userRepository
    private val syncRepo = coordinator.syncRepository

    val fundInfo: StateFlow<FundInfo> = settingsRepo.fundInfo
    val regions: StateFlow<List<RegionGroup>> = memberRepo.regions
    val members: StateFlow<List<Member>> = memberRepo.members
    val beneficiaries: StateFlow<List<Beneficiary>> = aidRepo.beneficiaries
    val appSettings: StateFlow<AppSettings> = settingsRepo.appSettings
    val printSettings: StateFlow<PrintSettings> = settingsRepo.printSettings
    val vouchers: StateFlow<List<Voucher>> = accountingRepo.vouchers
    val currencyRates: StateFlow<Map<CurrencyCode, Double>> = accountingRepo.currencyRates
    val isDarkMode: StateFlow<Boolean> = settingsRepo.isDarkMode
    val fontScale: StateFlow<Float> = settingsRepo.fontScale
    val serverConfig: StateFlow<LocalServerConfig> = syncRepo.serverConfig
    val autoSyncOnConnect: StateFlow<Boolean> = syncRepo.autoSyncOnConnect
    val isOnline: StateFlow<Boolean> = syncRepo.isOnline
    val syncQueue: StateFlow<List<SyncQueueItem>> = syncRepo.syncQueue
    val users: StateFlow<List<AppUser>> = userRepo.users
    val currentUser: StateFlow<AppUser> = userRepo.currentUser
    val auditLogs: StateFlow<List<AuditLog>> = userRepo.auditLogs

    fun setDarkModePreference(enabled: Boolean) = settingsRepo.setDarkModePreference(enabled)
    fun setFontScalePreference(scale: Float) = settingsRepo.setFontScalePreference(scale)
    fun updateFundLogo(base64: String) = settingsRepo.updateFundLogo(base64)
    fun updateFundInfo(info: FundInfo) = settingsRepo.updateFundInfo(info)
    fun updateAppSettings(settings: AppSettings) = settingsRepo.updateAppSettings(settings)
    fun updatePrintSettings(settings: PrintSettings) = settingsRepo.updatePrintSettings(settings)
    fun resetPrintSettingsToDefault() = settingsRepo.resetPrintSettingsToDefault()
    fun updateCurrencyRate(currency: CurrencyCode, rate: Double) = accountingRepo.updateCurrencyRate(currency, rate)
    fun setAutoSyncOnConnect(enabled: Boolean) = syncRepo.setAutoSyncOnConnect(enabled)
    fun toggleOnline() = syncRepo.toggleOnline()
    fun syncNow() = syncRepo.syncNow()
    fun updateServerNode(name: String, host: String, latencyMs: Int = 8) = syncRepo.updateServerNode(name, host, latencyMs)
    fun exportDatabaseJson(): String = coordinator.exportDatabaseJson()
    fun importDatabaseFromJson(json: String): Boolean = coordinator.importDatabaseFromJson(json)
    fun resetToDefaultSeedData() = coordinator.resetToDefaultSeedData()
    fun getTotalLocalRecordsCount(): Int = coordinator.getTotalLocalRecordsCount()

    fun addUser(username: String, fullName: String, role: UserRole, password: String) =
        userRepo.addUser(username, fullName, role, password)

    fun switchRole(role: UserRole) = userRepo.switchRole(role)
    fun toggleUserActive(userId: String) = userRepo.toggleUserActive(userId)

    fun addRegion(name: String): Boolean = memberRepo.addRegion(name)
    fun deleteRegion(id: String) = memberRepo.deleteRegion(id)
    fun addVillageToRegion(regionId: String, villageName: String): Boolean =
        memberRepo.addVillageToRegion(regionId, villageName)

    fun removeVillageFromRegion(regionId: String, villageName: String) =
        memberRepo.removeVillageFromRegion(regionId, villageName)
}
