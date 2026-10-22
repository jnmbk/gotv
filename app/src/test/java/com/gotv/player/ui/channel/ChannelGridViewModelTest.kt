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
    fun qrPayloadScanned_withValidJson_prefillsFormAndClosesScanner() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onOpenQrScanner()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isQrScannerOpen)

        viewModel.onQrPayloadScanned(
            """{"name":"Sports HD","url":"https://streams.example.com/sports.m3u8","logo":"https://example.com/logo.png"}"""
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isQrScannerOpen)
        assertNull(state.qrError)
        assertEquals("Sports HD", state.nameInput)
        assertEquals("https://streams.example.com/sports.m3u8", state.streamUrlInput)
        assertEquals("https://example.com/logo.png", state.logoUrlInput)
    }

    @Test
    fun qrPayloadScanned_withInvalidPayload_setsQrErrorAndClosesScanner() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        viewModel.onOpenQrScanner()

        viewModel.onQrPayloadScanned("not-a-valid-qr-payload")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isQrScannerOpen)
        assertNotNull(state.qrError)
        assertEquals("https://", state.streamUrlInput)
    }

    @Test
    fun qrPayloadScanned_withNullFromEmptyImage_setsQrError() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()

        viewModel.onQrPayloadScanned(null)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.qrError)
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
}
