package com.mascill.kiosync.utils.system

import android.app.Activity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class SystemBarsController(
    private val activity: Activity
) {

    fun hide() {
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    fun show() {
        val windowInsetsController =
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)

        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
}
