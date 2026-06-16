package com.mascill.kiosync.feature.kiosk.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mascill.kiosync.core.model.LaunchableApp

/**
 * Main kiosk home content with status text and the allowed app launcher.
 */
@Composable
fun KioSyncHomeContent(
    waitingForSystemInit: Boolean,
    kioskEnabled: Boolean,
    allowedApps: List<LaunchableApp>,
    onStatusTap: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    // The status label is also the hidden entry point for admin mode.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = when {
                waitingForSystemInit -> "Initializing device..."
                kioskEnabled -> "Kiosk Mode Aktif"
                else -> "Kiosk Mode Nonaktif"
            },
            modifier = Modifier.clickable(onClick = onStatusTap)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (waitingForSystemInit) {
            Text("Menunggu service Android selesai inisialisasi")
            return@Column
        }

        if (kioskEnabled) {
            KioSyncLauncher(
                allowedApps = allowedApps,
                onLaunchApp = onLaunchApp
            )
        } else {
            Spacer(modifier = Modifier.weight(1f))
            Text("Tap tulisan status 7x untuk Admin Mode")
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
