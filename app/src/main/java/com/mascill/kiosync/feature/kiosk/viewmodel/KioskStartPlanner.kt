package com.mascill.kiosync.feature.kiosk.viewmodel

import com.mascill.kiosync.core.system.ElapsedRealtimeClock
import com.mascill.kiosync.feature.kiosk.model.KioskSideEffect

/**
 * Decides whether kiosk mode can start immediately or should wait after boot.
 */
class KioskStartPlanner(
    private val elapsedRealtimeClock: ElapsedRealtimeClock,
    private val bootKioskGracePeriodMs: Long = BOOT_KIOSK_GRACE_PERIOD_MS
) {

    /**
     * Builds the next kiosk-start side effect based on the remaining boot grace period.
     */
    fun planStart(lockTaskPackages: Set<String>): KioskStartPlan {
        val remainingGracePeriodMs = remainingBootKioskGracePeriodMs()

        return if (remainingGracePeriodMs > 0L) {
            KioskStartPlan(
                waitingForSystemInit = true,
                sideEffect = KioskSideEffect.DelayKioskStart(remainingGracePeriodMs)
            )
        } else {
            KioskStartPlan(
                waitingForSystemInit = false,
                sideEffect = KioskSideEffect.StartKiosk(lockTaskPackages)
            )
        }
    }

    /** Returns the remaining startup delay needed before Lock Task mode should be requested. */
    private fun remainingBootKioskGracePeriodMs(): Long {
        return (bootKioskGracePeriodMs - elapsedRealtimeClock.elapsedRealtimeMs())
            .coerceAtLeast(0L)
    }

    private companion object {
        const val BOOT_KIOSK_GRACE_PERIOD_MS = 60_000L
    }
}

/**
 * Result of kiosk startup planning.
 */
data class KioskStartPlan(
    /** True when UI should show the initialization state while a delayed start is pending. */
    val waitingForSystemInit: Boolean,
    /** Side effect the host should execute next. */
    val sideEffect: KioskSideEffect
)
