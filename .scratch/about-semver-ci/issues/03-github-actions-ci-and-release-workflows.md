# 03: GitHub Actions CI & Release Workflows

**What to build:** Create automated GitHub Actions workflows in `.github/workflows/`.
1. `ci.yml`: Trigger on pull requests and pushes to `main`. Set up Temurin JDK 17 with Gradle cache. Run unit tests (`./gradlew test`) and build debug APK (`./gradlew assembleDebug`). Upload the debug APK as a workflow artifact.
2. `release.yml`: Trigger on pushes to `main`. Run `googleapis/release-please-action@v4` with standard GITHUB_TOKEN permissions. When a release is created (`steps.release.outputs.releases_created == 'true'`), build `./gradlew assembleRelease` and upload `app/build/outputs/apk/release/app-release-unsigned.apk` to the GitHub release assets.

**Blocked by:** 01-semver-and-release-please-configuration.md

**Status:** resolved

- [x] Create `.github/workflows/ci.yml` with test and assembleDebug jobs and artifact upload
- [x] Create `.github/workflows/release.yml` with Release Please action and release APK packaging step
- [x] Ensure Gradle wrapper permissions and workflow trigger conditions are properly configured
- [x] Validate workflow YAML syntax

## Answer

Issue 03 has been fully implemented and verified:
1. **Continuous Integration Workflow (`.github/workflows/ci.yml`)**:
   - Triggers on push and pull request events targeting the `main` branch.
   - Defines a `test` job that sets up Temurin JDK 17 with Gradle caching, ensures Gradle wrapper execution permissions (`chmod +x gradlew`), and runs unit tests via `./gradlew test`.
   - Defines an `assembleDebug` job that sets up Temurin JDK 17 with Gradle caching, ensures Gradle wrapper permissions, compiles the debug APK via `./gradlew assembleDebug`, and uploads the artifact `app/build/outputs/apk/debug/app-debug.apk` via `actions/upload-artifact@v4`.
2. **Release Workflow (`.github/workflows/release.yml`)**:
   - Triggers on push events targeting the `main` branch with `contents: write` and `pull-requests: write` permissions.
   - Executes `googleapis/release-please-action@v4` pointing to `release-please-config.json` and `.release-please-manifest.json`.
   - Conditionally triggers packaging when a release is cut (`steps.release.outputs.releases_created == 'true'`), configuring JDK 17, compiling `./gradlew assembleRelease`, and uploading `app/build/outputs/apk/release/app-release-unsigned.apk` to the GitHub release assets using `gh release upload --clobber`.
3. **Verification**:
   - Validated YAML syntax for both workflow files.
   - Verified locally that `./gradlew test assembleDebug assembleRelease` succeeds and produces the expected APK assets at the exact paths targeted by the workflows.

## Comments

- Fixed CI failure where `org.gradle.java.home=/usr/lib/jvm/java-17-openjdk` hardcoded in `gradle.properties` caused GitHub Actions runner (`ubuntu-latest`) to fail with invalid Java home. Removed the hardcoded property from repository-tracked `gradle.properties` so CI can use the JDK 17 configured by `actions/setup-java`. Local developer environments requiring specific JDK overrides should place them in `~/.gradle/gradle.properties` or set `JAVA_HOME`.

