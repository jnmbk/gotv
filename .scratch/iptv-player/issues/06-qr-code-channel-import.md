# 06: QR Code Channel Import (Camera & File)

**What to build:** In the channel creation screen, users can tap a "Scan QR Code" option instead of typing long stream URLs manually. Users can either point their device camera at a QR code for real-time detection or select an existing image file from their device. When a valid QR code containing channel information or an M3U8 link is detected, the app parses the payload and automatically populates the channel creation form fields (stream URL, channel name, and optional logo URL), ready for review and saving.

**Blocked by:** 01: Channel Persistence & Grid Dashboard

**Status:** done

- [x] Channel creation screen provides options to scan a QR code via device camera or import from an image file
- [x] Camera scanner recognizes QR codes in real time and handles camera permission requests gracefully
- [x] Image file picker reads and extracts QR code data from selected device images
- [x] Successfully parsed QR payload pre-fills the channel name, stream URL, and logo fields in the form
- [x] User is notified with clear feedback if a scanned QR code contains an invalid format
- [x] End-to-end tests verify camera and image QR parsing flows using fake scanner inputs

