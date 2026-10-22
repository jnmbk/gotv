# 02: In-App About Dialog & Overflow Menu Integration

**What to build:** Add an "About" option in the `ChannelGridScreen` top bar overflow menu (`channel_overflow_dropdown_menu`). When selected, open an `AboutDialog` (Material 3 dialog) displaying the application title ("GoTV"), semantic version from `BuildConfig.VERSION_NAME` (e.g., `v1.0.0`), author name ("Uğur Çetin") with interactive link / intent action, open-source license ("Apache License 2.0") with notice and link, and a dismiss button. Add string resources in `strings.xml`. Add unit and Compose UI tests to verify opening, rendering of elements, and dismissal of the About dialog.

**Blocked by:** 01-semver-and-release-please-configuration.md

**Status:** open

- [ ] Add string resources for About dialog title, author, license, and action links in `strings.xml`
- [ ] Implement `AboutDialog` composable in `com.gotv.player.ui.about.AboutDialog`
- [ ] Add "About" `DropdownMenuItem` to `ChannelGridScreen` overflow menu
- [ ] Display `AboutDialog` when menu option is tapped and dismiss on request
- [ ] Add Compose/Robolectric tests for `AboutDialog` verifying version, author, license display, and dismiss actions
- [ ] Add end-to-end / UI test in `ChannelGridScreenTest` verifying About dialog triggers from the overflow menu
