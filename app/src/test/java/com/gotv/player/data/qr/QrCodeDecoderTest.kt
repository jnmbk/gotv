package com.gotv.player.data.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QrCodeDecoderTest {

    private fun render(text: String, size: Int = 300): IntArray {
        val matrix = QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size)
        return IntArray(size * size) { i ->
            if (matrix.get(i % size, i / size)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
    }

    @Test
    fun decodeArgb_readsQrCode() {
        val text = "https://example.com/live.m3u8"
        assertEquals(text, QrCodeDecoder.decodeArgb(render(text), 300, 300))
    }

    @Test
    fun decodeLuminance_readsQrCode() {
        val text = """{"name":"A","url":"https://example.com/live.m3u8"}"""
        val y = render(text).map { (it and 0xFF).toByte() }.toByteArray()
        assertEquals(text, QrCodeDecoder.decodeLuminance(y, 300, 300))
    }

    @Test
    fun blankImage_returnsNull() {
        val blank = IntArray(100 * 100) { 0xFFFFFFFF.toInt() }
        assertNull(QrCodeDecoder.decodeArgb(blank, 100, 100))
    }
}
