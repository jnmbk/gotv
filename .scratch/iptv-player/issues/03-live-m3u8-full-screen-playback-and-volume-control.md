# 03: Live M3U8 Full-Screen Playback & Volume Control

**What to build:** Tapping any channel tile in the main grid launches an uncluttered, full-screen live video player streaming the channel's M3U8 feed. The player interface deliberately omits unnecessary pause, seek, and scrub bars to keep memory low and match the live broadcast experience. Users can adjust sound volume using an on-screen slider or touch gesture without relying exclusively on hardware volume keys. When leaving the player to return to the grid, the playback pipeline and hardware video decoders are immediately released to prevent memory leaks.

**Blocked by:** 01: Channel Persistence & Grid Dashboard

**Status:** ready-for-agent

- [ ] Tapping a channel tile in the grid navigates to a dedicated full-screen live player view
- [ ] M3U8 live stream begins playing automatically using a lightweight media pipeline
- [ ] Playback interface excludes seek bar, pause, and scrubber controls
- [ ] On-screen volume control allows adjusting audio levels via touch gesture or slider
- [ ] Navigating back to the grid stops playback and immediately frees media engine and decoder resources
- [ ] End-to-end tests verify grid-to-player navigation, live playback initialization, and resource cleanup on exit

