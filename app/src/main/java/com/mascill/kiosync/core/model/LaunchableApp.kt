package com.mascill.kiosync.core.model

import android.graphics.drawable.Drawable

data class LaunchableApp(
    val label: String,
    val packageName: String,
    val icon: Drawable
)
