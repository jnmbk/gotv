# Issue 01: Auto-dismiss volume bar and back-to-grid button after timeout

Status: resolved

## Description
The volume control panel and back button / title bar currently remain on screen permanently during playback. They should automatically fade out and disappear after 3 seconds of inactivity. Tapping the screen, pressing volume keys, or dragging vertically to change volume should restore control visibility and reset the auto-dismiss timer.

## Acceptance Criteria
- [x] On-screen controls (top bar with back button, bottom bar with volume slider) auto-dismiss after 3 seconds of inactivity.
- [x] Tapping the video viewport restores controls visibility and resets the countdown.
- [x] Pressing physical volume keys (`VolumeUp`, `VolumeDown`, `VolumeMute`) makes controls visible and resets the countdown.
- [x] Vertical drag gestures adjusting volume make controls visible and reset the countdown.
- [x] Interacting with the volume controls resets the countdown.
- [x] Unit and end-to-end tests verify controls auto-dismiss after timeout, reappear upon interaction, and reset when touched.

## Comments
- Implemented `controlsVisible` state and `LaunchedEffect` auto-dismiss timer (`CONTROLS_AUTO_DISMISS_MS = 3_000L`) in `PlayerScreen.kt`.
- Wrapped top bar and bottom volume controls in `AnimatedVisibility` with fade in/out transitions.
- Hooked pointer input observers and key listeners to reveal controls and reset the dismiss countdown.
- Added 4 automated Compose tests in `PlayerScreenTest.kt` verifying auto-dismiss after timeout, tap restoration, physical key restoration, and interaction timer reset.

