package com.mascill.kiosync.core.system

import android.os.SystemClock

/**
 * Production elapsed realtime source backed by Android SystemClock.
 */
object SystemElapsedRealtimeClock : ElapsedRealtimeClock {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
}
