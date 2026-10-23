Status: ready-for-agent

# Add Project Page Link to About Dialog Specification

## Problem Statement

The GoTV in-app About dialog currently displays the application version, author credits (Uğur Çetin), and open-source license information (Apache License 2.0). However, it lacks a direct link to the project source code repository (`https://github.com/jnmbk/gotv`), making it harder for users to access the source repository, report issues, or contribute.

## Solution

1. **String Resources**:
   - Define string resources in `app/src/main/res/values/strings.xml`:
     - `about_source_code_label`: `"Source Code"`
     - `about_source_code_name`: `"GitHub (jnmbk/gotv)"`
     - `about_source_code_url`: `"https://github.com/jnmbk/gotv"`

2. **AboutDialog Composable**:
   - Add parameters to `AboutDialog`:
     - `sourceCodeLabel: String = stringResource(R.string.about_source_code_label)`
     - `sourceCodeName: String = stringResource(R.string.about_source_code_name)`
     - `sourceCodeUrl: String = stringResource(R.string.about_source_code_url)`
   - Place the Source Code section right before the Author section in the layout:
     - Version (`about_dialog_version`)
     - **Source Code** (`about_dialog_source_code_section` with label and clickable link `about_dialog_source_code_link`)
     - Author (`about_dialog_author_section` with label and clickable link)
     - License (`about_dialog_license_section` with label, notice, and clickable link)
   - Ensure clicking the source code link invokes `handleOpenUrl(sourceCodeUrl)`.

3. **Testing**:
   - Update `AboutDialogTest` to verify:
     - The "Source Code" label and "GitHub (jnmbk/gotv)" link render and are displayed.
     - Tapping the source code link triggers `onOpenUrl` with `https://github.com/jnmbk/gotv`.
     - Custom source code name and URL parameters work as expected.
