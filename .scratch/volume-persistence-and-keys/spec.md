# Spec: Volume Preservation and Physical Volume Keys Support

## Goal
1. Maintain the user's volume level across channel changes so the volume bar remains where it was instead of resetting to 100% (full volume).
2. Allow controlling the volume bar and live stream audio using physical volume keys (Volume Up, Volume Down, and Volume Mute).

## Requirements & Design Decisions

### 1. Volume Preservation Across Channels
- When a user adjusts the player volume (via the slider, vertical drag gesture, mute button, or physical keys), the volume level is tracked at the application session level (`GoTvApp`).
- When switching channels (either through the channel sidebar in the player screen or by navigating back to the channel grid and picking a new channel), the new player session/engine is initialized with the preserved volume level.
- The on-screen volume slider and audio playback reflect this preserved volume immediately upon entering the new channel.
- Volume is remembered across configuration changes and activity recreation using `rememberSaveable`.

### 2. Physical Volume Key Support
- While in `PlayerScreen`, pressing the physical `Volume Up` hardware key increases the volume by a discrete step (`0.05f` / 5%), clamped to `1.0f`.
- While in `PlayerScreen`, pressing the physical `Volume Down` hardware key decreases the volume by a discrete step (`0.05f` / 5%), clamped to `0.0f`.
- Pressing `Volume Mute` toggles between `0.0f` and `1.0f` (matching the on-screen mute button behavior).
- Both `KeyDown` and `KeyUp` events for these keys are consumed in the player to adjust the volume bar directly and prevent disruptive system volume overlays from occluding video playback.
- The player screen maintains focus and handles key events using Compose key event modifiers with `FocusRequester`.
