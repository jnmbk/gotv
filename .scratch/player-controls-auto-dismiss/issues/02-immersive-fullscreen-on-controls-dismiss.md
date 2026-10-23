# Issue 02: Immersive fullscreen mode when player controls auto-dismiss

Status: resolved

## Description
When watching a channel and player controls auto-dismiss after the 3-second timeout of inactivity ("left alone"), the player should enter Android immersive fullscreen mode by hiding the system status bar and navigation bar. Touching the screen, pressing volume keys, or performing vertical drag gestures restores the controls as well as the system bars. When navigating back from the player to the channel grid, the system bars must be restored.

## Acceptance Criteria
- [x] System status bar and navigation bar are hidden when player controls auto-dismiss after inactivity.
- [x] System status bar and navigation bar are restored when controls reappear (touching screen, pressing volume keys, adjusting volume).
- [x] Swiping from screen edges shows transient system bars without breaking playback (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`).
- [x] System bars are safely restored when navigating back to the channel grid.
- [x] Unit/Compose tests verify system UI visibility changes during player lifecycle and controls dismissal.

## Comments
- Enabled edge-to-edge on `MainActivity` for seamless edge-to-edge window drawing.
- Added `WindowInsetsControllerCompat` integration in `PlayerScreen.kt` using `LaunchedEffect(controlsVisible)` and `DisposableEffect(view, lifecycleOwner)` to hide status and navigation bars on auto-dismiss, restore them on interaction/reappearance, and restore them upon exiting the player.
- Configured `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE` so edge swipes show transient bars.
- Added 3 automated tests in `PlayerScreenTest.kt` verifying immersive mode entry on timeout, restoration on tap/volume keys, and cleanup on exit.

