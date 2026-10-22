package com.gotv.player.ui.channel

import com.gotv.player.data.model.Channel
import com.gotv.player.data.repository.InMemoryChannelRepository
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
    fun openAndDismissDialog_togglesDialogState() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onOpenAddDialog()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isAddDialogOpen)

        viewModel.onDismissAddDialog()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isAddDialogOpen)
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
}
