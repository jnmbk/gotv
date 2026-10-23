Status: open

# Extract Hardcoded Strings & Add UiText Support

## Description

Extract all user-facing strings across UI components, dialogs, and ViewModels into `app/src/main/res/values/strings.xml`. Introduce a `UiText` wrapper to allow ViewModels to return translatable string resources with optional arguments or plurals without leaking `Context`.

## Tasks

- [ ] Create `UiText` utility class supporting `DynamicString`, `StringResource`, and `PluralResource`.
- [ ] Extract all hardcoded strings from `AddChannelDialog`, `EmptyChannelsView`, `ChannelGridScreen`, `PlayerScreen`, `ChannelSidebar`, and `AndroidQrScannerProvider` into `values/strings.xml`.
- [ ] Add plural string resource for channel import feedback (`channel_import_success_count`).
- [ ] Refactor `ChannelGridUiState` and `ChannelGridViewModel` to use `UiText` for validation errors and feedback messages.
- [ ] Update Compose components to resolve strings using `stringResource()` or `UiText.asString()`.
