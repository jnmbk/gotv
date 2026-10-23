package com.gotv.player.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gotv.player.data.model.Channel
import com.gotv.player.ui.channel.ChannelLogo

/** Delay without interaction after which the in-player sidebar auto-dismisses. */
const val SIDEBAR_AUTO_DISMISS_MS = 10_000L

/**
 * Vertical list of saved channels shown beside the shrunken video.
 * Any touch on the sidebar (including scrolling) invokes [onInteraction].
 */
@Composable
fun ChannelSidebar(
    channels: List<Channel>,
    activeChannelId: String,
    onChannelSelected: (Channel) -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnInteraction by rememberUpdatedState(onInteraction)
    LazyColumn(
        modifier = modifier
            .fillMaxHeight()
            .background(Color.Black.copy(alpha = 0.9f))
            .pointerInput(Unit) {
                detectTapGestures {
                    currentOnInteraction()
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        currentOnInteraction()
                    }
                }
            }
            .statusBarsPadding()
            .testTag("channel_sidebar")
    ) {
        items(channels, key = { it.id }) { channel ->
            val isActive = channel.id == activeChannelId
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        else Color.Transparent
                    )
                    .clickable { onChannelSelected(channel) }
                    .padding(8.dp)
                    .testTag("sidebar_channel_${channel.id}")
            ) {
                ChannelLogo(
                    channel = channel,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color.White,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("sidebar_channel_name_${channel.id}")
                )
            }
        }
    }
}

