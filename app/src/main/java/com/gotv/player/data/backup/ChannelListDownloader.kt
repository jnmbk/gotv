package com.gotv.player.data.backup

sealed class DownloadResult {
    data class Success(val content: String) : DownloadResult()
    data class Failure(val reason: String) : DownloadResult()
}

/**
 * Interface for downloading raw channel list content from a remote URL.
 */
fun interface ChannelListDownloader {
    /**
     * Downloads raw string content from the provided [url].
     *
     * @param url The remote HTTP/HTTPS URL.
     * @return [DownloadResult.Success] containing the downloaded string, or [DownloadResult.Failure] with an error description.
     */
    suspend fun download(url: String): DownloadResult
}
