# 01: SemVer & Release Please Configuration

**What to build:** Configure semantic versioning (`MAJOR.MINOR.PATCH`) starting at `1.0.0` defined centrally in `gradle.properties`. In `app/build.gradle.kts`, dynamically parse the version property to set `versionName` and calculate an integer `versionCode` (`major * 10000 + minor * 100 + patch`). Enable `buildFeatures.buildConfig = true` in Android application configuration so version information is accessible in code via `BuildConfig.VERSION_NAME`. Set up Google Release Please configuration files (`release-please-config.json` and `.release-please-manifest.json`) targeting `gradle.properties` so that Conventional Commits automatically generate changelogs and bump versions upon merge to `main`.

**Blocked by:** None (can start immediately)

**Status:** resolved

- [x] Add `version=1.0.0` in `gradle.properties`
- [x] Parse `version` in `app/build.gradle.kts` to compute `versionCode` and `versionName` dynamically
- [x] Enable `buildFeatures.buildConfig = true` in `app/build.gradle.kts`
- [x] Add `.release-please-manifest.json` initializing root version at `1.0.0`
- [x] Add `release-please-config.json` configured for generic/simple strategy with `extra-files` updating `gradle.properties`
- [x] Verify Gradle sync and build succeed with dynamic versioning

## Answer

Issue 1 has been fully implemented:
1. **SemVer Centralization**: Defined `version=1.0.0` in `gradle.properties` with release-please boundary comments (`# {x-release-please-start-version}` / `# {x-release-please-end-version}`).
2. **Dynamic Version Parsing**: Configured `app/build.gradle.kts` to dynamically parse the version property, setting `versionName = "1.0.0"` and calculating `versionCode = 10000` (`major * 10000 + minor * 100 + patch`).
3. **BuildConfig Generation**: Enabled `buildFeatures.buildConfig = true` in `app/build.gradle.kts`, exposing `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE`.
4. **Release Please Config**: Created `.release-please-manifest.json` initialized at `1.0.0` and `release-please-config.json` targeting `gradle.properties` as an `extra-files` entry using the simple release strategy.
5. **Verification**: Executed `./gradlew test`, verifying successful compilation, test pass, and accurate `BuildConfig` generation (`VERSION_NAME = "1.0.0"`, `VERSION_CODE = 10000`).
