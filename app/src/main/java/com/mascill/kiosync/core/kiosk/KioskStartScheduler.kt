package com.mascill.kiosync.core.kiosk

import android.os.Handler
import android.os.Looper

class KioskStartScheduler(
    private val handler: Handler = Handler(Looper.getMainLooper())
) {

    private var delayedKioskStart: Runnable? = null

    fun schedule(delayMs: Long, onStart: () -> Unit) {
        cancel()

        val runnable = Runnable {
            delayedKioskStart = null
            onStart()
        }

        delayedKioskStart = runnable
        handler.postDelayed(runnable, delayMs)
    }

    fun cancel() {
        delayedKioskStart?.let(handler::removeCallbacks)
        delayedKioskStart = null
    }
}
