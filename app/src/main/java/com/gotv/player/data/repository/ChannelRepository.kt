package com.gotv.player.data.repository

import com.gotv.player.data.model.Channel
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing persistent IPTV channels.
 */
interface ChannelRepository {
    /**
     * Observes the list of all saved channels ordered by creation date descending.
     */
    fun getAllChannels(): Flow<List<Channel>>

    /**
     * Retrieves a single channel by its unique identifier.
     */
    suspend fun getChannelById(id: String): Channel?

    /**
     * Persists a new or updated channel.
     */
    suspend fun addChannel(channel: Channel)

    /**
     * Updates an existing channel.
     */
    suspend fun updateChannel(channel: Channel)

    /**
     * Removes a channel by its identifier.
     */
    suspend fun deleteChannel(id: String)
}

