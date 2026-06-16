package com.mascill.kiosync.core.system

import android.os.SystemClock

object SystemElapsedRealtimeClock : ElapsedRealtimeClock {
    override fun elapsedRealtimeMs(): Long = SystemClock.elapsedRealtime()
}
