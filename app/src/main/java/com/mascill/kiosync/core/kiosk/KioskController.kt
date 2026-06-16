package com.mascill.kiosync.core.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log

/**
 * Activity-scoped bridge for starting and stopping Android Lock Task mode.
 */
class KioskController(
    private val activity: Activity
) {

    /** Applies Device Owner kiosk policy for the current allowlist. */
    fun applyPolicy(lockTaskPackages: Set<String>) {
        KioSyncKioskPolicy.apply(
            context = activity,
            lockTaskPackages = lockTaskPackages
        )
    }

    /** Removes Device Owner kiosk policy. */
    fun disablePolicy() {
        KioSyncKioskPolicy.disable(activity)
    }

    /**
     * Starts Lock Task only after DevicePolicyManager confirms this package is allowlisted.
     */
    fun startLockTaskIfAllowed() {
        val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (dpm.isLockTaskPermitted(activity.packageName)) {
            try {
                activity.startLockTask()
                Log.d(TAG, "Lock Task Mode started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Lock Task Mode", e)
            }
        } else {
            Log.w(TAG, "Lock Task not permitted. App is not allowlisted yet.")
        }
    }

    /** Stops Lock Task mode if the activity is currently pinned by kiosk mode. */
    fun stopLockTask() {
        try {
            activity.stopLockTask()
            Log.d(TAG, "Lock Task Mode stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Lock Task Mode", e)
        }
    }

    /** Logs Device Owner and Lock Task status for provisioning/debugging sessions. */
    fun logDeviceOwnerStatus(isKioskEnabled: Boolean) {
        val dpm = activity.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        val isDeviceOwner = dpm.isDeviceOwnerApp(activity.packageName)
        val isLockTaskPermitted = dpm.isLockTaskPermitted(activity.packageName)

        Log.d(TAG, "packageName=${activity.packageName}")
        Log.d(TAG, "isDeviceOwner=$isDeviceOwner")
        Log.d(TAG, "isLockTaskPermitted=$isLockTaskPermitted")
        Log.d(TAG, "isKioskEnabled=$isKioskEnabled")
    }

    private companion object {
        const val TAG = "KioSyncDPC"
    }
}
