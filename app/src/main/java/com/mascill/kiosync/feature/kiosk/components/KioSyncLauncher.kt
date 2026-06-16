package com.mascill.kiosync.feature.kiosk.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mascill.kiosync.core.model.LaunchableApp

/**
 * Grid launcher that shows only apps selected by the admin allowlist.
 */
@Composable
fun KioSyncLauncher(
    allowedApps: List<LaunchableApp>,
    onLaunchApp: (String) -> Unit
) {
    Spacer(modifier = Modifier.height(24.dp))

    if (allowedApps.isEmpty()) {
        Spacer(modifier = Modifier.height(120.dp))
        Text("Belum ada aplikasi yang diizinkan")
        Spacer(modifier = Modifier.height(8.dp))
        Text("Tap status 7x untuk Admin Mode")
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 88.dp),
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items = allowedApps,
            key = { it.packageName }
        ) { app ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .clickable {
                        onLaunchApp(app.packageName)
                    }
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LauncherIcon(
                    icon = app.icon,
                    contentDescription = app.label,
                    size = 56.dp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = app.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
