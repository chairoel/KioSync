package com.mascill.kiosync.feature.kiosk.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap

@Composable
fun LauncherIcon(
    icon: Drawable,
    contentDescription: String,
    size: Dp
) {
    val iconSizePx = with(LocalDensity.current) {
        size.roundToPx().coerceAtLeast(1)
    }
    val bitmap = remember(icon, iconSizePx) {
        icon.toBitmap(width = iconSizePx, height = iconSizePx).asImageBitmap()
    }

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = Modifier.size(size)
    )
}
