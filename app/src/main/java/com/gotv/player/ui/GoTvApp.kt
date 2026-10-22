package com.gotv.player.ui

import androidx.compose.runtime.Composable
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
        val playerEngine = remember(currentChannel.id) {
            playerEngineFactory()
        }
        val playerViewModel = remember(currentChannel.id, playerEngine) {
            PlayerViewModel(currentChannel, playerEngine)
        }

        PlayerScreen(
            viewModel = playerViewModel,
            playerEngine = playerEngine,
            onBack = {
                activeChannel = null
            },
            modifier = modifier
        )
    }
}
