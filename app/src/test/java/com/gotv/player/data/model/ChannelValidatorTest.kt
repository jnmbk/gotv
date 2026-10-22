package com.gotv.player.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelValidatorTest {

    @Test
    fun validChannelInputs_passesValidation() {
        val result = ChannelValidator.validate(
            name = "BBC News",
            streamUrl = "https://example.com/live/bbc.m3u8",
            logoUrl = "https://example.com/logo.png"
        )
        assertTrue(result.isValid)
        assertNull(result.nameError)
        assertNull(result.streamUrlError)
        assertNull(result.logoUrlError)
    }

    @Test
    fun validChannelWithoutLogo_passesValidation() {
        val result = ChannelValidator.validate(
            name = "Euronews",
            streamUrl = "http://streams.example.org/playlist.m3u8?token=123",
            logoUrl = null
        )
        assertTrue(result.isValid)
    }

    @Test
    fun emptyName_failsValidation() {
        val result = ChannelValidator.validate(
            name = "   ",
            streamUrl = "https://example.com/live.m3u8"
        )
        assertFalse(result.isValid)
        assertEquals("Channel name cannot be empty", result.nameError)
    }

    @Test
    fun emptyStreamUrl_failsValidation() {
        val result = ChannelValidator.validate(
            name = "CNN",
            streamUrl = ""
        )
        assertFalse(result.isValid)
        assertEquals("Stream URL cannot be empty", result.streamUrlError)
    }

    @Test
    fun nonM3u8Url_failsValidation() {
        val result = ChannelValidator.validate(
            name = "CNN",
            streamUrl = "https://example.com/stream.mp4"
        )
        assertFalse(result.isValid)
        assertEquals("Please enter a valid M3U8 URL (e.g. https://example.com/live.m3u8)", result.streamUrlError)
    }

    @Test
    fun invalidProtocol_failsValidation() {
        val result = ChannelValidator.validate(
            name = "CNN",
            streamUrl = "ftp://example.com/stream.m3u8"
        )
        assertFalse(result.isValid)
        assertEquals("Please enter a valid M3U8 URL (e.g. https://example.com/live.m3u8)", result.streamUrlError)
    }

    @Test
    fun invalidLogoUrl_failsValidation() {
        val result = ChannelValidator.validate(
            name = "CNN",
            streamUrl = "https://example.com/stream.m3u8",
            logoUrl = "invalid-url"
        )
        assertFalse(result.isValid)
        assertEquals("Please enter a valid logo URL", result.logoUrlError)
    }
}

