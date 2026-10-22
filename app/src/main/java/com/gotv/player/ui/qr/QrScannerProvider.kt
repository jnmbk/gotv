package com.gotv.player.ui.qr

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Source of QR code payloads for the channel creation form. Abstracted so that the camera and
 * image picker can be replaced with fake inputs in tests.
 */
interface QrScannerProvider {

    /**
     * Live camera scanner. Invokes [onQrDetected] with the raw payload of a detected QR code and
     * [onClose] when the user dismisses the scanner. Handles camera permission itself.
     */
    @Composable
    fun CameraScanner(
        onQrDetected: (String) -> Unit,
        onClose: () -> Unit,
        modifier: Modifier
    )

    /**
     * Returns a function that opens an image picker. The picked image is decoded and
     * [onResult] receives the QR payload, or null if the image has no readable QR code.
     */
    @Composable
    fun rememberImagePicker(onResult: (String?) -> Unit): () -> Unit
}
