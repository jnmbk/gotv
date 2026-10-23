# Issue 02: Support physical volume keys in player

Status: open

## Description
The on-screen volume bar in `PlayerScreen` does not respond to physical hardware volume keys (Volume Up, Volume Down, Volume Mute). Physical volume key presses while watching a stream should adjust the in-app volume bar and audio.

## Acceptance Criteria
1. While `PlayerScreen` is active, pressing the physical Volume Up key increases the player volume by a step (`0.05f`), up to `1.0f`.
2. While `PlayerScreen` is active, pressing the physical Volume Down key decreases the player volume by a step (`0.05f`), down to `0.0f`.
3. Pressing Volume Mute toggles volume between `0.0f` and `1.0f`.
4. The key events are consumed so that system volume dialogs do not overlay the live stream playback.
5. Unit and Compose tests verify volume changes upon hardware key events.
