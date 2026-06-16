package com.mascill.kiosync

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.core.content.edit

object KioSyncAppAllowlist {

    private const val PREF_NAME = "kiosync_settings"
    private const val KEY_ALLOWED_APP_PACKAGES = "allowed_app_packages"

    data class LaunchableApp(
        val label: String,
        val packageName: String,
        val icon: Drawable
    )

    fun getAllowedPackages(context: Context): Set<String> {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_ALLOWED_APP_PACKAGES, emptySet())
            .orEmpty()
            .toSet()
    }

    fun setAllowedPackages(context: Context, packageNames: Set<String>) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { putStringSet(KEY_ALLOWED_APP_PACKAGES, packageNames) }
    }

    fun getAllowedLaunchablePackages(context: Context): Set<String> {
        val launchablePackages = getLaunchableApps(context)
            .mapTo(mutableSetOf()) { it.packageName }

        return getAllowedPackages(context)
            .filterTo(mutableSetOf()) { it in launchablePackages }
    }

    fun getLaunchableApps(context: Context): List<LaunchableApp> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager.queryLauncherActivities(launcherIntent)
            .asSequence()
            .mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) {
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

    private fun PackageManager.queryLauncherActivities(intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, 0)
        }
    }
}
