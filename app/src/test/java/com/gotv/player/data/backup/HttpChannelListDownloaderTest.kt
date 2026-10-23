package com.gotv.player.data.backup

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class HttpChannelListDownloaderTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeHttpURLConnection(
        url: URL,
        private val responseCodeValue: Int = 200,
        private val responseMessageValue: String = "OK",
        private val responseBody: ByteArray = ByteArray(0),
        private val contentEncodingValue: String? = null,
        private val throwOnConnect: IOException? = null,
        private val throwOnRead: IOException? = null
    ) : HttpURLConnection(url) {

        var disconnected = false

        override fun connect() {
            throwOnConnect?.let { throw it }
        }

        override fun disconnect() {
            disconnected = true
        }

        override fun usingProxy(): Boolean = false

        override fun getResponseCode(): Int {
            throwOnConnect?.let { throw it }
            return responseCodeValue
        }

        override fun getResponseMessage(): String {
            return responseMessageValue
        }

        override fun getContentEncoding(): String? {
            return contentEncodingValue
        }

        override fun getInputStream(): InputStream {
            throwOnConnect?.let { throw it }
            if (throwOnRead != null) {
                return object : InputStream() {
                    override fun read(): Int = throw throwOnRead
                    override fun read(b: ByteArray, off: Int, len: Int): Int = throw throwOnRead
                }
            }
            return ByteArrayInputStream(responseBody)
        }
    }

    @Test
    fun download_success() = runTest(testDispatcher) {
        val json = """{"version": 1, "channels": []}"""
        var fakeConnection: FakeHttpURLConnection? = null
        val downloader = HttpChannelListDownloader(
            ioDispatcher = testDispatcher,
            urlConnectionFactory = { url ->
                FakeHttpURLConnection(
                    url = url,
                    responseCodeValue = 200,
                    responseBody = json.toByteArray(Charsets.UTF_8)
                ).also { fakeConnection = it }
            }
        )

        val result = downloader.download("https://example.com/channels.json")

        assertTrue(result is DownloadResult.Success)
        assertEquals(json, (result as DownloadResult.Success).content)
        assertTrue(fakeConnection?.disconnected == true)
    }

    @Test
    fun download_invalidUrlFormat_returnsFailure() = runTest(testDispatcher) {
        val downloader = HttpChannelListDownloader(ioDispatcher = testDispatcher)

        val result = downloader.download("invalid-url-not-http")

        assertTrue(result is DownloadResult.Failure)
        val failure = result as DownloadResult.Failure
        assertTrue(failure.reason.contains("Invalid URL format") || failure.reason.contains("Unsupported protocol"))
    }

    @Test
    fun download_unsupportedProtocol_returnsFailure() = runTest(testDispatcher) {
        val downloader = HttpChannelListDownloader(ioDispatcher = testDispatcher)

        val result = downloader.download("ftp://example.com/channels.json")

        assertTrue(result is DownloadResult.Failure)
        val failure = result as DownloadResult.Failure
        assertTrue(failure.reason.contains("Unsupported protocol"))
    }

    @Test
    fun download_httpErrorStatusCode_returnsFailure() = runTest(testDispatcher) {
        var fakeConnection: FakeHttpURLConnection? = null
        val downloader = HttpChannelListDownloader(
            ioDispatcher = testDispatcher,
            urlConnectionFactory = { url ->
                FakeHttpURLConnection(
                    url = url,
                    responseCodeValue = 404,
                    responseMessageValue = "Not Found"
                ).also { fakeConnection = it }
            }
        )

        val result = downloader.download("https://example.com/missing.json")

        assertTrue(result is DownloadResult.Failure)
        val failure = result as DownloadResult.Failure
        assertTrue(failure.reason.contains("404"))
        assertTrue(fakeConnection?.disconnected == true)
    }

    @Test
    fun download_socketTimeout_returnsFailure() = runTest(testDispatcher) {
        var fakeConnection: FakeHttpURLConnection? = null
        val downloader = HttpChannelListDownloader(
            ioDispatcher = testDispatcher,
            urlConnectionFactory = { url ->
                FakeHttpURLConnection(
                    url = url,
                    throwOnConnect = SocketTimeoutException("Read timed out")
                ).also { fakeConnection = it }
            }
        )

        val result = downloader.download("https://example.com/timeout.json")

        assertTrue(result is DownloadResult.Failure)
        val failure = result as DownloadResult.Failure
        assertTrue(failure.reason.contains("timed out"))
        assertTrue(fakeConnection?.disconnected == true)
    }

    @Test
    fun download_networkIOException_returnsFailure() = runTest(testDispatcher) {
        var fakeConnection: FakeHttpURLConnection? = null
        val downloader = HttpChannelListDownloader(
            ioDispatcher = testDispatcher,
            urlConnectionFactory = { url ->
                FakeHttpURLConnection(
                    url = url,
                    throwOnRead = IOException("Connection reset by peer")
                ).also { fakeConnection = it }
            }
        )

        val result = downloader.download("https://example.com/error.json")

        assertTrue(result is DownloadResult.Failure)
        val failure = result as DownloadResult.Failure
        assertTrue(failure.reason.contains("Network error"))
        assertTrue(fakeConnection?.disconnected == true)
    }

    @Test
    fun download_payloadExceedsLimit_returnsFailure() = runTest(testDispatcher) {
        val largeBytes = ByteArray(1024 * 1024) // 1 MB
        var fakeConnection: FakeHttpURLConnection? = null
        val downloader = HttpChannelListDownloader(
            ioDispatcher = testDispatcher,
            maxSizeBytes = 512 * 1024, // 512 KB limit
            urlConnectionFactory = { url ->
                FakeHttpURLConnection(
                    url = url,
                    responseCodeValue = 200,
                    responseBody = largeBytes
                ).also { fakeConnection = it }
            }
        )

        val result = downloader.download("https://example.com/large.json")

        assertTrue(result is DownloadResult.Failure)
        val failure = result as DownloadResult.Failure
        assertTrue(failure.reason.contains("payload exceeds limit"))
        assertTrue(fakeConnection?.disconnected == true)
    }
}
