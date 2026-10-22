# GoTV

A lightweight, low-RAM Android IPTV player designed for smooth live stream playback on low-spec hardware and Android Go devices.

## Features

- **Low RAM Footprint**: Minimal heap allocation, constrained buffer pipelines, and aggressive memory management designed for 1 GB–2 GB RAM devices.
- **Channel Dashboard**: Clean grid layout displaying channels with locally cached, downsampled logos.
- **Easy Channel Import**: Add channels manually (name, M3U8 stream URL, optional logo URL) or scan QR codes via camera / gallery image.
- **Focused Live Playback**: Distraction-free full-screen live streaming using AndroidX Media3 (ExoPlayer) without seek bars, DVR buffering, or unnecessary scrubbing controls.
- **Quick In-Player Channel Switching**: Tap the screen during playback to shrink the video viewport and browse channels via a 10-second auto-dismissing sidebar.
- **Stream Resilience**: Graceful error handling with an automatic loading spinner and instant reconnection when network connectivity drops.
- **Local Persistence**: Channels and downsampled logos persist across app launches.

## Tech Stack & Architecture

- **UI**: Modern Android Jetpack Compose with Unidirectional Data Flow (UDF)
- **Asynchronous State**: Kotlin Coroutines & Flow
- **Media Engine**: AndroidX Media3 (ExoPlayer) configured with a lean HLS/M3U8 source pipeline
- **QR Code Scanning**: CameraX & ML Kit Barcode Scanning
- **Storage**: Local persistence for channel metadata and disk-cached logo assets

## Documentation

- [Architecture Specification](docs/architechture-spec.md)
- [Agent Specifications & Issue Breakdown](.scratch/iptv-player/)
- [Contributing & Commit Conventions](CONTRIBUTING.md)

## Contributing & Development

All code contributions, whether from human developers or AI agents, must adhere to:
- **[Conventional Commits v1.0.0](https://www.conventionalcommits.org/en/v1.0.0/)**: Structured prefixes (`feat`, `fix`, `docs`, `refactor`, `test`, `chore`, etc.).
- **Granular commits**: Small, atomic commits reflecting a single logical change rather than large batch commits.

See [CONTRIBUTING.md](CONTRIBUTING.md) for full details and scope guidelines.

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

