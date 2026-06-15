package com.mascill.kiosync

import android.app.admin.DevicePolicyManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.mascill.kiosync.dpc.KioSyncKioskPolicy

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "KioSyncDPC"

        private const val PREF_NAME = "kiosync_settings"
        private const val KEY_KIOSK_ENABLED = "kiosk_enabled"

        // Untuk development dulu.
        // Untuk production jangan hardcode PIN seperti ini.
        private const val ADMIN_PIN = "123456"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (isKioskEnabled()) {
            KioSyncKioskPolicy.apply(this)
            hideSystemBars()
        } else {
            showSystemBars()
        }

        checkDeviceOwnerStatus()

        setContent {
            MaterialTheme {
                var kioskEnabled by remember {
                    mutableStateOf(isKioskEnabled())
                }

                var tapCount by remember {
                    mutableIntStateOf(0)
                }

                var showAdminDialog by remember {
                    mutableStateOf(false)
                }

                var pin by remember {
                    mutableStateOf("")
                }

                var pinError by remember {
                    mutableStateOf(false)
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (kioskEnabled) {
                            "Kiosk Mode Aktif"
                        } else {
                            "Kiosk Mode Nonaktif"
                        },
                        modifier = Modifier.clickable {
                            tapCount++

                            if (tapCount >= 7) {
                                tapCount = 0
                                showAdminDialog = true
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Tap tulisan status 7x untuk Admin Mode")
                }

                if (showAdminDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            showAdminDialog = false
                            pin = ""
                            pinError = false
                        },
                        title = {
                            Text("Admin Mode")
                        },
                        text = {
                            Column {
                                Text(
                                    text = if (kioskEnabled) {
                                        "Masukkan PIN admin untuk mematikan kiosk."
                                    } else {
                                        "Masukkan PIN admin untuk mengaktifkan kiosk."
                                    }
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = pin,
                                    onValueChange = {
                                        pin = it
                                        pinError = false
                                    },
                                    label = {
                                        Text("PIN")
                                    },
                                    isError = pinError,
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
                            Button(
                                onClick = {
                                    if (pin == ADMIN_PIN) {
                                        showAdminDialog = false
                                        pin = ""
                                        pinError = false

                                        if (kioskEnabled) {
                                            disableKioskMode()
                                            kioskEnabled = false
                                        } else {
                                            enableKioskMode()
                                            kioskEnabled = true
                                        }
                                    } else {
                                        pinError = true
                                    }
                                }
                            ) {
                                Text(
                                    text = if (kioskEnabled) {
                                        "Disable Kiosk"
                                    } else {
                                        "Enable Kiosk"
                                    }
                                )
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    showAdminDialog = false
                                    pin = ""
                                    pinError = false
                                }
                            ) {
                                Text("Batal")
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (isKioskEnabled()) {
            hideSystemBars()
            startKioskIfAllowed()
        } else {
            showSystemBars()
            Log.d(TAG, "Kiosk disabled, skip startLockTask")
        }
    }

    private fun enableKioskMode() {
        Log.d(TAG, "Enable kiosk requested")

        setKioskEnabled(true)

        KioSyncKioskPolicy.apply(this)

        hideSystemBars()
        startKioskIfAllowed()

        checkDeviceOwnerStatus()
    }

    private fun disableKioskMode() {
        Log.d(TAG, "Disable kiosk requested")

        setKioskEnabled(false)

        try {
            stopLockTask()
            Log.d(TAG, "Lock Task Mode stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Lock Task Mode", e)
        }

        KioSyncKioskPolicy.disable(this)

        showSystemBars()

        checkDeviceOwnerStatus()
    }

    private fun isKioskEnabled(): Boolean {
        return getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .getBoolean(KEY_KIOSK_ENABLED, false)
    }

    private fun setKioskEnabled(enabled: Boolean) {
        getSharedPreferences(PREF_NAME, MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_KIOSK_ENABLED, enabled)
            .apply()
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
}