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
}
