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

/**
 * Main host activity for the kiosk shell.
 *
 * Compose renders the screen, while this activity owns Android operations that require an Activity
 * instance, such as Lock Task mode and system bar visibility.
 */
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

    /** Initializes controllers and renders the root kiosk screen. */
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

    /** Lets the ViewModel re-evaluate kiosk state whenever the activity becomes active again. */
    override fun onResume() {
        super.onResume()
        viewModel.onHostResumed()
    }

    /** Cancels delayed kiosk startup to avoid callbacks after the activity is destroyed. */
    override fun onDestroy() {
        kioskStartScheduler.cancel()
        super.onDestroy()
    }

    /** Fully exits kiosk mode and returns the user to the normal HOME flow. */
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

    /** Keeps the app in normal mode when persisted settings say kiosk is disabled. */
    private fun setKioskInactive() {
        kioskStartScheduler.cancel()
        systemBarsController.show()
        Log.d(TAG, "Kiosk disabled, skip startLockTask")
    }

    /** Defers Lock Task startup during the Android boot grace period. */
    private fun delayKioskStart(delayMs: Long) {
        systemBarsController.show()
        kioskStartScheduler.schedule(delayMs) {
            viewModel.onDelayedKioskStartReady()
        }
        Log.d(TAG, "Delaying kiosk start for ${delayMs}ms after boot")
    }

    /** Applies policy, hides system bars, and requests Lock Task mode. */
    private fun startKiosk(lockTaskPackages: Set<String>) {
        applyPolicy(lockTaskPackages)
        systemBarsController.hide()
        kioskController.startLockTaskIfAllowed()
        logDeviceOwnerStatus()
    }

    /** Reapplies Device Owner policy for the current allowlist. */
    private fun applyPolicy(lockTaskPackages: Set<String>) {
        kioskController.applyPolicy(lockTaskPackages = lockTaskPackages)
    }

    /** Writes Device Owner diagnostics to Logcat. */
    private fun logDeviceOwnerStatus() {
        kioskController.logDeviceOwnerStatus(
            isKioskEnabled = viewModel.uiState.value.kioskEnabled
        )
    }

    /** Launches an allowed app and lets the ViewModel recover if the intent is missing. */
    private fun launchApp(packageName: String) {
        appLauncher.launchApp(
            packageName = packageName,
            onLaunchIntentMissing = viewModel::onLaunchIntentMissing
        )
    }
}
