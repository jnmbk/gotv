# 09: Per-Input QR Scanning & Dialog Scanner Removal

**What to build:** Remove the dialog-level QR scanning buttons ("Scan QR Code" and "From Image") and full-channel payload parsing from the channel creation dialog. Instead, provide dedicated inline QR scan actions directly on individual input fields (stream URL fields and logo URL field) so scanning directly populates the specific targeted input.

**Blocked by:** 06: QR Code Channel Import (Camera & File)

**Status:** done

## Design Decisions

1. **Remove Dialog-Level & Full-Channel QR Parsing:**
   - Remove the dialog header row containing "Scan QR Code" and "From Image" buttons.
   - Remove QR channel index/object parsing (`QrChannelParser` multi-field JSON and deep-link parsing).
   - Scanning now treats detected QR payloads as raw input text/URL directly filling the targeted field.

2. **Inline Input QR Scan Action:**
   - Provide an inline camera icon action within or adjacent to each URL input field.
   - Tapping the icon directly opens the camera scanner overlay.
   - When a QR code is detected, its raw decoded text is set into the targeted input field and the camera scanner dismisses.
   - Camera scanner dismisses gracefully if the user cancels or closes the scanner.

## Acceptance Criteria

- [x] Dialog-level QR buttons and image picker are removed from the channel dialog
- [x] Each stream URL input field and logo URL input field has an inline QR scan action
- [x] Tapping an input field's QR action opens the camera scanner directly
- [x] Scanning a QR code populates only the targeted input field with the scanned text/URL
- [x] Dismissing the scanner leaves existing input field contents unchanged
- [x] Unit and Compose UI tests verify targeted per-input QR scanning and camera scanner dismiss flows
