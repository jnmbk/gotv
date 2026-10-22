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
    private val _logoReloadVersions = MutableStateFlow<Map<String, Int>>(emptyMap())

    val uiState: StateFlow<ChannelGridUiState> = combine(
        repository.getAllChannels(),
        _dialogState,
        _logoReloadVersions
    ) { channels, dialog, logoVersions ->
        ChannelGridUiState(
            channels = channels,
            isLoading = false,
            isAddDialogOpen = dialog.isOpen,
            editingChannel = dialog.editingChannel,
            nameInput = dialog.name,
            streamUrlInput = dialog.streamUrl,
            logoUrlInput = dialog.logoUrl,
            nameError = dialog.nameError,
            streamUrlError = dialog.streamUrlError,
            logoUrlError = dialog.logoUrlError,
            selectedChannel = dialog.selectedChannel,
            isQrScannerOpen = dialog.isQrScannerOpen,
            qrError = dialog.qrError,
            deletingChannel = dialog.deletingChannel,
            logoReloadVersions = logoVersions
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ChannelGridUiState(isLoading = true)
    )

    fun onOpenAddDialog() {
        _dialogState.value = _dialogState.value.copy(
            isOpen = true,
            editingChannel = null,
            name = "",
            streamUrl = ChannelValidator.DEFAULT_URL_PREFIX,
            logoUrl = ChannelValidator.DEFAULT_URL_PREFIX,
            nameError = null,
            streamUrlError = null,
            logoUrlError = null,
            isQrScannerOpen = false,
            qrError = null
        )
    }

    fun onOpenEditDialog(channel: Channel) {
        _dialogState.value = _dialogState.value.copy(
            isOpen = true,
            editingChannel = channel,
            name = channel.name,
            streamUrl = channel.streamUrl,
            logoUrl = channel.logoUrl ?: ChannelValidator.DEFAULT_URL_PREFIX,
            nameError = null,
            streamUrlError = null,
            logoUrlError = null,
            isQrScannerOpen = false,
            qrError = null
        )
    }

    fun onDismissAddDialog() {
        _dialogState.value = _dialogState.value.copy(
            isOpen = false,
            editingChannel = null,
            name = "",
            streamUrl = "",
            logoUrl = "",
            nameError = null,
            streamUrlError = null,
            logoUrlError = null,
            isQrScannerOpen = false,
            qrError = null
        )
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
        val existingChannel = current.editingChannel

        onDismissAddDialog()

        if (existingChannel != null) {
            viewModelScope.launch {
                val logoChanged = normalizedLogoUrl != existingChannel.logoUrl
                val updatedStreamUrls = if (existingChannel.streamUrls.size > 1 &&
                    (existingChannel.streamUrls.firstOrNull() == normalizedStreamUrl || existingChannel.lastPlayedUrl == normalizedStreamUrl)
                ) {
                    existingChannel.streamUrls
                } else {
                    listOf(normalizedStreamUrl)
                }
                val updatedChannel = when {
                    normalizedLogoUrl == null -> {
                        logoStorage?.deleteCachedLogo(existingChannel.id)
                        existingChannel.copy(
                            name = trimmedName,
                            streamUrls = updatedStreamUrls,
                            logoUrl = null,
                            localLogoPath = null
                        )
                    }
                    logoChanged -> {
                        val baseUpdated = existingChannel.copy(
                            name = trimmedName,
                            streamUrls = updatedStreamUrls,
                            logoUrl = normalizedLogoUrl
                        )
                        repository.updateChannel(baseUpdated)
                        val cachedPath = logoStorage?.downloadAndCacheLogo(
                            channelId = existingChannel.id,
                            logoUrl = normalizedLogoUrl,
                            forceRefresh = true
                        )
                        if (cachedPath != null) {
                            incrementLogoVersion(existingChannel.id)
                            baseUpdated.copy(localLogoPath = cachedPath)
                        } else {
                            logoStorage?.deleteCachedLogo(existingChannel.id)
                            incrementLogoVersion(existingChannel.id)
                            baseUpdated.copy(localLogoPath = null)
                        }
                    }
                    else -> {
                        val baseUpdated = existingChannel.copy(
                            name = trimmedName,
                            streamUrls = updatedStreamUrls,
                            logoUrl = normalizedLogoUrl
                        )
                        if (existingChannel.localLogoPath == null && logoStorage != null) {
                            repository.updateChannel(baseUpdated)
                            val cachedPath = logoStorage.downloadAndCacheLogo(
                                channelId = existingChannel.id,
                                logoUrl = normalizedLogoUrl
                            )
                            if (cachedPath != null) {
                                incrementLogoVersion(existingChannel.id)
                                baseUpdated.copy(localLogoPath = cachedPath)
                            } else {
                                baseUpdated
                            }
                        } else {
                            baseUpdated
                        }
                    }
                }
                repository.updateChannel(updatedChannel)
            }
            return true
        }

        val channelId = UUID.randomUUID().toString()

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

    fun onRedownloadLogo(channel: Channel) {
        val logoUrl = channel.logoUrl ?: return
        val storage = logoStorage ?: return
        viewModelScope.launch {
            val cachedPath = storage.downloadAndCacheLogo(
                channelId = channel.id,
                logoUrl = logoUrl,
                forceRefresh = true
            )
            if (cachedPath != null) {
                val latest = repository.getChannelById(channel.id) ?: channel
                repository.updateChannel(latest.copy(localLogoPath = cachedPath))
                incrementLogoVersion(channel.id)
            }
        }
    }

    fun onRequestDeleteChannel(channel: Channel) {
        _dialogState.value = _dialogState.value.copy(deletingChannel = channel)
    }

    fun onDismissDeleteDialog() {
        _dialogState.value = _dialogState.value.copy(deletingChannel = null)
    }

    fun onConfirmDeleteChannel() {
        val channel = _dialogState.value.deletingChannel ?: return
        _dialogState.value = _dialogState.value.copy(deletingChannel = null)
        viewModelScope.launch {
            repository.deleteChannel(channel.id)
            logoStorage?.deleteCachedLogo(channel.id)
        }
    }

    fun onReorderChannels(orderedChannelIds: List<String>) {
        viewModelScope.launch {
            repository.reorderChannels(orderedChannelIds)
        }
    }

    private fun incrementLogoVersion(channelId: String) {
        val currentMap = _logoReloadVersions.value
        _logoReloadVersions.value = currentMap + (channelId to ((currentMap[channelId] ?: 0) + 1))
    }

    fun onChannelClicked(channel: Channel) {
        _dialogState.value = _dialogState.value.copy(selectedChannel = channel)
    }

    fun onChannelSelectionHandled() {
        _dialogState.value = _dialogState.value.copy(selectedChannel = null)
    }

    private data class DialogState(
        val isOpen: Boolean = false,
        val editingChannel: Channel? = null,
        val name: String = "",
        val streamUrl: String = "",
        val logoUrl: String = "",
        val nameError: String? = null,
        val streamUrlError: String? = null,
        val logoUrlError: String? = null,
        val selectedChannel: Channel? = null,
        val isQrScannerOpen: Boolean = false,
        val qrError: String? = null,
        val deletingChannel: Channel? = null
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
