Status: ready-for-agent

# Localization & Turkish Translation Specification

## Problem Statement

GoTV currently has many user-facing strings hardcoded across Jetpack Compose UI components (`ChannelGridScreen`, `AddChannelDialog`, `EmptyChannelsView`, `PlayerScreen`, `ChannelSidebar`, `AndroidQrScannerProvider`) as well as in ViewModels and validator messages. Only the About dialog strings are currently extracted to `app/src/main/res/values/strings.xml`.

The goal is to make the application fully translatable and add a complete Turkish (`values-tr`) translation following Android system locale settings.

## Architecture & Decisions

1. **Locale Management**:
   - Follow Android system default locale. When the system locale is Turkish (`tr` / `tr-TR`), the application automatically displays Turkish text.
2. **String Extraction Scope**:
   - All user-facing UI labels, dialog headers, buttons, error messages, placeholders, feedback messages (snackbars), and accessibility content descriptions.
   - Channel counts / plural messages using Android `<plurals>` resources (e.g. "Successfully imported %d channel" vs "%d channels").
3. **ViewModel & Validation Layer**:
   - Introduce a `UiText` sealed interface (`UiText.DynamicString` and `UiText.StringResource` with format arguments and plurals support) in `com.gotv.player.ui.common` or `com.gotv.player.ui.util`.
   - Update `ChannelGridUiState` and `ChannelGridViewModel` to emit `UiText` for dynamic validation and snackbar messages, resolving strings in Compose via `stringResource()` or `asString()`.
4. **Translations (`res/values-tr/strings.xml`)**:
   - Full Turkish localization for all extracted string resources and plurals.
5. **Testing**:
   - Maintain existing tests passing in default locale.
   - Dedicated unit tests verifying `UiText` resolution in English and Turkish contexts.
   - Dedicated localization tests verifying Turkish translations and plurals completeness.
