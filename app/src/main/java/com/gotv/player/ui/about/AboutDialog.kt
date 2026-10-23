package com.gotv.player.ui.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.gotv.player.BuildConfig
import com.gotv.player.R

@Composable
fun AboutDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    versionName: String = BuildConfig.VERSION_NAME,
    sourceCodeLabel: String = stringResource(R.string.about_source_code_label),
    sourceCodeName: String = stringResource(R.string.about_source_code_name),
    sourceCodeUrl: String = stringResource(R.string.about_source_code_url),
    authorName: String = stringResource(R.string.about_author_name),
    authorUrl: String = stringResource(R.string.about_author_url),
    licenseName: String = stringResource(R.string.about_license_name),
    licenseNotice: String = stringResource(R.string.about_license_notice),
    licenseUrl: String = stringResource(R.string.about_license_url),
    onOpenUrl: ((String) -> Unit)? = null
) {
    val uriHandler = LocalUriHandler.current
    val handleOpenUrl = onOpenUrl ?: remember(uriHandler) {
        { url: String ->
            try {
                uriHandler.openUri(url)
            } catch (_: Exception) {
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = modifier.testTag("about_dialog"),
        title = {
            Text(
                text = stringResource(R.string.about_dialog_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.testTag("about_dialog_title")
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_dialog_content"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.about_version_format, versionName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("about_dialog_version")
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_dialog_source_code_section")
                ) {
                    Text(
                        text = sourceCodeLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = sourceCodeName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .testTag("about_dialog_source_code_link")
                            .clickable { handleOpenUrl(sourceCodeUrl) }
                            .padding(vertical = 2.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_dialog_author_section")
                ) {
                    Text(
                        text = stringResource(R.string.about_author_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = authorName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .testTag("about_dialog_author_link")
                            .clickable { handleOpenUrl(authorUrl) }
                            .padding(vertical = 2.dp)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("about_dialog_license_section")
                ) {
                    Text(
                        text = stringResource(R.string.about_license_label),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = licenseNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .testTag("about_dialog_license_notice")
                            .padding(vertical = 2.dp)
                    )
                    Text(
                        text = licenseName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .testTag("about_dialog_license_link")
                            .clickable { handleOpenUrl(licenseUrl) }
                            .padding(vertical = 2.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("about_dialog_dismiss_button")
            ) {
                Text(text = stringResource(R.string.about_dismiss_button))
            }
        }
    )
}
