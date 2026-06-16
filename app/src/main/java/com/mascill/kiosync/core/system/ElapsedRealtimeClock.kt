package com.mascill.kiosync.core.system

/**
 * Time source based on elapsed realtime so boot-delay logic can be tested deterministically.
 */
interface ElapsedRealtimeClock {
    /** Milliseconds since boot, including time spent in sleep. */
    fun elapsedRealtimeMs(): Long
}
