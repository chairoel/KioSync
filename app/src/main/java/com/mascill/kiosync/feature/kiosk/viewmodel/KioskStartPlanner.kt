package com.mascill.kiosync.feature.kiosk.viewmodel

import com.mascill.kiosync.core.system.ElapsedRealtimeClock
import com.mascill.kiosync.feature.kiosk.model.KioskSideEffect

class KioskStartPlanner(
    private val elapsedRealtimeClock: ElapsedRealtimeClock,
    private val bootKioskGracePeriodMs: Long = BOOT_KIOSK_GRACE_PERIOD_MS
) {

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

    private fun remainingBootKioskGracePeriodMs(): Long {
        return (bootKioskGracePeriodMs - elapsedRealtimeClock.elapsedRealtimeMs())
            .coerceAtLeast(0L)
    }

    private companion object {
        const val BOOT_KIOSK_GRACE_PERIOD_MS = 60_000L
    }
}

data class KioskStartPlan(
    val waitingForSystemInit: Boolean,
    val sideEffect: KioskSideEffect
)
