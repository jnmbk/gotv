package com.gotv.player.ui.channel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gotv.player.R
import com.gotv.player.data.backup.ChannelImportResult
import com.gotv.player.data.backup.ChannelJsonSerializer
import com.gotv.player.data.backup.ChannelListDownloader
import com.gotv.player.data.backup.DownloadResult
import com.gotv.player.data.backup.HttpChannelListDownloader
import com.gotv.player.data.model.Channel
import com.gotv.player.data.model.ChannelValidator
import com.gotv.player.data.repository.ChannelRepository
import com.gotv.player.data.storage.LogoStorage
import com.gotv.player.ui.util.UiText
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
    private val logoStorage: LogoStorage? = null,
    private val channelListDownloader: ChannelListDownloader = HttpChannelListDownloader()
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
            userFeedbackMessage = dialog.userFeedbackMessage,
            isImportUrlDialogOpen = dialog.isImportUrlDialogOpen,
            importUrlInput = dialog.importUrlInput,
            importUrlError = dialog.importUrlError,
            isImportUrlLoading = dialog.isImportUrlLoading
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
            is QrScanTarget.ImportUrl -> current.copy(
                isQrScannerOpen = false,
                qrScanTarget = null,
                importUrlInput = ChannelValidator.stripDuplicateScheme(rawText),
                importUrlError = null
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
                nameError = validation.nameError?.let { UiText.StringResource(R.string.error_channel_name_empty) },
                streamUrlsErrors = validation.streamUrlsErrors.map { err ->
                    when (err) {
                        "Stream URL cannot be empty" -> UiText.StringResource(R.string.error_stream_url_empty)
                        "Please enter a valid stream URL" -> UiText.StringResource(R.string.error_stream_url_invalid)
                        null -> null
                        else -> UiText.DynamicString(err)
                    }
                },
                logoUrlError = validation.logoUrlError?.let { UiText.StringResource(R.string.error_logo_url_invalid) }
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
                    userFeedbackMessage = UiText.DynamicString(result.reason)
                )
            }
        }
    }

    fun onOpenImportUrlDialog() {
        _dialogState.value = _dialogState.value.copy(
            isImportUrlDialogOpen = true,
            importUrlInput = ChannelValidator.DEFAULT_URL_PREFIX,
            importUrlError = null,
            isImportUrlLoading = false
        )
    }

    fun onDismissImportUrlDialog() {
        _dialogState.value = _dialogState.value.copy(
            isImportUrlDialogOpen = false,
            importUrlInput = "",
            importUrlError = null,
            isImportUrlLoading = false
        )
    }

    fun onImportUrlChanged(url: String) {
        _dialogState.value = _dialogState.value.copy(
            importUrlInput = ChannelValidator.stripDuplicateScheme(url),
            importUrlError = null
        )
    }

    fun onClearImportUrl() {
        _dialogState.value = _dialogState.value.copy(
            importUrlInput = "",
            importUrlError = null
        )
    }

    fun onSubmitImportUrl() {
        val current = _dialogState.value
        if (current.isImportUrlLoading) return

        val normalizedUrl = ChannelValidator.normalizeUrl(current.importUrlInput)
        if (normalizedUrl.isEmpty()) {
            _dialogState.value = current.copy(importUrlError = UiText.StringResource(R.string.error_import_url_empty))
            return
        }

        if (!ChannelValidator.isValidHttpUrl(normalizedUrl)) {
            _dialogState.value = current.copy(importUrlError = UiText.StringResource(R.string.error_import_url_invalid))
            return
        }

        _dialogState.value = current.copy(isImportUrlLoading = true, importUrlError = null)

        viewModelScope.launch {
            when (val downloadResult = channelListDownloader.download(normalizedUrl)) {
                is DownloadResult.Failure -> {
                    _dialogState.value = _dialogState.value.copy(
                        isImportUrlLoading = false,
                        importUrlError = UiText.DynamicString(downloadResult.reason)
                    )
                }
                is DownloadResult.Success -> {
                    when (val parseResult = ChannelJsonSerializer.importChannels(downloadResult.content)) {
                        is ChannelImportResult.Failure -> {
                            _dialogState.value = _dialogState.value.copy(
                                isImportUrlLoading = false,
                                importUrlError = UiText.DynamicString(parseResult.reason)
                            )
                        }
                        is ChannelImportResult.Success -> {
                            _dialogState.value = _dialogState.value.copy(
                                isImportUrlDialogOpen = false,
                                isImportUrlLoading = false,
                                importUrlInput = "",
                                importUrlError = null,
                                pendingImportChannels = parseResult.channels,
                                isImportConfirmDialogOpen = true
                            )
                        }
                    }
                }
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
            val feedback = UiText.PluralResource(
                resId = R.plurals.channel_import_success_count,
                count = count
            )
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
            UiText.StringResource(R.string.feedback_export_success)
        } else {
            if (errorMessage != null) {
                UiText.DynamicString(errorMessage)
            } else {
                UiText.StringResource(R.string.feedback_export_failed)
            }
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
        val nameError: UiText? = null,
        val streamUrlsErrors: List<UiText?> = emptyList(),
        val logoUrlError: UiText? = null,
        val selectedChannel: Channel? = null,
        val isQrScannerOpen: Boolean = false,
        val qrScanTarget: QrScanTarget? = null,
        val qrError: UiText? = null,
        val deletingChannel: Channel? = null,
        val isImportConfirmDialogOpen: Boolean = false,
        val pendingImportChannels: List<Channel> = emptyList(),
        val userFeedbackMessage: UiText? = null,
        val isImportUrlDialogOpen: Boolean = false,
        val importUrlInput: String = "",
        val importUrlError: UiText? = null,
        val isImportUrlLoading: Boolean = false
    )

    class Factory(
        private val repository: ChannelRepository,
        private val logoStorage: LogoStorage? = null,
        private val channelListDownloader: ChannelListDownloader = HttpChannelListDownloader()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChannelGridViewModel(repository, logoStorage, channelListDownloader) as T
        }
    }
}
