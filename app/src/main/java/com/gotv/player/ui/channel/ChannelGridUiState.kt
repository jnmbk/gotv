package com.gotv.player.ui.channel

import com.gotv.player.data.model.Channel

/**
 * UI state representing the channel grid dashboard.
 */
data class ChannelGridUiState(
    val channels: List<Channel> = emptyList(),
    val isLoading: Boolean = false,
    val isAddDialogOpen: Boolean = false,
    val editingChannel: Channel? = null,
    val nameInput: String = "",
    val streamUrlInput: String = "",
    val logoUrlInput: String = "",
    val nameError: String? = null,
    val streamUrlError: String? = null,
    val logoUrlError: String? = null,
    val selectedChannel: Channel? = null,
    val isQrScannerOpen: Boolean = false,
    val qrError: String? = null,
    val deletingChannel: Channel? = null,
    val logoReloadVersions: Map<String, Int> = emptyMap()
) {
    val isEmpty: Boolean
        get() = !isLoading && channels.isEmpty()

    val isEditMode: Boolean
        get() = editingChannel != null
}
