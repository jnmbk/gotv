package com.gotv.player.ui.channel

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.gotv.player.data.model.Channel
import java.io.File

@Composable
fun ChannelTile(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoVersion: Int = 0,
    isContextMenuOpen: Boolean = false,
    onDismissContextMenu: () -> Unit = {},
    onEditChannel: () -> Unit = {},
    onRedownloadLogo: () -> Unit = {},
    onDeleteChannel: () -> Unit = {}
) {
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onClick)
                .testTag("channel_tile_${channel.id}"),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                ChannelLogo(
                    channel = channel,
                    logoVersion = logoVersion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("channel_name_${channel.id}")
                )
            }
        }

        if (isContextMenuOpen) {
            DropdownMenu(
                expanded = true,
                onDismissRequest = onDismissContextMenu,
                properties = PopupProperties(focusable = false),
                modifier = Modifier.testTag("channel_context_menu_${channel.id}")
            ) {
                DropdownMenuItem(
                    text = { Text("Edit Channel") },
                    onClick = {
                        onDismissContextMenu()
                        onEditChannel()
                    },
                    modifier = Modifier.testTag("context_menu_edit")
                )
                if (!channel.logoUrl.isNullOrBlank()) {
                    DropdownMenuItem(
                        text = { Text("Redownload Logo") },
                        onClick = {
                            onDismissContextMenu()
                            onRedownloadLogo()
                        },
                        modifier = Modifier.testTag("context_menu_redownload_logo")
                    )
                }
                DropdownMenuItem(
                    text = { Text("Delete Channel") },
                    onClick = {
                        onDismissContextMenu()
                        onDeleteChannel()
                    },
                    modifier = Modifier.testTag("context_menu_delete")
                )
            }
        }
    }
}

@Composable
fun ChannelLogo(
    channel: Channel,
    modifier: Modifier = Modifier,
    logoVersion: Int = 0
) {
    val localPath = channel.localLogoPath
    val bitmap = remember(localPath, logoVersion) {
        if (!localPath.isNullOrBlank()) {
            loadLocalBitmap(localPath)
        } else {
            null
        }
    }

    if (bitmap != null) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surface)
                .testTag("channel_logo_container_${channel.id}"),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "${channel.name} logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .testTag("channel_logo_image")
            )
        }
    } else {
        ChannelLogoPlaceholder(
            channelName = channel.name,
            modifier = modifier
        )
    }
}

private fun loadLocalBitmap(path: String): Bitmap? {
    return runCatching {
        val file = File(path)
        if (file.exists() && file.length() > 0) {
            BitmapFactory.decodeFile(file.absolutePath)
        } else {
            null
        }
    }.getOrNull()
}

@Composable
fun ChannelLogoPlaceholder(
    channelName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface)
            .testTag("channel_logo_placeholder"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tv,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            val initial = channelName.trim().firstOrNull()?.uppercase() ?: ""
            if (initial.isNotEmpty()) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}


