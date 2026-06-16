package com.mascill.kiosync.feature.kiosk.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mascill.kiosync.core.data.repository.KioskRepository
import com.mascill.kiosync.feature.kiosk.model.KioskDialogState
import com.mascill.kiosync.feature.kiosk.model.KioskSideEffect
import com.mascill.kiosync.feature.kiosk.model.KioskUiState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Coordinates kiosk UI state, persisted settings, and one-time Android side effects.
 */
class KioskViewModel(
    private val repository: KioskRepository,
    private val appPackageName: String,
    private val kioskStartPlanner: KioskStartPlanner
) : ViewModel() {

    private val waitingForSystemInit = MutableStateFlow(false)
    private val launchableApps = MutableStateFlow(repository.getLaunchableApps())
    private val dialogState = MutableStateFlow(KioskDialogState())
    private val sideEffectChannel = Channel<KioskSideEffect>(Channel.BUFFERED)

    /** One-time commands collected by the host activity/composable. */
    val sideEffects = sideEffectChannel.receiveAsFlow()

    /** Combined screen state consumed by Compose. */
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

    /** Refreshes kiosk side effects when the host activity returns to the foreground. */
    fun onHostResumed() {
        viewModelScope.launch {
            emitCurrentKioskStateEffect()
        }
    }

    /** Continues kiosk startup after the boot grace-period delay finishes. */
    fun onDelayedKioskStartReady() {
        viewModelScope.launch {
            waitingForSystemInit.value = false

            if (repository.kioskEnabled.first()) {
                sendSideEffect(KioskSideEffect.StartKiosk(lockTaskPackages()))
            } else {
                sendSideEffect(KioskSideEffect.SetKioskInactive)
            }
        }
    }

    /** Persists the admin kiosk toggle and emits the matching host command. */
    fun onKioskEnabledChange(enabled: Boolean) {
        viewModelScope.launch {
            repository.setKioskEnabled(enabled)
            waitingForSystemInit.value = false

            sendSideEffect(
                if (enabled) {
                    kioskStartSideEffect()
                } else {
                    KioskSideEffect.StopKiosk
                }
            )
        }
    }

    /** Opens the hidden admin PIN prompt after repeated status taps. */
    fun onStatusTap() {
        statusTapCount++

        if (statusTapCount >= STATUS_TAP_TO_OPEN_ADMIN) {
            statusTapCount = 0
            dialogState.value = dialogState.value.copy(showPinDialog = true)
        }
    }

    /** Accepts only numeric input and limits it to the configured PIN length. */
    fun onPinChange(value: String) {
        dialogState.value = dialogState.value.copy(
            pin = value
                .filter(Char::isDigit)
                .take(ADMIN_PIN.length),
            pinError = false
        )
    }

    /** Validates the admin PIN and opens the settings panel on success. */
    fun confirmPin() {
        if (dialogState.value.pin == ADMIN_PIN) {
            closePinDialog()
            refreshApps()
            dialogState.value = dialogState.value.copy(showAdminPanel = true)
        } else {
            dialogState.value = dialogState.value.copy(pinError = true)
        }
    }

    /** Clears PIN input and hides the PIN dialog. */
    fun closePinDialog() {
        dialogState.value = dialogState.value.copy(
            showPinDialog = false,
            pin = "",
            pinError = false
        )
    }

    /** Hides the admin settings panel. */
    fun closeAdminPanel() {
        dialogState.value = dialogState.value.copy(showAdminPanel = false)
    }

    /** Reloads installed launcher apps from PackageManager. */
    fun refreshApps() {
        launchableApps.value = repository.getLaunchableApps()
    }

    /** Updates the app allowlist and reapplies policy when kiosk mode is already active. */
    fun onAllowedAppChange(packageName: String, allowed: Boolean) {
        viewModelScope.launch {
            val nextAllowedPackages = if (allowed) {
                uiState.value.allowedPackages + packageName
            } else {
                uiState.value.allowedPackages - packageName
            }

            repository.setAllowedPackages(nextAllowedPackages)

            if (uiState.value.kioskEnabled) {
                sendSideEffect(KioskSideEffect.ApplyPolicy(lockTaskPackages(nextAllowedPackages)))
            }
        }
    }

    /** Emits a launch command only for packages currently allowed and still installed. */
    fun onLaunchApp(packageName: String) {
        viewModelScope.launch {
            if (packageName in allowedLaunchablePackages()) {
                sendSideEffect(KioskSideEffect.LaunchApp(packageName))
            }
        }
    }

    /** Reapplies policy when an allowed package can no longer provide a launch intent. */
    fun onLaunchIntentMissing() {
        viewModelScope.launch {
            sendSideEffect(KioskSideEffect.ApplyPolicy(lockTaskPackages()))
        }
    }

    /** Returns the complete Lock Task allowlist, including KioSync itself. */
    fun lockTaskPackages(): Set<String> {
        return lockTaskPackages(uiState.value.allowedPackages)
    }

    /** Combines KioSync with allowed packages that are still launchable. */
    private fun lockTaskPackages(allowedPackages: Set<String>): Set<String> {
        return setOf(appPackageName) + allowedLaunchablePackages(allowedPackages)
    }

    /**
     * Filters persisted package names against the live launcher list to avoid stale allowlist data.
     */
    private fun allowedLaunchablePackages(
        allowedPackages: Set<String> = uiState.value.allowedPackages
    ): Set<String> {
        val launchablePackageNames = uiState.value.launchableApps
            .mapTo(mutableSetOf()) { it.packageName }

        return allowedPackages
            .filterTo(mutableSetOf()) { it in launchablePackageNames }
    }

    /** Emits the correct side effect for the current persisted kiosk setting. */
    private suspend fun emitCurrentKioskStateEffect() {
        val kioskEnabled = repository.kioskEnabled.first()

        sendSideEffect(
            if (kioskEnabled) {
                kioskStartSideEffect()
            } else {
                waitingForSystemInit.value = false
                KioskSideEffect.SetKioskInactive
            }
        )
    }

    /** Sends one-time work to the host without storing it in persistent UI state. */
    private suspend fun sendSideEffect(sideEffect: KioskSideEffect) {
        sideEffectChannel.send(sideEffect)
    }

    /** Creates the correct startup side effect and mirrors its waiting state into the UI. */
    private fun kioskStartSideEffect(): KioskSideEffect {
        val plan = kioskStartPlanner.planStart(lockTaskPackages())
        waitingForSystemInit.value = plan.waitingForSystemInit
        return plan.sideEffect
    }

    private companion object {
        const val STATUS_TAP_TO_OPEN_ADMIN = 7

        // Development-only PIN. Production builds should not hardcode admin credentials.
        const val ADMIN_PIN = "123456"
    }
}
