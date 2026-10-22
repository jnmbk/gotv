# 02: Local Logo Caching & Downsampled Rendering

**What to build:** Users can provide an optional logo image URL when adding or editing a channel. When a logo URL is provided, the application fetches the image, downsamples it to target thumbnail dimensions to keep memory consumption low, and saves the file directly to local application storage. The channel grid loads logos from local disk cache rather than continuously querying remote servers. If the image link fails or is unreachable, the channel gracefully falls back to the default placeholder logo without crashing or lagging the interface.

**Blocked by:** 01: Channel Persistence & Grid Dashboard

**Status:** ready-for-agent

- [ ] The channel creation form accepts an optional logo image URL
- [ ] Providing a valid logo URL downloads the image, downsamples it to target thumbnail bounds, and stores it in local app disk storage
- [ ] Channel grid displays the downsampled cached logo loaded from local disk storage
- [ ] Subsequent app launches and grid scrolling load cached logos locally without re-downloading
- [ ] Network errors or invalid image URLs fall back gracefully to the placeholder logo without blocking the UI
- [ ] End-to-end UI tests verify logo downloading, local persistence, and fallback behavior

