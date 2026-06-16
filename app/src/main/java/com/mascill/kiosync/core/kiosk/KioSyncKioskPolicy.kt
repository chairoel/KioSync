package com.mascill.kiosync.core.kiosk

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import com.mascill.kiosync.core.dpc.KioSyncDeviceAdminReceiver

object KioSyncKioskPolicy {

    private const val TAG = "KioSyncDPC"
    private const val KIOSK_HOME_ACTIVITY = "KioskHomeActivity"

    fun apply(
        context: Context,
        lockTaskPackages: Set<String> = emptySet()
    ) {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val admin = ComponentName(context, KioSyncDeviceAdminReceiver::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.w(TAG, "Cannot apply kiosk policy. App is not Device Owner.")
            return
        }

        setKioskHomeEnabled(context, true)

        try {
            val strictLockTaskPackages = getStrictLockTaskPackages(
                context = context,
                allowedPackages = lockTaskPackages
            )
            dpm.setLockTaskPackages(admin, strictLockTaskPackages)
            Log.d(TAG, "Strict Lock Task allowlist applied: ${strictLockTaskPackages.joinToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Lock Task packages", e)
        }

        try {
            val homeFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }

            dpm.addPersistentPreferredActivity(
                admin,
                homeFilter,
                kioskHomeComponent(context)
            )

            Log.d(TAG, "Persistent HOME activity applied")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set persistent HOME activity", e)
        }

        try {
            dpm.setLockTaskFeatures(
                admin,
                DevicePolicyManager.LOCK_TASK_FEATURE_HOME
            )
            Log.d(TAG, "Strict Lock Task features applied")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Lock Task features", e)
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

        setKioskHomeEnabled(context, false)

        try {
            dpm.setStatusBarDisabled(admin, false)
            Log.d(TAG, "Status bar enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable status bar", e)
        }

        try {
            dpm.setKeyguardDisabled(admin, false)
            Log.d(TAG, "Keyguard enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable keyguard", e)
        }

        Log.d(TAG, "Kiosk policy disabled")
    }

    private fun kioskHomeComponent(context: Context): ComponentName {
        return ComponentName(context.packageName, "${context.packageName}.$KIOSK_HOME_ACTIVITY")
    }

    private fun setKioskHomeEnabled(context: Context, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        try {
            context.packageManager.setComponentEnabledSetting(
                kioskHomeComponent(context),
                state,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "Kiosk HOME alias enabled=$enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set Kiosk HOME alias enabled=$enabled", e)
        }
    }

    private fun getStrictLockTaskPackages(
        context: Context,
        allowedPackages: Set<String>
    ): Array<String> {
        return (setOf(context.packageName) + allowedPackages)
            .distinct()
            .toTypedArray()
    }
}
