package com.gotv.player.data.storage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
class FileLogoStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var logoDir: File

    @Before
    fun setUp() {
        logoDir = tempFolder.newFolder("logos")
    }

    private fun createTestBitmapBytes(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        bitmap.recycle()
        return stream.toByteArray()
    }

    @Test
    fun calculateInSampleSize_computesCorrectPowerOfTwo() {
        // Equal or smaller
        assertEquals(1, FileLogoStorage.calculateInSampleSize(320, 180, 320, 180))
        assertEquals(1, FileLogoStorage.calculateInSampleSize(160, 90, 320, 180))

        // Double size -> sample size 2
        assertEquals(2, FileLogoStorage.calculateInSampleSize(640, 360, 320, 180))

        // 4x size -> sample size 4
        assertEquals(4, FileLogoStorage.calculateInSampleSize(1280, 720, 320, 180))

        // 6x size -> sample size 4 (since half is 3x, still >= req)
        assertEquals(4, FileLogoStorage.calculateInSampleSize(1920, 1080, 320, 180))
    }

    @Test
    fun downloadAndCacheLogo_downsamplesAndSavesToDisk() = runTest {
        val largeImageBytes = createTestBitmapBytes(1280, 720)
        var downloadCallCount = 0
        val fakeDownloader = ImageDownloader {
            downloadCallCount++
            largeImageBytes
        }

        val storage = FileLogoStorage(
            baseDir = logoDir,
            imageDownloader = fakeDownloader,
            targetWidth = 320,
            targetHeight = 180
        )

        val cachedPath = storage.downloadAndCacheLogo("chan-1", "https://example.com/logo.png")
        assertNotNull(cachedPath)
        assertEquals(1, downloadCallCount)

        val cachedFile = File(cachedPath!!)
        assertTrue(cachedFile.exists())
        assertTrue(cachedFile.length() > 0)

        // Verify downsampled dimensions on disk
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(cachedFile.absolutePath, options)
        assertTrue(options.outWidth <= 320)
        assertTrue(options.outHeight <= 180)

        assertTrue(storage.hasCachedLogo("chan-1"))
        assertNotNull(storage.getCachedLogoFile("chan-1"))
    }

    @Test
    fun downloadAndCacheLogo_whenAlreadyCached_doesNotReDownload() = runTest {
        val imageBytes = createTestBitmapBytes(200, 200)
        var downloadCallCount = 0
        val fakeDownloader = ImageDownloader {
            downloadCallCount++
            imageBytes
        }

        val storage = FileLogoStorage(
            baseDir = logoDir,
            imageDownloader = fakeDownloader
        )

        // First download
        val firstPath = storage.downloadAndCacheLogo("chan-repeat", "https://example.com/logo.png")
        assertEquals(1, downloadCallCount)
        assertNotNull(firstPath)

        // Second call should return existing file without downloading again
        val secondPath = storage.downloadAndCacheLogo("chan-repeat", "https://example.com/logo.png")
        assertEquals(1, downloadCallCount)
        assertEquals(firstPath, secondPath)
    }

    @Test
    fun downloadAndCacheLogo_onNetworkFailure_returnsNullGracefully() = runTest {
        val fakeDownloader = ImageDownloader { null }
        val storage = FileLogoStorage(
            baseDir = logoDir,
            imageDownloader = fakeDownloader
        )

        val result = storage.downloadAndCacheLogo("failed-chan", "https://example.com/missing.png")
        assertNull(result)
        assertFalse(storage.hasCachedLogo("failed-chan"))
        assertNull(storage.getCachedLogoFile("failed-chan"))
    }

    @Test
    fun downloadAndCacheLogo_onCorruptedImageBytes_returnsNullGracefully() = runTest {
        val fakeDownloader = ImageDownloader { "not-a-valid-image-data".toByteArray() }
        val storage = FileLogoStorage(
            baseDir = logoDir,
            imageDownloader = fakeDownloader
        )

        val result = storage.downloadAndCacheLogo("corrupt-chan", "https://example.com/broken.png")
        assertNull(result)
        assertFalse(storage.hasCachedLogo("corrupt-chan"))
    }

    @Test
    fun downloadAndCacheLogo_withForceRefresh_redownloadsAndOverwritesCachedFile() = runTest {
        val firstBytes = createTestBitmapBytes(200, 200)
        val secondBytes = createTestBitmapBytes(100, 100)
        var downloadCallCount = 0
        val fakeDownloader = ImageDownloader {
            downloadCallCount++
            if (downloadCallCount == 1) firstBytes else secondBytes
        }

        val storage = FileLogoStorage(
            baseDir = logoDir,
            imageDownloader = fakeDownloader
        )

        val firstPath = storage.downloadAndCacheLogo("chan-force", "https://example.com/logo.png")
        assertNotNull(firstPath)
        assertEquals(1, downloadCallCount)

        // Force refresh should download again and overwrite the file
        val secondPath = storage.downloadAndCacheLogo(
            channelId = "chan-force",
            logoUrl = "https://example.com/logo.png",
            forceRefresh = true
        )
        assertNotNull(secondPath)
        assertEquals(2, downloadCallCount)
        assertEquals(firstPath, secondPath)

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(secondPath!!, options)
        assertEquals(100, options.outWidth)
        assertEquals(100, options.outHeight)
    }

    @Test
    fun downloadAndCacheLogo_withForceRefresh_preservesExistingCacheWhenDownloadFails() = runTest {
        val validBytes = createTestBitmapBytes(150, 150)
        var shouldFail = false
        val fakeDownloader = ImageDownloader {
            if (shouldFail) null else validBytes
        }

        val storage = FileLogoStorage(
            baseDir = logoDir,
            imageDownloader = fakeDownloader
        )

        val initialPath = storage.downloadAndCacheLogo("chan-preserve", "https://example.com/logo.png")
        assertNotNull(initialPath)
        assertTrue(storage.hasCachedLogo("chan-preserve"))

        // Subsequent forced refresh fails
        shouldFail = true
        val failedResult = storage.downloadAndCacheLogo(
            channelId = "chan-preserve",
            logoUrl = "https://example.com/logo.png",
            forceRefresh = true
        )
        assertNull(failedResult)

        // Existing cached logo file on disk must be preserved
        assertTrue(storage.hasCachedLogo("chan-preserve"))
        val existingFile = storage.getCachedLogoFile("chan-preserve")
        assertNotNull(existingFile)
        assertTrue(existingFile!!.exists())
    }

    @Test
    fun deleteCachedLogo_removesFileFromDisk() = runTest {
        val validBytes = createTestBitmapBytes(120, 120)
        val storage = FileLogoStorage(
            baseDir = logoDir,
            imageDownloader = { validBytes }
        )

        storage.downloadAndCacheLogo("chan-delete", "https://example.com/logo.png")
        assertTrue(storage.hasCachedLogo("chan-delete"))

        storage.deleteCachedLogo("chan-delete")
        assertFalse(storage.hasCachedLogo("chan-delete"))
        assertNull(storage.getCachedLogoFile("chan-delete"))
    }
}
