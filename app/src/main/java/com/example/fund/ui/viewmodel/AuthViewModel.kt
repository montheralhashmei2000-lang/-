package com.example.fund.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fund.data.FundRepository
import com.example.fund.data.repository.AuthResult
import com.example.fund.model.AppUser
import com.example.fund.model.FundInfo
import com.example.fund.model.LocalServerConfig
import com.example.fund.model.SyncQueueItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthViewModel : ViewModel() {

    private val coordinator = FundRepository.getInstance()
    private val userRepo = coordinator.userRepository
    private val settingsRepo = coordinator.settingsRepository
    private val syncRepo = coordinator.syncRepository

    val fundInfo: StateFlow<FundInfo> = settingsRepo.fundInfo
    val users: StateFlow<List<AppUser>> = userRepo.users
    val currentUser: StateFlow<AppUser> = userRepo.currentUser
    val isLoggedIn: StateFlow<Boolean> = userRepo.isLoggedIn
    val hasCompletedInitialLogin: StateFlow<Boolean> = userRepo.hasCompletedInitialLogin
    val hasQuickPin: StateFlow<Boolean> = userRepo.hasQuickPin
    val requiresPasswordChange: StateFlow<Boolean> = userRepo.requiresPasswordChange
    val serverConfig: StateFlow<LocalServerConfig> = syncRepo.serverConfig
    val isOnline: StateFlow<Boolean> = syncRepo.isOnline
    val syncQueue: StateFlow<List<SyncQueueItem>> = syncRepo.syncQueue

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    fun clearError() {
        _authError.value = null
    }

    fun getBoundQuickAuthUsername(): String? = userRepo.getBoundQuickAuthUser()?.username

    fun getBoundQuickAuthUserId(): String? = userRepo.getBoundQuickAuthUser()?.id

    fun loginWithPassword(username: String, password: String): AuthResult {
        val result = userRepo.authenticateWithPassword(username, password)
        _authError.value = if (result is AuthResult.Error) result.message else null
        return result
    }

    fun changePassword(currentPassword: String, newPassword: String): AuthResult {
        val result = userRepo.changePassword(currentPassword, newPassword)
        _authError.value = if (result is AuthResult.Error) result.message else null
        return result
    }

    fun setQuickPin(pin: String): Boolean {
        val ok = userRepo.setQuickPin(pin)
        if (!ok) {
            _authError.value = "يجب أن يتكون رمز PIN السريع من 4 إلى 6 أرقام."
        } else {
            _authError.value = null
        }
        return ok
    }

    fun loginWithQuickPin(username: String, pin: String): AuthResult {
        val res = userRepo.authenticateWithQuickPin(username, pin)
        _authError.value = if (res is AuthResult.Error) res.message else null
        return res
    }

    fun loginWithBiometrics(userId: String, cryptoVerified: Boolean): AuthResult {
        val res = userRepo.authenticateWithBiometrics(userId, cryptoVerified)
        _authError.value = if (res is AuthResult.Error) res.message else null
        return res
    }

    fun logout() {
        userRepo.logout()
    }

    fun toggleOnline() {
        syncRepo.toggleOnline()
    }

    fun updateServerNode(name: String, host: String, latencyMs: Int = 8) {
        syncRepo.updateServerNode(name, host, latencyMs)
    }
}
