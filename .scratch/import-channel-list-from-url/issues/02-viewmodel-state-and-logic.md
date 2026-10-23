# 02: ViewModel State and URL Import Logic

**What to build:** Extend `ChannelGridUiState` and `ChannelGridViewModel` to support the URL import workflow: dialog visibility, URL input state, QR scanning into URL input, network fetch state (loading, error message), parsing via `ChannelJsonSerializer`, and bridging to the confirmation dialog.

**Blocked by:** 01-channel-list-downloader

**Status:** open

## Design Details
1. Add state fields to `ChannelGridUiState`:
   - `isImportUrlDialogOpen: Boolean`
   - `importUrlInput: String`
   - `importUrlError: String?`
   - `isImportUrlLoading: Boolean`
   - Extend `QrScanTarget` with `ImportUrl` (or similar) to handle QR code scans directed to the import URL input field.
2. In `ChannelGridViewModel`:
   - Add actions: `onOpenImportUrlDialog()`, `onDismissImportUrlDialog()`, `onImportUrlChanged(String)`, `onClearImportUrl()`, and `onSubmitImportUrl()`.
   - Validate URL format prior to network call.
   - Execute fetch asynchronously via `ChannelListDownloader`, showing loading state.
   - If download or parsing fails, surface an inline error message in `importUrlError` and keep the dialog open.
   - If download and parsing succeed, dismiss the URL dialog, populate `pendingImportChannels`, and open `isImportConfirmDialogOpen = true` for confirmation.
3. Add unit tests for `ChannelGridViewModel` covering all states and edge cases (success flow, invalid URL, network error, parsing error, QR scan target).

## Acceptance Criteria
- [ ] UiState models URL import dialog, loading, error, and QR scanner target.
- [ ] ViewModel orchestrates download, validation, parsing, error reporting, and confirmation.
- [ ] Comprehensive unit tests verifying ViewModel behavior.

