package com.mascill.kiosync.core.dpc

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.mascill.kiosync.core.kiosk.KioSyncKioskPolicy

/**
 * Device admin receiver used during Device Owner provisioning.
 */
class KioSyncDeviceAdminReceiver : DeviceAdminReceiver() {

    /** Logs successful activation of the admin receiver. */
    override fun onEnabled(context: Context, intent: Intent) {
        super.onEnabled(context, intent)
        Log.d("KioSyncDPC", "Device admin enabled")
    }

    /** Applies initial kiosk policy as soon as provisioning completes. */
    override fun onProfileProvisioningComplete(context: Context, intent: Intent) {
        super.onProfileProvisioningComplete(context, intent)
        Log.d("KioSyncDPC", "Provisioning complete")

        KioSyncKioskPolicy.apply(context)
    }
}
