# 04: End-to-End Testing and Integration Verification

**What to build:** Add end-to-end integration tests verifying the entire flow from tapping "Import from URL" in the overflow menu (or scanning via QR), fetching, confirming replacement, and observing the updated channel list and snackbar.

**Blocked by:** 03-dialog-ui-and-menu-integration

**Status:** resolved

## Design Details
1. Create end-to-end Compose / Robolectric tests for the URL import flow:
   - Success path: opens overflow menu -> opens Import from URL dialog -> submits URL (mock downloader) -> sees replacement confirmation dialog -> confirms -> channels replaced in grid & feedback snackbar shown.
   - Failure path: invalid response/network error -> inline error shown -> dialog remains open -> cancel closes dialog.
   - QR scanner path: opens dialog -> launches QR scanner -> scans URL -> input field populated.
2. Verify all existing tests and new tests pass cleanly with `./gradlew test`.

## Acceptance Criteria
- [x] End-to-end integration tests for happy path and error paths.
- [x] QR code scanning end-to-end test for import URL input.
- [x] Full test suite passes without regressions.

## Comments
Added `GoTvAppUrlImportEndToEndTest` covering the complete URL import flow from top-bar overflow menu trigger, QR scan URL population, network error handling with inline messages, and success flow including replacement confirmation dialog, database update, and feedback snackbar. Wired `channelListDownloader` parameter into `GoTvApp` with default `HttpChannelListDownloader` to facilitate injection in tests. Verified full test suite (`./gradlew testDebugUnitTest -PslowTests`) passed cleanly.
