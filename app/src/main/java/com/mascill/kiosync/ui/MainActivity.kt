package com.mascill.kiosync.ui

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mascill.kiosync.ui.screen.KioSyncAppContent
import com.mascill.kiosync.ui.theme.KioSyncTheme
import com.mascill.kiosync.ui.viewmodel.MainViewModel
import com.mascill.kiosync.ui.viewmodel.KioSyncViewModelFactory
import com.mascill.kiosync.utils.kiosk.KioSyncKioskController
import com.mascill.kiosync.utils.kiosk.KioskStartScheduler
import com.mascill.kiosync.utils.navigation.AppLauncher
import com.mascill.kiosync.utils.system.SystemBarsController

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KioSyncDPC"
        private const val BOOT_KIOSK_GRACE_PERIOD_MS = 60_000L
    }

    private val viewModel: MainViewModel by viewModels {
        KioSyncViewModelFactory.getInstance(applicationContext)
    }

    private val appLauncher by lazy {
        AppLauncher(applicationContext)
    }

    private val kioskController by lazy {
        KioSyncKioskController(this)
    }

    private val kioskStartScheduler = KioskStartScheduler()

    private val systemBarsController by lazy {
        SystemBarsController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        systemBarsController.show()
        viewModel.setWaitingForSystemInit(isAutomaticKioskStartDelayed())

        logDeviceOwnerStatus()

        renderContent()

        startAutomaticKioskWhenReady()
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.isKioskEnabled()) {
            startAutomaticKioskWhenReady()
        } else {
            kioskStartScheduler.cancel()
            systemBarsController.show()
            Log.d(TAG, "Kiosk disabled, skip startLockTask")
        }
    }

    override fun onDestroy() {
        kioskStartScheduler.cancel()
        super.onDestroy()
    }

    private fun renderContent() {
        setContent {
            KioSyncTheme {
                KioSyncAppContent(
                    state = viewModel.uiState,
                    onStatusTap = viewModel::onStatusTap,
                    onLaunchApp = ::launchAllowedApp,
                    onPinChange = viewModel::onPinChange,
                    onConfirmPin = viewModel::confirmPin,
                    onDismissPin = viewModel::closePinDialog,
                    onKioskEnabledChange = ::setKioskMode,
                    onAllowedAppChange = ::updateAllowedPackage,
                    onRefreshApps = viewModel::refreshApps,
                    onDismissAdminPanel = viewModel::closeAdminPanel
                )
            }
        }
    }

    private fun setKioskMode(enabled: Boolean) {
        viewModel.setKioskEnabled(enabled)

        if (enabled) {
            enableKioskMode()
        } else {
            disableKioskMode()
        }
    }

    private fun updateAllowedPackage(packageName: String, allowed: Boolean) {
        val shouldApplyPolicy = viewModel.updateAllowedPackage(packageName, allowed)

        if (shouldApplyPolicy) {
            kioskController.applyPolicy()
        }
    }

    private fun enableKioskMode() {
        Log.d(TAG, "Enable kiosk requested")

        kioskStartScheduler.cancel()
        startKioskNow()
    }

    private fun disableKioskMode() {
        Log.d(TAG, "Disable kiosk requested")

        kioskStartScheduler.cancel()
        kioskController.stopLockTask()
        kioskController.disablePolicy()

        systemBarsController.show()

        logDeviceOwnerStatus()

        appLauncher.exitToHome(
            onExitedToHome = ::finishAndRemoveTask
        )
    }

    private fun startAutomaticKioskWhenReady() {
        if (!viewModel.isKioskEnabled()) {
            kioskStartScheduler.cancel()
            viewModel.setWaitingForSystemInit(false)
            systemBarsController.show()
            return
        }

        val remainingGracePeriod = remainingBootKioskGracePeriodMs()
        if (remainingGracePeriod > 0L) {
            viewModel.setWaitingForSystemInit(true)
            systemBarsController.show()
            scheduleDelayedKioskStart(remainingGracePeriod)
            Log.d(TAG, "Delaying kiosk start for ${remainingGracePeriod}ms after boot")
            return
        }

        viewModel.setWaitingForSystemInit(false)
        startKioskNow()
    }

    private fun scheduleDelayedKioskStart(delayMs: Long) {
        kioskStartScheduler.schedule(delayMs) {
            viewModel.setWaitingForSystemInit(false)

            if (viewModel.isKioskEnabled()) {
                startKioskNow()
            } else {
                systemBarsController.show()
                Log.d(TAG, "Kiosk disabled before delayed start")
            }
        }
    }

    private fun startKioskNow() {
        kioskController.applyPolicy()
        systemBarsController.hide()
        kioskController.startLockTaskIfAllowed()
        logDeviceOwnerStatus()
    }

    private fun isAutomaticKioskStartDelayed(): Boolean {
        return viewModel.isKioskEnabled() && remainingBootKioskGracePeriodMs() > 0L
    }

    private fun remainingBootKioskGracePeriodMs(): Long {
        return (BOOT_KIOSK_GRACE_PERIOD_MS - SystemClock.elapsedRealtime())
            .coerceAtLeast(0L)
    }

    private fun logDeviceOwnerStatus() {
        kioskController.logDeviceOwnerStatus(
            isKioskEnabled = viewModel.isKioskEnabled()
        )
    }

    private fun launchAllowedApp(packageName: String) {
        if (!viewModel.isAllowedLaunchablePackage(packageName)) {
            Log.w(TAG, "Blocked launch for non-allowlisted package: $packageName")
            return
        }

        appLauncher.launchApp(
            packageName = packageName,
            onLaunchIntentMissing = kioskController::applyPolicy
        )
    }
}
