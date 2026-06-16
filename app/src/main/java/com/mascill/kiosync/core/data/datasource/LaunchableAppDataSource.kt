package com.mascill.kiosync.core.data.datasource

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.mascill.kiosync.core.model.LaunchableApp

/**
 * Reads installed launcher applications from PackageManager.
 *
 * This class only returns apps that expose a launcher activity, because those are the apps the
 * kiosk home screen can safely present and start.
 */
class LaunchableAppDataSource(
    context: Context
) {

    private val appContext = context.applicationContext

    /**
     * Returns launchable apps sorted by label and excludes KioSync itself from the result.
     */
    fun getLaunchableApps(): List<LaunchableApp> {
        val packageManager = appContext.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager.queryLauncherActivities(launcherIntent)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == appContext.packageName) {
                    return@mapNotNull null
                }

                LaunchableApp(
                    label = resolveInfo.loadLabel(packageManager).toString().takeIf(String::isNotBlank)
                        ?: packageName,
                    packageName = packageName,
                    icon = resolveInfo.loadIcon(packageManager)
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }

    /**
     * Keeps PackageManager querying compatible across Android API levels.
     */
    private fun PackageManager.queryLauncherActivities(intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, 0)
        }
    }
}
