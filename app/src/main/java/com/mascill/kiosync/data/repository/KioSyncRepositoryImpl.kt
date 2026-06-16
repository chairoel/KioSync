package com.mascill.kiosync.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import androidx.core.content.edit
import com.mascill.kiosync.data.model.LaunchableApp

class KioSyncRepositoryImpl(context: Context) : KioSyncRepository {

    private val appContext = context.applicationContext

    override fun isKioskEnabled(): Boolean {
        return appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_KIOSK_ENABLED, false)
    }

    override fun setKioskEnabled(enabled: Boolean) {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { putBoolean(KEY_KIOSK_ENABLED, enabled) }
    }

    override fun getAllowedPackages(): Set<String> {
        return appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getStringSet(KEY_ALLOWED_APP_PACKAGES, emptySet())
            .orEmpty()
            .toSet()
    }

    override fun setAllowedPackages(packageNames: Set<String>) {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit { putStringSet(KEY_ALLOWED_APP_PACKAGES, packageNames) }
    }

    override fun getAllowedLaunchablePackages(): Set<String> {
        val launchablePackages = getLaunchableApps()
            .mapTo(mutableSetOf()) { it.packageName }

        return getAllowedPackages()
            .filterTo(mutableSetOf()) { it in launchablePackages }
    }

    override fun getLaunchableApps(): List<LaunchableApp> {
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

    private fun PackageManager.queryLauncherActivities(intent: Intent): List<ResolveInfo> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            queryIntentActivities(intent, 0)
        }
    }

    private companion object {
        const val PREF_NAME = "kiosync_settings"
        const val KEY_KIOSK_ENABLED = "kiosk_enabled"
        const val KEY_ALLOWED_APP_PACKAGES = "allowed_app_packages"
    }
}
