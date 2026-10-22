package com.gotv.player.data.repository

import com.gotv.player.data.model.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory implementation of [ChannelRepository], primarily used for testing and UI previews.
 */
class InMemoryChannelRepository(
    initialChannels: List<Channel> = emptyList()
) : ChannelRepository {

    private val _channels = MutableStateFlow(initialChannels)

    override fun getAllChannels(): Flow<List<Channel>> = _channels.asStateFlow()

    override suspend fun getChannelById(id: String): Channel? {
        return _channels.value.find { it.id == id }
    }

    override suspend fun addChannel(channel: Channel) {
        _channels.value = listOf(channel) + _channels.value.filter { it.id != channel.id }
    }

    override suspend fun updateChannel(channel: Channel) {
        _channels.value = _channels.value.map { if (it.id == channel.id) channel else it }
    }

    override suspend fun deleteChannel(id: String) {
        _channels.value = _channels.value.filter { it.id != id }
    }
}

