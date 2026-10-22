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

    private val _channels = MutableStateFlow(
        initialChannels.mapIndexed { index, ch ->
            if (ch.position == 0 && index > 0 && initialChannels.all { it.position == 0 }) {
                ch.copy(position = index)
            } else {
                ch
            }
        }
    )

    override fun getAllChannels(): Flow<List<Channel>> = _channels.asStateFlow()

    override suspend fun getChannelById(id: String): Channel? {
        return _channels.value.find { it.id == id }
    }

    override suspend fun addChannel(channel: Channel) {
        val updatedList = listOf(channel) + _channels.value.filter { it.id != channel.id }
        _channels.value = updatedList.mapIndexed { index, ch -> ch.copy(position = index) }
    }

    override suspend fun updateChannel(channel: Channel) {
        _channels.value = _channels.value.map {
            if (it.id == channel.id) channel.copy(position = it.position) else it
        }
    }

    override suspend fun deleteChannel(id: String) {
        _channels.value = _channels.value
            .filter { it.id != id }
            .mapIndexed { index, ch -> ch.copy(position = index) }
    }

    override suspend fun reorderChannels(orderedChannelIds: List<String>) {
        val currentById = _channels.value.associateBy { it.id }
        val reordered = orderedChannelIds.mapNotNull { currentById[it] }
        val remaining = _channels.value.filter { it.id !in orderedChannelIds.toSet() }
        _channels.value = (reordered + remaining).mapIndexed { index, ch ->
            ch.copy(position = index)
        }
    }

    override suspend fun updateLastPlayedUrl(channelId: String, streamUrl: String) {
        _channels.value = _channels.value.map {
            if (it.id == channelId) it.copy(lastPlayedUrl = streamUrl) else it
        }
    }
}

