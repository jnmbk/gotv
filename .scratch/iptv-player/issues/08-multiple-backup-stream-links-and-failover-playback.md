# 08: Multiple Backup Stream Links & Failover Playback

**What to build:** Channels can contain multiple stream links for backup redundancy. When playing a channel, the player starts with the channel's `lastPlayedUrl` (if present in `streamUrls`), otherwise the first available URL. If the active stream encounters an error or disconnection, the player cycles through the backup URLs in order during reconnection. Once any stream URL successfully starts playing, it is persisted to the database as the channel's new `lastPlayedUrl` for subsequent sessions.

**Blocked by:** 01: Channel Persistence & Grid Dashboard, 04: Stream Resilience, Loading Spinner & Auto-Reconnection, 07: Long-Press Channel Management & Drag-and-Drop Reordering

**Status:** ready-for-agent

## Design Decisions

1. **Channel Model & Persistence:**
   - Extend `Channel` with `streamUrls: List<String>` and `lastPlayedUrl: String? = null`.
   - Provide a backwards-compatible convenience property `streamUrl: String get() = lastPlayedUrl ?: streamUrls.firstOrNull().orEmpty()`.
   - Upgrade SQLite database (`DATABASE_VERSION = 3`) with schema migration:
     - Add `stream_urls TEXT` (storing URLs as a JSON array string).
     - Add `last_played_url TEXT`.
     - Migrate existing rows by populating `stream_urls` with `[stream_url]`.
   - Add a repository method or update `updateLastPlayedUrl(channelId: String, streamUrl: String)` to persist the last-played URL without disrupting channel ordering or other fields.

2. **Failover Reconnection Policy:**
   - In `PlayerViewModel`, track candidate stream URLs starting from `lastPlayedUrl` (if valid) followed by remaining `streamUrls` in order.
   - During reconnection on playback error or stall:
     - Cycle to the next backup stream URL.
     - Attempt playback with timeout and exponential backoff.
   - When playback transitions to `PlaybackStatus.PLAYING`:
     - Clear reconnection state.
     - Persist the currently working URL as `lastPlayedUrl` in `ChannelRepository`.
   - If all stream URLs in the candidate list fail, transition to `ReconnectPhase.FAILED` with user option to retry.

## Acceptance Criteria

- [ ] `Channel` model supports multiple `streamUrls: List<String>` and `lastPlayedUrl: String?`
- [ ] SQLite schema migration upgrades database to version 3 without data loss, migrating legacy single URLs into `streamUrls`
- [ ] Player initializes playback using `lastPlayedUrl` if valid, falling back to the first URL in `streamUrls`
- [ ] On playback error, player cycles through available backup stream URLs during auto-reconnection
- [ ] Successfully playing a backup link updates and persists `lastPlayedUrl` in the database
- [ ] When all backup stream URLs fail, the player enters the failed state with retry option
- [ ] Unit tests verify database migration, repository last-played persistence, and failover stream switching
