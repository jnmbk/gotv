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
    fun nonM3u8Url_passesValidation() {
        val result = ChannelValidator.validate(
            name = "CNN",
            streamUrl = "https://example.com/stream.mp4"
        )
        assertTrue(result.isValid)
        assertNull(result.streamUrlError)
    }

    @Test
    fun urlWithoutFileExtension_passesValidation() {
        val result = ChannelValidator.validate(
            name = "CNN",
            streamUrl = "https://example.com/live/channel1"
        )
        assertTrue(result.isValid)
        assertNull(result.streamUrlError)
    }

    @Test
    fun handwrittenUrlsWithoutScheme_autoPrependsHttpsAndPassesValidation() {
        val result = ChannelValidator.validate(
            name = "CNN",
            streamUrl = "example.com/live/stream",
            logoUrl = "example.com/logo.png"
        )
        assertTrue(result.isValid)
        assertEquals("https://example.com/live/stream", ChannelValidator.normalizeUrl("example.com/live/stream"))
        assertEquals("https://example.com/logo.png", ChannelValidator.normalizeOptionalUrl("example.com/logo.png"))
    }

    @Test
    fun bareDefaultHttpsPrefix_treatedAsEmpty() {
        val result = ChannelValidator.validate(
            name = "CNN",
            streamUrl = "https://",
            logoUrl = "https://"
        )
        assertFalse(result.isValid)
        assertEquals("Stream URL cannot be empty", result.streamUrlError)
        assertNull(result.logoUrlError)
        assertNull(ChannelValidator.normalizeOptionalUrl("https://"))
    }

    @Test
    fun invalidProtocol_failsValidation() {
        val result = ChannelValidator.validate(
            name = "CNN",
            streamUrl = "ftp://example.com/stream.m3u8"
        )
        assertFalse(result.isValid)
        assertEquals("Please enter a valid stream URL", result.streamUrlError)
    }

    @Test
    fun invalidStreamUrl_failsValidation() {
        val result = ChannelValidator.validate(
            name = "CNN",
            streamUrl = "invalid-url"
        )
        assertFalse(result.isValid)
        assertEquals("Please enter a valid stream URL", result.streamUrlError)
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

    @Test
    fun validChannelWithMultipleStreamUrls_passesValidation() {
        val result = ChannelValidator.validate(
            name = "BBC One",
            streamUrls = listOf(
                "https://example.com/primary.m3u8",
                "https://backup.example.com/stream.m3u8",
                "example.com/fallback.m3u8"
            ),
            logoUrl = "https://example.com/logo.png"
        )
        assertTrue(result.isValid)
        assertNull(result.streamUrlError)
        assertEquals(listOf(null, null, null), result.streamUrlsErrors)
    }

    @Test
    fun multipleStreamUrlsWithInvalidSecondaryUrl_identifiesErrorAtIndex() {
        val result = ChannelValidator.validate(
            name = "BBC One",
            streamUrls = listOf(
                "https://example.com/primary.m3u8",
                "invalid-link",
                "https://example.com/fallback.m3u8"
            )
        )
        assertFalse(result.isValid)
        assertEquals("Please enter a valid stream URL", result.streamUrlError)
        assertEquals(
            listOf(null, "Please enter a valid stream URL", null),
            result.streamUrlsErrors
        )
    }

    @Test
    fun multipleStreamUrlsWithEmptySecondaryUrl_identifiesErrorAtIndex() {
        val result = ChannelValidator.validate(
            name = "BBC One",
            streamUrls = listOf(
                "https://example.com/primary.m3u8",
                "https://"
            )
        )
        assertFalse(result.isValid)
        assertEquals("Stream URL cannot be empty", result.streamUrlError)
        assertEquals(
            listOf(null, "Stream URL cannot be empty"),
            result.streamUrlsErrors
        )
    }

    @Test
    fun emptyStreamUrlsList_failsValidation() {
        val result = ChannelValidator.validate(
            name = "BBC One",
            streamUrls = emptyList()
        )
        assertFalse(result.isValid)
        assertEquals("Stream URL cannot be empty", result.streamUrlError)
        assertEquals(listOf("Stream URL cannot be empty"), result.streamUrlsErrors)
    }
}

