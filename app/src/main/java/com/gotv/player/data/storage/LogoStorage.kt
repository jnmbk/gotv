package com.gotv.player.data.storage

import java.io.File

/**
 * Functional interface for downloading raw image bytes from a remote URL.
 */
fun interface ImageDownloader {
    suspend fun download(url: String): ByteArray?
}

/**
 * Storage interface for managing locally-cached channel logos.
 */
interface LogoStorage {
    /**
     * Downloads the logo from [logoUrl] (if not already cached locally, unless [forceRefresh] is true),
     * downsamples it to target thumbnail dimensions, and saves it to local app storage for [channelId].
     *
     * When [forceRefresh] is true and the download or decoding fails, any pre-existing cached logo file
     * on disk is preserved.
     *
     * @return Absolute file path of the cached downsampled logo, or null if download/caching fails.
     */
    suspend fun downloadAndCacheLogo(
        channelId: String,
        logoUrl: String,
        forceRefresh: Boolean = false
    ): String?

    /**
     * Deletes the cached logo file for [channelId], if one exists.
     */
    fun deleteCachedLogo(channelId: String)

    /**
     * Checks if a local cached logo exists for [channelId].
     */
    fun hasCachedLogo(channelId: String): Boolean

    /**
     * Retrieves the cached logo file for [channelId], or null if not cached.
     */
    fun getCachedLogoFile(channelId: String): File?
}
