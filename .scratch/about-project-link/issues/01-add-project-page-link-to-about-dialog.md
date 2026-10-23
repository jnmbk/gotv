# 01: Add Project Page Link to About Dialog

**What to build:** Add a "Source Code" section to the `AboutDialog` placed directly before the "Author" section (between Version and Author). Display the section label ("Source Code") and a clickable link ("GitHub (jnmbk/gotv)") that opens `https://github.com/jnmbk/gotv`. Add string resources in `strings.xml`. Add unit and Compose UI tests to verify display and click interactions.

**Blocked by:** none

**Status:** open

- [ ] Add string resources for `about_source_code_label`, `about_source_code_name`, and `about_source_code_url` in `strings.xml`
- [ ] Update `AboutDialog` composable to accept source code parameters and render the section with test tags `about_dialog_source_code_section` and `about_dialog_source_code_link` before Author
- [ ] Add unit and Compose tests in `AboutDialogTest` verifying rendering and click handling of the project source code link
- [ ] Run test suite (`./gradlew test`) to verify all tests pass
