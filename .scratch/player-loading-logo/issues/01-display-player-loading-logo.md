# Issue 01: Display channel logo in player loading and error overlays

Status: open

## Description
When a channel is loading/buffering or fails in `PlayerScreen`, render the channel logo (or channel placeholder if not available) centered on screen, with the loading spinner or error UI positioned beneath it.

## Acceptance Criteria
1. While `uiState.showSpinner` is true, display the channel logo (size 96dp x 96dp) centered with the `CircularProgressIndicator` below it.
2. If `channel.localLogoPath` is absent or the file is missing, fallback to rendering the initials placeholder.
3. While `uiState.hasFailed` is true, display the channel logo above the error message and retry buttons.
4. Logo/placeholder has test tags for automated verification (e.g., `player_loading_logo` or reusing `ChannelLogo`).
5. Compose UI tests verify the logo is visible when buffering/loading and in error states, and gone when playing.

