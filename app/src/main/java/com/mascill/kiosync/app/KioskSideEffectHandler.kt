package com.mascill.kiosync.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.mascill.kiosync.feature.kiosk.model.KioskSideEffect
import kotlinx.coroutines.flow.Flow

/**
 * Collects ViewModel side effects and forwards them to host-owned Android operations.
 */
@Composable
fun KioskSideEffectHandler(
    sideEffects: Flow<KioskSideEffect>,
    onStartKiosk: (Set<String>) -> Unit,
    onDelayKioskStart: (Long) -> Unit,
    onStopKiosk: () -> Unit,
    onSetKioskInactive: () -> Unit,
    onApplyPolicy: (Set<String>) -> Unit,
    onLaunchApp: (String) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnStartKiosk by rememberUpdatedState(onStartKiosk)
    val currentOnDelayKioskStart by rememberUpdatedState(onDelayKioskStart)
    val currentOnStopKiosk by rememberUpdatedState(onStopKiosk)
    val currentOnSetKioskInactive by rememberUpdatedState(onSetKioskInactive)
    val currentOnApplyPolicy by rememberUpdatedState(onApplyPolicy)
    val currentOnLaunchApp by rememberUpdatedState(onLaunchApp)

    // Keep callback references fresh while collecting only when the lifecycle is visible enough.
    LaunchedEffect(sideEffects, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            sideEffects.collect { sideEffect ->
                when (sideEffect) {
                    is KioskSideEffect.StartKiosk ->
                        currentOnStartKiosk(sideEffect.lockTaskPackages)

                    is KioskSideEffect.DelayKioskStart ->
                        currentOnDelayKioskStart(sideEffect.delayMs)

                    KioskSideEffect.StopKiosk ->
                        currentOnStopKiosk()

                    KioskSideEffect.SetKioskInactive ->
                        currentOnSetKioskInactive()

                    is KioskSideEffect.ApplyPolicy ->
                        currentOnApplyPolicy(sideEffect.lockTaskPackages)

                    is KioskSideEffect.LaunchApp ->
                        currentOnLaunchApp(sideEffect.packageName)
                }
            }
        }
    }
}
