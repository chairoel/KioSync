package com.mascill.kiosync.app

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.kiosync.core.designsystem.KioSyncTheme
import com.mascill.kiosync.core.kiosk.KioskController
import com.mascill.kiosync.core.kiosk.KioskStartScheduler
import com.mascill.kiosync.core.navigation.AppLauncher
import com.mascill.kiosync.core.system.SystemBarsController
import com.mascill.kiosync.feature.kiosk.model.KioskSideEffect
import com.mascill.kiosync.feature.kiosk.screen.KioskScreen
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskViewModel
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskViewModelFactory
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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

        renderContent()
        collectSideEffects()
        collectKioskEnabled()
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.uiState.value.kioskEnabled) {
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
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                KioskScreen(
                    state = state,
                    onStatusTap = viewModel::onStatusTap,
                    onLaunchApp = viewModel::onLaunchApp,
                    onPinChange = viewModel::onPinChange,
                    onConfirmPin = viewModel::confirmPin,
                    onDismissPin = viewModel::closePinDialog,
                    onKioskEnabledChange = viewModel::onKioskEnabledChange,
                    onAllowedAppChange = viewModel::onAllowedAppChange,
                    onRefreshApps = viewModel::refreshApps,
                    onDismissAdminPanel = viewModel::closeAdminPanel
                )
            }
        }
    }

    private fun collectSideEffects() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.sideEffects.collect { sideEffect ->
                    when (sideEffect) {
                        KioskSideEffect.StartKiosk -> startKioskNow()
                        KioskSideEffect.StopKiosk -> disableKioskMode()
                        KioskSideEffect.ApplyPolicy -> applyCurrentPolicy()
                        is KioskSideEffect.LaunchApp -> launchApp(sideEffect.packageName)
                    }
                }
            }
        }
    }

    private fun collectKioskEnabled() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .map { it.kioskEnabled }
                    .distinctUntilChanged()
                    .collect { kioskEnabled ->
                        if (kioskEnabled) {
                            startAutomaticKioskWhenReady()
                        } else {
                            kioskStartScheduler.cancel()
                            viewModel.setWaitingForSystemInit(false)
                            systemBarsController.show()
                        }
                    }
            }
        }
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

            if (viewModel.uiState.value.kioskEnabled) {
                startKioskNow()
            } else {
                systemBarsController.show()
                Log.d(TAG, "Kiosk disabled before delayed start")
            }
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
