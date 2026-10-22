# 10: Dynamic Stream URLs in Add/Edit Channel Dialog

**What to build:** In the Add/Edit Channel dialog, allow users to manage multiple stream links dynamically. Users can add additional backup stream URLs with an "Add Stream URL" button, remove backup URLs with a delete button, and edit existing URLs. In edit mode, all existing backup URLs are loaded into the form.

**Blocked by:** 08: Multiple Backup Stream Links & Failover Playback, 09: Per-Input QR Scanning & Dialog Scanner Removal

**Status:** ready-for-agent

## Design Decisions

1. **Dynamic Stream URL Form Fields:**
   - Instead of a single stream URL field, maintain a list of stream URL inputs (`List<String>`).
   - Display each stream URL field with:
     - An inline QR scan action button.
     - A delete / remove button (visible when there are two or more stream URL fields).
   - Display an "Add Stream URL" outlined or text button below the stream URL fields.
   - Channel creation defaults to a single prefilled stream URL field (`https://`).

2. **Validation & State Handling:**
   - In `ChannelValidator`:
     - Ensure at least one valid stream URL is supplied.
     - Validate all provided stream URLs and return appropriate per-field or summary error messages.
     - Empty trailing extra fields can be ignored or flagged.
   - In `ChannelGridViewModel`:
     - Support actions: `onAddStreamUrlField()`, `onRemoveStreamUrlField(index: Int)`, `onStreamUrlChanged(index: Int, url: String)`.
     - In edit mode (`onOpenEditDialog`), populate all `streamUrls` from the channel being edited.

## Acceptance Criteria

- [ ] Add/Edit Channel dialog displays a dynamic list of stream URL fields
- [ ] Users can add new backup stream URL fields via the "Add Stream URL" button
- [ ] Users can delete secondary stream URL fields (delete button hidden or disabled when only 1 field remains)
- [ ] Each stream URL field has an inline QR scan button that targets that specific URL field
- [ ] Edit mode pre-fills all existing stream URLs from the channel
- [ ] Saving validates all stream URLs and persists the full list to `ChannelRepository`
- [ ] Unit and Compose UI tests verify adding, removing, editing, and validating multiple stream URLs
