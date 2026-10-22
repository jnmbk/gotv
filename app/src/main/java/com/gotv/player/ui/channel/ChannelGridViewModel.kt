package com.gotv.player.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gotv.player.data.model.Channel
import com.gotv.player.data.model.ChannelValidator
import com.gotv.player.data.qr.QrChannelParser
import com.gotv.player.data.qr.QrParseResult
import com.gotv.player.data.repository.ChannelRepository
import com.gotv.player.data.storage.LogoStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * ViewModel responsible for the channel grid dashboard and channel creation dialog.
 */
class ChannelGridViewModel(
    private val repository: ChannelRepository,
    private val logoStorage: LogoStorage? = null
) : ViewModel() {

    private val _dialogState = MutableStateFlow(DialogState())

    val uiState: StateFlow<ChannelGridUiState> = combine(
        repository.getAllChannels(),
        _dialogState
    ) { channels, dialog ->
        ChannelGridUiState(
            channels = channels,
            isLoading = false,
            isAddDialogOpen = dialog.isOpen,
            nameInput = dialog.name,
            streamUrlInput = dialog.streamUrl,
            logoUrlInput = dialog.logoUrl,
            nameError = dialog.nameError,
            streamUrlError = dialog.streamUrlError,
            logoUrlError = dialog.logoUrlError,
            selectedChannel = dialog.selectedChannel,
            isQrScannerOpen = dialog.isQrScannerOpen,
            qrError = dialog.qrError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChannelGridUiState(isLoading = true)
    )

    fun onOpenAddDialog() {
        _dialogState.value = DialogState(
            isOpen = true,
            streamUrl = ChannelValidator.DEFAULT_URL_PREFIX,
            logoUrl = ChannelValidator.DEFAULT_URL_PREFIX
        )
    }

    fun onDismissAddDialog() {
        _dialogState.value = DialogState(isOpen = false)
    }

    fun onOpenQrScanner() {
        _dialogState.value = _dialogState.value.copy(isQrScannerOpen = true, qrError = null)
    }

    fun onCloseQrScanner() {
        _dialogState.value = _dialogState.value.copy(isQrScannerOpen = false)
    }

    /**
     * Handles a payload from the camera or an image file ([payload] is null when an image had no
     * readable QR code). A valid payload pre-fills the form; otherwise [ChannelGridUiState.qrError]
     * is set.
     */
    fun onQrPayloadScanned(payload: String?) {
        val current = _dialogState.value
        if (payload == null) {
            _dialogState.value = current.copy(
                isQrScannerOpen = false,
                qrError = "No QR code was found in the selected image"
            )
            return
        }
        _dialogState.value = when (val result = QrChannelParser.parse(payload)) {
            is QrParseResult.Success -> current.copy(
                isQrScannerOpen = false,
                qrError = null,
                name = result.name,
                streamUrl = result.streamUrl,
                logoUrl = result.logoUrl.orEmpty(),
                nameError = null,
                streamUrlError = null,
                logoUrlError = null
            )
            is QrParseResult.Invalid -> current.copy(
                isQrScannerOpen = false,
                qrError = result.reason
            )
        }
    }

    fun onNameChanged(name: String) {
        _dialogState.value = _dialogState.value.copy(name = name, nameError = null)
    }

    fun onStreamUrlChanged(streamUrl: String) {
        _dialogState.value = _dialogState.value.copy(
            streamUrl = ChannelValidator.stripDuplicateScheme(streamUrl),
            streamUrlError = null
        )
    }

    fun onLogoUrlChanged(logoUrl: String) {
        _dialogState.value = _dialogState.value.copy(
            logoUrl = ChannelValidator.stripDuplicateScheme(logoUrl),
            logoUrlError = null
        )
    }

    fun onSaveChannel(): Boolean {
        val current = _dialogState.value
        val validation = ChannelValidator.validate(
            name = current.name,
            streamUrl = current.streamUrl,
            logoUrl = current.logoUrl
        )

        if (!validation.isValid) {
            _dialogState.value = current.copy(
                nameError = validation.nameError,
                streamUrlError = validation.streamUrlError,
                logoUrlError = validation.logoUrlError
            )
            return false
        }

        val trimmedName = current.name.trim()
        val normalizedStreamUrl = ChannelValidator.normalizeUrl(current.streamUrl)
        val normalizedLogoUrl = ChannelValidator.normalizeOptionalUrl(current.logoUrl)
        val channelId = UUID.randomUUID().toString()

        onDismissAddDialog()

        viewModelScope.launch {
            val initialChannel = Channel(
                id = channelId,
                name = trimmedName,
                streamUrl = normalizedStreamUrl,
                logoUrl = normalizedLogoUrl,
                localLogoPath = null
            )
            repository.addChannel(initialChannel)

            if (normalizedLogoUrl != null && logoStorage != null) {
                val cachedPath = logoStorage.downloadAndCacheLogo(channelId, normalizedLogoUrl)
                if (cachedPath != null) {
                    repository.updateChannel(initialChannel.copy(localLogoPath = cachedPath))
                }
            }
        }
        return true
    }

    fun onChannelClicked(channel: Channel) {
        _dialogState.value = _dialogState.value.copy(selectedChannel = channel)
    }

    fun onChannelSelectionHandled() {
        _dialogState.value = _dialogState.value.copy(selectedChannel = null)
    }

    private data class DialogState(
        val isOpen: Boolean = false,
        val name: String = "",
        val streamUrl: String = "",
        val logoUrl: String = "",
        val nameError: String? = null,
        val streamUrlError: String? = null,
        val logoUrlError: String? = null,
        val selectedChannel: Channel? = null,
        val isQrScannerOpen: Boolean = false,
        val qrError: String? = null
    )

    class Factory(
        private val repository: ChannelRepository,
        private val logoStorage: LogoStorage? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChannelGridViewModel(repository, logoStorage) as T
        }
    }
}
