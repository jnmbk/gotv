package com.gotv.player.data.repository

import androidx.test.core.app.ApplicationProvider
import com.gotv.player.data.model.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ChannelRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    @Test
    fun inMemoryRepository_crudOperations() = runTest {
        val repo = InMemoryChannelRepository()
        assertTrue(repo.getAllChannels().first().isEmpty())

        val ch1 = Channel(
            id = "1",
            name = "Test Channel 1",
            streamUrl = "https://test.com/stream1.m3u8"
        )
        repo.addChannel(ch1)
        val channels = repo.getAllChannels().first()
        assertEquals(1, channels.size)
        assertEquals("Test Channel 1", channels[0].name)

        val fetched = repo.getChannelById("1")
        assertNotNull(fetched)
        assertEquals("1", fetched?.id)

        val updated = ch1.copy(name = "Updated Name")
        repo.updateChannel(updated)
        assertEquals("Updated Name", repo.getChannelById("1")?.name)

        repo.deleteChannel("1")
        assertTrue(repo.getAllChannels().first().isEmpty())
    }

    @Test
    fun sqliteRepository_persistsAndRetrievesChannels() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = SqliteChannelRepository(
            context = context,
            dbName = "test_channels.db",
            ioDispatcher = testDispatcher
        )

        val ch1 = Channel(
            id = "c1",
            name = "News 24",
            streamUrl = "https://news.com/live.m3u8",
            logoUrl = "https://news.com/logo.png",
            createdAt = 1000L
        )
        val ch2 = Channel(
            id = "c2",
            name = "Sports 1",
            streamUrl = "https://sports.com/stream.m3u8",
            createdAt = 2000L
        )

        repo.addChannel(ch1)
        repo.addChannel(ch2)

        val channels = repo.getAllChannels().first()
        assertEquals(2, channels.size)
        // Ordered by createdAt DESC
        assertEquals("Sports 1", channels[0].name)
        assertEquals("News 24", channels[1].name)

        // Verify across a new repository instance (simulating app restart)
        val restartedRepo = SqliteChannelRepository(
            context = context,
            dbName = "test_channels.db",
            ioDispatcher = testDispatcher
        )
        val restartedChannels = restartedRepo.getAllChannels().first()
        assertEquals(2, restartedChannels.size)
        assertEquals("Sports 1", restartedChannels[0].name)
        assertEquals("News 24", restartedChannels[1].name)

        // Delete channel
        restartedRepo.deleteChannel("c1")
        val remaining = restartedRepo.getAllChannels().first()
        assertEquals(1, remaining.size)
        assertEquals("Sports 1", remaining[0].name)
        assertNull(restartedRepo.getChannelById("c1"))
    }
}

