# Spec: Channel Logo on Player Loading & Error Screens

## Goal
Display the channel logo (or channel initial placeholder if logo is unavailable) in the center of the player screen when a channel is loading/buffering/reconnecting or has failed playback, with the loading spinner or error UI positioned below it.

## User Decisions & Requirements
1. **Placement**: Centered in the video viewport.
2. **Spinner Position**: Placed below the channel logo when loading/buffering.
3. **Fallback**: If channel has no logo URL or the cached local logo file does not exist, display the channel placeholder (initials fallback box, identical to `ChannelLogoPlaceholder` style).
4. **Dimensions**: Large square (96dp x 96dp) with rounded corners (8dp).
5. **Error State**: The logo remains visible above the playback error message and retry buttons when in `hasFailed` state.
6. **Visibility**:
   - While `uiState.showSpinner` is true: Logo displayed with `CircularProgressIndicator` below it.
   - While `uiState.hasFailed` is true: Logo displayed above the error text and action buttons.
   - Once video is playing normally (`!showSpinner && !hasFailed`): Centered logo and spinner are hidden, letting the video play cleanly.

