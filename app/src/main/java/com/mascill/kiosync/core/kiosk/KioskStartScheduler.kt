package com.mascill.kiosync.core.kiosk

import android.os.Handler
import android.os.Looper

/**
 * Schedules delayed kiosk startup on the main thread.
 *
 * The delay prevents Lock Task start attempts from racing Android services immediately after boot.
 */
class KioskStartScheduler(
    private val handler: Handler = Handler(Looper.getMainLooper())
) {

    private var delayedKioskStart: Runnable? = null

    /** Replaces any pending start callback with a new delayed callback. */
    fun schedule(delayMs: Long, onStart: () -> Unit) {
        cancel()

        val runnable = Runnable {
            delayedKioskStart = null
            onStart()
        }

        delayedKioskStart = runnable
        handler.postDelayed(runnable, delayMs)
    }

    /** Cancels any pending delayed kiosk start. */
    fun cancel() {
        delayedKioskStart?.let(handler::removeCallbacks)
        delayedKioskStart = null
    }
}
