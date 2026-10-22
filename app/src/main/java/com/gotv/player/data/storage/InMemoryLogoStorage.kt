package com.gotv.player.data.storage

import java.io.File

/**
 * In-memory test implementation of [LogoStorage] for hermetic testing.
 */
class InMemoryLogoStorage(
    var shouldFail: Boolean = false,
    private val cachedLogos: MutableMap<String, String> = mutableMapOf()
) : LogoStorage {

    var downloadCalls: Int = 0
        private set

    override suspend fun downloadAndCacheLogo(channelId: String, logoUrl: String): String? {
        downloadCalls++
        if (shouldFail) return null
        val path = cachedLogos[channelId] ?: "/mock/cache/$channelId.png"
        cachedLogos[channelId] = path
        return path
    }

    override fun hasCachedLogo(channelId: String): Boolean {
        return cachedLogos.containsKey(channelId)
    }

    override fun getCachedLogoFile(channelId: String): File? {
        val path = cachedLogos[channelId] ?: return null
        return File(path)
    }

    fun setCachedLogo(channelId: String, path: String) {
        cachedLogos[channelId] = path
    }
}
