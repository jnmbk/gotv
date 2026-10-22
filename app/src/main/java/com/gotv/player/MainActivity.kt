package com.gotv.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.gotv.player.data.repository.SqliteChannelRepository
import com.gotv.player.ui.channel.ChannelGridScreen
import com.gotv.player.ui.channel.ChannelGridViewModel
import com.gotv.player.ui.theme.GoTvTheme

class MainActivity : ComponentActivity() {

    private val channelRepository by lazy {
        SqliteChannelRepository(applicationContext)
    }

    private val channelViewModel by viewModels<ChannelGridViewModel> {
        ChannelGridViewModel.Factory(channelRepository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoTvTheme {
                ChannelGridScreen(
                    viewModel = channelViewModel,
                    onChannelSelected = {
                        // Channel selection will be connected to full-screen player in Issue 03
                    }
                )
            }
        }
    }
}

