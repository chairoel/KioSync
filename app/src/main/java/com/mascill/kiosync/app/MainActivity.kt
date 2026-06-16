package com.mascill.kiosync.app

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mascill.kiosync.core.kiosk.KioskController
import com.mascill.kiosync.core.kiosk.KioskStartScheduler
import com.mascill.kiosync.core.navigation.AppLauncher
import com.mascill.kiosync.core.system.SystemBarsController
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskViewModel
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskViewModelFactory

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KioSyncDPC"
        private const val BOOT_KIOSK_GRACE_PERIOD_MS = 60_000L
    }

    private val viewModel: KioskViewModel by viewModels {
        KioskViewModelFactory.getInstance(applicationContext)
    }

    private val appLauncher by lazy {
        AppLauncher(applicationContext)
    }

    private val kioskController by lazy {
        KioskController(this)
    }

    private val kioskStartScheduler = KioskStartScheduler()

    private val systemBarsController by lazy {
        SystemBarsController(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        systemBarsController.show()
        logDeviceOwnerStatus()

        setContent {
            KioSyncAppScreen(
                viewModel = viewModel,
                onStartKiosk = ::startKioskWhenReady,
                onStopKiosk = ::disableKioskMode,
                onSetKioskInactive = ::setKioskInactive,
                onApplyPolicy = ::applyCurrentPolicy,
                onLaunchApp = ::launchApp
            )
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onHostResumed()
    }

    override fun onDestroy() {
        kioskStartScheduler.cancel()
        super.onDestroy()
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

    private fun setKioskInactive() {
        kioskStartScheduler.cancel()
        systemBarsController.show()
        Log.d(TAG, "Kiosk disabled, skip startLockTask")
    }

    private fun startKioskWhenReady() {
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
            viewModel.onDelayedKioskStartReady()
        }
    }

    private fun startKioskNow() {
        applyCurrentPolicy()
        systemBarsController.hide()
        kioskController.startLockTaskIfAllowed()
        logDeviceOwnerStatus()
    }

    private fun applyCurrentPolicy() {
        kioskController.applyPolicy(
            lockTaskPackages = viewModel.lockTaskPackages(packageName)
        )
    }

    private fun remainingBootKioskGracePeriodMs(): Long {
        return (BOOT_KIOSK_GRACE_PERIOD_MS - SystemClock.elapsedRealtime())
            .coerceAtLeast(0L)
    }

    private fun logDeviceOwnerStatus() {
        kioskController.logDeviceOwnerStatus(
            isKioskEnabled = viewModel.uiState.value.kioskEnabled
        )
    }

    private fun launchApp(packageName: String) {
        appLauncher.launchApp(
            packageName = packageName,
            onLaunchIntentMissing = ::applyCurrentPolicy
        )
    }
}
