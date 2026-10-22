# 05: Shrunken Viewport & In-Player Channel Switching Sidebar

**What to build:** While watching a live stream in full-screen mode, touching anywhere on the screen smoothly scales and translates the video viewport into a shrunken view while opening a channel list sidebar along the left side. Users can scroll the sidebar and tap another channel to immediately switch playback to the new stream without exiting the player. A 10-second auto-dismiss countdown begins as soon as the sidebar appears; any touch on the sidebar resets the timer. If 10 seconds elapse without interaction, or if the user taps outside the sidebar, the overlay dismisses and the video returns smoothly to full-screen mode.

**Blocked by:** 03: Live M3U8 Full-Screen Playback & Volume Control

**Status:** done

- [x] Tapping the screen during live playback shrinks the video viewport and exposes a vertical channel sidebar on the left
- [x] Sidebar displays the list of saved channels with their names and logos
- [x] A 10-second inactivity countdown timer automatically dismisses the sidebar and restores full-screen video
- [x] Interacting with or touching the sidebar resets the 10-second countdown timer
- [x] Tapping outside the sidebar immediately dismisses the sidebar and restores full screen
- [x] Selecting a channel in the sidebar switches the active stream immediately, releasing previous stream resources
- [x] End-to-end tests verify touch-to-open sidebar, auto-dismiss after timeout, touch reset, and channel switching

