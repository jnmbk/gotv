# 07: Long-Press Channel Management & Drag-and-Drop Reordering

**What to build:** Users can click and hold (long-press) any channel tile in the dashboard grid to either manage the channel via a context menu or drag the tile to reorder its position.

**Blocked by:** 01: Channel Persistence & Grid Dashboard, 02: Local Logo Caching & Downsampled Rendering, 05: Shrunken Viewport & In-Player Channel Switching Sidebar, 06: QR Code Channel Import (Camera & File)

**Status:** done

## Design Decisions

1. **Long-Press & Drag Gesture Coordination**:
   - Long-pressing a channel tile immediately opens a context menu anchored to the tile.
   - If the user continues holding and starts dragging past the touch slop threshold, the context menu automatically dismisses and the tile enters a drag-and-drop reorder state.
   - Dragging the tile over other grid positions dynamically reorders the channels, and releasing the drag persists the updated channel positions.

2. **Context Menu Actions**:
   - **Edit Channel**: Opens the channel form dialog (`AddChannelDialog` reused in edit mode, titled `"Edit Channel"` with a `"Save"` button) pre-filled with the channel's current `name`, `streamUrl`, and `logoUrl` (including QR code scan and image import buttons).
     - If `logoUrl` is cleared on save: deletes the cached logo file on disk and clears `localLogoPath` so the tile falls back to the placeholder logo.
     - If `logoUrl` is changed on save: forces a re-download and overwrites the cached logo file (if the new download fails, deletes the old cached logo and falls back to the placeholder).
     - If `logoUrl` is unchanged on save: preserves the existing `localLogoPath` (or attempts a download if `localLogoPath` was null).
   - **Redownload Logo**: Disabled or hidden when the channel has no `logoUrl`. When triggered, bypasses the local file existence check (`forceRefresh = true`) to re-fetch, downsample, and overwrite the cached logo file on disk.
     - If the forced download succeeds, the UI refreshes the cached bitmap (e.g., via file modification timestamp or version key so `remember` invalidates).
     - If the forced download fails (network error or invalid payload), any existing cached logo file on disk is preserved rather than wiped out.
   - **Delete Channel**: Opens a confirmation `AlertDialog` (`"Delete <Channel Name>?"`). Confirming deletes the channel from `ChannelRepository` and deletes its cached logo file from `LogoStorage`.

3. **Channel Ordering & Persistence**:
   - Channels track a persistent `position` (sort order) in `Channel` and `SqliteChannelRepository` (with a database schema migration/version bump so existing channels receive deterministic initial positions).
   - Newly added channels are prepended to the top of the list (position `0`, matching the existing newest-first behavior).
   - `ChannelRepository.getAllChannels()` emits channels ordered by their custom position, which is automatically reflected in both the main `ChannelGridScreen` and the in-player `ChannelSidebar`.

## Acceptance Criteria

- [x] Long-pressing a channel tile immediately opens a context menu with "Edit Channel", "Redownload Logo" (when `logoUrl` is present), and "Delete Channel"
- [x] Starting a drag while holding a channel tile dismisses the context menu and reorders the channel in the grid
- [x] Reordered channel positions persist in `ChannelRepository` across app restarts and are reflected in both the main grid and the in-player sidebar
- [x] Newly added channels appear at the top of the channel order (position 0)
- [x] Selecting "Edit Channel" opens the form dialog pre-populated with the channel's details (supporting QR scan/import) and updates the channel and its cached logo according to logo URL changes
- [x] Selecting "Redownload Logo" forces a fresh download and overwrites the cached logo, refreshing the tile UI on success and preserving the existing cached logo on failure
- [x] Selecting "Delete Channel" shows a confirmation dialog and, upon confirmation, removes the channel and deletes its local cached logo file
- [x] Unit and end-to-end Compose UI tests verify editing, logo redownloading, deletion with confirmation, and drag-and-drop reordering

