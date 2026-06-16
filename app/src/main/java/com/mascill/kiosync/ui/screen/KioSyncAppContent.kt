package com.mascill.kiosync.ui.screen

import androidx.compose.runtime.Composable
import com.mascill.kiosync.ui.model.KioSyncUiState
import com.mascill.kiosync.ui.components.AdminPanelDialog
import com.mascill.kiosync.ui.components.AdminPinDialog
import com.mascill.kiosync.ui.components.KioSyncHomeContent

@Composable
fun KioSyncAppContent(
    state: KioSyncUiState,
    onStatusTap: () -> Unit,
    onLaunchApp: (String) -> Unit,
    onPinChange: (String) -> Unit,
    onConfirmPin: () -> Unit,
    onDismissPin: () -> Unit,
    onKioskEnabledChange: (Boolean) -> Unit,
    onAllowedAppChange: (String, Boolean) -> Unit,
    onRefreshApps: () -> Unit,
    onDismissAdminPanel: () -> Unit
) {
    KioSyncHomeContent(
        waitingForSystemInit = state.waitingForSystemInit,
        kioskEnabled = state.kioskEnabled,
        allowedApps = state.allowedApps,
        onStatusTap = onStatusTap,
        onLaunchApp = onLaunchApp
    )

    if (state.showPinDialog) {
        AdminPinDialog(
            pin = state.pin,
            pinError = state.pinError,
            onPinChange = onPinChange,
            onConfirm = onConfirmPin,
            onDismiss = onDismissPin
        )
    }

    if (state.showAdminPanel) {
        AdminPanelDialog(
            kioskEnabled = state.kioskEnabled,
            launchableApps = state.launchableApps,
            allowedPackages = state.allowedPackages,
            onKioskEnabledChange = onKioskEnabledChange,
            onAllowedAppChange = onAllowedAppChange,
            onRefreshApps = onRefreshApps,
            onDismiss = onDismissAdminPanel
        )
    }
}
