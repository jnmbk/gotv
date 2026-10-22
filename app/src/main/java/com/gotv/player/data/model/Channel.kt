package com.gotv.player.data.model

import java.util.UUID

/**
 * Represents an IPTV channel.
 *
 * @property id Unique identifier for the channel.
 * @property name Display name of the channel.
 * @property streamUrl The M3U8 stream URL.
 * @property logoUrl Optional remote URL for the channel logo.
 * @property localLogoPath Optional local file path for cached logo image.
 * @property createdAt Epoch timestamp in milliseconds when the channel was added.
 * @property position Custom zero-based display order position (lower values appear first).
 */
data class Channel(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val localLogoPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val position: Int = 0
)

