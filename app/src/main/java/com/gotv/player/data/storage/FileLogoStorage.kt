package com.gotv.player.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * File-backed implementation of [LogoStorage] optimized for low memory.
 *
 * Downloads logos, calculates appropriate [BitmapFactory.Options.inSampleSize]
 * bounds before native memory decoding, scales to target thumbnail dimensions,
 * and caches as PNG in local app disk storage.
 */
class FileLogoStorage(
    val baseDir: File,
    private val imageDownloader: ImageDownloader = HttpImageDownloader(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    val targetWidth: Int = TARGET_THUMBNAIL_WIDTH,
    val targetHeight: Int = TARGET_THUMBNAIL_HEIGHT
) : LogoStorage {

    constructor(
        context: Context,
        imageDownloader: ImageDownloader = HttpImageDownloader(),
        ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    ) : this(
        baseDir = File(context.filesDir, "logos"),
        imageDownloader = imageDownloader,
        ioDispatcher = ioDispatcher
    )

    override suspend fun downloadAndCacheLogo(channelId: String, logoUrl: String): String? =
        withContext(ioDispatcher) {
            runCatching {
                if (!baseDir.exists()) {
                    baseDir.mkdirs()
                }

                val targetFile = File(baseDir, "$channelId.png")
                if (targetFile.exists() && targetFile.length() > 0) {
                    return@withContext targetFile.absolutePath
                }

                val bytes = imageDownloader.download(logoUrl) ?: return@withContext null
                if (bytes.isEmpty() || !isSupportedImageFormat(bytes)) return@withContext null

                // 1. Decode bounds only to prevent memory bloat
                val boundsOptions = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
                if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                    return@withContext null
                }

                // 2. Compute inSampleSize for downsampling
                val sampleSize = calculateInSampleSize(
                    actualWidth = boundsOptions.outWidth,
                    actualHeight = boundsOptions.outHeight,
                    reqWidth = targetWidth,
                    reqHeight = targetHeight
                )

                // 3. Decode downsampled bitmap
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions)
                    ?: return@withContext null

                // 4. Downscale precisely to target bounds if still larger
                val finalBitmap = if (decoded.width > targetWidth || decoded.height > targetHeight) {
                    val ratio = minOf(
                        targetWidth.toFloat() / decoded.width,
                        targetHeight.toFloat() / decoded.height
                    )
                    val scaledW = (decoded.width * ratio).toInt().coerceAtLeast(1)
                    val scaledH = (decoded.height * ratio).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(decoded, scaledW, scaledH, true)
                    if (scaled != decoded) {
                        decoded.recycle()
                    }
                    scaled
                } else {
                    decoded
                }

                // 5. Persist to local disk storage
                FileOutputStream(targetFile).use { out ->
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                }

                finalBitmap.recycle()
                targetFile.absolutePath
            }.getOrNull()
        }

    override fun hasCachedLogo(channelId: String): Boolean {
        val file = File(baseDir, "$channelId.png")
        return file.exists() && file.length() > 0
    }

    override fun getCachedLogoFile(channelId: String): File? {
        val file = File(baseDir, "$channelId.png")
        return if (file.exists() && file.length() > 0) file else null
    }

    companion object {
        const val TARGET_THUMBNAIL_WIDTH = 320
        const val TARGET_THUMBNAIL_HEIGHT = 180

        fun isSupportedImageFormat(bytes: ByteArray): Boolean {
            if (bytes.size < 4) return false

            // PNG: 89 50 4E 47
            val isPng = (bytes[0] == 0x89.toByte()) &&
                (bytes[1] == 0x50.toByte()) &&
                (bytes[2] == 0x4E.toByte()) &&
                (bytes[3] == 0x47.toByte())
            if (isPng) return true

            // JPEG: FF D8 FF
            val isJpg = (bytes[0] == 0xFF.toByte()) &&
                (bytes[1] == 0xD8.toByte()) &&
                (bytes[2] == 0xFF.toByte())
            if (isJpg) return true

            // GIF: 47 49 46 (GIF)
            val isGif = (bytes[0] == 'G'.code.toByte()) &&
                (bytes[1] == 'I'.code.toByte()) &&
                (bytes[2] == 'F'.code.toByte())
            if (isGif) return true

            // WebP: RIFF .... WEBP
            if (bytes.size >= 12 &&
                bytes[0] == 'R'.code.toByte() &&
                bytes[1] == 'I'.code.toByte() &&
                bytes[2] == 'F'.code.toByte() &&
                bytes[3] == 'F'.code.toByte() &&
                bytes[8] == 'W'.code.toByte() &&
                bytes[9] == 'E'.code.toByte() &&
                bytes[10] == 'B'.code.toByte() &&
                bytes[11] == 'P'.code.toByte()
            ) {
                return true
            }

            return false
        }

        fun calculateInSampleSize(
            actualWidth: Int,
            actualHeight: Int,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            var inSampleSize = 1
            if (actualHeight > reqHeight || actualWidth > reqWidth) {
                val halfHeight = actualHeight / 2
                val halfWidth = actualWidth / 2
                while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                    inSampleSize *= 2
                }
            }
            return inSampleSize
        }
    }
}
