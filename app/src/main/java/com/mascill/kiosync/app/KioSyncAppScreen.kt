package com.mascill.kiosync.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mascill.kiosync.core.designsystem.KioSyncTheme
import com.mascill.kiosync.feature.kiosk.screen.KioskScreen
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskViewModel

@Composable
fun KioSyncAppScreen(
    viewModel: KioskViewModel,
    onStartKiosk: (Set<String>) -> Unit,
    onDelayKioskStart: (Long) -> Unit,
    onStopKiosk: () -> Unit,
    onSetKioskInactive: () -> Unit,
    onApplyPolicy: (Set<String>) -> Unit,
    onLaunchApp: (String) -> Unit
) {
    KioskSideEffectHandler(
        sideEffects = viewModel.sideEffects,
        onStartKiosk = onStartKiosk,
        onDelayKioskStart = onDelayKioskStart,
        onStopKiosk = onStopKiosk,
        onSetKioskInactive = onSetKioskInactive,
        onApplyPolicy = onApplyPolicy,
        onLaunchApp = onLaunchApp
    )

    KioSyncTheme {
        val state by viewModel.uiState.collectAsStateWithLifecycle()

        KioskScreen(
            state = state,
            onStatusTap = viewModel::onStatusTap,
            onLaunchApp = viewModel::onLaunchApp,
            onPinChange = viewModel::onPinChange,
            onConfirmPin = viewModel::confirmPin,
            onDismissPin = viewModel::closePinDialog,
            onKioskEnabledChange = viewModel::onKioskEnabledChange,
            onAllowedAppChange = viewModel::onAllowedAppChange,
            onRefreshApps = viewModel::refreshApps,
            onDismissAdminPanel = viewModel::closeAdminPanel
        )
    }
}
