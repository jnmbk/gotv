# Issue 01: Preserve volume level across channel changes

Status: closed

## Description
Currently, when switching channels, a new `PlayerSession` and `PlayerEngine` are created, resetting the volume to full (1.0). The volume must be preserved across channel switches so that the on-screen volume bar and audio remain where the user set them.

## Acceptance Criteria
1. When volume is adjusted in `PlayerScreen` (e.g. to 0.4f), switching to another channel from the sidebar preserves the volume at 0.4f.
2. Navigating back to the channel grid and selecting another channel preserves the volume at the previously set level.
3. The new channel's `PlayerEngine` is initialized with the preserved volume, and the volume slider displays the preserved volume immediately.
4. Preserved volume is maintained across state restoration (`rememberSaveable`).
5. Automated tests verify volume preservation when switching channels.

## Comments
- Implemented state tracking in `GoTvApp` using `rememberSaveable` and applying the preserved volume to new player engines upon channel change.
- Updated `PlayerViewModel` initial UI state to immediately reflect the player engine's volume.
- Verified with unit tests in `PlayerViewModelTest` and end-to-end tests in `GoTvAppEndToEndTest`.
