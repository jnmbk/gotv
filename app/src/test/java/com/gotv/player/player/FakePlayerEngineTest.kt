package com.gotv.player.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakePlayerEngineTest {

    @Test
    fun initialState_hasDefaults() {
        val engine = FakePlayerEngine()

        assertEquals(PlaybackStatus.IDLE, engine.playbackState.value.status)
        assertEquals(1.0f, engine.volume.value)
        assertFalse(engine.isReleased)
        assertFalse(engine.isStopped)
        assertNull(engine.currentStreamUrl)
        assertNull(engine.player)
    }

    @Test
    fun play_updatesStreamUrlAndState() {
        val engine = FakePlayerEngine()
        val url = "https://example.com/live/stream.m3u8"

        engine.play(url)

        assertEquals(url, engine.currentStreamUrl)
        assertEquals(PlaybackStatus.PLAYING, engine.playbackState.value.status)
        assertEquals(1, engine.playCount)
    }

    @Test
    fun setVolume_clampsBetweenZeroAndOne() {
        val engine = FakePlayerEngine()

        engine.setVolume(0.5f)
        assertEquals(0.5f, engine.volume.value)

        engine.setVolume(-0.2f)
        assertEquals(0.0f, engine.volume.value)

        engine.setVolume(1.5f)
        assertEquals(1.0f, engine.volume.value)
    }

    @Test
    fun stopAndRelease_updateFlagsAndState() {
        val engine = FakePlayerEngine()
        engine.play("https://example.com/stream.m3u8")

        engine.stop()
        assertTrue(engine.isStopped)
        assertEquals(PlaybackStatus.IDLE, engine.playbackState.value.status)
        assertEquals(1, engine.stopCount)

        engine.release()
        assertTrue(engine.isReleased)
        assertEquals(PlaybackStatus.IDLE, engine.playbackState.value.status)
        assertEquals(1, engine.releaseCount)
    }
}
