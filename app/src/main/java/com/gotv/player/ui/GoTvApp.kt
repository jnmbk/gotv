package com.gotv.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gotv.player.data.model.Channel
import com.gotv.player.data.repository.ChannelRepository
import com.gotv.player.data.storage.LogoStorage
import com.gotv.player.player.PlayerEngine
import com.gotv.player.ui.channel.ChannelGridScreen
import com.gotv.player.ui.channel.ChannelGridViewModel
import com.gotv.player.ui.player.PlayerScreen
import com.gotv.player.ui.player.PlayerViewModel

@Composable
fun GoTvApp(
    channelRepository: ChannelRepository,
    logoStorage: LogoStorage? = null,
    playerEngineFactory: () -> PlayerEngine,
    modifier: Modifier = Modifier
) {
    var activeChannel by remember { mutableStateOf<Channel?>(null) }
    val channelViewModel = remember(channelRepository, logoStorage) {
        ChannelGridViewModel(channelRepository, logoStorage)
    }

    val currentChannel = activeChannel
    if (currentChannel == null) {
        ChannelGridScreen(
            viewModel = channelViewModel,
            onChannelSelected = { channel ->
                activeChannel = channel
            },
            modifier = modifier
        )
    } else {
        val session = remember(currentChannel.id) {
            PlayerSession(currentChannel, playerEngineFactory)
        }

        PlayerScreen(
            viewModel = session.viewModel,
            playerEngine = session.engine,
            onBack = {
                activeChannel = null
            },
            modifier = modifier
        )
    }
}

/**
 * Owns the engine and view model of one playback session. Releases them when the session
 * leaves composition, including when composition is abandoned before being committed.
 */
private class PlayerSession(
    channel: Channel,
    engineFactory: () -> PlayerEngine
) : RememberObserver {
    val engine: PlayerEngine = engineFactory()
    val viewModel = PlayerViewModel(channel, engine)

    override fun onRemembered() = Unit
    override fun onForgotten() = viewModel.release()
    override fun onAbandoned() = viewModel.release()
}
