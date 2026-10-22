package com.gotv.player.data.backup

import com.gotv.player.data.model.Channel
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelJsonSerializerTest {

    @Test
    fun exportChannels_serializesChannelsToVersionedJson() {
        val channels = listOf(
            Channel(
                name = "BBC One",
                streamUrls = listOf("https://stream1.m3u8", "https://stream2.m3u8"),
                logoUrl = "https://logo1.png"
            ),
            Channel(
                name = "CNN",
                streamUrl = "https://cnn.m3u8",
                logoUrl = null
            )
        )

        val json = ChannelJsonSerializer.exportChannels(channels)
        val root = JSONObject(json)

        assertEquals(1, root.getInt("version"))
        val array = root.getJSONArray("channels")
        assertEquals(2, array.length())

        val ch1 = array.getJSONObject(0)
        assertEquals("BBC One", ch1.getString("name"))
        val urls1 = ch1.getJSONArray("streamUrls")
        assertEquals(2, urls1.length())
        assertEquals("https://stream1.m3u8", urls1.getString(0))
        assertEquals("https://stream2.m3u8", urls1.getString(1))
        assertEquals("https://logo1.png", ch1.getString("logoUrl"))

        val ch2 = array.getJSONObject(1)
        assertEquals("CNN", ch2.getString("name"))
        val urls2 = ch2.getJSONArray("streamUrls")
        assertEquals(1, urls2.length())
        assertEquals("https://cnn.m3u8", urls2.getString(0))
        assertFalse(ch2.has("logoUrl"))
    }

    @Test
    fun importChannels_parsesVersionedJsonWithStreamUrlsArray() {
        val json = """
            {
              "version": 1,
              "channels": [
                {
                  "name": "Euronews",
                  "streamUrls": [
                    "https://euronews.com/live1.m3u8",
                    "https://euronews.com/live2.m3u8"
                  ],
                  "logoUrl": "https://euronews.com/logo.png"
                }
              ]
            }
        """.trimIndent()

        val result = ChannelJsonSerializer.importChannels(json)
        assertTrue(result is ChannelImportResult.Success)

        val channels = (result as ChannelImportResult.Success).channels
        assertEquals(1, channels.size)
        assertEquals("Euronews", channels[0].name)
        assertEquals(listOf("https://euronews.com/live1.m3u8", "https://euronews.com/live2.m3u8"), channels[0].streamUrls)
        assertEquals("https://euronews.com/logo.png", channels[0].logoUrl)
    }

    @Test
    fun importChannels_toleratesLegacySingleStreamUrlOrUrl() {
        val json = """
            {
              "version": 1,
              "channels": [
                {
                  "name": "Channel Legacy",
                  "streamUrl": "https://legacy.com/live.m3u8",
                  "logoUrl": "https://legacy.com/logo.png"
                },
                {
                  "name": "Channel Alt",
                  "url": "https://alt.com/live.m3u8"
                }
              ]
            }
        """.trimIndent()

        val result = ChannelJsonSerializer.importChannels(json)
        assertTrue(result is ChannelImportResult.Success)

        val channels = (result as ChannelImportResult.Success).channels
        assertEquals(2, channels.size)
        assertEquals("Channel Legacy", channels[0].name)
        assertEquals(listOf("https://legacy.com/live.m3u8"), channels[0].streamUrls)
        assertEquals("Channel Alt", channels[1].name)
        assertEquals(listOf("https://alt.com/live.m3u8"), channels[1].streamUrls)
        assertNull(channels[1].logoUrl)
    }

    @Test
    fun importChannels_toleratesRootArrayFormat() {
        val json = """
            [
              {
                "name": "Direct Array Channel",
                "streamUrl": "https://direct.com/live.m3u8"
              }
            ]
        """.trimIndent()

        val result = ChannelJsonSerializer.importChannels(json)
        assertTrue(result is ChannelImportResult.Success)
        val channels = (result as ChannelImportResult.Success).channels
        assertEquals(1, channels.size)
        assertEquals("Direct Array Channel", channels[0].name)
    }

    @Test
    fun importChannels_normalizesUrlsWithoutScheme() {
        val json = """
            {
              "version": 1,
              "channels": [
                {
                  "name": "Scheme Channel",
                  "streamUrls": ["streams.example.com/feed.m3u8"],
                  "logoUrl": "example.com/logo.png"
                }
              ]
            }
        """.trimIndent()

        val result = ChannelJsonSerializer.importChannels(json)
        assertTrue(result is ChannelImportResult.Success)
        val channels = (result as ChannelImportResult.Success).channels
        assertEquals(1, channels.size)
        assertEquals("https://streams.example.com/feed.m3u8", channels[0].streamUrls[0])
        assertEquals("https://example.com/logo.png", channels[0].logoUrl)
    }

    @Test
    fun importChannels_skipsInvalidEntries_andImportsValidOnes() {
        val json = """
            {
              "version": 1,
              "channels": [
                {
                  "name": "",
                  "streamUrls": ["https://valid.com/live.m3u8"]
                },
                {
                  "name": "Valid Channel",
                  "streamUrls": ["https://valid.com/live.m3u8"],
                  "logoUrl": "https://valid.com/logo.png"
                },
                {
                  "name": "Invalid Stream Url",
                  "streamUrls": ["not a url"]
                },
                {
                  "name": "Invalid Logo Url",
                  "streamUrls": ["https://valid.com/live.m3u8"],
                  "logoUrl": "bad logo url"
                }
              ]
            }
        """.trimIndent()

        val result = ChannelJsonSerializer.importChannels(json)
        assertTrue(result is ChannelImportResult.Success)
        val channels = (result as ChannelImportResult.Success).channels
        assertEquals(1, channels.size)
        assertEquals("Valid Channel", channels[0].name)
    }

    @Test
    fun importChannels_returnsFailure_whenFileIsEmptyOrMalformed() {
        val emptyResult = ChannelJsonSerializer.importChannels("   ")
        assertTrue(emptyResult is ChannelImportResult.Failure)

        val malformedResult = ChannelJsonSerializer.importChannels("{invalid: json}")
        assertTrue(malformedResult is ChannelImportResult.Failure)

        val missingChannelsResult = ChannelJsonSerializer.importChannels("""{"version": 1}""")
        assertTrue(missingChannelsResult is ChannelImportResult.Failure)
    }

    @Test
    fun importChannels_returnsFailure_whenNoValidChannelsExist() {
        val json = """
            {
              "version": 1,
              "channels": [
                {
                  "name": "Bad URL",
                  "streamUrls": ["not valid"]
                }
              ]
            }
        """.trimIndent()

        val result = ChannelJsonSerializer.importChannels(json)
        assertTrue(result is ChannelImportResult.Failure)
    }
}
