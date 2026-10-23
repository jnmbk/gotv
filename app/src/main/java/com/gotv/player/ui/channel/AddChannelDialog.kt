package com.gotv.player.ui.channel

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gotv.player.R
import com.gotv.player.ui.qr.QrScannerProvider

@Composable
fun AddChannelDialog(
    uiState: ChannelGridUiState,
    onNameChange: (String) -> Unit,
    onStreamUrlChange: (Int, String) -> Unit,
    onAddStreamUrl: () -> Unit,
    onRemoveStreamUrl: (Int) -> Unit,
    onLogoUrlChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    qrScanner: QrScannerProvider? = null,
    onScanQr: (QrScanTarget) -> Unit = {}
) {
    val hasQrScanner = qrScanner != null
    val streamUrls = if (uiState.streamUrlsInput.isNotEmpty()) {
        uiState.streamUrlsInput
    } else {
        listOf(uiState.streamUrlInput)
    }
    val canDelete = streamUrls.size > 1

    AlertDialog(
        modifier = modifier.testTag("add_channel_dialog"),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    if (uiState.isEditMode) R.string.dialog_edit_channel_title else R.string.dialog_add_channel_title
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = uiState.nameInput,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(R.string.dialog_channel_name_label)) },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it.asString()) } },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("channel_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                streamUrls.forEachIndexed { index, streamUrl ->
                    val fieldError = uiState.streamUrlsErrors.getOrNull(index)
                    val labelText = if (index == 0) {
                        stringResource(R.string.dialog_stream_url_label)
                    } else {
                        stringResource(R.string.dialog_backup_stream_url_label, index)
                    }
                    val inputTag = if (index == 0) "stream_url_input" else "stream_url_input_$index"
                    val scanTag = if (index == 0) "scan_stream_url_qr_button" else "scan_stream_url_qr_button_$index"

                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { onStreamUrlChange(index, it) },
                        label = { Text(labelText) },
                        placeholder = { Text(stringResource(R.string.dialog_stream_url_placeholder)) },
                        trailingIcon = if (hasQrScanner || canDelete) {
                            {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (hasQrScanner) {
                                        IconButton(
                                            onClick = { onScanQr(QrScanTarget.StreamUrl(index)) },
                                            modifier = Modifier.testTag(scanTag)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PhotoCamera,
                                                contentDescription = stringResource(R.string.cd_scan_stream_url_qr)
                                            )
                                        }
                                    }
                                    if (canDelete) {
                                        IconButton(
                                            onClick = { onRemoveStreamUrl(index) },
                                            modifier = Modifier.testTag("remove_stream_url_button_$index")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = stringResource(R.string.cd_remove_stream_url)
                                            )
                                        }
                                    }
                                }
                            }
                        } else null,
                        isError = fieldError != null,
                        supportingText = fieldError?.let { { Text(it.asString()) } },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Next
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(inputTag)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = onAddStreamUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_stream_url_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dialog_add_stream_url_button))
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.logoUrlInput,
                    onValueChange = onLogoUrlChange,
                    label = { Text(stringResource(R.string.dialog_logo_url_label)) },
                    placeholder = { Text(stringResource(R.string.dialog_logo_url_placeholder)) },
                    trailingIcon = if (hasQrScanner) {
                        {
                            IconButton(
                                onClick = { onScanQr(QrScanTarget.LogoUrl) },
                                modifier = Modifier.testTag("scan_logo_url_qr_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoCamera,
                                    contentDescription = stringResource(R.string.cd_scan_logo_url_qr)
                                )
                            }
                        }
                    } else null,
                    isError = uiState.logoUrlError != null,
                    supportingText = uiState.logoUrlError?.let { { Text(it.asString()) } },
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
                Text(
                    stringResource(
                        if (uiState.isEditMode) R.string.action_save else R.string.action_add
                    )
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_channel_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

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
    onScanQr: (QrScanTarget) -> Unit = {}
) = AddChannelDialog(
    uiState = uiState,
    onNameChange = onNameChange,
    onStreamUrlChange = { _, url -> onStreamUrlChange(url) },
    onAddStreamUrl = {},
    onRemoveStreamUrl = {},
    onLogoUrlChange = onLogoUrlChange,
    onDismiss = onDismiss,
    onConfirm = onConfirm,
    modifier = modifier,
    qrScanner = qrScanner,
    onScanQr = onScanQr
)
