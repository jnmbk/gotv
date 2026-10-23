# Spec: App Disk Space & APK Size Optimization

## Goal
Reduce the disk footprint of the application and its distribution APK without losing any functionality or breaking playback resilience, Jetpack Compose runtime state, or media playback engines.

## Background & Measurements
Prior to minification:
- APK Size: ~15.0 MB
- Uncompressed DEX: 47.4 MB (3 DEX files: `classes.dex`, `classes2.dex`, `classes3.dex`)
- Uncompressed Total Package: 49.1 MB
When installed on Android devices, ART pre-compiles and optimizes multi-dex bytecode, expanding total disk usage to 60–100+ MB on disk.

With R8 code minification and resource shrinking enabled:
- APK Size: ~3.7 MB (down by ~75%)
- Uncompressed DEX: ~3.76 MB (single `classes.dex`, down by ~92%)
- Uncompressed Total Package: ~5.0 MB (down by ~90%)

## Requirements & Design Decisions

### 1. Enable R8 Code Minification & Resource Shrinking
- In `app/build.gradle.kts`, configure `release` build type with `isMinifyEnabled = true` and `isShrinkResources = true`.
- Maintain ProGuard optimization profile: `getDefaultProguardFile("proguard-android-optimize.txt")` and `proguard-rules.pro`.

### 2. ProGuard & R8 Compatibility Rules
- Preserve Compose UI platform locals (`AndroidCompositionLocals_androidKt`) to prevent runtime `IllegalStateException: CompositionLocal LocalLifecycleOwner not present`.
- Keep ViewTree owners (`ViewTreeLifecycleOwner`, `ViewTreeSavedStateRegistryOwner`, `ViewTreeViewModelStoreOwner`).
- Keep Media3 ExoPlayer classes to avoid runtime reflection errors during audio/video renderer discovery.

### 3. Verification & Validation
- Ensure full unit test suite passes (`./gradlew test`).
- Ensure release build succeeds (`./gradlew assembleRelease`).
- Validate app installation and execution on an active Android device / emulator.
- Retake documentation feature screenshots with the optimized app running.
