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

    var lastForceRefresh: Boolean = false
        private set

    var deletedChannelIds: MutableList<String> = mutableListOf()
        private set

    override suspend fun downloadAndCacheLogo(
        channelId: String,
        logoUrl: String,
        forceRefresh: Boolean
    ): String? {
        lastForceRefresh = forceRefresh
        if (!forceRefresh && cachedLogos.containsKey(channelId)) {
            return cachedLogos[channelId]
        }
        downloadCalls++
        if (shouldFail) return null
        val path = "/mock/cache/$channelId.png"
        cachedLogos[channelId] = path
        return path
    }

    override fun deleteCachedLogo(channelId: String) {
        deletedChannelIds.add(channelId)
        cachedLogos.remove(channelId)
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
