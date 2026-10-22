# 11: JSON Channel List Import & Export

**What to build:** Allow users to export their entire channel list to a JSON file and import channels from a JSON file. Import and export actions are accessible via an overflow menu in the main dashboard's top bar. Importing replaces all existing channels after confirmation warning, skips invalid channel records, and provides feedback on the number of imported channels.

**Blocked by:** 08: Multiple Backup Stream Links & Failover Playback

**Status:** ready-for-agent

## Design Decisions

1. **Top Bar Overflow Menu:**
   - Add a three-dots menu icon to `ChannelGridScreen`'s `TopAppBar`.
   - Menu items: "Import channels" and "Export channels".

2. **JSON Schema & Serialization:**
   - Format is a versioned wrapper object:
     ```json
     {
       "version": 1,
       "channels": [
         {
           "name": "Channel Name",
           "streamUrls": [
             "https://example.com/primary.m3u8",
             "https://example.com/backup.m3u8"
           ],
           "logoUrl": "https://example.com/logo.png"
         }
       ]
     }
     ```
   - On import, also tolerate legacy/alternative single `"streamUrl"` or `"url"` string fields.

3. **Import Flow & Validation:**
   - Triggering import opens an Android document picker (`OpenDocument`).
   - Before executing the replacement, display a confirmation `AlertDialog`: "This will replace all current channels. Proceed?".
   - During parsing, validate channels using `ChannelValidator`:
     - Discard invalid channel entries.
     - Import all valid entries into `ChannelRepository` (replacing existing channels).
   - Display a user feedback message (toast or snackbar) with the count of successfully imported channels (e.g., "Successfully imported 5 channels").
   - If the file is completely unparseable or contains no valid channels, display an error message without replacing the existing channel list.

4. **Export Flow:**
   - Triggering export launches `CreateDocument` with suggested file name `gotv_channels.json`.
   - Serializes current channels from `ChannelRepository` into the versioned JSON format and writes to the selected URI.

## Acceptance Criteria

- [ ] Top bar provides an overflow menu with "Import channels" and "Export channels"
- [ ] Export serializes all current channels into the versioned JSON format and saves to the user's chosen location
- [ ] Import parses valid versioned JSON files and supports both `streamUrls` array and fallback single `streamUrl`
- [ ] Import presents a confirmation warning dialog before replacing existing channels
- [ ] Invalid channel entries in the imported file are skipped while valid channels are imported
- [ ] User receives feedback on the number of successfully imported channels
- [ ] Unit and UI tests verify JSON serialization, parsing, error recovery, and import replacement flow
