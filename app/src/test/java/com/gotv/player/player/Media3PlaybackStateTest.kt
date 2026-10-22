package com.gotv.player.player

import androidx.media3.common.Player
import org.junit.Assert.assertEquals
import org.junit.Test

class Media3PlaybackStateTest {

    private val error = PlaybackState(PlaybackStatus.ERROR, "boom")

    @Test
    fun idleAfterError_keepsError() {
        assertEquals(error, reducePlaybackState(error, Player.STATE_IDLE))
    }

    @Test
    fun endedAfterError_keepsError() {
        assertEquals(error, reducePlaybackState(error, Player.STATE_ENDED))
    }

    @Test
    fun bufferingAfterError_clearsError() {
        assertEquals(
            PlaybackState(PlaybackStatus.BUFFERING),
            reducePlaybackState(error, Player.STATE_BUFFERING)
        )
    }

    @Test
    fun readyAfterError_clearsError() {
        assertEquals(
            PlaybackState(PlaybackStatus.PLAYING),
            reducePlaybackState(error, Player.STATE_READY)
        )
    }

    @Test
    fun idleWithoutError_isIdle() {
        assertEquals(
            PlaybackState(PlaybackStatus.IDLE),
            reducePlaybackState(PlaybackState(PlaybackStatus.PLAYING), Player.STATE_IDLE)
        )
    }
}
