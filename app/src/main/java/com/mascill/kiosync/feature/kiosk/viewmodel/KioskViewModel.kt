package com.mascill.kiosync.feature.kiosk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.kiosync.core.data.repository.KioskRepository
import com.mascill.kiosync.core.model.LaunchableApp
import com.mascill.kiosync.feature.kiosk.model.KioskDialogState
import com.mascill.kiosync.feature.kiosk.model.KioskSideEffect
import com.mascill.kiosync.feature.kiosk.model.KioskUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KioskViewModel(
    private val repository: KioskRepository
) : ViewModel() {

    private val waitingForSystemInit = MutableStateFlow(false)
    private val launchableApps = MutableStateFlow(repository.getLaunchableApps())
    private val dialogState = MutableStateFlow(KioskDialogState())
    private val _sideEffects = MutableSharedFlow<KioskSideEffect>()

    val sideEffects = _sideEffects.asSharedFlow()

    val uiState: StateFlow<KioskUiState> = combine(
        waitingForSystemInit,
        repository.kioskEnabled,
        repository.allowedPackages,
        launchableApps,
        dialogState
    ) { waiting, kioskEnabled, allowedPackages, apps, dialog ->
        KioskUiState(
            waitingForSystemInit = waiting,
            kioskEnabled = kioskEnabled,
            launchableApps = apps,
            allowedPackages = allowedPackages,
            showPinDialog = dialog.showPinDialog,
            showAdminPanel = dialog.showAdminPanel,
            pin = dialog.pin,
            pinError = dialog.pinError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = KioskUiState(launchableApps = launchableApps.value)
    )

    private var statusTapCount = 0

    fun setWaitingForSystemInit(waiting: Boolean) {
        waitingForSystemInit.value = waiting
    }

    fun onHostResumed() {
        viewModelScope.launch {
            emitCurrentKioskStateEffect()
        }
    }

    fun onDelayedKioskStartReady() {
        viewModelScope.launch {
            waitingForSystemInit.value = false
            emitCurrentKioskStateEffect()
        }
    }

    fun onKioskEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            repository.setKioskEnabled(enabled)
            waitingForSystemInit.value = false

            _sideEffects.emit(
                if (enabled) {
                    KioskSideEffect.StartKiosk
                } else {
                    KioskSideEffect.StopKiosk
                }
            )
        }
    }

    fun onStatusTap() {
        statusTapCount++

        if (statusTapCount >= STATUS_TAP_TO_OPEN_ADMIN) {
            statusTapCount = 0
            dialogState.value = dialogState.value.copy(showPinDialog = true)
        }
    }

    fun onPinChange(value: String) {
        dialogState.value = dialogState.value.copy(
            pin = value
                .filter(Char::isDigit)
                .take(ADMIN_PIN.length),
            pinError = false
        )
    }

    fun confirmPin() {
        if (dialogState.value.pin == ADMIN_PIN) {
            closePinDialog()
            refreshApps()
            dialogState.value = dialogState.value.copy(showAdminPanel = true)
        } else {
            dialogState.value = dialogState.value.copy(pinError = true)
        }
    }

    fun closePinDialog() {
        dialogState.value = dialogState.value.copy(
            showPinDialog = false,
            pin = "",
            pinError = false
        )
    }

    fun closeAdminPanel() {
        dialogState.value = dialogState.value.copy(showAdminPanel = false)
    }

    fun refreshApps() {
        launchableApps.value = repository.getLaunchableApps()
    }

    fun onAllowedAppChange(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            val nextAllowedPackages = if (allowed) {
                uiState.value.allowedPackages + packageName
            } else {
                uiState.value.allowedPackages - packageName
            }

            repository.setAllowedPackages(nextAllowedPackages)

            if (uiState.value.kioskEnabled) {
                _sideEffects.emit(KioskSideEffect.ApplyPolicy)
            }
        }
    }

    fun onLaunchApp(packageName: String) {
        viewModelScope.launch {
            if (packageName in allowedLaunchablePackages()) {
                _sideEffects.emit(KioskSideEffect.LaunchApp(packageName))
            }
        }
    }

    fun lockTaskPackages(appPackageName: String): Set<String> {
        return setOf(appPackageName) + allowedLaunchablePackages()
    }

    private fun allowedLaunchablePackages(): Set<String> {
        val launchablePackageNames = uiState.value.launchableApps
            .mapTo(mutableSetOf()) { it.packageName }

        return uiState.value.allowedPackages
            .filterTo(mutableSetOf()) { it in launchablePackageNames }
    }

    private suspend fun emitCurrentKioskStateEffect() {
        val kioskEnabled = repository.kioskEnabled.first()

        _sideEffects.emit(
            if (kioskEnabled) {
                KioskSideEffect.StartKiosk
            } else {
                waitingForSystemInit.value = false
                KioskSideEffect.SetKioskInactive
            }
        )
    }

    private companion object {
        const val STATUS_TAP_TO_OPEN_ADMIN = 7

        // Untuk development dulu. Untuk production jangan hardcode PIN seperti ini.
        const val ADMIN_PIN = "123456"
    }
}
