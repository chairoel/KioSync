package com.mascill.kiosync.data.repository

import com.mascill.kiosync.data.model.LaunchableApp

interface KioSyncRepository {
    fun isKioskEnabled(): Boolean

    fun setKioskEnabled(enabled: Boolean)

    fun getAllowedPackages(): Set<String>

    fun setAllowedPackages(packageNames: Set<String>)

    fun getAllowedLaunchablePackages(): Set<String>

    fun getLaunchableApps(): List<LaunchableApp>
}
