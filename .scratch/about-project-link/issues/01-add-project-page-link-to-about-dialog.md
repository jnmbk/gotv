# 01: Add Project Page Link to About Dialog

**What to build:** Add a "Source Code" section to the `AboutDialog` placed directly before the "Author" section (between Version and Author). Display the section label ("Source Code") and a clickable link ("GitHub (jnmbk/gotv)") that opens `https://github.com/jnmbk/gotv`. Add string resources in `strings.xml`. Add unit and Compose UI tests to verify display and click interactions.

**Blocked by:** none

**Status:** resolved

- [x] Add string resources for `about_source_code_label`, `about_source_code_name`, and `about_source_code_url` in `strings.xml`
- [x] Update `AboutDialog` composable to accept source code parameters and render the section with test tags `about_dialog_source_code_section` and `about_dialog_source_code_link` before Author
- [x] Add unit and Compose tests in `AboutDialogTest` verifying rendering and click handling of the project source code link
- [x] Run test suite (`./gradlew test`) to verify all tests pass

## Comments

### Implementation Summary
- Added string resources `about_source_code_label` ("Source Code"), `about_source_code_name` ("GitHub (jnmbk/gotv)"), and `about_source_code_url` ("https://github.com/jnmbk/gotv") in `strings.xml`.
- Extended `AboutDialog` to accept `sourceCodeLabel`, `sourceCodeName`, and `sourceCodeUrl`, rendering the Source Code section right before the Author section with test tags `about_dialog_source_code_section` and `about_dialog_source_code_link`.
- Added unit and Compose tests in `AboutDialogTest` covering rendering of label and link, URL dispatch on click, and custom parameter rendering.
- Verified test suite passes via `./gradlew test` and `./gradlew testDebugUnitTest -PslowTests`.
