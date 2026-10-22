# 03: GitHub Actions CI & Release Workflows

**What to build:** Create automated GitHub Actions workflows in `.github/workflows/`.
1. `ci.yml`: Trigger on pull requests and pushes to `main`. Set up Temurin JDK 17 with Gradle cache. Run unit tests (`./gradlew test`) and build debug APK (`./gradlew assembleDebug`). Upload the debug APK as a workflow artifact.
2. `release.yml`: Trigger on pushes to `main`. Run `googleapis/release-please-action@v4` with standard GITHUB_TOKEN permissions. When a release is created (`steps.release.outputs.releases_created == 'true'`), build `./gradlew assembleRelease` and upload `app/build/outputs/apk/release/app-release-unsigned.apk` to the GitHub release assets.

**Blocked by:** 01-semver-and-release-please-configuration.md

**Status:** open

- [ ] Create `.github/workflows/ci.yml` with test and assembleDebug jobs and artifact upload
- [ ] Create `.github/workflows/release.yml` with Release Please action and release APK packaging step
- [ ] Ensure Gradle wrapper permissions and workflow trigger conditions are properly configured
- [ ] Validate workflow YAML syntax
