package com.mascill.kiosync.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.mascill.kiosync.core.designsystem.KioSyncTheme
import com.mascill.kiosync.feature.kiosk.model.KioskSideEffect
import com.mascill.kiosync.feature.kiosk.screen.KioskScreen
import com.mascill.kiosync.feature.kiosk.viewmodel.KioskViewModel
import kotlinx.coroutines.flow.collect

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
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.sideEffects.collect { sideEffect ->
                when (sideEffect) {
                    is KioskSideEffect.StartKiosk -> onStartKiosk(sideEffect.lockTaskPackages)
                    is KioskSideEffect.DelayKioskStart -> onDelayKioskStart(sideEffect.delayMs)
                    KioskSideEffect.StopKiosk -> onStopKiosk()
                    KioskSideEffect.SetKioskInactive -> onSetKioskInactive()
                    is KioskSideEffect.ApplyPolicy -> onApplyPolicy(sideEffect.lockTaskPackages)
                    is KioskSideEffect.LaunchApp -> onLaunchApp(sideEffect.packageName)
                }
            }
        }
    }

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
