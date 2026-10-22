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

    @Test
    fun inMemoryRepository_prependsNewChannels_andReordersChannels() = runTest {
        val repo = InMemoryChannelRepository()
        val ch1 = Channel(id = "1", name = "First", streamUrl = "https://test.com/1.m3u8")
        val ch2 = Channel(id = "2", name = "Second", streamUrl = "https://test.com/2.m3u8")
        val ch3 = Channel(id = "3", name = "Third", streamUrl = "https://test.com/3.m3u8")

        repo.addChannel(ch1)
        repo.addChannel(ch2)
        repo.addChannel(ch3)

        // Newest added is at position 0
        assertEquals(listOf("3", "2", "1"), repo.getAllChannels().first().map { it.id })
        assertEquals(listOf(0, 1, 2), repo.getAllChannels().first().map { it.position })

        // Reorder to 1, 3, 2
        repo.reorderChannels(listOf("1", "3", "2"))
        val reordered = repo.getAllChannels().first()
        assertEquals(listOf("1", "3", "2"), reordered.map { it.id })
        assertEquals(listOf(0, 1, 2), reordered.map { it.position })

        // Adding a 4th channel puts it at position 0 ahead of custom order
        val ch4 = Channel(id = "4", name = "Fourth", streamUrl = "https://test.com/4.m3u8")
        repo.addChannel(ch4)
        assertEquals(listOf("4", "1", "3", "2"), repo.getAllChannels().first().map { it.id })
    }

    @Test
    fun sqliteRepository_reordersAndPersistsPositionsAcrossRestarts() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "test_reorder_channels.db"
        val repo = SqliteChannelRepository(
            context = context,
            dbName = dbName,
            ioDispatcher = testDispatcher
        )

        val ch1 = Channel(id = "c1", name = "Alpha", streamUrl = "https://a.com/1.m3u8", createdAt = 1000L)
        val ch2 = Channel(id = "c2", name = "Beta", streamUrl = "https://a.com/2.m3u8", createdAt = 2000L)
        val ch3 = Channel(id = "c3", name = "Gamma", streamUrl = "https://a.com/3.m3u8", createdAt = 3000L)

        repo.addChannel(ch1)
        repo.addChannel(ch2)
        repo.addChannel(ch3)

        // Newly added channels appear at position 0 (Gamma, Beta, Alpha)
        assertEquals(listOf("c3", "c2", "c1"), repo.getAllChannels().first().map { it.id })

        // Reorder to Alpha, Gamma, Beta
        repo.reorderChannels(listOf("c1", "c3", "c2"))
        assertEquals(listOf("c1", "c3", "c2"), repo.getAllChannels().first().map { it.id })
        assertEquals(listOf(0, 1, 2), repo.getAllChannels().first().map { it.position })

        // Verify persistence across restart
        val restartedRepo = SqliteChannelRepository(
            context = context,
            dbName = dbName,
            ioDispatcher = testDispatcher
        )
        assertEquals(listOf("c1", "c3", "c2"), restartedRepo.getAllChannels().first().map { it.id })

        // Adding a new channel places it at position 0
        val ch4 = Channel(id = "c4", name = "Delta", streamUrl = "https://a.com/4.m3u8", createdAt = 4000L)
        restartedRepo.addChannel(ch4)
        assertEquals(listOf("c4", "c1", "c3", "c2"), restartedRepo.getAllChannels().first().map { it.id })
    }

    @Test
    fun sqliteRepository_supportsMultipleStreamUrlsAndLastPlayedPersistence() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "test_multiple_urls.db"
        val repo = SqliteChannelRepository(
            context = context,
            dbName = dbName,
            ioDispatcher = testDispatcher
        )

        val ch = Channel(
            id = "c1",
            name = "Failover HD",
            streamUrls = listOf("https://s1.test/live.m3u8", "https://s2.test/live.m3u8", "https://s3.test/live.m3u8"),
            lastPlayedUrl = "https://s2.test/live.m3u8"
        )
        repo.addChannel(ch)

        val loaded = repo.getChannelById("c1")
        assertNotNull(loaded)
        assertEquals(listOf("https://s1.test/live.m3u8", "https://s2.test/live.m3u8", "https://s3.test/live.m3u8"), loaded?.streamUrls)
        assertEquals("https://s2.test/live.m3u8", loaded?.lastPlayedUrl)
        assertEquals("https://s2.test/live.m3u8", loaded?.streamUrl)

        // Update last played URL to s3
        repo.updateLastPlayedUrl("c1", "https://s3.test/live.m3u8")
        val updated = repo.getChannelById("c1")
        assertEquals("https://s3.test/live.m3u8", updated?.lastPlayedUrl)
        assertEquals("https://s3.test/live.m3u8", updated?.streamUrl)
        assertEquals(0, updated?.position)

        // Verify persistence across repository restart
        val restartedRepo = SqliteChannelRepository(
            context = context,
            dbName = dbName,
            ioDispatcher = testDispatcher
        )
        val restartedLoaded = restartedRepo.getChannelById("c1")
        assertEquals(listOf("https://s1.test/live.m3u8", "https://s2.test/live.m3u8", "https://s3.test/live.m3u8"), restartedLoaded?.streamUrls)
        assertEquals("https://s3.test/live.m3u8", restartedLoaded?.lastPlayedUrl)
        assertEquals("https://s3.test/live.m3u8", restartedLoaded?.streamUrl)
    }

    @Test
    fun sqliteRepository_migratesV2DatabaseWithoutDataLoss() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val dbName = "test_migration_v2_to_v3.db"

        // Set up database directly with V2 schema
        val v2Helper = object : android.database.sqlite.SQLiteOpenHelper(context, dbName, null, 2) {
            override fun onCreate(db: android.database.sqlite.SQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE channels (
                        id TEXT PRIMARY KEY,
                        name TEXT NOT NULL,
                        stream_url TEXT NOT NULL,
                        logo_url TEXT,
                        local_logo_path TEXT,
                        created_at INTEGER NOT NULL,
                        position INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
            }
            override fun onUpgrade(db: android.database.sqlite.SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
        }

        val db = v2Helper.writableDatabase
        val cv1 = android.content.ContentValues().apply {
            put("id", "c1")
            put("name", "Legacy News")
            put("stream_url", "https://legacy.test/news.m3u8")
            put("logo_url", "https://legacy.test/news.png")
            put("created_at", 1000L)
            put("position", 0)
        }
        val cv2 = android.content.ContentValues().apply {
            put("id", "c2")
            put("name", "Legacy Sports")
            put("stream_url", "https://legacy.test/sports.m3u8")
            put("created_at", 2000L)
            put("position", 1)
        }
        db.insert("channels", null, cv1)
        db.insert("channels", null, cv2)
        v2Helper.close()

        // Open database using SqliteChannelRepository (which runs V3 migration)
        val v3Repo = SqliteChannelRepository(
            context = context,
            dbName = dbName,
            ioDispatcher = testDispatcher
        )

        val channels = v3Repo.getAllChannels().first()
        assertEquals(2, channels.size)

        val c1 = channels.first { it.id == "c1" }
        assertEquals("Legacy News", c1.name)
        assertEquals("https://legacy.test/news.m3u8", c1.streamUrl)
        assertEquals(listOf("https://legacy.test/news.m3u8"), c1.streamUrls)
        assertNull(c1.lastPlayedUrl)
        assertEquals("https://legacy.test/news.png", c1.logoUrl)
        assertEquals(0, c1.position)

        val c2 = channels.first { it.id == "c2" }
        assertEquals("Legacy Sports", c2.name)
        assertEquals("https://legacy.test/sports.m3u8", c2.streamUrl)
        assertEquals(listOf("https://legacy.test/sports.m3u8"), c2.streamUrls)
        assertNull(c2.lastPlayedUrl)
        assertEquals(1, c2.position)
    }

    @Test
    fun inMemoryRepository_replaceAllChannels_replacesAllAndMaintainsOrder() = runTest {
        val repo = InMemoryChannelRepository(
            listOf(
                Channel(id = "old1", name = "Old 1", streamUrl = "https://old1.m3u8"),
                Channel(id = "old2", name = "Old 2", streamUrl = "https://old2.m3u8")
            )
        )
        assertEquals(2, repo.getAllChannels().first().size)

        val newChannels = listOf(
            Channel(id = "new1", name = "New 1", streamUrls = listOf("https://new1.m3u8")),
            Channel(id = "new2", name = "New 2", streamUrls = listOf("https://new2.m3u8", "https://new2-backup.m3u8"))
        )
        repo.replaceAllChannels(newChannels)

        val replaced = repo.getAllChannels().first()
        assertEquals(2, replaced.size)
        assertEquals("new1", replaced[0].id)
        assertEquals(0, replaced[0].position)
        assertEquals("new2", replaced[1].id)
        assertEquals(1, replaced[1].position)
        assertEquals(listOf("https://new2.m3u8", "https://new2-backup.m3u8"), replaced[1].streamUrls)
    }

    @Test
    fun sqliteRepository_replaceAllChannels_replacesAllAndMaintainsOrder() = runTest(testDispatcher) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repo = SqliteChannelRepository(
            context = context,
            dbName = "test_replace_channels.db",
            ioDispatcher = testDispatcher
        )

        repo.addChannel(Channel(id = "old1", name = "Old 1", streamUrl = "https://old1.m3u8"))
        repo.addChannel(Channel(id = "old2", name = "Old 2", streamUrl = "https://old2.m3u8"))
        assertEquals(2, repo.getAllChannels().first().size)

        val newChannels = listOf(
            Channel(id = "new1", name = "New 1", streamUrls = listOf("https://new1.m3u8")),
            Channel(id = "new2", name = "New 2", streamUrls = listOf("https://new2.m3u8", "https://new2-backup.m3u8"))
        )
        repo.replaceAllChannels(newChannels)

        val replaced = repo.getAllChannels().first()
        assertEquals(2, replaced.size)
        assertEquals("new1", replaced[0].id)
        assertEquals(0, replaced[0].position)
        assertEquals("new2", replaced[1].id)
        assertEquals(1, replaced[1].position)
        assertEquals(listOf("https://new2.m3u8", "https://new2-backup.m3u8"), replaced[1].streamUrls)
    }
}

