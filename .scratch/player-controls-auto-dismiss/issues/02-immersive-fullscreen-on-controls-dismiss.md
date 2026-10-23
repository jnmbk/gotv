# Issue 02: Immersive fullscreen mode when player controls auto-dismiss

Status: in_progress

## Description
When watching a channel and player controls auto-dismiss after the 3-second timeout of inactivity ("left alone"), the player should enter Android immersive fullscreen mode by hiding the system status bar and navigation bar. Touching the screen, pressing volume keys, or performing vertical drag gestures restores the controls as well as the system bars. When navigating back from the player to the channel grid, the system bars must be restored.

## Acceptance Criteria
- [ ] System status bar and navigation bar are hidden when player controls auto-dismiss after inactivity.
- [ ] System status bar and navigation bar are restored when controls reappear (touching screen, pressing volume keys, adjusting volume).
- [ ] Swiping from screen edges shows transient system bars without breaking playback (`BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`).
- [ ] System bars are safely restored when navigating back to the channel grid.
- [ ] Unit/Compose tests verify system UI visibility changes during player lifecycle and controls dismissal.

## Comments
