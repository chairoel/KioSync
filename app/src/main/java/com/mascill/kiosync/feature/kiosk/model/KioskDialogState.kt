package com.mascill.kiosync.feature.kiosk.model

/**
 * Transient state for admin-only dialogs.
 *
 * This is separated from the persisted kiosk settings so temporary input, such as a typed PIN,
 * does not leak into repository or DataStore concerns.
 */
data class KioskDialogState(
    /** Whether the hidden admin PIN prompt is visible. */
    val showPinDialog: Boolean = false,
    /** Whether the settings panel is visible after a valid PIN. */
    val showAdminPanel: Boolean = false,
    /** Current PIN input. */
    val pin: String = "",
    /** True when the last PIN confirmation failed. */
    val pinError: Boolean = false
)
