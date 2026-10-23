# 03: Import from URL Dialog UI and Overflow Menu Integration

**What to build:** Add "Import from URL" to the top bar overflow menu in `ChannelGridScreen` and implement the `ImportFromUrlDialog` composable with text field, clear button, QR scanner button, loading indicator, and inline error text.

**Blocked by:** 02-viewmodel-state-and-logic

**Status:** open

## Design Details
1. **Overflow Menu:**
   - Add "Import from URL" item to `ChannelGridScreen`'s overflow menu.
2. **Dialog Composable (`ImportFromUrlDialog`):**
   - Title: "Import from URL"
   - OutlinedTextField for URL input with:
     - Clear trailing icon when not empty.
     - QR code scan icon button to trigger QR scanner provider targeting the URL input.
     - `isError = uiState.importUrlError != null` and supporting text showing error message.
   - CircularProgressIndicator / disabled buttons during network loading.
   - Cancel and Import action buttons.
3. Hook up QR scanner integration when scanning URL into the dialog.
4. Compose tests validating dialog rendering, button interactions, error state display, and loading indicator.

## Acceptance Criteria
- [ ] Top bar overflow menu includes "Import from URL".
- [ ] Dialog allows typing, clearing, and QR scanning a URL.
- [ ] Inline errors and loading spinner are displayed accurately.
- [ ] UI / Compose tests covering dialog actions and state transitions.

