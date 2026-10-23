# 04: End-to-End Testing and Integration Verification

**What to build:** Add end-to-end integration tests verifying the entire flow from tapping "Import from URL" in the overflow menu (or scanning via QR), fetching, confirming replacement, and observing the updated channel list and snackbar.

**Blocked by:** 03-dialog-ui-and-menu-integration

**Status:** open

## Design Details
1. Create end-to-end Compose / Robolectric tests for the URL import flow:
   - Success path: opens overflow menu -> opens Import from URL dialog -> submits URL (mock downloader) -> sees replacement confirmation dialog -> confirms -> channels replaced in grid & feedback snackbar shown.
   - Failure path: invalid response/network error -> inline error shown -> dialog remains open -> cancel closes dialog.
   - QR scanner path: opens dialog -> launches QR scanner -> scans URL -> input field populated.
2. Verify all existing tests and new tests pass cleanly with `./gradlew test`.

## Acceptance Criteria
- [ ] End-to-end integration tests for happy path and error paths.
- [ ] QR code scanning end-to-end test for import URL input.
- [ ] Full test suite passes without regressions.

