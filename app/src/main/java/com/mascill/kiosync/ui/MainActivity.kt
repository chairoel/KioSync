package com.mascill.kiosync.ui

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mascill.kiosync.di.KioSyncDependencies
import com.mascill.kiosync.ui.screen.KioSyncAppContent
import com.mascill.kiosync.ui.theme.KioSyncTheme
import com.mascill.kiosync.utils.dpc.KioSyncKioskPolicy

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KioSyncDPC"
        private const val BOOT_KIOSK_GRACE_PERIOD_MS = 60_000L
    }

    private val viewModel: MainViewModel by viewModels {
        MainViewModel.Factory(
            repository = KioSyncDependencies.repository(applicationContext)
        )
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var delayedKioskStart: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showSystemBars()
        viewModel.setWaitingForSystemInit(isAutomaticKioskStartDelayed())

        checkDeviceOwnerStatus()

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

        startAutomaticKioskWhenReady()
    }

    override fun onResume() {
        super.onResume()

        if (viewModel.isKioskEnabled()) {
            startAutomaticKioskWhenReady()
        } else {
            cancelDelayedKioskStart()
            showSystemBars()
            Log.d(TAG, "Kiosk disabled, skip startLockTask")
        }
    }

    override fun onDestroy() {
        cancelDelayedKioskStart()
        super.onDestroy()
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
            KioSyncKioskPolicy.apply(this)
        }
    }

    private fun enableKioskMode() {
        Log.d(TAG, "Enable kiosk requested")

        cancelDelayedKioskStart()
        startKioskNow()
    }

    private fun disableKioskMode() {
        Log.d(TAG, "Disable kiosk requested")

        cancelDelayedKioskStart()

        try {
            stopLockTask()
            Log.d(TAG, "Lock Task Mode stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Lock Task Mode", e)
        }

        KioSyncKioskPolicy.disable(this)

        showSystemBars()

        checkDeviceOwnerStatus()

        exitToHome()
    }

    private fun startAutomaticKioskWhenReady() {
        if (!viewModel.isKioskEnabled()) {
            cancelDelayedKioskStart()
            viewModel.setWaitingForSystemInit(false)
            showSystemBars()
            return
        }

        val remainingGracePeriod = remainingBootKioskGracePeriodMs()
        if (remainingGracePeriod > 0L) {
            viewModel.setWaitingForSystemInit(true)
            showSystemBars()
            scheduleDelayedKioskStart(remainingGracePeriod)
            Log.d(TAG, "Delaying kiosk start for ${remainingGracePeriod}ms after boot")
            return
        }

        viewModel.setWaitingForSystemInit(false)
        startKioskNow()
    }

    private fun scheduleDelayedKioskStart(delayMs: Long) {
        cancelDelayedKioskStart()

        delayedKioskStart = Runnable {
            delayedKioskStart = null
            viewModel.setWaitingForSystemInit(false)

            if (viewModel.isKioskEnabled()) {
                startKioskNow()
            } else {
                showSystemBars()
                Log.d(TAG, "Kiosk disabled before delayed start")
            }
        }

        mainHandler.postDelayed(delayedKioskStart!!, delayMs)
    }

    private fun cancelDelayedKioskStart() {
        delayedKioskStart?.let(mainHandler::removeCallbacks)
        delayedKioskStart = null
    }

    private fun startKioskNow() {
        KioSyncKioskPolicy.apply(this)
        hideSystemBars()
        startKioskIfAllowed()
        checkDeviceOwnerStatus()
    }

    private fun isAutomaticKioskStartDelayed(): Boolean {
        return viewModel.isKioskEnabled() && remainingBootKioskGracePeriodMs() > 0L
    }

    private fun remainingBootKioskGracePeriodMs(): Long {
        return (BOOT_KIOSK_GRACE_PERIOD_MS - SystemClock.elapsedRealtime())
            .coerceAtLeast(0L)
    }

    private fun checkDeviceOwnerStatus() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager

        val isDeviceOwner = dpm.isDeviceOwnerApp(packageName)
        val isLockTaskPermitted = dpm.isLockTaskPermitted(packageName)

        Log.d(TAG, "packageName=$packageName")
        Log.d(TAG, "isDeviceOwner=$isDeviceOwner")
        Log.d(TAG, "isLockTaskPermitted=$isLockTaskPermitted")
        Log.d(TAG, "isKioskEnabled=${viewModel.isKioskEnabled()}")
    }

    private fun startKioskIfAllowed() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (dpm.isLockTaskPermitted(packageName)) {
            try {
                startLockTask()
                Log.d(TAG, "Lock Task Mode started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Lock Task Mode", e)
            }
        } else {
            Log.w(TAG, "Lock Task not permitted. App is not allowlisted yet.")
        }
    }

    private fun launchAllowedApp(packageName: String) {
        if (packageName !in viewModel.getAllowedLaunchablePackages()) {
            Log.w(TAG, "Blocked launch for non-allowlisted package: $packageName")
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Log.w(TAG, "No launch intent for package: $packageName")
            KioSyncKioskPolicy.apply(this)
            return
        }

        try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            Log.d(TAG, "Launched allowlisted package: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package: $packageName", e)
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun exitToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            startActivity(homeIntent)
            finishAndRemoveTask()
            Log.d(TAG, "Exited to HOME after kiosk disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to exit to HOME after kiosk disabled", e)
        }
    }
}
