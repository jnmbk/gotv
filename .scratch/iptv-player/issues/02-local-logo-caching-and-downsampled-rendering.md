# 02: Local Logo Caching & Downsampled Rendering

**What to build:** Users can provide an optional logo image URL when adding or editing a channel. When a logo URL is provided, the application fetches the image, downsamples it to target thumbnail dimensions to keep memory consumption low, and saves the file directly to local application storage. The channel grid loads logos from local disk cache rather than continuously querying remote servers. If the image link fails or is unreachable, the channel gracefully falls back to the default placeholder logo without crashing or lagging the interface.

**Blocked by:** 01: Channel Persistence & Grid Dashboard

**Status:** resolved

- [x] The channel creation form accepts an optional logo image URL
- [x] Providing a valid logo URL downloads the image, downsamples it to target thumbnail bounds, and stores it in local app disk storage
- [x] Channel grid displays the downsampled cached logo loaded from local disk storage
- [x] Subsequent app launches and grid scrolling load cached logos locally without re-downloading
- [x] Network errors or invalid image URLs fall back gracefully to the placeholder logo without blocking the UI
- [x] End-to-end UI tests verify logo downloading, local persistence, and fallback behavior

## Answer

Issue 2 has been fully implemented with:
1. **Logo Storage & Downsampling**:
   - Defined `ImageDownloader` interface and `HttpImageDownloader` implementation using platform `HttpURLConnection` on `Dispatchers.IO` with connection timeouts and payload size limits.
   - Implemented `FileLogoStorage` adhering strictly to low-memory targets:
     - Validates image header magic bytes (PNG, JPEG, GIF, WebP) to prevent decoding non-image payloads.
     - Decodes dimensions with `inJustDecodeBounds = true`.
     - Calculates optimal `inSampleSize` power of two to decode directly to thumbnail dimensions without heap bloat.
     - Scales down precisely to target bounds (320x180 thumbnail bounds) and recycles intermediate native bitmaps.
     - Compresses and saves the downsampled bitmap as PNG into persistent app storage (`context.filesDir/logos/`).
   - Implemented `InMemoryLogoStorage` for fast, hermetic unit and screen testing.
2. **ViewModel Integration**:
   - Integrated `LogoStorage` into `ChannelGridViewModel` and `ChannelGridViewModel.Factory`.
   - On saving a valid channel with a `logoUrl`, asynchronously downloads, downsamples, and caches the logo to local storage before updating the channel with `localLogoPath`.
   - Handles network timeouts, unreachable servers, and corrupted image formats gracefully by catching failures and keeping `localLogoPath = null` without blocking or lagging the UI.
3. **UI Rendering & Caching**:
   - Updated `ChannelTile` with `ChannelLogo` composable that loads the downsampled bitmap from `localLogoPath` using `remember(localLogoPath)` to prevent repeated disk hits during grid scrolling.
   - Falls back automatically to `ChannelLogoPlaceholder` when no logo is set, the URL fails, or the local file is inaccessible.
   - Subsequent app launches and grid scrolling load logos directly from local disk cache without re-downloading.
4. **Testing**:
   - Comprehensive unit tests in `FileLogoStorageTest` verifying `inSampleSize` computation, downsampling logic, disk persistence, cache deduplication, network errors, and corrupted payload handling.
   - Unit tests in `ChannelGridViewModelTest` validating logo downloading, local path assignment, and failure fallback.
   - Full Compose UI end-to-end tests in `ChannelGridScreenTest` verifying end-to-end logo downloading, grid rendering, graceful fallback to placeholder on network error, and cross-session persistence across cold restarts without re-downloading.
