package com.mascill.kiosync.feature.kiosk.model

sealed interface KioskSideEffect {
    data object StartKiosk : KioskSideEffect

    data object StopKiosk : KioskSideEffect

    data object SetKioskInactive : KioskSideEffect

    data object ApplyPolicy : KioskSideEffect

    data class LaunchApp(val packageName: String) : KioskSideEffect
}
