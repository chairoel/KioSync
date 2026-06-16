package com.mascill.kiosync.feature.kiosk.screen

import androidx.compose.runtime.Composable
import com.mascill.kiosync.feature.kiosk.components.AdminPanelDialog
import com.mascill.kiosync.feature.kiosk.components.AdminPinDialog
import com.mascill.kiosync.feature.kiosk.components.KioSyncHomeContent
import com.mascill.kiosync.feature.kiosk.model.KioskUiState

/**
 * Kiosk feature screen that decides which dialogs sit on top of the home content.
 */
@Composable
fun KioskScreen(
    state: KioskUiState,
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
