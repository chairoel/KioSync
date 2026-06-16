package com.mascill.kiosync.utils.kiosk

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.util.Log
import com.mascill.kiosync.utils.dpc.KioSyncKioskPolicy

class KioSyncKioskController(
    private val activity: Activity
) {

    fun applyPolicy() {
        KioSyncKioskPolicy.apply(activity)
    }

    fun disablePolicy() {
        KioSyncKioskPolicy.disable(activity)
    }

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

    fun stopLockTask() {
        try {
            activity.stopLockTask()
            Log.d(TAG, "Lock Task Mode stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Lock Task Mode", e)
        }
    }

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
