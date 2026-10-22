package com.gotv.player.ui.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gotv.player.ui.qr.QrScannerProvider

@Composable
fun AddChannelDialog(
    uiState: ChannelGridUiState,
    onNameChange: (String) -> Unit,
    onStreamUrlChange: (String) -> Unit,
    onLogoUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    qrScanner: QrScannerProvider? = null,
    onScanQrClick: () -> Unit = {},
    onQrImagePicked: (String?) -> Unit = {}
) {
    val pickImage = qrScanner?.rememberImagePicker(onQrImagePicked)
    AlertDialog(
        modifier = modifier.testTag("add_channel_dialog"),
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Add Channel")
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (pickImage != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onScanQrClick,
                            modifier = Modifier.testTag("scan_qr_button")
                        ) {
                            Text("Scan QR Code")
                        }
                        OutlinedButton(
                            onClick = pickImage,
                            modifier = Modifier.testTag("import_qr_image_button")
                        ) {
                            Text("From Image")
                        }
                    }
                    if (uiState.qrError != null) {
                        Text(
                            text = uiState.qrError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .testTag("qr_error_text")
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedTextField(
                    value = uiState.nameInput,
                    onValueChange = onNameChange,
                    label = { Text("Channel Name") },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.streamUrlInput,
                    onValueChange = onStreamUrlChange,
                    label = { Text("Stream URL") },
                    placeholder = { Text("https://example.com/live.m3u8") },
                    isError = uiState.streamUrlError != null,
                    supportingText = uiState.streamUrlError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stream_url_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.logoUrlInput,
                    onValueChange = onLogoUrlChange,
                    label = { Text("Logo URL (Optional)") },
                    placeholder = { Text("https://example.com/logo.png") },
                    isError = uiState.logoUrlError != null,
                    supportingText = uiState.logoUrlError?.let { { Text(it) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("logo_url_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("save_channel_button")
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_channel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

