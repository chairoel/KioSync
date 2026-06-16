package com.mascill.kiosync.feature.kiosk.model

/**
 * One-time commands emitted by the ViewModel for Android APIs that must run outside Compose state.
 */
sealed interface KioskSideEffect {
    /** Apply kiosk policy and enter Lock Task mode. */
    data class StartKiosk(val lockTaskPackages: Set<String>) : KioskSideEffect

    /** Wait before entering kiosk mode so Android services can finish boot initialization. */
    data class DelayKioskStart(val delayMs: Long) : KioskSideEffect

    /** Exit Lock Task mode and remove kiosk policy. */
    data object StopKiosk : KioskSideEffect

    /** Keep the device in normal mode when persisted settings say kiosk is disabled. */
    data object SetKioskInactive : KioskSideEffect

    /** Re-apply device policy after the allowlist changes. */
    data class ApplyPolicy(val lockTaskPackages: Set<String>) : KioskSideEffect

    /** Launch an allowed app from the kiosk launcher. */
    data class LaunchApp(val packageName: String) : KioskSideEffect
}
