package com.mascill.kiosync.core.data.repository

import com.mascill.kiosync.core.model.LaunchableApp
import kotlinx.coroutines.flow.Flow

interface KioskRepository {
    val kioskEnabled: Flow<Boolean>

    val allowedPackages: Flow<Set<String>>

    suspend fun setKioskEnabled(enabled: Boolean)

    suspend fun setAllowedPackages(packageNames: Set<String>)

    fun getLaunchableApps(): List<LaunchableApp>
}
