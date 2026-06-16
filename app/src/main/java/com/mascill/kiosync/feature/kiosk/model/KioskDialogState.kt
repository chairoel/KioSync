package com.mascill.kiosync.feature.kiosk.model

data class KioskDialogState(
    val showPinDialog: Boolean = false,
    val showAdminPanel: Boolean = false,
    val pin: String = "",
    val pinError: Boolean = false
)