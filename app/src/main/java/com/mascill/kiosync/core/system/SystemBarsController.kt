package com.mascill.kiosync.core.system

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Controls system bar visibility for the current activity.
 */
class SystemBarsController(
    private val activity: Activity
) {

    /** Hides system bars while still allowing transient swipe access. */
    fun hide() {
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    /** Shows system bars for normal/admin flows. */
    fun show() {
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)

        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}
