package com.mascill.kiosync.feature.kiosk.model

sealed interface KioskSideEffect {
    data class StartKiosk(val lockTaskPackages: Set<String>) : KioskSideEffect

    data class DelayKioskStart(val delayMs: Long) : KioskSideEffect

    data object StopKiosk : KioskSideEffect

    data object SetKioskInactive : KioskSideEffect

    data class ApplyPolicy(val lockTaskPackages: Set<String>) : KioskSideEffect

    data class LaunchApp(val packageName: String) : KioskSideEffect
}
