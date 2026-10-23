package com.gotv.player.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.ui.PlayerView
import com.gotv.player.R
import com.gotv.player.data.model.Channel
import com.gotv.player.player.PlayerEngine
import com.gotv.player.ui.channel.ChannelLogo
import kotlinx.coroutines.delay

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    playerEngine: PlayerEngine,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    channels: List<Channel> = emptyList(),
    onChannelSelected: (Channel) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var sidebarVisible by remember { mutableStateOf(false) }
    var interactionTick by remember { mutableIntStateOf(0) }
    val sidebarProgress by animateFloatAsState(
        targetValue = if (sidebarVisible) 1f else 0f,
        label = "sidebarProgress"
    )

    LaunchedEffect(sidebarVisible, interactionTick) {
        if (sidebarVisible) {
            delay(SIDEBAR_AUTO_DISMISS_MS)
            sidebarVisible = false
        }
    }

    BackHandler(onBack = {
        viewModel.release()
        onBack()
    })

    DisposableEffect(viewModel) {
        onDispose {
            viewModel.release()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(viewModel, lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> viewModel.onBackground()
                Lifecycle.Event.ON_START -> viewModel.onForeground()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("player_screen")
    ) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val scale = 1f - SIDEBAR_VIDEO_SHRINK * sidebarProgress
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(1f, 0.5f)
            }
            .testTag("video_viewport")
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    val delta = -dragAmount / 500f
                    val newVolume = (uiState.volume + delta).coerceIn(0f, 1f)
                    viewModel.setVolume(newVolume)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures {
                    if (sidebarVisible) {
                        sidebarVisible = false
                    } else if (channels.isNotEmpty()) {
                        interactionTick++
                        sidebarVisible = true
                    }
                }
            }
    ) {
        // Video surface
        val player = playerEngine.player
        if (player != null) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        this.player = player
                    }
                },
                update = { view ->
                    // The player instance changes when switching channels from the sidebar;
                    // the factory only runs once, so rebind here.
                    if (view.player !== player) view.player = player
                },
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_surface")
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_surface")
            )
        }

        if (uiState.showSpinner) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .testTag("player_loading_overlay"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ChannelLogo(
                    channel = uiState.channel,
                    modifier = Modifier
                        .size(96.dp)
                        .testTag("player_loading_logo")
                )
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.testTag("loading_spinner")
                )
            }
        }

        if (uiState.hasFailed) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp)
                    .testTag("playback_error"),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ChannelLogo(
                    channel = uiState.channel,
                    modifier = Modifier
                        .size(96.dp)
                        .testTag("player_loading_logo")
                )
                Text(
                    text = stringResource(R.string.player_error_message),
                    style = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("playback_error_message")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { viewModel.retry() },
                        modifier = Modifier.testTag("retry_button")
                    ) { Text(stringResource(R.string.player_error_retry)) }
                    Button(
                        onClick = {
                            viewModel.release()
                            onBack()
                        },
                        modifier = Modifier.testTag("error_back_button")
                    ) { Text(stringResource(R.string.player_error_back)) }
                }
            }
        }

        // Top bar overlay with back button and channel name
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.7f),
                            Color.Transparent
                        )
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    viewModel.release()
                    onBack()
                },
                modifier = Modifier.testTag("player_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back_to_channels),
                    tint = Color.White
                )
            }

            Text(
                text = uiState.channel.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
                    .testTag("player_channel_title")
            )
        }

        // Bottom on-screen volume control (no seek bar, no pause, pure live stream volume control)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .testTag("volume_control_panel")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val volumeIcon = when {
                    uiState.volume <= 0.001f -> Icons.AutoMirrored.Filled.VolumeMute
                    uiState.volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
                    else -> Icons.AutoMirrored.Filled.VolumeUp
                }

                IconButton(
                    onClick = {
                        val newVol = if (uiState.volume > 0.001f) 0.0f else 1.0f
                        viewModel.setVolume(newVol)
                    },
                    modifier = Modifier.testTag("volume_icon_button")
                ) {
                    Icon(
                        imageVector = volumeIcon,
                        contentDescription = stringResource(R.string.cd_volume_control),
                        tint = Color.White,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("volume_icon")
                    )
                }

                Slider(
                    value = uiState.volume,
                    onValueChange = { newVolume ->
                        viewModel.setVolume(newVolume)
                    },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("volume_slider")
                )
            }
        }
    }

    if (sidebarProgress > 0f) {
        ChannelSidebar(
            channels = channels,
            activeChannelId = uiState.channel.id,
            onChannelSelected = { channel ->
                if (channel.id != uiState.channel.id) {
                    sidebarVisible = false
                    onChannelSelected(channel)
                }
            },
            onInteraction = { interactionTick++ },
            modifier = Modifier
                .fillMaxWidth(SIDEBAR_VIDEO_SHRINK)
                .align(Alignment.CenterStart)
                .graphicsLayer { alpha = sidebarProgress }
        )
    }
    }
}

/** Fraction of the screen width given to the sidebar (and taken from the video). */
private const val SIDEBAR_VIDEO_SHRINK = 0.3f
