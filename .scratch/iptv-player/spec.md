Status: ready-for-agent

# IPTV Player Specification

## Problem Statement

Users who watch live IPTV streams on Android devices often struggle with bloated, memory-heavy media players that consume excessive RAM, crash on low-spec hardware, and provide cluttered user interfaces with unneeded controls. Furthermore, managing custom IPTV channels and quickly switching between channels while watching a broadcast is often cumbersome and disrupts the viewing experience, particularly when network connections fluctuate.

## Solution

A lightweight, low-RAM Android IPTV application tailored for smooth stream playback. The app provides a clean channel grid displaying locally-cached logos, easy channel entry via manual input or QR code scanning, and streamlined playback focusing solely on volume control. During playback, touching the screen shrinks the video viewport and exposes a fast channel-switching sidebar for 10 seconds before auto-dismissing. Network drops and stream interruptions are handled gracefully with an instant loading spinner and automatic playback resumption upon reconnection.

## User Stories

1. As a viewer, I want to see all my configured IPTV channels in a responsive grid with their logos, so that I can quickly browse and pick what to watch.
2. As a viewer, I want channel logos to be cached and loaded from local storage, so that the app saves network bandwidth and renders quickly.
3. As a viewer on a low-end Android device, I want the application to use minimal RAM during browsing and playback, so that my device does not lag or kill background processes.
4. As a viewer, I want a prominent button on the main screen to add a new channel, so that I can easily expand my channel list.
5. As a viewer, I want to input a channel name and an M3U8 stream URL when adding a channel, so that the player knows how to stream it.
6. As a viewer, I want to optionally provide a logo image URL when adding a channel, so that the channel has visual branding in the grid.
7. As a viewer, I want the option to scan a QR code using my device camera when adding a channel, so that I do not have to type lengthy stream URLs manually.
8. As a viewer, I want the option to scan a QR code from an image file, so that I can import channel links shared with me as screenshots.
9. As a viewer, I want input validation when adding a channel, so that I am notified if the M3U8 link is invalid or if the channel name is empty.
10. As a viewer, I want channels without logos (or with failing logo links) to display an attractive default placeholder, so that the grid remains uniform and readable.
11. As a viewer, I want to tap any channel in the grid to immediately begin full-screen live playback, so that I can start watching with minimal latency.
12. As a viewer watching a live stream, I want the player interface to exclude unnecessary seek bars and pause/scrubbing controls, so that the screen is uncluttered and focused purely on live TV.
13. As a viewer, I want simple sound volume control accessible during stream playback, so that I can adjust audio without relying on physical hardware buttons alone.
14. As a viewer watching a stream, I want touching anywhere on the screen to shrink the video view and reveal a channel list on the left side, so that I can browse other channels without stopping the current stream.
15. As a viewer browsing the sidebar channel list, I want the sidebar and shrunken view to automatically dismiss and return to full screen after 10 seconds of inactivity, so that my viewing experience is restored without manual dismissal.
16. As a viewer interacting with the sidebar, I want any touch on the sidebar to reset the 10-second timer, so that the menu does not vanish while I am actively selecting a channel.
17. As a viewer, I want to select a different channel from the sidebar list to switch playback instantly, so that I can channel-surf quickly.
18. As a viewer, I want to tap outside the sidebar to immediately restore the full-screen playback view before the 10 seconds elapse, so that I can dismiss the overlay on demand.
19. As a viewer, I want to see a loading spinner when a stream encounters buffering or network connection issues, so that I know the application is actively waiting for data.
20. As a viewer experiencing network interruption, I want playback to resume automatically once network connectivity is re-established, so that I do not need to manually reload the stream.
21. As a viewer, I want clear feedback when a stream URL is permanently unreachable or offline after multiple retry attempts, so that I understand why the stream cannot play.
22. As a viewer, I want previous playback sessions and decoder resources to be released immediately when switching channels or returning to the grid, so that memory usage remains strictly bounded.
23. As a viewer, I want my channel list and cached logos to persist across app launches, so that my channels are always available when I open the app.
24. As a viewer opening the app for the first time, I want an empty state with a clear call-to-action to add my first channel, so that I know how to get started.
25. As a viewer with multiple stream sources, I want channels to hold backup stream links so that playback can automatically failover to working backups if the primary stream fails.
26. As a viewer returning to a channel, I want the player to automatically try the last successfully played stream URL first, so that I avoid repeating failed links.
27. As a viewer configuring a channel, I want per-input QR scanning directly on stream and logo URL fields, so that I can quickly populate individual URLs without parsing complex multi-field payloads.
28. As a viewer managing channels across devices or backups, I want to export and import my channel list as a JSON file, so that I can easily backup or restore my configuration.

## Implementation Decisions

- **Architecture Pattern**: Unidirectional Data Flow (UDF) with Jetpack Compose for UI rendering and Kotlin Coroutines/Flow for asynchronous state management.
- **Media Playback Engine**: AndroidX Media3 (ExoPlayer) configured with a lean HLS/M3U8 media source pipeline. Seek controls and DVR buffering are disabled to minimize memory footprint. Buffer sizes and cache allocations are constrained strictly to low-RAM targets.
- **Memory Optimization Strategy**:
  - Decode and downsample channel logos strictly to display dimensions before caching to local app storage.
  - Media player instances are singletons or scoped strictly to active playback, releasing codec instances on stop.
  - Image loader memory cache is capped aggressively, relying on fast local disk cache for logos.
- **Channel Storage & Persistence**: Local relational database or structured lightweight storage tracking channel identifier, name, M3U8 URL, and local logo file path.
- **QR Code Scanning**: CameraX combined with ML Kit Barcode Scanning for real-time QR detection, with fallback to gallery image decoding.
- **Dual-Mode Player Layout**:
  - Full-screen mode: Edge-to-edge video presentation with transparent volume control gesture/slider overlay.
  - PIP / Shrunken Split mode: Video surface smoothly scales and translates to the right while a vertical channel list overlays the left column.
  - Timer mechanism: A reactive 10-second countdown restarted on any user touch within the overlay area.
- **Resilience and Reconnection State Machine**:
  - States: `Idle`, `Connecting`, `Playing`, `BufferingOrReconnecting`, `Failed`.
  - When connection is dropped or media player reports buffering timeout, transition to `BufferingOrReconnecting` with visible spinner, holding player position/pipeline and subscribing to system network connectivity callbacks to trigger immediate retry.
- **Commit Standards**: Granular atomic commits adhering strictly to the Conventional Commits specification (`feat`, `fix`, `docs`, `refactor`, `test`, `chore`, etc.). Monolithic multi-purpose commits are prohibited.

## Testing Decisions

- **Testing Philosophy**: Tests must verify externally visible behavior and user workflows at the highest possible architectural seam, avoiding assertions on internal classes, private methods, or specific library callbacks.
- **Primary Seam**: End-to-end Compose Screen UI test harness (using Compose Test Rule with Robolectric / Android instrumented test runner).
  - Tests verify that clicking a channel transitions to the player screen.
  - Tests verify that touching the video surface displays the sidebar and initiates the 10-second timer.
  - Tests verify that tapping another channel in the sidebar triggers playback of the new stream.
  - Tests verify that simulated network drops present the loading spinner and restoring connectivity resumes playback.
  - Tests verify that adding a channel via form or simulated QR payload adds the item to the persistent grid.
- **Test Doubles**:
  - Controllable fake media engine to simulate playback states, errors, and buffering without requiring actual network streams or video decoders.
  - Fake camera/scanner source providing predefined QR payload strings.
  - In-memory channel and file storage to ensure tests execute hermetically and rapidly.

## Out of Scope

- EPG (Electronic Program Guide) scheduling and timeline data.
- Recording / DVR capabilities (timeshift, pause live TV, fast-forward, rewind).
- Multi-audio track / subtitle selection.
- Casting to Chromecast, DLNA, or external TV devices.
- User accounts, cloud syncing, or authentication.
- Paid subscription management or DRM license acquisition (Widevine/PlayReady).

## Further Notes

- The primary performance target is Android Go / low-RAM devices (devices with 1GB to 2GB total RAM), meaning heap allocation must remain tightly governed and bitmap leaks must be actively guarded against.
- Future enhancements may include M3U playlist file import, but the initial scope focuses purely on single channel additions via input or QR code.

