# Changelog

## [1.3.0](https://github.com/jnmbk/gotv/compare/v1.2.0...v1.3.0) (2026-10-23)


### Features

* **player:** preserve volume level across channel changes ([404f18a](https://github.com/jnmbk/gotv/commit/404f18a331239f2b90a740875946c498201b7b72))
* **player:** support physical volume keys to control player volume ([fb1570e](https://github.com/jnmbk/gotv/commit/fb1570ea919fdeec95ce8370deb4ea465ed2531f))

## [1.2.0](https://github.com/jnmbk/gotv/compare/v1.1.0...v1.2.0) (2026-10-23)


### Features

* **about:** add project repository string resources ([3a9235e](https://github.com/jnmbk/gotv/commit/3a9235e2e10fbe6b66b83dd97f0991645453b4d9))
* **about:** add source code repository link to AboutDialog ([d9100ea](https://github.com/jnmbk/gotv/commit/d9100eaead98d88381f686b83874ebf23cf80b53))
* **channel:** add ChannelListDownloader interface and HttpURLConnection implementation ([c6c701a](https://github.com/jnmbk/gotv/commit/c6c701aa004092e23a455da776764fed2eb7d170))
* **channel:** add import URL state to ChannelGridUiState and QrScanTarget ([54de436](https://github.com/jnmbk/gotv/commit/54de43655079c15d8a51c4b342e5d839cf5b1527))
* **channel:** add ImportFromUrlDialog and integrate into ChannelGridScreen ([7bcd2d8](https://github.com/jnmbk/gotv/commit/7bcd2d80fdefb78da457c9ba58faacef9cbc1c33))
* **channel:** add URL import logic and state to ChannelGridViewModel ([6e8d2e1](https://github.com/jnmbk/gotv/commit/6e8d2e1da8ccfb974494b210cf7ff96e49176985))
* **channel:** make isValidHttpUrl public in ChannelValidator ([e0c4afa](https://github.com/jnmbk/gotv/commit/e0c4afafffca3efde9f0321b6b886c581b7fe622))
* **i18n:** add Turkish translations ([00afd9a](https://github.com/jnmbk/gotv/commit/00afd9a9390497c45ca0111f8d6ca9b69e5917d5))
* **i18n:** extract UI strings and plurals to strings.xml ([1e391fc](https://github.com/jnmbk/gotv/commit/1e391fcc02e0d3816c211cc6e299775f544f6012))
* **player:** display channel logo in player loading and error overlays ([2776e5c](https://github.com/jnmbk/gotv/commit/2776e5c5d571d98442e2a08e74f8b7c5617923d3))
* **ui:** add UiText wrapper for translatable strings and plurals ([a2d7526](https://github.com/jnmbk/gotv/commit/a2d7526777143e81816467c734abb11a66df4c37))
* **ui:** resolve strings and content descriptions from resources across UI components ([b0eada5](https://github.com/jnmbk/gotv/commit/b0eada5568a591944a05d01ffc1f653554d5d335))


### Bug Fixes

* **player:** rebind video surface when switching channels ([81908c4](https://github.com/jnmbk/gotv/commit/81908c456e429e9b3b2e0c79c84f1f8ced88e325))
* unittest errors ([a32034c](https://github.com/jnmbk/gotv/commit/a32034ca1051412c5702e6581a6e51a0146e8b5f))

## [1.1.0](https://github.com/jnmbk/gotv/compare/v1.0.0...v1.1.0) (2026-10-23)


### Features

* **about:** add string resources for about dialog ([c62f0cd](https://github.com/jnmbk/gotv/commit/c62f0cd4bb86c9a34a2fe07115ac2d15007a67a7))
* **about:** implement AboutDialog component and tests ([e373f60](https://github.com/jnmbk/gotv/commit/e373f60537800f8c704e1def6a9a68d1617b19e4))
* **app:** use gotv.svg as application icon ([8a44f39](https://github.com/jnmbk/gotv/commit/8a44f390585374984bf3fa8227fa1342f9246fa8))
* **channel:** add channel editing, logo redownload, deletion, and reorder state ([2a636d8](https://github.com/jnmbk/gotv/commit/2a636d882ff71ca750f795368c4db14cd8ccbc38))
* **channel:** add Channel model and ChannelValidator with unit tests ([0f47e2a](https://github.com/jnmbk/gotv/commit/0f47e2a21ada38087c79365d1019bc51a69b833c))
* **channel:** add ChannelGridViewModel and state management with unit tests ([a2c8eee](https://github.com/jnmbk/gotv/commit/a2c8eee942fc5cc47553a71350a23db92de7a6be))
* **channel:** add ChannelJsonSerializer for JSON import and export ([13d80e7](https://github.com/jnmbk/gotv/commit/13d80e7da30debb2fac3af2d0069f6b01be4f51e))
* **channel:** add dynamic stream URL inputs and controls to AddChannelDialog ([befa37a](https://github.com/jnmbk/gotv/commit/befa37a4a532fda6ac8bca977f70a9a0ff16973c))
* **channel:** add long-press context menu and drag-and-drop grid reordering ([ca610f3](https://github.com/jnmbk/gotv/commit/ca610f32d3d94eb4f2c1a270a884162ddc88a745))
* **channel:** add overflow menu, import confirmation dialog, and file picker support to ChannelGridScreen ([92da8ad](https://github.com/jnmbk/gotv/commit/92da8ad19ccf4886ac1e8eb3d0785fa6aee9f7ef))
* **channel:** add persistent channel position ordering and reordering ([35e0469](https://github.com/jnmbk/gotv/commit/35e046903848dc575a21ce643ecf8ee8d32dc523))
* **channel:** add QrScanTarget and update ChannelGridUiState for targeted qr scanning ([4f7c506](https://github.com/jnmbk/gotv/commit/4f7c506fca2d1545e01e8a24a0be520f594fef38))
* **channel:** allow any stream URL ending and auto-prepend https ([9746bb1](https://github.com/jnmbk/gotv/commit/9746bb1e8a8c1b43448deef7a8cd0c4d9aa01904))
* **channel:** implement per-input QR scan actions in AddChannelDialog and remove dialog-level scanner buttons ([2f395ef](https://github.com/jnmbk/gotv/commit/2f395ef8774211c4fb546fc32c577a80d387ff9e))
* **channel:** integrate about dialog into overflow menu and screen ([604e1d0](https://github.com/jnmbk/gotv/commit/604e1d076d466492d14c8cd30eec39bfea14ffd3))
* **channel:** integrate LogoStorage in ChannelGridViewModel with unit tests ([3d19887](https://github.com/jnmbk/gotv/commit/3d19887edbf481996db32cfab4e63b8eb26c63ef))
* **channel:** prefill https in add channel dialog and normalize on save ([354d6d3](https://github.com/jnmbk/gotv/commit/354d6d393a5fe7736df18d762b029491c2c42f38))
* **channel:** support dynamic stream URLs in ChannelGridUiState and ChannelGridViewModel ([fc88afa](https://github.com/jnmbk/gotv/commit/fc88afa119d08e3e601be48c581f98b44e8064e0))
* **channel:** support import and export flows in ChannelGridViewModel ([4dd98c5](https://github.com/jnmbk/gotv/commit/4dd98c5682b6deecd6fe214e5489ab2388fb1b23))
* **channel:** support multi-stream URL validation in ChannelValidator ([719ae85](https://github.com/jnmbk/gotv/commit/719ae856291f8b55e8635e40f029a167b8cfeb7c))
* **channel:** support multiple stream urls and last played url in channel model ([5228179](https://github.com/jnmbk/gotv/commit/522817953bb188133e8f0bfd09d0251c15629ab1))
* **channel:** update ChannelGridViewModel for targeted per-input QR scanning and remove QrChannelParser ([2d25194](https://github.com/jnmbk/gotv/commit/2d251942e19dc26b4d70d4b59d5a8c14109efe78))
* **network:** add network connectivity monitor ([f6ae23a](https://github.com/jnmbk/gotv/commit/f6ae23a02395742f5a25e2034c7d265c27777191))
* **player:** add PlayerEngine interface and playback state models ([1233f3f](https://github.com/jnmbk/gotv/commit/1233f3f6242e34b62da3ec6a2707471fd75c455d))
* **player:** add PlayerViewModel with volume and playback management ([fa801b7](https://github.com/jnmbk/gotv/commit/fa801b7ddb3ce52140a435d42c69c25bc570c684))
* **player:** add shrinking viewport and channel-switching sidebar ([86d25df](https://github.com/jnmbk/gotv/commit/86d25dff8f7794474724d32774134fd9ec0a82df))
* **player:** auto-reconnect streams with retries and failure state ([a2f6b4c](https://github.com/jnmbk/gotv/commit/a2f6b4cdab89889269b4a55419f4474b4ff5fdb0))
* **player:** implement backup stream failover and last played url persistence ([a7ebb77](https://github.com/jnmbk/gotv/commit/a7ebb772001d9ba14798ef93799e82aeb9387b9c))
* **player:** implement Media3PlayerEngine with constrained buffers and quick release ([25539d0](https://github.com/jnmbk/gotv/commit/25539d0a6f09d66576d0889dd7d64eac4836dbe9))
* **qr:** add QR code channel payload parser and ZXing decoder ([0b7b3f9](https://github.com/jnmbk/gotv/commit/0b7b3f99bb435f1028f5dc02cedef0e6bc50f025))
* **qr:** integrate camera and image QR scanning into channel creation ([7cb3935](https://github.com/jnmbk/gotv/commit/7cb393519df8c00f9df935481eaa8d98dae11c8b))
* **repository:** support replacing all channels in ChannelRepository ([5c19f14](https://github.com/jnmbk/gotv/commit/5c19f1491f184e1330b721a24ed22f141aeb6d80))
* **storage:** implement ChannelRepository with SQLite and in-memory backends ([c516349](https://github.com/jnmbk/gotv/commit/c516349089a6227c5ac9bfbda6bc5d6609d9fe45))
* **storage:** implement LogoStorage with downsampled caching and tests ([7741f8a](https://github.com/jnmbk/gotv/commit/7741f8a1673c5cab60ef3eb09519eb96a06e0836))
* **storage:** migrate channel database to version 3 with multiple urls and last played tracking ([684d977](https://github.com/jnmbk/gotv/commit/684d9776adee6c564c417a07ed4912430f5888f4))
* **storage:** support forced logo redownload and cached logo deletion ([4442faf](https://github.com/jnmbk/gotv/commit/4442fafca37779fbe23c782ec4b7188f5df4e34f))
* **ui:** implement ChannelGridScreen, ChannelTile, AddChannelDialog, and Compose UI tests ([a22b15f](https://github.com/jnmbk/gotv/commit/a22b15f20f376a755e7139da547a6b4c8aeced35))
* **ui:** implement full-screen PlayerScreen with on-screen volume control and navigation ([008b1a2](https://github.com/jnmbk/gotv/commit/008b1a237c8c2d98c2a70d22fb7105a265b536e3))
* **ui:** show loading spinner and error overlay during reconnection ([42a3391](https://github.com/jnmbk/gotv/commit/42a33917e59a32234b4e4a12ba705b53c0e39f10))
* **ui:** update ChannelTile to render cached downsampled logo with fallback ([2f5e017](https://github.com/jnmbk/gotv/commit/2f5e0179cf29baa98b94cb6ec7d43960f80dab52))


### Bug Fixes

* **app:** handle config changes to keep playback across rotation ([9ef2792](https://github.com/jnmbk/gotv/commit/9ef2792820f30946ee0d1f4495226036ab743573))
* **build:** remove hardcoded org.gradle.java.home from gradle.properties ([8a00517](https://github.com/jnmbk/gotv/commit/8a00517c93b06e6c15777e94044cc1a12cc7bec8))
* **channel:** consume pointer up after long-press to prevent starting playback ([5f65e4d](https://github.com/jnmbk/gotv/commit/5f65e4d86ae5dc00d14e9f350e0b618d019045ac))
* **player:** keep ERROR state when ExoPlayer drops to IDLE ([8da8010](https://github.com/jnmbk/gotv/commit/8da8010a4a65e637ca70aeff96e8baa87d6b309b))
* **player:** stop playback when app is backgrounded and resume on return ([a30def0](https://github.com/jnmbk/gotv/commit/a30def001b2495768d7ccf89e9612de94d23b36c))
* **ui:** release player session when it leaves or is abandoned in composition ([b6b72bc](https://github.com/jnmbk/gotv/commit/b6b72bc7497279d4e482cdc3ee437b91c79e9c78))
