package com.mascill.kiosync.core.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import com.mascill.kiosync.core.data.datastore.KioskPreferencesDataSource
import com.mascill.kiosync.core.model.LaunchableApp
import kotlinx.coroutines.flow.Flow

class KioskRepositoryImpl(
    context: Context,
    private val preferencesDataSource: KioskPreferencesDataSource
) : KioskRepository {

    private val appContext = context.applicationContext

    override val kioskEnabled: Flow<Boolean> = preferencesDataSource.kioskEnabled

    override val allowedPackages: Flow<Set<String>> = preferencesDataSource.allowedPackages

    override suspend fun setKioskEnabled(enabled: Boolean) {
        preferencesDataSource.setKioskEnabled(enabled)
    }

    override suspend fun setAllowedPackages(packageNames: Set<String>) {
        preferencesDataSource.setAllowedPackages(packageNames)
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
}
