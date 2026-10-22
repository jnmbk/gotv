package com.gotv.player.ui.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotv.player.data.model.Channel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelGridScreen(
    viewModel: ChannelGridViewModel,
    onChannelSelected: (Channel) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("channel_grid_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GoTV",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = viewModel::onOpenAddDialog,
                        modifier = Modifier.testTag("add_channel_top_bar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Channel"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isEmpty) {
            EmptyChannelsView(
                onAddChannelClick = viewModel::onOpenAddDialog,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag("channels_grid")
            ) {
                items(
                    items = uiState.channels,
                    key = { it.id }
                ) { channel ->
                    ChannelTile(
                        channel = channel,
                        onClick = {
                            viewModel.onChannelClicked(channel)
                            onChannelSelected(channel)
                        }
                    )
                }
            }
        }

        if (uiState.isAddDialogOpen) {
            AddChannelDialog(
                uiState = uiState,
                onNameChange = viewModel::onNameChanged,
                onStreamUrlChange = viewModel::onStreamUrlChanged,
                onLogoUrlChange = viewModel::onLogoUrlChanged,
                onDismiss = viewModel::onDismissAddDialog,
                onConfirm = { viewModel.onSaveChannel() }
            )
        }
    }
}

