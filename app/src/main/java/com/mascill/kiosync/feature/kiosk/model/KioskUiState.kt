package com.mascill.kiosync.feature.kiosk.model

import com.mascill.kiosync.core.model.LaunchableApp

/**
 * Complete render state for the kiosk feature.
 *
 * The UI reads this immutable snapshot and sends user events back to the ViewModel instead of
 * directly mutating preferences or Android kiosk APIs.
 */
data class KioskUiState(
    /** True while the app is intentionally delaying Lock Task start after device boot. */
    val waitingForSystemInit: Boolean = false,
    /** Persisted setting that controls whether kiosk mode should be active. */
    val kioskEnabled: Boolean = false,
    /** All installed apps that expose a launcher activity. */
    val launchableApps: List<LaunchableApp> = emptyList(),
    /** Persisted package names that the admin selected for kiosk access. */
    val allowedPackages: Set<String> = emptySet(),
    /** Whether the admin PIN dialog should be shown. */
    val showPinDialog: Boolean = false,
    /** Whether the admin settings panel should be shown. */
    val showAdminPanel: Boolean = false,
    /** Current PIN input shown in the admin dialog. */
    val pin: String = "",
    /** True when the current PIN input should be displayed as invalid. */
    val pinError: Boolean = false
) {
    /** Launchable apps filtered to the persisted allowlist. */
    val allowedApps: List<LaunchableApp>
        get() = launchableApps.filter { it.packageName in allowedPackages }
}
