package com.gotv.player.ui.qr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Test double for [QrScannerProvider]. Tapping the fake camera view "detects" [cameraPayload];
 * invoking the image picker yields [imagePayload].
 */
class FakeQrScannerProvider(
    var cameraPayload: String? = null,
    var imagePayload: String? = null
) : QrScannerProvider {

    @Composable
    override fun CameraScanner(
        onQrDetected: (String) -> Unit,
        onClose: () -> Unit,
        modifier: Modifier
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .testTag("fake_camera")
                .clickable { cameraPayload?.let(onQrDetected) }
        ) {
            Text("Fake camera")
        }
    }

    @Composable
    override fun rememberImagePicker(onResult: (String?) -> Unit): () -> Unit = {
        onResult(imagePayload)
    }
}
