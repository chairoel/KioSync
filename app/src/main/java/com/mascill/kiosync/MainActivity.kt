package com.mascill.kiosync

import android.app.admin.DevicePolicyManager
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mascill.kiosync.dpc.KioSyncKioskPolicy

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KioSyncDPC"

        private const val PREF_NAME = "kiosync_settings"
        private const val KEY_KIOSK_ENABLED = "kiosk_enabled"
        private const val BOOT_KIOSK_GRACE_PERIOD_MS = 60_000L

        // Untuk development dulu.
        // Untuk production jangan hardcode PIN seperti ini.
        private const val ADMIN_PIN = "123456"
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var delayedKioskStart: Runnable? = null
    private var waitingForSystemInit by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        showSystemBars()
        waitingForSystemInit = isAutomaticKioskStartDelayed()

        checkDeviceOwnerStatus()

        setContent {
            MaterialTheme {
                var kioskEnabled by remember {
                    mutableStateOf(isKioskEnabled())
                }

                val launchableApps = remember {
                    mutableStateOf(KioSyncAppAllowlist.getLaunchableApps(this))
                }

                var allowedPackages by remember {
                    mutableStateOf(KioSyncAppAllowlist.getAllowedPackages(this))
                }

                val tapCount = remember {
                    mutableIntStateOf(0)
                }

                var showPinDialog by remember {
                    mutableStateOf(false)
                }

                val showAdminPanel = remember {
                    mutableStateOf(false)
                }

                val pin = remember {
                    mutableStateOf("")
                }

                val pinError = remember {
                    mutableStateOf(false)
                }

                fun closePinDialog() {
                    showPinDialog = false
                    pin.value = ""
                    pinError.value = false
                }

                fun openAdminPanel() {
                    closePinDialog()
                    launchableApps.value = KioSyncAppAllowlist.getLaunchableApps(this)
                    allowedPackages = KioSyncAppAllowlist.getAllowedPackages(this)
                    showAdminPanel.value = true
                }

                fun setKioskMode(enabled: Boolean) {
                    if (enabled) {
                        enableKioskMode()
                    } else {
                        disableKioskMode()
                    }

                    kioskEnabled = enabled
                }

                fun updateAllowedPackage(packageName: String, allowed: Boolean) {
                    val nextAllowedPackages = if (allowed) {
                        allowedPackages + packageName
                    } else {
                        allowedPackages - packageName
                    }

                    allowedPackages = nextAllowedPackages
                    KioSyncAppAllowlist.setAllowedPackages(this, nextAllowedPackages)

                    if (kioskEnabled) {
                        KioSyncKioskPolicy.apply(this)
                    }
                }

                KioSyncHomeContent(
                    waitingForSystemInit = waitingForSystemInit,
                    kioskEnabled = kioskEnabled,
                    allowedApps = launchableApps.value.filter { it.packageName in allowedPackages },
                    onStatusTap = {
                        tapCount.intValue++

                        if (tapCount.intValue >= 7) {
                            tapCount.intValue = 0
                            showPinDialog = true
                        }
                    },
                    onLaunchApp = ::launchAllowedApp
                )

                if (showPinDialog) {
                    AdminPinDialog(
                        pin = pin.value,
                        pinError = pinError.value,
                        onPinChange = {
                            pin.value = it
                                .filter(Char::isDigit)
                                .take(ADMIN_PIN.length)
                            pinError.value = false
                        },
                        onConfirm = {
                            if (pin.value == ADMIN_PIN) {
                                openAdminPanel()
                            } else {
                                pinError.value = true
                            }
                        },
                        onDismiss = {
                            closePinDialog()
                        }
                    )
                }

                if (showAdminPanel.value) {
                    AdminPanelDialog(
                        kioskEnabled = kioskEnabled,
                        launchableApps = launchableApps.value,
                        allowedPackages = allowedPackages,
                        onKioskEnabledChange = ::setKioskMode,
                        onAllowedAppChange = ::updateAllowedPackage,
                        onRefreshApps = {
                            launchableApps.value = KioSyncAppAllowlist.getLaunchableApps(this)
                            allowedPackages = KioSyncAppAllowlist.getAllowedPackages(this)
                        },
                        onDismiss = {
                            showAdminPanel.value = false
                        }
                    )
                }
            }
        }

        startAutomaticKioskWhenReady()
    }

    override fun onResume() {
        super.onResume()

        if (isKioskEnabled()) {
            startAutomaticKioskWhenReady()
        } else {
            cancelDelayedKioskStart()
            showSystemBars()
            Log.d(TAG, "Kiosk disabled, skip startLockTask")
        }
    }

    override fun onDestroy() {
        cancelDelayedKioskStart()
        super.onDestroy()
    }

    private fun enableKioskMode() {
        Log.d(TAG, "Enable kiosk requested")

        setKioskEnabled(true)

        cancelDelayedKioskStart()
        startKioskNow()
    }

    private fun disableKioskMode() {
        Log.d(TAG, "Disable kiosk requested")

        setKioskEnabled(false)
        cancelDelayedKioskStart()

        try {
            stopLockTask()
            Log.d(TAG, "Lock Task Mode stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Lock Task Mode", e)
        }

        KioSyncKioskPolicy.disable(this)

        showSystemBars()

        checkDeviceOwnerStatus()

        exitToHome()
    }

    private fun startAutomaticKioskWhenReady() {
        if (!isKioskEnabled()) {
            cancelDelayedKioskStart()
            waitingForSystemInit = false
            showSystemBars()
            return
        }

        val remainingGracePeriod = remainingBootKioskGracePeriodMs()
        if (remainingGracePeriod > 0L) {
            waitingForSystemInit = true
            showSystemBars()
            scheduleDelayedKioskStart(remainingGracePeriod)
            Log.d(TAG, "Delaying kiosk start for ${remainingGracePeriod}ms after boot")
            return
        }

        waitingForSystemInit = false
        startKioskNow()
    }

    private fun scheduleDelayedKioskStart(delayMs: Long) {
        cancelDelayedKioskStart()

        delayedKioskStart = Runnable {
            delayedKioskStart = null
            waitingForSystemInit = false

            if (isKioskEnabled()) {
                startKioskNow()
            } else {
                showSystemBars()
                Log.d(TAG, "Kiosk disabled before delayed start")
            }
        }

        mainHandler.postDelayed(delayedKioskStart!!, delayMs)
    }

    private fun cancelDelayedKioskStart() {
        delayedKioskStart?.let(mainHandler::removeCallbacks)
        delayedKioskStart = null
    }

    private fun startKioskNow() {
        KioSyncKioskPolicy.apply(this)
        hideSystemBars()
        startKioskIfAllowed()
        checkDeviceOwnerStatus()
    }

    private fun isAutomaticKioskStartDelayed(): Boolean {
        return isKioskEnabled() && remainingBootKioskGracePeriodMs() > 0L
    }

    private fun remainingBootKioskGracePeriodMs(): Long {
        return (BOOT_KIOSK_GRACE_PERIOD_MS - SystemClock.elapsedRealtime())
            .coerceAtLeast(0L)
    }

    private fun isKioskEnabled(): Boolean {
        return getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .getBoolean(KEY_KIOSK_ENABLED, false)
    }

    private fun setKioskEnabled(enabled: Boolean) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .edit { putBoolean(KEY_KIOSK_ENABLED, enabled) }
    }

    private fun checkDeviceOwnerStatus() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager

        val isDeviceOwner = dpm.isDeviceOwnerApp(packageName)
        val isLockTaskPermitted = dpm.isLockTaskPermitted(packageName)

        Log.d(TAG, "packageName=$packageName")
        Log.d(TAG, "isDeviceOwner=$isDeviceOwner")
        Log.d(TAG, "isLockTaskPermitted=$isLockTaskPermitted")
        Log.d(TAG, "isKioskEnabled=${isKioskEnabled()}")
    }

    private fun startKioskIfAllowed() {
        val dpm = getSystemService(DEVICE_POLICY_SERVICE) as DevicePolicyManager

        if (dpm.isLockTaskPermitted(packageName)) {
            try {
                startLockTask()
                Log.d(TAG, "Lock Task Mode started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start Lock Task Mode", e)
            }
        } else {
            Log.w(TAG, "Lock Task not permitted. App is not allowlisted yet.")
        }
    }

    private fun launchAllowedApp(packageName: String) {
        if (packageName !in KioSyncAppAllowlist.getAllowedLaunchablePackages(this)) {
            Log.w(TAG, "Blocked launch for non-allowlisted package: $packageName")
            return
        }

        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Log.w(TAG, "No launch intent for package: $packageName")
            KioSyncKioskPolicy.apply(this)
            return
        }

        try {
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
            Log.d(TAG, "Launched allowlisted package: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch package: $packageName", e)
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun showSystemBars() {
        val windowInsetsController =
            WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun exitToHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            startActivity(homeIntent)
            finishAndRemoveTask()
            Log.d(TAG, "Exited to HOME after kiosk disabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to exit to HOME after kiosk disabled", e)
        }
    }
}

@Composable
private fun KioSyncHomeContent(
    waitingForSystemInit: Boolean,
    kioskEnabled: Boolean,
    allowedApps: List<KioSyncAppAllowlist.LaunchableApp>,
    onStatusTap: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
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

@Composable
private fun KioSyncLauncher(
    allowedApps: List<KioSyncAppAllowlist.LaunchableApp>,
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

@Composable
private fun LauncherIcon(
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

@Composable
private fun AdminPinDialog(
    pin: String,
    pinError: Boolean,
    onPinChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Admin Mode")
        },
        text = {
            Column {
                Text("Masukkan PIN admin untuk membuka pengaturan kiosk.")

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = onPinChange,
                    label = {
                        Text("PIN")
                    },
                    isError = pinError,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword
                    )
                )

                if (pinError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("PIN salah")
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Masuk")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal")
            }
        }
    )
}

@Composable
private fun AdminPanelDialog(
    kioskEnabled: Boolean,
    launchableApps: List<KioSyncAppAllowlist.LaunchableApp>,
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
