package com.mascill.kiosync.core.data.repository

import com.mascill.kiosync.core.data.datasource.LaunchableAppDataSource
import com.mascill.kiosync.core.data.datastore.KioskPreferencesDataSource
import com.mascill.kiosync.core.model.LaunchableApp
import kotlinx.coroutines.flow.Flow

/**
 * Repository implementation that combines persisted settings with installed-app discovery.
 */
class KioskRepositoryImpl(
    private val preferencesDataSource: KioskPreferencesDataSource,
    private val launchableAppDataSource: LaunchableAppDataSource
) : KioskRepository {

    override val kioskEnabled: Flow<Boolean> = preferencesDataSource.kioskEnabled

    override val allowedPackages: Flow<Set<String>> = preferencesDataSource.allowedPackages

    override suspend fun setKioskEnabled(enabled: Boolean) {
        preferencesDataSource.setKioskEnabled(enabled)
    }

    override suspend fun setAllowedPackages(packageNames: Set<String>) {
        preferencesDataSource.setAllowedPackages(packageNames)
    }

    override fun getLaunchableApps(): List<LaunchableApp> {
        return launchableAppDataSource.getLaunchableApps()
    }
}
