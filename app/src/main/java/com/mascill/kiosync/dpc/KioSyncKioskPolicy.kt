package com.mascill.kiosync.dpc

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import com.mascill.kiosync.MainActivity

object KioSyncKioskPolicy {

    private const val TAG = "KioSyncDPC"

    fun apply(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, KioSyncDeviceAdminReceiver::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.w(TAG, "Cannot apply kiosk policy. App is not Device Owner.")
            return
        }

        try {
            dpm.setLockTaskPackages(admin, arrayOf(context.packageName))
            Log.d(TAG, "Lock Task allowlist applied")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Lock Task packages", e)
        }

        try {
            val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }

            val homeActivity = ComponentName(context, MainActivity::class.java)

            dpm.addPersistentPreferredActivity(
                admin,
                homeFilter,
                homeActivity
            )

            Log.d(TAG, "Persistent HOME activity applied")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set persistent HOME activity", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                dpm.setLockTaskFeatures(
                    admin,
                    DevicePolicyManager.LOCK_TASK_FEATURE_NONE
                )
                Log.d(TAG, "Lock Task features disabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set Lock Task features", e)
            }
        }

        Log.d(TAG, "Kiosk policy applied")
    }

    fun disable(context: Context) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, KioSyncDeviceAdminReceiver::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.w(TAG, "Cannot disable kiosk policy. App is not Device Owner.")
            return
        }

        try {
            dpm.setLockTaskPackages(admin, emptyArray())
            Log.d(TAG, "Lock Task allowlist cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear Lock Task packages", e)
        }

        try {
            dpm.clearPackagePersistentPreferredActivities(
                admin,
                context.packageName
            )
            Log.d(TAG, "Persistent HOME activity cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear persistent preferred activities", e)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                dpm.setStatusBarDisabled(admin, false)
                Log.d(TAG, "Status bar enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable status bar", e)
            }
        }

        try {
            dpm.setKeyguardDisabled(admin, false)
            Log.d(TAG, "Keyguard enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable keyguard", e)
        }

        Log.d(TAG, "Kiosk policy disabled")
    }
}