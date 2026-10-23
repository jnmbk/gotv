Status: open
Blocked by: 01, 02

# Localization Tests & Verification

## Description

Verify all strings are properly localized and that Turkish translations load correctly without crashes, missing keys, or UI test regressions.

## Tasks

- [ ] Add unit tests verifying `UiText` resolution across string resource IDs, plurals, and dynamic strings.
- [ ] Add tests verifying string resource parity between English (`values/strings.xml`) and Turkish (`values-tr/strings.xml`).
- [ ] Add Compose test with `Locale("tr")` to verify Turkish UI text rendering.
- [ ] Ensure all existing tests pass (`./gradlew test`).
