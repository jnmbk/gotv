package com.gotv.player.data.backup

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLConnection

/**
 * Platform HTTP/HTTPS implementation of [ChannelListDownloader] using [HttpURLConnection].
 */
class HttpChannelListDownloader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectTimeoutMs: Int = 10000,
    private val readTimeoutMs: Int = 10000,
    private val maxSizeBytes: Int = 5 * 1024 * 1024, // 5 MB ceiling
    private val urlConnectionFactory: (URL) -> URLConnection = { it.openConnection() }
) : ChannelListDownloader {

    override suspend fun download(url: String): DownloadResult = withContext(ioDispatcher) {
        val parsedUrl = try {
            URL(url)
        } catch (e: MalformedURLException) {
            return@withContext DownloadResult.Failure("Invalid URL format: ${e.message}")
        }

        val protocol = parsedUrl.protocol?.lowercase()
        if (protocol != "http" && protocol != "https") {
            return@withContext DownloadResult.Failure("Unsupported protocol: $protocol. Only HTTP and HTTPS are supported.")
        }

        var connection: HttpURLConnection? = null
        try {
            val opened = urlConnectionFactory(parsedUrl)
            if (opened !is HttpURLConnection) {
                return@withContext DownloadResult.Failure("Connection is not an HTTP connection")
            }
            connection = opened.apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
                requestMethod = "GET"
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val errorMsg = connection.responseMessage?.takeIf { it.isNotBlank() } ?: "HTTP $responseCode"
                return@withContext DownloadResult.Failure("HTTP error $responseCode: $errorMsg")
            }

            val inputStream = connection.inputStream
                ?: return@withContext DownloadResult.Failure("Empty response from server")

            inputStream.use { input ->
                val buffer = ByteArray(8192)
                val output = ByteArrayOutputStream()
                var bytesRead: Int
                var totalRead = 0
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    totalRead += bytesRead
                    if (totalRead > maxSizeBytes) {
                        return@withContext DownloadResult.Failure("Response payload exceeds limit of ${maxSizeBytes / 1024 / 1024} MB")
                    }
                    output.write(buffer, 0, bytesRead)
                }
                val charset = connection.contentEncoding?.let { encoding ->
                    runCatching { java.nio.charset.Charset.forName(encoding) }.getOrNull()
                } ?: Charsets.UTF_8
                DownloadResult.Success(output.toString(charset.name()))
            }
        } catch (e: SocketTimeoutException) {
            DownloadResult.Failure("Connection timed out: ${e.message}")
        } catch (e: IOException) {
            DownloadResult.Failure("Network error: ${e.message}")
        } catch (e: Exception) {
            DownloadResult.Failure("Failed to download channel list: ${e.message}")
        } finally {
            connection?.disconnect()
        }
    }
}
