package com.example.fund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fund.data.FundRepository
import com.example.fund.model.LocalServerConfig
import com.example.fund.model.ServerProbeResult
import com.example.fund.model.ServerProfile
import com.example.fund.model.SyncBatchRecord
import com.example.fund.model.SyncQueueItem
import kotlinx.coroutines.flow.StateFlow

class SyncViewModel : ViewModel() {

    private val coordinator = FundRepository.getInstance()
    private val syncRepo = coordinator.syncRepository

    val isOnline: StateFlow<Boolean> = syncRepo.isOnline
    val isTransportAvailable: StateFlow<Boolean> = syncRepo.isTransportAvailable
    val autoSyncOnConnect: StateFlow<Boolean> = syncRepo.autoSyncOnConnect
    val isSyncing: StateFlow<Boolean> = syncRepo.isSyncing
    val syncProgress: StateFlow<Float> = syncRepo.syncProgress
    val syncStageMessage: StateFlow<String> = syncRepo.syncStageMessage
    val serverConfig: StateFlow<LocalServerConfig> = syncRepo.serverConfig
    val serverProfiles: StateFlow<List<ServerProfile>> = syncRepo.serverProfiles
    val syncQueue: StateFlow<List<SyncQueueItem>> = syncRepo.syncQueue
    val syncHistory: StateFlow<List<SyncBatchRecord>> = syncRepo.syncHistory
    val lastSyncTimestamp: StateFlow<String> = syncRepo.lastSyncTimestamp
    val localVaultBatchCount: StateFlow<Int> = syncRepo.localVaultBatchCount
    val localVaultSizeBytes: StateFlow<Long> = syncRepo.localVaultSizeBytes

    fun getTotalLocalRecordsCount(): Int = coordinator.getTotalLocalRecordsCount()

    fun isBuiltInVaultHost(host: String = serverConfig.value.serverHost): Boolean =
        syncRepo.isBuiltInVaultHost(host)

    fun toggleOnline() = syncRepo.toggleOnline()

    suspend fun toggleOnlineSuspending(): Boolean {
        if (syncRepo.isOnline.value) {
            syncRepo.toggleOnline()
            return false
        }
        return syncRepo.connectAndVerifyHealthBlocking()
    }

    suspend fun connectAndSyncSmartSuspending(): Boolean = syncRepo.connectAndSyncSmartSuspending()

    fun setAutoSyncOnConnect(enabled: Boolean) = syncRepo.setAutoSyncOnConnect(enabled)

    fun simulateOfflineFieldOperations() = syncRepo.simulateOfflineFieldOperations()

    fun syncNow() = syncRepo.syncNow()

    suspend fun syncNowSuspending(): Boolean = syncRepo.syncNowBlocking()

    fun updateServerNode(name: String, host: String, latencyMs: Int = 0, authToken: String? = null) =
        syncRepo.updateServerNode(name, host, latencyMs, authToken)

    suspend fun probeServerConnection(
        hostOverride: String = serverConfig.value.serverHost,
        nameOverride: String = serverConfig.value.serverName,
        saveAsActiveNode: Boolean = false,
        authTokenOverride: String? = null
    ): ServerProbeResult = syncRepo.probeServerConnection(
        hostOverride = hostOverride,
        nameOverride = nameOverride,
        saveAsActiveNode = saveAsActiveNode,
        authTokenOverride = authTokenOverride
    )

    fun deleteServerProfile(profileId: String): Boolean = syncRepo.deleteServerProfile(profileId)

    fun restoreLatestFromLocalVault(): Pair<Boolean, String> = syncRepo.restoreLatestFromLocalVault()
}
