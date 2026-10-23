package com.gotv.player.ui.channel

/**
 * Identifies the targeted input field when opening the QR scanner overlay.
 */
sealed interface QrScanTarget {
    data class StreamUrl(val index: Int = 0) : QrScanTarget
    data object LogoUrl : QrScanTarget
    data object ImportUrl : QrScanTarget
}
