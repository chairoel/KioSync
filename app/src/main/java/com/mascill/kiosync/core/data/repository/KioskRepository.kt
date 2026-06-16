package com.mascill.kiosync.core.data.repository

import com.mascill.kiosync.core.model.LaunchableApp
import kotlinx.coroutines.flow.Flow

/**
 * Single data contract for kiosk state and installed-app discovery.
 */
interface KioskRepository {
    /** Persisted flag that says whether kiosk mode should be enabled. */
    val kioskEnabled: Flow<Boolean>

    /** Persisted package names selected by the admin. */
    val allowedPackages: Flow<Set<String>>

    /** Updates the desired kiosk mode state. */
    suspend fun setKioskEnabled(enabled: Boolean)

    /** Replaces the selected package allowlist. */
    suspend fun setAllowedPackages(packageNames: Set<String>)

    /** Reads currently installed apps that can be launched from the kiosk home screen. */
    fun getLaunchableApps(): List<LaunchableApp>
}
