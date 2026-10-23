# Spec: Auto-Dismiss Player Controls Overlay

## Goal
The on-screen player controls (top bar with back-to-grid button and channel title, and bottom bar with volume control slider and mute toggle) should automatically disappear after a few seconds of inactivity so that live video playback can be viewed completely clean and unobstructed. Tapping the screen, pressing volume keys, or dragging to change volume brings the controls back into view and resets the auto-dismiss timer.

## Requirements & Design Decisions

### 1. Auto-Dismiss Timeout
- When `PlayerScreen` is launched, the overlay controls (top bar and volume bar) are initially visible.
- If no user interaction occurs for 3 seconds (`CONTROLS_AUTO_DISMISS_MS = 3_000L`), the controls smoothly fade out and dismiss.

### 2. Interaction & Reset
- Tapping the video viewport restores the visibility of the controls and starts the auto-dismiss countdown.
- Pressing physical volume keys (`VolumeUp`, `VolumeDown`, `VolumeMute`) makes the controls visible and resets the countdown.
- Adjusting volume via vertical drag gestures on the video makes the controls visible and resets the countdown.
- Interacting directly with the controls (e.g., sliding volume, clicking mute button) resets the countdown.
- While the channel sidebar is open, the overlay controls remain visible; once the sidebar closes, the controls auto-dismiss after the standard timeout.

### 3. Smooth Transitions
- Use `AnimatedVisibility` with `fadeIn()` and `fadeOut()` for the top bar and the bottom volume bar so the controls appear and disappear smoothly.

### 4. Immersive Fullscreen Mode
- When left alone and controls auto-dismiss, the player enters immersive fullscreen mode by hiding the system status bar and navigation bar via `WindowInsetsControllerCompat`.
- When user interaction restores controls visibility, the system bars are re-shown.
- Exiting the player restores the system bars for standard dashboard navigation.
- Transient swipes from the edge reveal transient system bars (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`) without breaking playback.

