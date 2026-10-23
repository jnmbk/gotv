package com.gotv.player.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.gotv.player.data.backup.ChannelListDownloader
import com.gotv.player.data.backup.HttpChannelListDownloader
import com.gotv.player.data.model.Channel
import com.gotv.player.data.repository.ChannelRepository
import com.gotv.player.data.storage.LogoStorage
import com.gotv.player.network.AlwaysOnlineNetworkMonitor
import com.gotv.player.network.NetworkMonitor
import com.gotv.player.player.PlayerEngine
import com.gotv.player.ui.channel.ChannelGridScreen
import com.gotv.player.ui.channel.ChannelGridViewModel
import com.gotv.player.ui.player.PlayerScreen
import com.gotv.player.ui.qr.QrScannerProvider
import com.gotv.player.ui.player.PlayerViewModel

@Composable
fun GoTvApp(
    channelRepository: ChannelRepository,
    logoStorage: LogoStorage? = null,
    playerEngineFactory: () -> PlayerEngine,
    networkMonitor: NetworkMonitor = AlwaysOnlineNetworkMonitor,
    modifier: Modifier = Modifier,
    qrScanner: QrScannerProvider? = null,
    channelListDownloader: ChannelListDownloader = HttpChannelListDownloader()
) {
    var activeChannel by remember { mutableStateOf<Channel?>(null) }
    val channelViewModel = remember(channelRepository, logoStorage, channelListDownloader) {
        ChannelGridViewModel(
            repository = channelRepository,
            logoStorage = logoStorage,
            channelListDownloader = channelListDownloader
        )
    }

    val channelState by channelViewModel.uiState.collectAsState()
    val currentChannel = activeChannel
    if (currentChannel == null) {
        ChannelGridScreen(
            viewModel = channelViewModel,
            onChannelSelected = { channel ->
                activeChannel = channel
            },
            modifier = modifier,
            qrScanner = qrScanner
        )
    } else {
        val session = remember(currentChannel.id) {
            PlayerSession(currentChannel, playerEngineFactory, networkMonitor, channelRepository)
        }

        PlayerScreen(
            viewModel = session.viewModel,
            playerEngine = session.engine,
            onBack = {
                activeChannel = null
            },
            modifier = modifier,
            channels = channelState.channels,
            onChannelSelected = { channel -> activeChannel = channel }
        )
    }
}

/**
 * Owns the engine and view model of one playback session. Releases them when the session
 * leaves composition, including when composition is abandoned before being committed.
 */
private class PlayerSession(
    channel: Channel,
    engineFactory: () -> PlayerEngine,
    networkMonitor: NetworkMonitor,
    channelRepository: ChannelRepository? = null
) : RememberObserver {
    val engine: PlayerEngine = engineFactory()
    val viewModel = PlayerViewModel(channel, engine, networkMonitor, channelRepository = channelRepository)

    override fun onRemembered() = Unit
    override fun onForgotten() = viewModel.release()
    override fun onAbandoned() = viewModel.release()
}
