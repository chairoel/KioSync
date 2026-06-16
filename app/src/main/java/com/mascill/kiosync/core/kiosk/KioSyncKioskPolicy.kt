package com.mascill.kiosync.core.kiosk

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.util.Log
import com.mascill.kiosync.core.dpc.KioSyncDeviceAdminReceiver

/**
 * Applies and removes Device Owner policies needed for KioSync kiosk mode.
 *
 * All calls are defensive because DevicePolicyManager APIs throw when the app is not the device
 * owner or when the device is in an unexpected provisioning state.
 */
object KioSyncKioskPolicy {

    private const val TAG = "KioSyncDPC"
    private const val KIOSK_HOME_ACTIVITY = "KioskHomeActivity"

    /**
     * Applies all Device Owner policy required before Lock Task mode can start.
     */
    fun apply(
        context: Context,
        lockTaskPackages: Set<String> = emptySet()
    ) {
        val dpm = context.devicePolicyManager()
        val admin = ComponentName(context, KioSyncDeviceAdminReceiver::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.w(TAG, "Cannot apply kiosk policy. App is not Device Owner.")
            return
        }

        setKioskHomeEnabled(context, true)

        applyLockTaskAllowlist(
            context = context,
            dpm = dpm,
            admin = admin,
            lockTaskPackages = lockTaskPackages
        )
        applyPersistentHomeActivity(
            context = context,
            dpm = dpm,
            admin = admin
        )
        applyLockTaskFeatures(
            dpm = dpm,
            admin = admin
        )

        Log.d(TAG, "Kiosk policy applied")
    }

    /**
     * Removes kiosk policy and restores system surfaces that were controlled by Device Owner APIs.
     */
    fun disable(context: Context) {
        val dpm = context.devicePolicyManager()
        val admin = ComponentName(context, KioSyncDeviceAdminReceiver::class.java)

        if (!dpm.isDeviceOwnerApp(context.packageName)) {
            Log.w(TAG, "Cannot disable kiosk policy. App is not Device Owner.")
            return
        }

        clearLockTaskAllowlist(
            dpm = dpm,
            admin = admin
        )
        clearPersistentHomeActivity(
            context = context,
            dpm = dpm,
            admin = admin
        )

        setKioskHomeEnabled(context, false)

        enableStatusBar(
            dpm = dpm,
            admin = admin
        )
        enableKeyguard(
            dpm = dpm,
            admin = admin
        )

        Log.d(TAG, "Kiosk policy disabled")
    }

    /**
     * Sets the exact packages allowed to run while Lock Task mode is active.
     */
    private fun applyLockTaskAllowlist(
        context: Context,
        dpm: DevicePolicyManager,
        admin: ComponentName,
        lockTaskPackages: Set<String>
    ) {
        runPolicyAction(
            errorMessage = "Failed to set Lock Task packages"
        ) {
            val strictLockTaskPackages = getStrictLockTaskPackages(
                context = context,
                allowedPackages = lockTaskPackages
            )

            dpm.setLockTaskPackages(admin, strictLockTaskPackages)
            Log.d(TAG, "Strict Lock Task allowlist applied: ${strictLockTaskPackages.joinToString()}")
        }
    }

    /**
     * Makes the kiosk home activity handle HOME so users return to KioSync instead of a launcher.
     */
    private fun applyPersistentHomeActivity(
        context: Context,
        dpm: DevicePolicyManager,
        admin: ComponentName
    ) {
        runPolicyAction(
            errorMessage = "Failed to set persistent HOME activity"
        ) {
            dpm.addPersistentPreferredActivity(
                admin,
                homeIntentFilter(),
                kioskHomeComponent(context)
            )

            Log.d(TAG, "Persistent HOME activity applied")
        }
    }

    /**
     * Restricts Lock Task system affordances to the minimum needed for a home-style kiosk shell.
     */
    private fun applyLockTaskFeatures(
        dpm: DevicePolicyManager,
        admin: ComponentName
    ) {
        runPolicyAction(
            errorMessage = "Failed to set Lock Task features"
        ) {
            dpm.setLockTaskFeatures(
                admin,
                DevicePolicyManager.LOCK_TASK_FEATURE_HOME
            )
            Log.d(TAG, "Strict Lock Task features applied")
        }
    }

    /** Clears the Lock Task allowlist when kiosk mode is disabled. */
    private fun clearLockTaskAllowlist(
        dpm: DevicePolicyManager,
        admin: ComponentName
    ) {
        runPolicyAction(
            errorMessage = "Failed to clear Lock Task packages"
        ) {
            dpm.setLockTaskPackages(admin, emptyArray())
            Log.d(TAG, "Lock Task allowlist cleared")
        }
    }

    /** Removes the persistent HOME handler so Android can return to the normal launcher. */
    private fun clearPersistentHomeActivity(
        context: Context,
        dpm: DevicePolicyManager,
        admin: ComponentName
    ) {
        runPolicyAction(
            errorMessage = "Failed to clear persistent preferred activities"
        ) {
            dpm.clearPackagePersistentPreferredActivities(
                admin,
                context.packageName
            )
            Log.d(TAG, "Persistent HOME activity cleared")
        }
    }

    /** Re-enables the status bar after leaving kiosk mode. */
    private fun enableStatusBar(
        dpm: DevicePolicyManager,
        admin: ComponentName
    ) {
        runPolicyAction(
            errorMessage = "Failed to enable status bar"
        ) {
            dpm.setStatusBarDisabled(admin, false)
            Log.d(TAG, "Status bar enabled")
        }
    }

    /** Re-enables the keyguard after leaving kiosk mode. */
    private fun enableKeyguard(
        dpm: DevicePolicyManager,
        admin: ComponentName
    ) {
        runPolicyAction(
            errorMessage = "Failed to enable keyguard"
        ) {
            dpm.setKeyguardDisabled(admin, false)
            Log.d(TAG, "Keyguard enabled")
        }
    }

    /** ComponentName for the manifest alias that acts as kiosk HOME. */
    private fun kioskHomeComponent(context: Context): ComponentName {
        return ComponentName(context.packageName, "${context.packageName}.$KIOSK_HOME_ACTIVITY")
    }

    /** Intent filter that matches Android's HOME resolution flow. */
    private fun homeIntentFilter(): IntentFilter {
        return IntentFilter(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addCategory(Intent.CATEGORY_DEFAULT)
        }
    }

    /**
     * Enables or disables the kiosk HOME alias without killing the running process.
     */
    private fun setKioskHomeEnabled(context: Context, enabled: Boolean) {
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        runPolicyAction(
            errorMessage = "Failed to set Kiosk HOME alias enabled=$enabled"
        ) {
            context.packageManager.setComponentEnabledSetting(
                kioskHomeComponent(context),
                state,
                PackageManager.DONT_KILL_APP
            )
            Log.d(TAG, "Kiosk HOME alias enabled=$enabled")
        }
    }

    /**
     * Always includes KioSync itself so it can remain active while other packages are allowlisted.
     */
    private fun getStrictLockTaskPackages(
        context: Context,
        allowedPackages: Set<String>
    ): Array<String> {
        return (setOf(context.packageName) + allowedPackages)
            .distinct()
            .toTypedArray()
    }

    private fun Context.devicePolicyManager(): DevicePolicyManager {
        return getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    }

    /**
     * Keeps policy application resilient to partial failures and logs enough context for debugging.
     */
    private inline fun runPolicyAction(
        errorMessage: String,
        action: () -> Unit
    ) {
        try {
            action()
        } catch (e: Exception) {
            Log.e(TAG, errorMessage, e)
        }
    }
}
