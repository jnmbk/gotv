# Feature Spec: Import Channel List from URL

## Overview
Enable users to import their channel list by providing a remote URL pointing to a GoTV JSON file. This complements local file import, allowing easy distribution, migration, and setup across devices.

## Decisions & Requirements

1. **Format:**
   - GoTV JSON format only (versioned schema as handled by `ChannelJsonSerializer`).
   - Tolerate legacy and alternative single stream URL attributes as handled by `ChannelJsonSerializer.importChannels`.
2. **Access & UI Entry Point:**
   - Dedicated "Import from URL" item in the top app bar overflow menu on `ChannelGridScreen`.
3. **Import from URL Dialog:**
   - Text input for the URL with a Clear button.
   - Per-input QR code scanner button allowing users to scan a URL from camera or screen.
   - Validation & Feedback:
     - URL format validation before fetch.
     - Inline error message below the input field on network, HTTP, or parse failures (e.g., malformed URL, HTTP 404, connection timeout, invalid JSON). The dialog remains open so the user can edit/correct the URL.
     - Loading state / progress indicator during network fetch.
4. **Data Handling & Replacement:**
   - Once downloaded and parsed successfully, show the existing confirmation dialog: *"This will replace all current channels. Proceed?"*.
   - Upon confirmation, replace all channels in repository with the imported channels.
   - Display a snackbar confirming how many channels were imported (e.g. *"Successfully imported 10 channels"*).
5. **Synchronization:**
   - One-time manual import only (no periodic background syncing).

