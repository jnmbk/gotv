Status: ready-for-agent

# About Page, SemVer, and GitHub CI/Release Specification

## Problem Statement

The GoTV application currently:
1. Uses hardcoded static version strings (`versionCode = 1`, `versionName = "1.0"`) without SemVer compliance or automated version management.
2. Lacks an in-app About page to inform users of the application version, author credits (Uğur Çetin), and open-source license (Apache License 2.0).
3. Lacks automated GitHub Actions Continuous Integration (CI) to run unit tests and verify APK compilation across pull requests and pushes to `main`.
4. Lacks automated release management and changelog generation using Conventional Commits.

## Solution

1. **Semantic Versioning & Release Please**:
   - Centralize project versioning in `gradle.properties` (`version=1.0.0`).
   - Dynamically compute `versionCode` and `versionName` in `app/build.gradle.kts`.
   - Enable `buildFeatures.buildConfig = true` to expose version metadata programmatically.
   - Configure Google Release Please (`release-please-config.json` and `.release-please-manifest.json`) targeting `gradle.properties` for automated SemVer bumps, tags, and changelogs.

2. **In-App About Dialog**:
   - Provide an "About" action within the `ChannelGridScreen` top bar overflow menu.
   - Display a lightweight Material 3 `AboutDialog` showing:
     - Application name ("GoTV") and SemVer version (e.g. `v1.0.0` from `BuildConfig.VERSION_NAME`).
     - Author information: "Uğur Çetin" with clickable contact/website action.
     - License information: "Apache License 2.0" with action to view license terms.
     - Dismiss/close button.
   - Comprehensive unit and Compose tests verifying dialog behavior, UI elements, and interactions.

3. **GitHub Actions CI & Release Pipelines**:
   - `.github/workflows/ci.yml`: Triggered on PRs and pushes to `main`, setting up JDK 17 with Gradle caching, running `./gradlew test assembleDebug`, and archiving the debug APK.
   - `.github/workflows/release.yml`: Triggered on pushes to `main`, executing Release Please. Upon release creation, it builds `./gradlew assembleRelease` and uploads the unsigned release APK as a GitHub release asset.

## User Stories

1. As a user, I want to view an About dialog from the main screen menu so that I can see the application version, author, and license.
2. As a user, I want clickable links for the author and license so that I can easily visit author credits and review open-source licensing terms.
3. As a developer, I want versioning centralized in `gradle.properties` conforming to SemVer (`MAJOR.MINOR.PATCH`) so that version increments are standard and predictable.
4. As a maintainer, I want Release Please to automate changelogs, SemVer bumps, and GitHub Releases based on Conventional Commits.
5. As a maintainer, I want GitHub Actions CI to validate unit tests and APK compilation on every PR and commit to `main`.
6. As a maintainer, I want GitHub releases to automatically include the compiled release APK artifact.

## Implementation Decisions

- **UI Pattern**: Material 3 `AlertDialog` embedded within `ChannelGridScreen` triggered from the existing top-bar overflow dropdown menu. Keeps architecture simple without extra navigation routes.
- **Dynamic SemVer**: Read `version` from `gradle.properties` and compute `versionCode = major * 10000 + minor * 100 + patch`. Expose `BuildConfig.VERSION_NAME` to Compose.
- **CI Tooling**: Standard GitHub Actions using `actions/setup-java@v4` with Temurin JDK 17 and built-in Gradle caching, `actions/upload-artifact@v4` for debug APKs, and `googleapis/release-please-action@v4` for releases.
