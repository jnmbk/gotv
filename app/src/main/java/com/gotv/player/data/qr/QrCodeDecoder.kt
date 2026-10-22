package com.gotv.player.data.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.LuminanceSource
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader

/** Decodes QR codes from raw image data using ZXing. Returns null when no QR code is found. */
object QrCodeDecoder {

    private val hints = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true
    )

    /** Decodes from ARGB pixels (e.g. from a [android.graphics.Bitmap]). */
    fun decodeArgb(pixels: IntArray, width: Int, height: Int): String? =
        decode(RGBLuminanceSource(width, height, pixels))

    /** Decodes from the luminance (Y) plane of a camera frame. */
    fun decodeLuminance(yData: ByteArray, dataWidth: Int, dataHeight: Int): String? =
        decode(PlanarYUVLuminanceSource(yData, dataWidth, dataHeight, 0, 0, dataWidth, dataHeight, false))

    private fun decode(source: LuminanceSource): String? {
        val reader = QRCodeReader()
        return try {
            reader.decode(BinaryBitmap(HybridBinarizer(source)), hints).text
        } catch (e: com.google.zxing.ReaderException) {
            null
        } finally {
            reader.reset()
        }
    }
}
