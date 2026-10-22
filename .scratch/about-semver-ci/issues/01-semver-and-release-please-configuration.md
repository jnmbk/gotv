# 01: SemVer & Release Please Configuration

**What to build:** Configure semantic versioning (`MAJOR.MINOR.PATCH`) starting at `1.0.0` defined centrally in `gradle.properties`. In `app/build.gradle.kts`, dynamically parse the version property to set `versionName` and calculate an integer `versionCode` (`major * 10000 + minor * 100 + patch`). Enable `buildFeatures.buildConfig = true` in Android application configuration so version information is accessible in code via `BuildConfig.VERSION_NAME`. Set up Google Release Please configuration files (`release-please-config.json` and `.release-please-manifest.json`) targeting `gradle.properties` so that Conventional Commits automatically generate changelogs and bump versions upon merge to `main`.

**Blocked by:** None (can start immediately)

**Status:** open

- [ ] Add `version=1.0.0` in `gradle.properties`
- [ ] Parse `version` in `app/build.gradle.kts` to compute `versionCode` and `versionName` dynamically
- [ ] Enable `buildFeatures.buildConfig = true` in `app/build.gradle.kts`
- [ ] Add `.release-please-manifest.json` initializing root version at `1.0.0`
- [ ] Add `release-please-config.json` configured for generic/simple strategy with `extra-files` updating `gradle.properties`
- [ ] Verify Gradle sync and build succeed with dynamic versioning
