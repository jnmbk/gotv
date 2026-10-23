# Issue 01: Enable R8 minification and resource shrinking for release builds

Status: resolved

## Description
Configure Android release builds to enable R8 code shrinking, optimization, and resource stripping (`isMinifyEnabled = true`, `isShrinkResources = true`). Supply required ProGuard keep rules for Jetpack Compose UI platform locals, ViewTree owners, and Media3 ExoPlayer classes to avoid runtime reflection or missing CompositionLocal issues.

## Acceptance Criteria
- [x] `isMinifyEnabled = true` and `isShrinkResources = true` enabled on the `release` build type in `app/build.gradle.kts`.
- [x] Required ProGuard / R8 rules added to `app/proguard-rules.pro` for Compose UI, ViewTree owners, and Media3.
- [x] APK size reduced from ~15 MB down to ~3.7 MB; uncompressed DEX reduced from 47.4 MB to 3.76 MB.
- [x] Release build compiles cleanly (`./gradlew assembleRelease`).
- [x] All unit tests pass (`./gradlew test`).
- [x] Verified runtime behavior on Android emulator without runtime crashes.

## Comments
- Enabled R8 and resource shrinking in `app/build.gradle.kts`.
- Added keep rules in `app/proguard-rules.pro` for `AndroidCompositionLocals_androidKt`, `ViewTreeLifecycleOwner`, `ViewTreeSavedStateRegistryOwner`, `ViewTreeViewModelStoreOwner`, and `androidx.media3.**`.
- Verified live playback and navigation on connected emulator.
