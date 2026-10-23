package com.gotv.player.ui.channel

import com.gotv.player.data.model.Channel
import com.gotv.player.ui.util.UiText

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
    val nameError: UiText? = null,
    val streamUrlsErrors: List<UiText?> = emptyList(),
    val logoUrlError: UiText? = null,
    val selectedChannel: Channel? = null,
    val isQrScannerOpen: Boolean = false,
    val qrScanTarget: QrScanTarget? = null,
    val qrError: UiText? = null,
    val deletingChannel: Channel? = null,
    val logoReloadVersions: Map<String, Int> = emptyMap(),
    val isImportConfirmDialogOpen: Boolean = false,
    val pendingImportChannels: List<Channel> = emptyList(),
    val userFeedbackMessage: UiText? = null,
    val isImportUrlDialogOpen: Boolean = false,
    val importUrlInput: String = "",
    val importUrlError: UiText? = null,
    val isImportUrlLoading: Boolean = false
) {
    val streamUrlInput: String
        get() = streamUrlsInput.firstOrNull().orEmpty()

    val streamUrlError: UiText?
        get() = streamUrlsErrors.firstOrNull { it != null }

    val isEmpty: Boolean
        get() = !isLoading && channels.isEmpty()

    val isEditMode: Boolean
        get() = editingChannel != null
}
