package com.mascill.kiosync.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mascill.kiosync.data.repository.KioSyncRepository

class MainViewModel(
    private val repository: KioSyncRepository
) : ViewModel() {

    var uiState by mutableStateOf(
        KioSyncUiState(
            kioskEnabled = repository.isKioskEnabled(),
            launchableApps = repository.getLaunchableApps(),
            allowedPackages = repository.getAllowedPackages()
        )
    )
        private set

    private var statusTapCount = 0

    fun setWaitingForSystemInit(waiting: Boolean) {
        uiState = uiState.copy(waitingForSystemInit = waiting)
    }

    fun setKioskEnabled(enabled: Boolean) {
        repository.setKioskEnabled(enabled)
        uiState = uiState.copy(kioskEnabled = enabled)
    }

    fun isKioskEnabled(): Boolean {
        return repository.isKioskEnabled()
    }

    fun getAllowedLaunchablePackages(): Set<String> {
        return repository.getAllowedLaunchablePackages()
    }

    fun onStatusTap() {
        statusTapCount++

        if (statusTapCount >= STATUS_TAP_TO_OPEN_ADMIN) {
            statusTapCount = 0
            uiState = uiState.copy(showPinDialog = true)
        }
    }

    fun onPinChange(value: String) {
        uiState = uiState.copy(
            pin = value
                .filter(Char::isDigit)
                .take(ADMIN_PIN.length),
            pinError = false
        )
    }

    fun confirmPin() {
        if (uiState.pin == ADMIN_PIN) {
            closePinDialog()
            refreshApps()
            uiState = uiState.copy(showAdminPanel = true)
        } else {
            uiState = uiState.copy(pinError = true)
        }
    }

    fun closePinDialog() {
        uiState = uiState.copy(
            showPinDialog = false,
            pin = "",
            pinError = false
        )
    }

    fun closeAdminPanel() {
        uiState = uiState.copy(showAdminPanel = false)
    }

    fun refreshApps() {
        uiState = uiState.copy(
            launchableApps = repository.getLaunchableApps(),
            allowedPackages = repository.getAllowedPackages()
        )
    }

    fun updateAllowedPackage(packageName: String, allowed: Boolean): Boolean {
        val nextAllowedPackages = if (allowed) {
            uiState.allowedPackages + packageName
        } else {
            uiState.allowedPackages - packageName
        }

        repository.setAllowedPackages(nextAllowedPackages)
        uiState = uiState.copy(allowedPackages = nextAllowedPackages)

        return uiState.kioskEnabled
    }

    class Factory(
        private val repository: KioSyncRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository) as T
            }

            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    private companion object {
        const val STATUS_TAP_TO_OPEN_ADMIN = 7

        // Untuk development dulu. Untuk production jangan hardcode PIN seperti ini.
        const val ADMIN_PIN = "123456"
    }
}
