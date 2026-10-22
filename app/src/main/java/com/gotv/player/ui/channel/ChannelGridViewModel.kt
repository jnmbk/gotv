package com.gotv.player.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gotv.player.data.backup.ChannelImportResult
import com.gotv.player.data.backup.ChannelJsonSerializer
import com.gotv.player.data.model.Channel
import com.gotv.player.data.model.ChannelValidator
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
            streamUrlsInput = dialog.streamUrls,
            logoUrlInput = dialog.logoUrl,
            nameError = dialog.nameError,
            streamUrlsErrors = dialog.streamUrlsErrors,
            logoUrlError = dialog.logoUrlError,
            selectedChannel = dialog.selectedChannel,
            isQrScannerOpen = dialog.isQrScannerOpen,
            qrScanTarget = dialog.qrScanTarget,
            qrError = dialog.qrError,
            deletingChannel = dialog.deletingChannel,
            logoReloadVersions = logoVersions,
            isImportConfirmDialogOpen = dialog.isImportConfirmDialogOpen,
            pendingImportChannels = dialog.pendingImportChannels,
            userFeedbackMessage = dialog.userFeedbackMessage
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
            streamUrls = listOf(ChannelValidator.DEFAULT_URL_PREFIX),
            logoUrl = ChannelValidator.DEFAULT_URL_PREFIX,
            nameError = null,
            streamUrlsErrors = emptyList(),
            logoUrlError = null,
            isQrScannerOpen = false,
            qrScanTarget = null,
            qrError = null
        )
    }

    fun onOpenEditDialog(channel: Channel) {
        val initialStreamUrls = if (channel.streamUrls.isNotEmpty()) {
            channel.streamUrls
        } else if (channel.streamUrl.isNotEmpty()) {
            listOf(channel.streamUrl)
        } else {
            listOf(ChannelValidator.DEFAULT_URL_PREFIX)
        }
        _dialogState.value = _dialogState.value.copy(
            isOpen = true,
            editingChannel = channel,
            name = channel.name,
            streamUrls = initialStreamUrls,
            logoUrl = channel.logoUrl ?: ChannelValidator.DEFAULT_URL_PREFIX,
            nameError = null,
            streamUrlsErrors = emptyList(),
            logoUrlError = null,
            isQrScannerOpen = false,
            qrScanTarget = null,
            qrError = null
        )
    }

    fun onDismissAddDialog() {
        _dialogState.value = _dialogState.value.copy(
            isOpen = false,
            editingChannel = null,
            name = "",
            streamUrls = emptyList(),
            logoUrl = "",
            nameError = null,
            streamUrlsErrors = emptyList(),
            logoUrlError = null,
            isQrScannerOpen = false,
            qrScanTarget = null,
            qrError = null
        )
    }

    fun onOpenQrScanner(target: QrScanTarget = QrScanTarget.StreamUrl()) {
        _dialogState.value = _dialogState.value.copy(
            isQrScannerOpen = true,
            qrScanTarget = target,
            qrError = null
        )
    }

    fun onCloseQrScanner() {
        _dialogState.value = _dialogState.value.copy(
            isQrScannerOpen = false,
            qrScanTarget = null
        )
    }

    /**
     * Handles a scanned QR payload by setting its raw decoded text into the targeted input field.
     */
    fun onQrPayloadScanned(payload: String?) {
        val current = _dialogState.value
        if (payload == null) {
            _dialogState.value = current.copy(
                isQrScannerOpen = false,
                qrScanTarget = null
            )
            return
        }

        val rawText = payload.trim()
        val target = current.qrScanTarget ?: QrScanTarget.StreamUrl()
        _dialogState.value = when (target) {
            is QrScanTarget.StreamUrl -> {
                val index = target.index
                val cleaned = ChannelValidator.stripDuplicateScheme(rawText)
                val updatedUrls = current.streamUrls.toMutableList()
                if (index in updatedUrls.indices) {
                    updatedUrls[index] = cleaned
                } else {
                    updatedUrls.add(cleaned)
                }
                val updatedErrors = if (current.streamUrlsErrors.size == current.streamUrls.size) {
                    current.streamUrlsErrors.toMutableList().apply {
                        if (index in indices) this[index] = null
                    }
                } else {
                    emptyList()
                }
                current.copy(
                    isQrScannerOpen = false,
                    qrScanTarget = null,
                    streamUrls = updatedUrls,
                    streamUrlsErrors = updatedErrors
                )
            }
            is QrScanTarget.LogoUrl -> current.copy(
                isQrScannerOpen = false,
                qrScanTarget = null,
                logoUrl = ChannelValidator.stripDuplicateScheme(rawText),
                logoUrlError = null
            )
        }
    }

    fun onNameChanged(name: String) {
        _dialogState.value = _dialogState.value.copy(name = name, nameError = null)
    }

    fun onAddStreamUrlField() {
        val current = _dialogState.value
        _dialogState.value = current.copy(
            streamUrls = current.streamUrls + ChannelValidator.DEFAULT_URL_PREFIX,
            streamUrlsErrors = if (current.streamUrlsErrors.isNotEmpty()) current.streamUrlsErrors + null else emptyList()
        )
    }

    fun onRemoveStreamUrlField(index: Int) {
        val current = _dialogState.value
        if (current.streamUrls.size <= 1 || index !in current.streamUrls.indices) {
            return
        }
        val updatedUrls = current.streamUrls.toMutableList().apply { removeAt(index) }
        val updatedErrors = if (current.streamUrlsErrors.size == current.streamUrls.size) {
            current.streamUrlsErrors.toMutableList().apply { removeAt(index) }
        } else {
            emptyList()
        }
        _dialogState.value = current.copy(
            streamUrls = updatedUrls,
            streamUrlsErrors = updatedErrors
        )
    }

    fun onStreamUrlChanged(index: Int, streamUrl: String) {
        val current = _dialogState.value
        if (index !in current.streamUrls.indices) return
        val updatedUrls = current.streamUrls.toMutableList()
        updatedUrls[index] = ChannelValidator.stripDuplicateScheme(streamUrl)
        val updatedErrors = if (current.streamUrlsErrors.size == current.streamUrls.size) {
            current.streamUrlsErrors.toMutableList().apply { this[index] = null }
        } else {
            emptyList()
        }
        _dialogState.value = current.copy(
            streamUrls = updatedUrls,
            streamUrlsErrors = updatedErrors
        )
    }

    fun onStreamUrlChanged(streamUrl: String) {
        onStreamUrlChanged(0, streamUrl)
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
            streamUrls = current.streamUrls,
            logoUrl = current.logoUrl
        )

        if (!validation.isValid) {
            _dialogState.value = current.copy(
                nameError = validation.nameError,
                streamUrlsErrors = validation.streamUrlsErrors,
                logoUrlError = validation.logoUrlError
            )
            return false
        }

        val trimmedName = current.name.trim()
        val normalizedStreamUrls = current.streamUrls
            .map { ChannelValidator.normalizeUrl(it) }
            .filter { it.isNotEmpty() }
        val normalizedLogoUrl = ChannelValidator.normalizeOptionalUrl(current.logoUrl)
        val existingChannel = current.editingChannel

        onDismissAddDialog()

        if (existingChannel != null) {
            viewModelScope.launch {
                val logoChanged = normalizedLogoUrl != existingChannel.logoUrl
                val updatedChannel = when {
                    normalizedLogoUrl == null -> {
                        logoStorage?.deleteCachedLogo(existingChannel.id)
                        existingChannel.copy(
                            name = trimmedName,
                            streamUrls = normalizedStreamUrls,
                            logoUrl = null,
                            localLogoPath = null
                        )
                    }
                    logoChanged -> {
                        val baseUpdated = existingChannel.copy(
                            name = trimmedName,
                            streamUrls = normalizedStreamUrls,
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
                            streamUrls = normalizedStreamUrls,
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
                streamUrls = normalizedStreamUrls,
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

    fun onImportFileContent(content: String) {
        when (val result = ChannelJsonSerializer.importChannels(content)) {
            is ChannelImportResult.Success -> {
                _dialogState.value = _dialogState.value.copy(
                    pendingImportChannels = result.channels,
                    isImportConfirmDialogOpen = true
                )
            }
            is ChannelImportResult.Failure -> {
                _dialogState.value = _dialogState.value.copy(
                    userFeedbackMessage = result.reason
                )
            }
        }
    }

    fun onConfirmImport() {
        val channelsToImport = _dialogState.value.pendingImportChannels
        if (channelsToImport.isEmpty()) return

        _dialogState.value = _dialogState.value.copy(
            isImportConfirmDialogOpen = false,
            pendingImportChannels = emptyList()
        )

        viewModelScope.launch {
            repository.replaceAllChannels(channelsToImport)
            val count = channelsToImport.size
            val feedback = if (count == 1) "Successfully imported 1 channel" else "Successfully imported $count channels"
            _dialogState.value = _dialogState.value.copy(userFeedbackMessage = feedback)

            if (logoStorage != null) {
                channelsToImport.forEach { channel ->
                    if (!channel.logoUrl.isNullOrEmpty()) {
                        val cachedPath = logoStorage.downloadAndCacheLogo(channel.id, channel.logoUrl)
                        if (cachedPath != null) {
                            repository.updateChannel(channel.copy(localLogoPath = cachedPath))
                            incrementLogoVersion(channel.id)
                        }
                    }
                }
            }
        }
    }

    fun onDismissImportConfirmDialog() {
        _dialogState.value = _dialogState.value.copy(
            isImportConfirmDialogOpen = false,
            pendingImportChannels = emptyList()
        )
    }

    fun onExportChannelsJson(): String {
        return ChannelJsonSerializer.exportChannels(uiState.value.channels)
    }

    fun onExportComplete(success: Boolean, errorMessage: String? = null) {
        val feedback = if (success) {
            "Channels exported successfully"
        } else {
            errorMessage ?: "Failed to export channels"
        }
        _dialogState.value = _dialogState.value.copy(userFeedbackMessage = feedback)
    }

    fun onFeedbackMessageDismissed() {
        _dialogState.value = _dialogState.value.copy(userFeedbackMessage = null)
    }

    private data class DialogState(
        val isOpen: Boolean = false,
        val editingChannel: Channel? = null,
        val name: String = "",
        val streamUrls: List<String> = emptyList(),
        val logoUrl: String = "",
        val nameError: String? = null,
        val streamUrlsErrors: List<String?> = emptyList(),
        val logoUrlError: String? = null,
        val selectedChannel: Channel? = null,
        val isQrScannerOpen: Boolean = false,
        val qrScanTarget: QrScanTarget? = null,
        val qrError: String? = null,
        val deletingChannel: Channel? = null,
        val isImportConfirmDialogOpen: Boolean = false,
        val pendingImportChannels: List<Channel> = emptyList(),
        val userFeedbackMessage: String? = null
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
