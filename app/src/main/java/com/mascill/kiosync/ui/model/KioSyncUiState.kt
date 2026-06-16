package com.mascill.kiosync.ui.model

import com.mascill.kiosync.data.model.LaunchableApp

data class KioSyncUiState(
    val waitingForSystemInit: Boolean = false,
    val kioskEnabled: Boolean = false,
    val launchableApps: List<LaunchableApp> = emptyList(),
    val allowedPackages: Set<String> = emptySet(),
    val showPinDialog: Boolean = false,
    val showAdminPanel: Boolean = false,
    val pin: String = "",
    val pinError: Boolean = false
) {
    val allowedApps: List<LaunchableApp>
        get() = launchableApps.filter { it.packageName in allowedPackages }
}