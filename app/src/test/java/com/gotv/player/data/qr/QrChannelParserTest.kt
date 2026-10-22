package com.gotv.player.data.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrChannelParserTest {

    private fun success(raw: String?) = parse(raw) as QrParseResult.Success

    private fun parse(raw: String?) = QrChannelParser.parse(raw)

    @Test
    fun plainM3u8Url_populatesStreamUrlAndDerivesName() {
        val result = success("  https://example.com/streams/bloomberg.m3u8 ")
        assertEquals("https://example.com/streams/bloomberg.m3u8", result.streamUrl)
        assertEquals("bloomberg", result.name)
        assertNull(result.logoUrl)
    }

    @Test
    fun json_populatesAllFields() {
        val result = success(
            """{"name":"News HD","url":"https://e.com/live.m3u8","logo":"https://e.com/l.png"}"""
        )
        assertEquals("News HD", result.name)
        assertEquals("https://e.com/live.m3u8", result.streamUrl)
        assertEquals("https://e.com/l.png", result.logoUrl)
    }

    @Test
    fun json_withoutName_derivesName() {
        val result = success("""{"streamUrl":"https://e.com/a/sports.m3u8"}""")
        assertEquals("sports", result.name)
    }

    @Test
    fun deepLink_decodesParameters() {
        val result = success(
            "gotv://channel?name=My%20Channel&url=https%3A%2F%2Fe.com%2Flive.m3u8&logo=https%3A%2F%2Fe.com%2Fl.png"
        )
        assertEquals("My Channel", result.name)
        assertEquals("https://e.com/live.m3u8", result.streamUrl)
        assertEquals("https://e.com/l.png", result.logoUrl)
    }

    @Test
    fun invalidPayloads_areRejected() {
        listOf(
            null,
            "",
            "hello world",
            "https://example.com/page.html",
            "{not json",
            """{"name":"x"}""",
            "gotv://channel",
            """{"url":"https://e.com/live.m3u8","logo":"nope"}"""
        ).forEach { raw ->
            assertTrue("Expected invalid for $raw", parse(raw) is QrParseResult.Invalid)
        }
    }
}
