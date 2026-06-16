package com.mascill.kiosync.app

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.mascill.kiosync.core.kiosk.KioskController
import com.mascill.kiosync.core.kiosk.KioskStartScheduler
import com.mascill.kiosync.core.navigation.AppLauncher
import com.mascill.kiosync.core.system.SystemBarsController
import com.mascill.kiosync.di.Injection
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskViewModel

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KioSyncDPC"
    }

    private val viewModel: KioskViewModel by viewModels {
        Injection.provideKioskViewModelFactory(applicationContext)
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
                onStartKiosk = ::startKiosk,
                onDelayKioskStart = ::delayKioskStart,
                onStopKiosk = ::disableKioskMode,
                onSetKioskInactive = ::setKioskInactive,
                onApplyPolicy = ::applyPolicy,
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

    private fun delayKioskStart(delayMs: Long) {
        systemBarsController.show()
        kioskStartScheduler.schedule(delayMs) {
            viewModel.onDelayedKioskStartReady()
        }
        Log.d(TAG, "Delaying kiosk start for ${delayMs}ms after boot")
    }

    private fun startKiosk(lockTaskPackages: Set<String>) {
        applyPolicy(lockTaskPackages)
        systemBarsController.hide()
        kioskController.startLockTaskIfAllowed()
        logDeviceOwnerStatus()
    }

    private fun applyPolicy(lockTaskPackages: Set<String>) {
        kioskController.applyPolicy(lockTaskPackages = lockTaskPackages)
    }

    private fun logDeviceOwnerStatus() {
        kioskController.logDeviceOwnerStatus(
            isKioskEnabled = viewModel.uiState.value.kioskEnabled
        )
    }

    private fun launchApp(packageName: String) {
        appLauncher.launchApp(
            packageName = packageName,
            onLaunchIntentMissing = viewModel::onLaunchIntentMissing
        )
    }
}
