package com.gotv.player.ui.channel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gotv.player.ui.qr.QrScannerProvider

@Composable
fun ImportFromUrlDialog(
    uiState: ChannelGridUiState,
    onUrlChange: (String) -> Unit,
    onClearUrl: () -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    qrScanner: QrScannerProvider? = null,
    onScanQr: () -> Unit = {}
) {
    val hasQrScanner = qrScanner != null
    val isLoading = uiState.isImportUrlLoading

    AlertDialog(
        modifier = modifier.testTag("import_from_url_dialog"),
        onDismissRequest = {
            if (!isLoading) {
                onDismiss()
            }
        },
        title = {
            Text(text = "Import from URL")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = uiState.importUrlInput,
                    onValueChange = onUrlChange,
                    label = { Text("URL") },
                    placeholder = { Text("https://example.com/channels.json") },
                    enabled = !isLoading,
                    singleLine = true,
                    isError = uiState.importUrlError != null,
                    supportingText = uiState.importUrlError?.let {
                        {
                            Text(
                                text = it,
                                modifier = Modifier.testTag("import_url_error_text")
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!isLoading) {
                                onSubmit()
                            }
                        }
                    ),
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.importUrlInput.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearUrl,
                                    enabled = !isLoading,
                                    modifier = Modifier.testTag("clear_import_url_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear URL"
                                    )
                                }
                            }
                            if (hasQrScanner) {
                                IconButton(
                                    onClick = onScanQr,
                                    enabled = !isLoading,
                                    modifier = Modifier.testTag("scan_import_url_qr_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PhotoCamera,
                                        contentDescription = "Scan URL QR code"
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("import_url_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onSubmit,
                enabled = !isLoading,
                modifier = Modifier.testTag("submit_import_url_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .testTag("import_url_loading_indicator"),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isLoading,
                modifier = Modifier.testTag("cancel_import_url_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
