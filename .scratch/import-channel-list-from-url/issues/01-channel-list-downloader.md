# 01: Remote Channel List Downloader

**What to build:** Implement a component/service (or downloader client) that fetches channel JSON content from a remote HTTP/HTTPS URL with configurable timeouts, proper error handling (network failures, non-2xx HTTP status codes, payload size ceilings), and unit tests.

**Status:** open

## Design Details
1. Create `ChannelListDownloader` interface and an implementation (e.g., using `HttpURLConnection` or existing HTTP infrastructure) that downloads the raw string content given an HTTP/HTTPS URL.
2. Handle network exceptions, timeouts, non-200 responses, and size limits safely returning a result (e.g. `Result<String>` or sealed outcome).
3. Provide unit tests covering success, HTTP error codes, timeout/network errors, and payload limits.

## Acceptance Criteria
- [ ] Interface and implementation for downloading channel list from a URL over HTTP/HTTPS.
- [ ] Safe timeout and error handling returning descriptive failure reasons.
- [ ] Unit tests covering valid and failure download scenarios.

