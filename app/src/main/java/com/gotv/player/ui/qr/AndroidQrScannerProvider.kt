package com.gotv.player.ui.qr

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.gotv.player.R
import com.gotv.player.data.qr.QrCodeDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Production [QrScannerProvider] backed by CameraX, ZXing and the system image picker. */
object AndroidQrScannerProvider : QrScannerProvider {

    @Composable
    override fun CameraScanner(
        onQrDetected: (String) -> Unit,
        onClose: () -> Unit,
        modifier: Modifier
    ) {
        val context = LocalContext.current
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
            )
        }
        var permissionDenied by remember { mutableStateOf(false) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
            permissionDenied = !granted
        }
        LaunchedEffect(Unit) {
            if (!hasPermission) launcher.launch(Manifest.permission.CAMERA)
        }

        Box(modifier = modifier.fillMaxSize().testTag("qr_camera_scanner")) {
            if (hasPermission) {
                CameraPreview(onQrDetected = onQrDetected)
            } else if (permissionDenied) {
                Column(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.camera_permission_required),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }) {
                        Text(stringResource(R.string.action_grant_permission))
                    }
                }
            }
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(8.dp)
                    .testTag("qr_scanner_close_button")
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cd_close_scanner))
            }
        }
    }

    @Composable
    private fun CameraPreview(onQrDetected: (String) -> Unit) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val currentOnDetected by rememberUpdatedState(onQrDetected)
        val previewView = remember { PreviewView(context) }

        DisposableEffect(lifecycleOwner) {
            val executor = Executors.newSingleThreadExecutor()
            val delivered = AtomicBoolean(false)
            val providerFuture = ProcessCameraProvider.getInstance(context)
            var provider: ProcessCameraProvider? = null
            providerFuture.addListener({
                val cameraProvider = providerFuture.get()
                provider = cameraProvider
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { image ->
                    try {
                        val plane = image.planes[0]
                        val buffer = plane.buffer
                        val width = image.width
                        val height = image.height
                        val rowStride = plane.rowStride
                        // Copy the Y plane, dropping row padding.
                        val data = ByteArray(width * height)
                        for (row in 0 until height) {
                            buffer.position(row * rowStride)
                            buffer.get(data, row * width, width)
                        }
                        val text = QrCodeDecoder.decodeLuminance(data, width, height)
                        if (text != null && delivered.compareAndSet(false, true)) {
                            ContextCompat.getMainExecutor(context).execute { currentOnDetected(text) }
                        }
                    } finally {
                        image.close()
                    }
                }
                runCatching {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                    )
                }
            }, ContextCompat.getMainExecutor(context))

            onDispose {
                provider?.unbindAll()
                executor.shutdown()
            }
        }

        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
    }

    @Composable
    override fun rememberImagePicker(onResult: (String?) -> Unit): () -> Unit {
        val context = LocalContext.current
        val currentOnResult by rememberUpdatedState(onResult)
        var pendingUri by remember { mutableStateOf<Uri?>(null) }
        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri -> pendingUri = uri }

        val uri = pendingUri
        if (uri != null) {
            LaunchedEffect(uri) {
                val text = withContext(Dispatchers.IO) { decodeImage(context, uri) }
                pendingUri = null
                currentOnResult(text)
            }
        }
        return { launcher.launch("image/*") }
    }

    private fun decodeImage(context: Context, uri: Uri): String? = runCatching {
        val bitmap = loadScaledBitmap(context, uri) ?: return null
        try {
            val pixels = IntArray(bitmap.width * bitmap.height)
            bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            QrCodeDecoder.decodeArgb(pixels, bitmap.width, bitmap.height)
        } finally {
            bitmap.recycle()
        }
    }.getOrNull()

    private fun loadScaledBitmap(context: Context, uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > MAX_DIMENSION || bounds.outHeight / sample > MAX_DIMENSION) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private const val MAX_DIMENSION = 2048
}
