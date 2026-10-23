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
    val streamUrlsInput: List<String> = emptyList(),
    val logoUrlInput: String = "",
    val nameError: String? = null,
    val streamUrlsErrors: List<String?> = emptyList(),
    val logoUrlError: String? = null,
    val selectedChannel: Channel? = null,
    val isQrScannerOpen: Boolean = false,
    val qrScanTarget: QrScanTarget? = null,
    val qrError: String? = null,
    val deletingChannel: Channel? = null,
    val logoReloadVersions: Map<String, Int> = emptyMap(),
    val isImportConfirmDialogOpen: Boolean = false,
    val pendingImportChannels: List<Channel> = emptyList(),
    val userFeedbackMessage: String? = null,
    val isImportUrlDialogOpen: Boolean = false,
    val importUrlInput: String = "",
    val importUrlError: String? = null,
    val isImportUrlLoading: Boolean = false
) {
    val streamUrlInput: String
        get() = streamUrlsInput.firstOrNull().orEmpty()

    val streamUrlError: String?
        get() = streamUrlsErrors.firstOrNull { it != null }

    val isEmpty: Boolean
        get() = !isLoading && channels.isEmpty()

    val isEditMode: Boolean
        get() = editingChannel != null
}
