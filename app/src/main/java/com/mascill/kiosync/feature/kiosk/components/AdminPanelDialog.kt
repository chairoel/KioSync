package com.mascill.kiosync.feature.kiosk.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mascill.kiosync.core.model.LaunchableApp

@Composable
fun AdminPanelDialog(
    kioskEnabled: Boolean,
    launchableApps: List<LaunchableApp>,
    allowedPackages: Set<String>,
    onKioskEnabledChange: (Boolean) -> Unit,
    onAllowedAppChange: (String, Boolean) -> Unit,
    onRefreshApps: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pengaturan Kiosk")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (kioskEnabled) {
                            "Kiosk aktif"
                        } else {
                            "Kiosk nonaktif"
                        }
                    )
                    Switch(
                        checked = kioskEnabled,
                        onCheckedChange = onKioskEnabledChange
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Aplikasi yang diizinkan")
                Spacer(modifier = Modifier.height(8.dp))

                if (launchableApps.isEmpty()) {
                    Text("Tidak ada aplikasi launchable")
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = launchableApps,
                            key = { it.packageName }
                        ) { app ->
                            val checked = app.packageName in allowedPackages

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = { allow ->
                                        onAllowedAppChange(app.packageName, allow)
                                    }
                                )

                                LauncherIcon(
                                    icon = app.icon,
                                    contentDescription = app.label,
                                    size = 40.dp
                                )

                                Spacer(modifier = Modifier.size(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = app.label,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = app.packageName,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Tutup")
            }
        },
        dismissButton = {
            TextButton(onClick = onRefreshApps) {
                Text("Muat ulang")
            }
        }
    )
}
