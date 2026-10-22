# 03: Live M3U8 Full-Screen Playback & Volume Control

**What to build:** Tapping any channel tile in the main grid launches an uncluttered, full-screen live video player streaming the channel's M3U8 feed. The player interface deliberately omits unnecessary pause, seek, and scrub bars to keep memory low and match the live broadcast experience. Users can adjust sound volume using an on-screen slider or touch gesture without relying exclusively on hardware volume keys. When leaving the player to return to the grid, the playback pipeline and hardware video decoders are immediately released to prevent memory leaks.

**Blocked by:** 01: Channel Persistence & Grid Dashboard

**Status:** resolved

- [x] Tapping a channel tile in the grid navigates to a dedicated full-screen live player view
- [x] M3U8 live stream begins playing automatically using a lightweight media pipeline
- [x] Playback interface excludes seek bar, pause, and scrubber controls
- [x] On-screen volume control allows adjusting audio levels via touch gesture or slider
- [x] Navigating back to the grid stops playback and immediately frees media engine and decoder resources
- [x] End-to-end tests verify grid-to-player navigation, live playback initialization, and resource cleanup on exit

## Answer

Issue 3 has been fully implemented with:
1. **Media Architecture & Low-RAM Pipeline**:
   - Defined `PlayerEngine` interface and `PlaybackState` / `PlaybackStatus` models.
   - Implemented `Media3PlayerEngine` using AndroidX Media3 (ExoPlayer 1.3.1) configured specifically for low-RAM devices:
     - Constrained buffer duration limits (`minBufferMs = 1500`, `maxBufferMs = 4000`, `bufferForPlaybackMs = 500`, `bufferForPlaybackAfterRebufferMs = 1000`).
     - Back buffering and DVR seek cache disabled (`setBackBuffer(0, false)`) to prevent heap bloat during live streams.
     - Lean `HlsMediaSource` pipeline.
     - Codec instances and hardware video decoders are immediately stopped, cleared, and released on `stop()` and `release()`.
   - Implemented `FakePlayerEngine` for hermetic testing with state flows, volume tracking, and idempotent release behavior.
2. **ViewModel & State Management**:
   - Implemented `PlayerViewModel` and `PlayerUiState` with unidirectional data flow.
   - Automatically initiates playback upon initialization.
   - Coordinates volume changes and guarantees immediate resource cleanup on `release()` and `onCleared()`.
3. **Jetpack Compose UI**:
   - Implemented full-screen `PlayerScreen`:
     - Renders edge-to-edge video presentation via `PlayerView` with `useController = false`.
     - Deliberately omits seek bar, pause button, and scrubber controls.
     - Provides top bar with channel title and back navigation button.
     - Provides on-screen volume control overlay with `Slider`, mute toggle button, and vertical drag gestures.
     - Hooks `BackHandler` and `DisposableEffect` to guarantee that returning to the grid immediately releases playback pipelines and hardware decoders.
   - Implemented `GoTvApp` root navigation seamlessly managing transitions between `ChannelGridScreen` and `PlayerScreen`.
   - Updated `MainActivity` to mount `GoTvApp` with `Media3PlayerEngine`.
4. **Testing**:
   - `FakePlayerEngineTest`: Verified state transitions, volume clamping, and lifecycle contracts.
   - `PlayerViewModelTest`: Verified automatic playback start, volume delegation, and engine resource cleanup.
   - `PlayerScreenTest`: Robolectric Compose test verifying full-screen player layout, exclusion of pause/seek/scrubber bars, volume slider and mute controls, and resource cleanup on back navigation.
   - `GoTvAppEndToEndTest`: Verified full user flow from channel grid selection to full-screen live playback, volume adjustment, and back navigation with immediate decoder release.
