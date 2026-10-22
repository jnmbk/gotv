package com.gotv.player.data.model

import java.util.UUID

/**
 * Represents an IPTV channel.
 *
 * @property id Unique identifier for the channel.
 * @property name Display name of the channel.
 * @property streamUrls Ordered list of M3U8 stream URLs, where the first is primary and others are failover backups.
 * @property logoUrl Optional remote URL for the channel logo.
 * @property localLogoPath Optional local file path for cached logo image.
 * @property createdAt Epoch timestamp in milliseconds when the channel was added.
 * @property position Custom zero-based display order position (lower values appear first).
 * @property lastPlayedUrl The stream URL that was most recently played successfully, or null if never played.
 */
data class Channel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val streamUrls: List<String> = emptyList(),
    val logoUrl: String? = null,
    val localLogoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val position: Int = 0,
    val lastPlayedUrl: String? = null
) {
    /**
     * Backwards-compatible convenience property returning the active stream URL:
     * [lastPlayedUrl] if it is present in [streamUrls], otherwise the first URL in [streamUrls].
     */
    val streamUrl: String
        get() = if (lastPlayedUrl != null && (streamUrls.isEmpty() || lastPlayedUrl in streamUrls)) {
            lastPlayedUrl
        } else {
            streamUrls.firstOrNull().orEmpty()
        }

    /**
     * Backwards-compatible constructor accepting a single [streamUrl].
     */
    constructor(
        id: String = UUID.randomUUID().toString(),
        name: String,
        streamUrl: String,
        logoUrl: String? = null,
        localLogoPath: String? = null,
        createdAt: Long = System.currentTimeMillis(),
        position: Int = 0,
        lastPlayedUrl: String? = null
    ) : this(
        id = id,
        name = name,
        streamUrls = if (streamUrl.isNotEmpty()) listOf(streamUrl) else emptyList(),
        logoUrl = logoUrl,
        localLogoPath = localLogoPath,
        createdAt = createdAt,
        position = position,
        lastPlayedUrl = lastPlayedUrl
    )
}

