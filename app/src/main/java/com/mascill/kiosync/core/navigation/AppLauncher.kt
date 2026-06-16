package com.mascill.kiosync.core.navigation

import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Starts external apps and navigates back to HOME from an application context.
 */
class AppLauncher(
    context: Context
) {

    private val appContext = context.applicationContext

    /**
     * Launches a package if Android can resolve a launcher intent for it.
     */
    fun launchApp(
        packageName: String,
        onLaunchIntentMissing: () -> Unit
    ) {
        val launchIntent = appContext.packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Log.w(TAG, "No launch intent for package: $packageName")
            onLaunchIntentMissing()
            return
        }

        try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appContext.startActivity(launchIntent)
            Log.d(TAG, "Launched package: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package: $packageName", e)
        }
    }

    /**
     * Sends the user to the current HOME activity after kiosk mode has been disabled.
     */
    fun exitToHome(onExitedToHome: () -> Unit) {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            appContext.startActivity(homeIntent)
            onExitedToHome()
            Log.d(TAG, "Exited to HOME after kiosk disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to exit to HOME after kiosk disabled", e)
        }
    }

    private companion object {
        const val TAG = "KioSyncDPC"
    }
}
