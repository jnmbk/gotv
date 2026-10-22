package com.gotv.player.data.storage

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Standard HTTP/HTTPS implementation of [ImageDownloader] using platform HttpURLConnection.
 */
class HttpImageDownloader(
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val connectTimeoutMs: Int = 5000,
    private val readTimeoutMs: Int = 5000,
    private val maxSizeBytes: Int = 5 * 1024 * 1024 // 5 MB ceiling to prevent low-RAM OOM
) : ImageDownloader {

    override suspend fun download(url: String): ByteArray? = withContext(ioDispatcher) {
        runCatching {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = connectTimeoutMs
                readTimeout = readTimeoutMs
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            try {
                val responseCode = connection.responseCode
                if (responseCode !in 200..299) {
                    return@withContext null
                }
                connection.inputStream.use { input ->
                    val buffer = ByteArray(8192)
                    val output = ByteArrayOutputStream()
                    var bytesRead: Int
                    var totalRead = 0
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        totalRead += bytesRead
                        if (totalRead > maxSizeBytes) {
                            return@withContext null
                        }
                        output.write(buffer, 0, bytesRead)
                    }
                    output.toByteArray()
                }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }
}
