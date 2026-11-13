package com.example.fund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fund.data.FundRepository
import com.example.fund.model.LocalServerConfig
import com.example.fund.model.SyncBatchRecord
import com.example.fund.model.SyncQueueItem
import kotlinx.coroutines.flow.StateFlow

class SyncViewModel : ViewModel() {

    private val coordinator = FundRepository.getInstance()
    private val syncRepo = coordinator.syncRepository

    val isOnline: StateFlow<Boolean> = syncRepo.isOnline
    val autoSyncOnConnect: StateFlow<Boolean> = syncRepo.autoSyncOnConnect
    val isSyncing: StateFlow<Boolean> = syncRepo.isSyncing
    val syncProgress: StateFlow<Float> = syncRepo.syncProgress
    val syncStageMessage: StateFlow<String> = syncRepo.syncStageMessage
    val serverConfig: StateFlow<LocalServerConfig> = syncRepo.serverConfig
    val syncQueue: StateFlow<List<SyncQueueItem>> = syncRepo.syncQueue
    val syncHistory: StateFlow<List<SyncBatchRecord>> = syncRepo.syncHistory
    val lastSyncTimestamp: StateFlow<String> = syncRepo.lastSyncTimestamp

    fun getTotalLocalRecordsCount(): Int = coordinator.getTotalLocalRecordsCount()

    fun toggleOnline() = syncRepo.toggleOnline()

    fun setAutoSyncOnConnect(enabled: Boolean) = syncRepo.setAutoSyncOnConnect(enabled)

    fun simulateOfflineFieldOperations() = syncRepo.simulateOfflineFieldOperations()

    fun syncNow() = syncRepo.syncNow()

    fun updateServerNode(name: String, host: String, latencyMs: Int = 8) =
        syncRepo.updateServerNode(name, host, latencyMs)
}
