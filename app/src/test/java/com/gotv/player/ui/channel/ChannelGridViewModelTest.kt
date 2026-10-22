package com.gotv.player.ui.channel

import com.gotv.player.data.model.Channel
import com.gotv.player.data.repository.InMemoryChannelRepository
import com.gotv.player.data.storage.InMemoryLogoStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChannelGridViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: InMemoryChannelRepository
    private lateinit var viewModel: ChannelGridViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = InMemoryChannelRepository()
        viewModel = ChannelGridViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_isEmptyWhenNoChannels() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state.channels.isEmpty())
        assertTrue(state.isEmpty)
        assertFalse(state.isAddDialogOpen)
    }

    @Test
    fun openAndDismissDialog_togglesDialogStateAndPrefillsHttps() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAddDialogOpen)
        assertEquals("https://", viewModel.uiState.value.streamUrlInput)
        assertEquals("https://", viewModel.uiState.value.logoUrlInput)

        viewModel.onDismissAddDialog()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isAddDialogOpen)
    }

    @Test
    fun saveChannel_withHandwrittenUrlsWithoutScheme_prependsHttpsAndSaves() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onNameChanged("BBC One")
        viewModel.onStreamUrlChanged("streams.example.com/live/bbc1")
        viewModel.onLogoUrlChanged("example.com/bbc1-logo.png")

        val saved = viewModel.onSaveChannel()
        assertTrue(saved)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.channels.size)
        assertEquals("https://streams.example.com/live/bbc1", state.channels[0].streamUrl)
        assertEquals("https://example.com/bbc1-logo.png", state.channels[0].logoUrl)
    }

    @Test
    fun saveChannel_withInvalidInputs_setsErrorsAndDoesNotSave() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onNameChanged("   ")
        viewModel.onStreamUrlChanged("invalid-url")

        val saved = viewModel.onSaveChannel()
        assertFalse(saved)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAddDialogOpen)
        assertEquals("Channel name cannot be empty", state.nameError)
        assertNotNull(state.streamUrlError)
        assertTrue(state.channels.isEmpty())
    }

    @Test
    fun saveChannel_withValidInputs_savesChannelAndDismissesDialog() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onNameChanged("BBC One")
        viewModel.onStreamUrlChanged("https://streams.example.com/bbc1.m3u8")

        val saved = viewModel.onSaveChannel()
        assertTrue(saved)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isAddDialogOpen)
        assertEquals(1, state.channels.size)
        assertEquals("BBC One", state.channels[0].name)
        assertEquals("https://streams.example.com/bbc1.m3u8", state.channels[0].streamUrl)
        assertFalse(state.isEmpty)
    }

    @Test
    fun saveChannel_withValidLogoUrl_downloadsAndUpdatesLocalLogoPath() = runTest(testDispatcher) {
        val logoStorage = InMemoryLogoStorage()
        val customViewModel = ChannelGridViewModel(repository, logoStorage)

        advanceUntilIdle()
        customViewModel.onOpenAddDialog()
        customViewModel.onNameChanged("CNN")
        customViewModel.onStreamUrlChanged("https://streams.example.com/cnn.m3u8")
        customViewModel.onLogoUrlChanged("https://example.com/cnn-logo.png")

        val saved = customViewModel.onSaveChannel()
        assertTrue(saved)
        advanceUntilIdle()

        val state = customViewModel.uiState.value
        assertEquals(1, state.channels.size)
        val channel = state.channels[0]
        assertEquals("CNN", channel.name)
        assertEquals("https://example.com/cnn-logo.png", channel.logoUrl)
        assertNotNull(channel.localLogoPath)
        assertTrue(channel.localLogoPath!!.endsWith(".png"))
        assertEquals(1, logoStorage.downloadCalls)
    }

    @Test
    fun saveChannel_withFailingLogoUrl_leavesLocalLogoPathNullGracefully() = runTest(testDispatcher) {
        val failingLogoStorage = InMemoryLogoStorage(shouldFail = true)
        val customViewModel = ChannelGridViewModel(repository, failingLogoStorage)

        advanceUntilIdle()
        customViewModel.onOpenAddDialog()
        customViewModel.onNameChanged("Discovery")
        customViewModel.onStreamUrlChanged("https://streams.example.com/disc.m3u8")
        customViewModel.onLogoUrlChanged("https://example.com/offline-logo.png")

        val saved = customViewModel.onSaveChannel()
        assertTrue(saved)
        advanceUntilIdle()

        val state = customViewModel.uiState.value
        assertEquals(1, state.channels.size)
        val channel = state.channels[0]
        assertEquals("Discovery", channel.name)
        assertEquals("https://example.com/offline-logo.png", channel.logoUrl)
        assertNull(channel.localLogoPath)
        assertEquals(1, failingLogoStorage.downloadCalls)
    }

    @Test
    fun channelClicked_updatesSelectedChannel() = runTest(testDispatcher) {
        val channel = Channel(name = "Discovery", streamUrl = "https://example.com/live.m3u8")
        repository.addChannel(channel)
        advanceUntilIdle()

        viewModel.onChannelClicked(channel)
        advanceUntilIdle()
        assertEquals(channel, viewModel.uiState.value.selectedChannel)

        viewModel.onChannelSelectionHandled()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.selectedChannel)
    }

    @Test
    fun qrScanning_streamUrlTarget_populatesOnlyStreamUrlAndDismissesScanner() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onNameChanged("Test Channel")
        viewModel.onLogoUrlChanged("https://example.com/logo.png")
        viewModel.onOpenQrScanner(QrScanTarget.StreamUrl())
        advanceUntilIdle()

        val scanningState = viewModel.uiState.value
        assertTrue(scanningState.isQrScannerOpen)
        assertEquals(QrScanTarget.StreamUrl(), scanningState.qrScanTarget)

        viewModel.onQrPayloadScanned("https://streams.example.com/live.m3u8")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isQrScannerOpen)
        assertNull(state.qrScanTarget)
        assertEquals("https://streams.example.com/live.m3u8", state.streamUrlInput)
        assertEquals("Test Channel", state.nameInput)
        assertEquals("https://example.com/logo.png", state.logoUrlInput)
    }

    @Test
    fun qrScanning_logoUrlTarget_populatesOnlyLogoUrlAndDismissesScanner() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onNameChanged("Test Channel 2")
        viewModel.onStreamUrlChanged("https://streams.example.com/stream.m3u8")
        viewModel.onOpenQrScanner(QrScanTarget.LogoUrl)
        advanceUntilIdle()

        val scanningState = viewModel.uiState.value
        assertTrue(scanningState.isQrScannerOpen)
        assertEquals(QrScanTarget.LogoUrl, scanningState.qrScanTarget)

        viewModel.onQrPayloadScanned("https://example.com/scanned-logo.png")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isQrScannerOpen)
        assertNull(state.qrScanTarget)
        assertEquals("https://example.com/scanned-logo.png", state.logoUrlInput)
        assertEquals("https://streams.example.com/stream.m3u8", state.streamUrlInput)
        assertEquals("Test Channel 2", state.nameInput)
    }

    @Test
    fun qrScanning_scannerDismiss_leavesInputContentsUnchanged() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onNameChanged("Original Channel")
        viewModel.onStreamUrlChanged("https://original.example.com/live.m3u8")
        viewModel.onLogoUrlChanged("https://original.example.com/logo.png")

        viewModel.onOpenQrScanner(QrScanTarget.StreamUrl())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isQrScannerOpen)

        viewModel.onCloseQrScanner()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isQrScannerOpen)
        assertNull(state.qrScanTarget)
        assertEquals("Original Channel", state.nameInput)
        assertEquals("https://original.example.com/live.m3u8", state.streamUrlInput)
        assertEquals("https://original.example.com/logo.png", state.logoUrlInput)
    }

    @Test
    fun qrScanning_nullPayload_dismissesScannerWithoutChangingInputs() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onStreamUrlChanged("https://existing.example.com/live.m3u8")
        viewModel.onOpenQrScanner(QrScanTarget.StreamUrl())
        advanceUntilIdle()

        viewModel.onQrPayloadScanned(null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isQrScannerOpen)
        assertNull(state.qrScanTarget)
        assertEquals("https://existing.example.com/live.m3u8", state.streamUrlInput)
    }

    @Test
    fun editChannel_prefillsFields_andUpdatesNameAndStreamUrl_preservingLogo() = runTest(testDispatcher) {
        val logoStorage = InMemoryLogoStorage()
        logoStorage.setCachedLogo("c1", "/mock/cache/c1.png")
        val existing = Channel(
            id = "c1",
            name = "Old Name",
            streamUrl = "https://old.example.com/live.m3u8",
            logoUrl = "https://example.com/logo.png",
            localLogoPath = "/mock/cache/c1.png"
        )
        repository.addChannel(existing)
        val customViewModel = ChannelGridViewModel(repository, logoStorage)
        advanceUntilIdle()

        customViewModel.onOpenEditDialog(existing)
        advanceUntilIdle()

        val dialogState = customViewModel.uiState.value
        assertTrue(dialogState.isAddDialogOpen)
        assertTrue(dialogState.isEditMode)
        assertEquals("Old Name", dialogState.nameInput)
        assertEquals("https://old.example.com/live.m3u8", dialogState.streamUrlInput)
        assertEquals("https://example.com/logo.png", dialogState.logoUrlInput)

        customViewModel.onNameChanged("New Name")
        customViewModel.onStreamUrlChanged("https://new.example.com/live.m3u8")
        assertTrue(customViewModel.onSaveChannel())
        advanceUntilIdle()

        val updated = customViewModel.uiState.value.channels.single()
        assertEquals("New Name", updated.name)
        assertEquals("https://new.example.com/live.m3u8", updated.streamUrl)
        assertEquals("https://example.com/logo.png", updated.logoUrl)
        assertEquals("/mock/cache/c1.png", updated.localLogoPath)
        assertEquals(0, logoStorage.downloadCalls)
    }

    @Test
    fun editChannel_whenLogoUrlCleared_deletesCachedLogoAndClearsLocalLogoPath() = runTest(testDispatcher) {
        val logoStorage = InMemoryLogoStorage()
        logoStorage.setCachedLogo("c1", "/mock/cache/c1.png")
        val existing = Channel(
            id = "c1",
            name = "Channel 1",
            streamUrl = "https://example.com/live.m3u8",
            logoUrl = "https://example.com/logo.png",
            localLogoPath = "/mock/cache/c1.png"
        )
        repository.addChannel(existing)
        val customViewModel = ChannelGridViewModel(repository, logoStorage)
        advanceUntilIdle()

        customViewModel.onOpenEditDialog(existing)
        customViewModel.onLogoUrlChanged("")
        assertTrue(customViewModel.onSaveChannel())
        advanceUntilIdle()

        val updated = customViewModel.uiState.value.channels.single()
        assertNull(updated.logoUrl)
        assertNull(updated.localLogoPath)
        assertTrue(logoStorage.deletedChannelIds.contains("c1"))
        assertFalse(logoStorage.hasCachedLogo("c1"))
    }

    @Test
    fun editChannel_whenLogoUrlChanged_forcesRedownloadAndUpdatesLogo() = runTest(testDispatcher) {
        val logoStorage = InMemoryLogoStorage()
        logoStorage.setCachedLogo("c1", "/mock/cache/c1.png")
        val existing = Channel(
            id = "c1",
            name = "Channel 1",
            streamUrl = "https://example.com/live.m3u8",
            logoUrl = "https://example.com/old-logo.png",
            localLogoPath = "/mock/cache/c1.png"
        )
        repository.addChannel(existing)
        val customViewModel = ChannelGridViewModel(repository, logoStorage)
        advanceUntilIdle()

        customViewModel.onOpenEditDialog(existing)
        customViewModel.onLogoUrlChanged("https://example.com/new-logo.png")
        assertTrue(customViewModel.onSaveChannel())
        advanceUntilIdle()

        val updated = customViewModel.uiState.value.channels.single()
        assertEquals("https://example.com/new-logo.png", updated.logoUrl)
        assertEquals("/mock/cache/c1.png", updated.localLogoPath)
        assertEquals(1, logoStorage.downloadCalls)
        assertTrue(logoStorage.lastForceRefresh)
        assertEquals(1, customViewModel.uiState.value.logoReloadVersions["c1"])
    }

    @Test
    fun editChannel_whenNewLogoDownloadFails_deletesOldCachedLogoAndClearsLocalLogoPath() = runTest(testDispatcher) {
        val logoStorage = InMemoryLogoStorage(shouldFail = true)
        logoStorage.setCachedLogo("c1", "/mock/cache/c1.png")
        val existing = Channel(
            id = "c1",
            name = "Channel 1",
            streamUrl = "https://example.com/live.m3u8",
            logoUrl = "https://example.com/old-logo.png",
            localLogoPath = "/mock/cache/c1.png"
        )
        repository.addChannel(existing)
        val customViewModel = ChannelGridViewModel(repository, logoStorage)
        advanceUntilIdle()

        customViewModel.onOpenEditDialog(existing)
        customViewModel.onLogoUrlChanged("https://example.com/broken-logo.png")
        assertTrue(customViewModel.onSaveChannel())
        advanceUntilIdle()

        val updated = customViewModel.uiState.value.channels.single()
        assertEquals("https://example.com/broken-logo.png", updated.logoUrl)
        assertNull(updated.localLogoPath)
        assertTrue(logoStorage.deletedChannelIds.contains("c1"))
    }

    @Test
    fun redownloadLogo_forcesRefreshAndIncrementsVersionOnSuccess_andPreservesOnFailure() = runTest(testDispatcher) {
        val logoStorage = InMemoryLogoStorage()
        val existing = Channel(
            id = "c1",
            name = "Channel 1",
            streamUrl = "https://example.com/live.m3u8",
            logoUrl = "https://example.com/logo.png",
            localLogoPath = "/mock/cache/c1.png"
        )
        logoStorage.setCachedLogo("c1", "/mock/cache/c1.png")
        repository.addChannel(existing)
        val customViewModel = ChannelGridViewModel(repository, logoStorage)
        advanceUntilIdle()

        customViewModel.onRedownloadLogo(existing)
        advanceUntilIdle()

        assertEquals(1, logoStorage.downloadCalls)
        assertTrue(logoStorage.lastForceRefresh)
        assertEquals(1, customViewModel.uiState.value.logoReloadVersions["c1"])

        // When forced redownload fails, existing localLogoPath and cached file are preserved
        logoStorage.shouldFail = true
        val afterFirst = customViewModel.uiState.value.channels.single()
        customViewModel.onRedownloadLogo(afterFirst)
        advanceUntilIdle()

        assertEquals(2, logoStorage.downloadCalls)
        assertEquals("/mock/cache/c1.png", customViewModel.uiState.value.channels.single().localLogoPath)
        assertTrue(logoStorage.hasCachedLogo("c1"))
        assertEquals(1, customViewModel.uiState.value.logoReloadVersions["c1"])
    }

    @Test
    fun deleteChannel_requiresConfirmation_andDeletesChannelAndCachedLogo() = runTest(testDispatcher) {
        val logoStorage = InMemoryLogoStorage()
        logoStorage.setCachedLogo("c1", "/mock/cache/c1.png")
        val existing = Channel(
            id = "c1",
            name = "Channel 1",
            streamUrl = "https://example.com/live.m3u8",
            logoUrl = "https://example.com/logo.png",
            localLogoPath = "/mock/cache/c1.png"
        )
        repository.addChannel(existing)
        val customViewModel = ChannelGridViewModel(repository, logoStorage)
        advanceUntilIdle()

        customViewModel.onRequestDeleteChannel(existing)
        advanceUntilIdle()
        assertEquals(existing, customViewModel.uiState.value.deletingChannel)

        // Dismissing keeps the channel
        customViewModel.onDismissDeleteDialog()
        advanceUntilIdle()
        assertNull(customViewModel.uiState.value.deletingChannel)
        assertEquals(1, customViewModel.uiState.value.channels.size)

        // Confirming deletes channel and cached logo
        customViewModel.onRequestDeleteChannel(existing)
        customViewModel.onConfirmDeleteChannel()
        advanceUntilIdle()

        assertNull(customViewModel.uiState.value.deletingChannel)
        assertTrue(customViewModel.uiState.value.channels.isEmpty())
        assertTrue(logoStorage.deletedChannelIds.contains("c1"))
    }

    @Test
    fun reorderChannels_updatesRepositoryOrder() = runTest(testDispatcher) {
        val c1 = Channel(id = "c1", name = "One", streamUrl = "https://example.com/1.m3u8")
        val c2 = Channel(id = "c2", name = "Two", streamUrl = "https://example.com/2.m3u8")
        val c3 = Channel(id = "c3", name = "Three", streamUrl = "https://example.com/3.m3u8")
        repository.addChannel(c1)
        repository.addChannel(c2)
        repository.addChannel(c3)
        advanceUntilIdle()

        assertEquals(listOf("c3", "c2", "c1"), viewModel.uiState.value.channels.map { it.id })

        viewModel.onReorderChannels(listOf("c1", "c3", "c2"))
        advanceUntilIdle()

        assertEquals(listOf("c1", "c3", "c2"), viewModel.uiState.value.channels.map { it.id })
    }

    @Test
    fun onOpenAddDialog_initializesWithSinglePrefilledStreamUrl() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("https://"), state.streamUrlsInput)
        assertEquals("https://", state.streamUrlInput)
        assertTrue(state.streamUrlsErrors.isEmpty())
    }

    @Test
    fun onAddStreamUrlField_appendsPrefilledStreamUrlField() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onAddStreamUrlField()
        viewModel.onAddStreamUrlField()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("https://", "https://", "https://"), state.streamUrlsInput)
    }

    @Test
    fun onRemoveStreamUrlField_removesFieldAtIndex_whenMultipleFieldsExist() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onStreamUrlChanged(0, "https://stream1.m3u8")
        viewModel.onAddStreamUrlField()
        viewModel.onStreamUrlChanged(1, "https://stream2.m3u8")
        viewModel.onAddStreamUrlField()
        viewModel.onStreamUrlChanged(2, "https://stream3.m3u8")
        advanceUntilIdle()

        // Remove middle field
        viewModel.onRemoveStreamUrlField(1)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("https://stream1.m3u8", "https://stream3.m3u8"), state.streamUrlsInput)
    }

    @Test
    fun onRemoveStreamUrlField_doesNotRemoveWhenOnlyOneFieldRemains() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onStreamUrlChanged(0, "https://only-stream.m3u8")
        advanceUntilIdle()

        viewModel.onRemoveStreamUrlField(0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(listOf("https://only-stream.m3u8"), state.streamUrlsInput)
    }

    @Test
    fun onStreamUrlChanged_updatesSpecificIndex() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onAddStreamUrlField()
        viewModel.onStreamUrlChanged(1, "https://backup.example.com/live.m3u8")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("https://", state.streamUrlsInput[0])
        assertEquals("https://backup.example.com/live.m3u8", state.streamUrlsInput[1])
    }

    @Test
    fun onOpenEditDialog_prefillsAllStreamUrlsFromChannel() = runTest(testDispatcher) {
        val channel = Channel(
            id = "c1",
            name = "Multi Stream News",
            streamUrls = listOf(
                "https://primary.example.com/live.m3u8",
                "https://backup1.example.com/live.m3u8",
                "https://backup2.example.com/live.m3u8"
            ),
            logoUrl = "https://example.com/logo.png"
        )
        repository.addChannel(channel)
        advanceUntilIdle()

        viewModel.onOpenEditDialog(channel)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAddDialogOpen)
        assertTrue(state.isEditMode)
        assertEquals("Multi Stream News", state.nameInput)
        assertEquals(channel.streamUrls, state.streamUrlsInput)
        assertEquals(channel.streamUrls.first(), state.streamUrlInput)
        assertEquals("https://example.com/logo.png", state.logoUrlInput)
    }

    @Test
    fun onSaveChannel_withMultipleValidStreamUrls_persistsListToRepository() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onNameChanged("Multi Channel")
        viewModel.onStreamUrlChanged(0, "example.com/primary.m3u8")
        viewModel.onAddStreamUrlField()
        viewModel.onStreamUrlChanged(1, "example.com/backup.m3u8")

        val saved = viewModel.onSaveChannel()
        assertTrue(saved)
        advanceUntilIdle()

        val channels = viewModel.uiState.value.channels
        assertEquals(1, channels.size)
        val savedChannel = channels[0]
        assertEquals("Multi Channel", savedChannel.name)
        assertEquals(
            listOf("https://example.com/primary.m3u8", "https://example.com/backup.m3u8"),
            savedChannel.streamUrls
        )
    }

    @Test
    fun onSaveChannel_withInvalidSecondaryStreamUrl_setsErrorsAndDoesNotSave() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onNameChanged("Valid Name")
        viewModel.onStreamUrlChanged(0, "https://example.com/live.m3u8")
        viewModel.onAddStreamUrlField()
        viewModel.onStreamUrlChanged(1, "invalid-url")

        val saved = viewModel.onSaveChannel()
        assertFalse(saved)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isAddDialogOpen)
        assertEquals(listOf(null, "Please enter a valid stream URL"), state.streamUrlsErrors)
        assertEquals("Please enter a valid stream URL", state.streamUrlError)
        assertTrue(state.channels.isEmpty())
    }

    @Test
    fun onSaveChannel_inEditMode_updatesMultipleStreamUrlsInRepository() = runTest(testDispatcher) {
        val channel = Channel(
            id = "c1",
            name = "Initial Name",
            streamUrls = listOf("https://initial.example.com/live.m3u8")
        )
        repository.addChannel(channel)
        advanceUntilIdle()

        viewModel.onOpenEditDialog(channel)
        viewModel.onNameChanged("Updated Name")
        viewModel.onAddStreamUrlField()
        viewModel.onStreamUrlChanged(1, "https://new-backup.example.com/live.m3u8")

        val saved = viewModel.onSaveChannel()
        assertTrue(saved)
        advanceUntilIdle()

        val updated = repository.getChannelById("c1")
        assertNotNull(updated)
        assertEquals("Updated Name", updated!!.name)
        assertEquals(
            listOf("https://initial.example.com/live.m3u8", "https://new-backup.example.com/live.m3u8"),
            updated.streamUrls
        )
    }

    @Test
    fun onQrPayloadScanned_targetingSecondaryStreamUrl_updatesTargetedField() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onStreamUrlChanged(0, "https://first.example.com/live.m3u8")
        viewModel.onAddStreamUrlField()
        advanceUntilIdle()

        viewModel.onOpenQrScanner(QrScanTarget.StreamUrl(index = 1))
        advanceUntilIdle()

        viewModel.onQrPayloadScanned("https://scanned-backup.example.com/live.m3u8")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isQrScannerOpen)
        assertNull(state.qrScanTarget)
        assertEquals(
            listOf("https://first.example.com/live.m3u8", "https://scanned-backup.example.com/live.m3u8"),
            state.streamUrlsInput
        )
    }

    @Test
    fun onImportFileContent_validJson_opensConfirmationDialogWithPendingChannels() = runTest(testDispatcher) {
        advanceUntilIdle()
        val validJson = """
            {
              "version": 1,
              "channels": [
                {
                  "name": "Sky News",
                  "streamUrls": ["https://sky.com/live.m3u8"]
                }
              ]
            }
        """.trimIndent()

        viewModel.onImportFileContent(validJson)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isImportConfirmDialogOpen)
        assertEquals(1, state.pendingImportChannels.size)
        assertEquals("Sky News", state.pendingImportChannels[0].name)
        assertNull(state.userFeedbackMessage)
    }

    @Test
    fun onImportFileContent_invalidOrNoChannels_setsFeedbackMessageWithoutDialog() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onImportFileContent("{bad json}")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isImportConfirmDialogOpen)
        assertTrue(state.pendingImportChannels.isEmpty())
        assertNotNull(state.userFeedbackMessage)

        viewModel.onFeedbackMessageDismissed()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.userFeedbackMessage)
    }

    @Test
    fun onConfirmImport_replacesChannelsAndSetsSuccessMessage() = runTest(testDispatcher) {
        advanceUntilIdle()
        repository.addChannel(Channel(name = "Existing Channel", streamUrl = "https://existing.com/live.m3u8"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.channels.size)

        val importJson = """
            {
              "version": 1,
              "channels": [
                {
                  "name": "Imported 1",
                  "streamUrls": ["https://imp1.com/live.m3u8"]
                },
                {
                  "name": "Imported 2",
                  "streamUrls": ["https://imp2.com/live.m3u8"]
                }
              ]
            }
        """.trimIndent()

        viewModel.onImportFileContent(importJson)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isImportConfirmDialogOpen)

        viewModel.onConfirmImport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isImportConfirmDialogOpen)
        assertEquals(2, state.channels.size)
        assertEquals("Imported 1", state.channels[0].name)
        assertEquals("Imported 2", state.channels[1].name)
        assertEquals("Successfully imported 2 channels", state.userFeedbackMessage)
    }

    @Test
    fun onDismissImportConfirmDialog_clearsPendingChannelsWithoutReplacing() = runTest(testDispatcher) {
        advanceUntilIdle()
        repository.addChannel(Channel(name = "Original", streamUrl = "https://orig.com/live.m3u8"))
        advanceUntilIdle()

        val importJson = """
            {
              "version": 1,
              "channels": [
                {
                  "name": "Should Not Import",
                  "streamUrls": ["https://no.com/live.m3u8"]
                }
              ]
            }
        """.trimIndent()

        viewModel.onImportFileContent(importJson)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isImportConfirmDialogOpen)

        viewModel.onDismissImportConfirmDialog()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isImportConfirmDialogOpen)
        assertTrue(state.pendingImportChannels.isEmpty())
        assertEquals(1, state.channels.size)
        assertEquals("Original", state.channels[0].name)
    }

    @Test
    fun onExportChannelsJson_returnsSerializedCurrentChannels() = runTest(testDispatcher) {
        advanceUntilIdle()
        repository.addChannel(Channel(name = "Export 1", streamUrl = "https://export1.com/live.m3u8"))
        advanceUntilIdle()

        val exportedJson = viewModel.onExportChannelsJson()
        val parsed = com.gotv.player.data.backup.ChannelJsonSerializer.importChannels(exportedJson)
        assertTrue(parsed is com.gotv.player.data.backup.ChannelImportResult.Success)
        val channels = (parsed as com.gotv.player.data.backup.ChannelImportResult.Success).channels
        assertEquals(1, channels.size)
        assertEquals("Export 1", channels[0].name)
    }

    @Test
    fun onExportComplete_setsFeedbackMessage() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onExportComplete(true)
        advanceUntilIdle()
        assertEquals("Channels exported successfully", viewModel.uiState.value.userFeedbackMessage)

        viewModel.onExportComplete(false, "Write failed")
        advanceUntilIdle()
        assertEquals("Write failed", viewModel.uiState.value.userFeedbackMessage)
    }
}
