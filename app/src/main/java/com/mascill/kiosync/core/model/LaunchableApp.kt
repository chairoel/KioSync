package com.mascill.kiosync.core.model

import android.graphics.drawable.Drawable

/**
 * App entry that can be shown in the kiosk launcher.
 *
 * The icon is kept as a Drawable because it comes directly from PackageManager and is converted
 * to Compose image data only when the UI renders it.
 */
data class LaunchableApp(
    /** Human-readable app name shown to the user. */
    val label: String,
    /** Android package name used for allowlisting and launching the app. */
    val packageName: String,
    /** Launcher icon loaded from the installed application. */
    val icon: Drawable
)
