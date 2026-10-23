package com.gotv.player.ui.channel

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gotv.player.data.model.Channel
import com.gotv.player.ui.about.AboutDialog
import com.gotv.player.ui.qr.QrScannerProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelGridScreen(
    viewModel: ChannelGridViewModel,
    onChannelSelected: (Channel) -> Unit = {},
    modifier: Modifier = Modifier,
    qrScanner: QrScannerProvider? = null,
    onImportRequested: (() -> Unit)? = null,
    onExportRequested: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var dragOrder by remember { mutableStateOf<List<Channel>?>(null) }
    var contextMenuChannelId by remember { mutableStateOf<String?>(null) }
    var draggedChannelId by remember { mutableStateOf<String?>(null) }
    var draggedItemInitialOffset by remember { mutableStateOf(Offset.Zero) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var hasDragged by remember { mutableStateOf(false) }
    var overflowMenuExpanded by remember { mutableStateOf(false) }
    var isAboutDialogOpen by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }.orEmpty()
                viewModel.onImportFileContent(content)
            } catch (_: Exception) {
                viewModel.onImportFileContent("")
            }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = viewModel.onExportChannelsJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.bufferedWriter().use { writer ->
                        writer.write(json)
                    }
                }
                viewModel.onExportComplete(true)
            } catch (e: Exception) {
                viewModel.onExportComplete(false, e.localizedMessage)
            }
        }
    }

    LaunchedEffect(uiState.userFeedbackMessage) {
        val message = uiState.userFeedbackMessage ?: return@LaunchedEffect
        try {
            snackbarHostState.showSnackbar(message)
        } finally {
            viewModel.onFeedbackMessageDismissed()
        }
    }

    val displayChannels = dragOrder ?: uiState.channels
    val currentChannelsState by rememberUpdatedState(displayChannels)
    val contentPaddingPx = with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.toPx() }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("channel_grid_screen"),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.testTag("user_feedback_snackbar")
            )
        },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "GoTV",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(
                        onClick = viewModel::onOpenAddDialog,
                        modifier = Modifier.testTag("add_channel_top_bar_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Channel"
                        )
                    }
                    IconButton(
                        onClick = { overflowMenuExpanded = true },
                        modifier = Modifier.testTag("channel_overflow_menu_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = overflowMenuExpanded,
                        onDismissRequest = { overflowMenuExpanded = false },
                        modifier = Modifier.testTag("channel_overflow_dropdown_menu")
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import channels") },
                            onClick = {
                                overflowMenuExpanded = false
                                if (onImportRequested != null) {
                                    onImportRequested()
                                } else {
                                    importLauncher.launch(arrayOf("application/json", "*/*"))
                                }
                            },
                            modifier = Modifier.testTag("menu_import_channels")
                        )
                        DropdownMenuItem(
                            text = { Text("Export channels") },
                            onClick = {
                                overflowMenuExpanded = false
                                if (onExportRequested != null) {
                                    onExportRequested()
                                } else {
                                    exportLauncher.launch("gotv_channels.json")
                                }
                            },
                            modifier = Modifier.testTag("menu_export_channels")
                        )
                        DropdownMenuItem(
                            text = { Text("About") },
                            onClick = {
                                overflowMenuExpanded = false
                                isAboutDialogOpen = true
                            },
                            modifier = Modifier.testTag("menu_about")
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isEmpty) {
            EmptyChannelsView(
                onAddChannelClick = viewModel::onOpenAddDialog,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val longPress = awaitLongPressOrCancellation(down.id) ?: return@awaitEachGesture
                            val pointerInViewport = (longPress.position - Offset(contentPaddingPx, contentPaddingPx)).round()
                            val hitItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                IntRect(item.offset, item.size).contains(pointerInViewport)
                            } ?: gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
                                IntRect(item.offset, item.size).contains(longPress.position.round())
                            }
                            val activeId = hitItem?.key as? String ?: return@awaitEachGesture

                            contextMenuChannelId = activeId
                            draggedChannelId = activeId
                            val initialSlots = gridState.layoutInfo.visibleItemsInfo.map { item ->
                                IntRect(item.offset, item.size)
                            }
                            var workingOrder = currentChannelsState
                            dragOrder = workingOrder
                            draggedItemInitialOffset = Offset(
                                hitItem.offset.x.toFloat(),
                                hitItem.offset.y.toFloat()
                            )
                            dragOffset = Offset.Zero
                            hasDragged = false

                            val touchSlop = viewConfiguration.touchSlop
                            var totalMove = Offset.Zero
                            var currentTargetSlotIndex = initialSlots.indexOfFirst {
                                it.topLeft == hitItem.offset
                            }

                            try {
                                while (true) {
                                    val event = awaitPointerEvent(PointerEventPass.Initial)
                                    val change = event.changes.firstOrNull { it.id == longPress.id } ?: break
                                    if (!change.pressed) {
                                        change.consume()
                                        break
                                    }
                                    val delta = change.positionChangeIgnoreConsumed()
                                    if (delta != Offset.Zero) {
                                        totalMove += delta
                                        if (!hasDragged && totalMove.getDistance() > touchSlop) {
                                            hasDragged = true
                                            contextMenuChannelId = null
                                        }
                                        if (hasDragged) {
                                            change.consume()
                                            dragOffset += delta

                                            val centerX = (draggedItemInitialOffset.x + hitItem.size.width / 2f + dragOffset.x).toInt()
                                            val centerY = (draggedItemInitialOffset.y + hitItem.size.height / 2f + dragOffset.y).toInt()
                                            val targetSlotIndex = initialSlots.indexOfFirst { rect ->
                                                centerX >= rect.left &&
                                                    centerX <= rect.right &&
                                                    centerY >= rect.top &&
                                                    centerY <= rect.bottom
                                            }
                                            if (targetSlotIndex != -1 && targetSlotIndex != currentTargetSlotIndex) {
                                                val fromIndex = workingOrder.indexOfFirst { it.id == activeId }
                                                val toIndex = targetSlotIndex.coerceIn(0, workingOrder.lastIndex)
                                                if (fromIndex != -1 && fromIndex != toIndex) {
                                                    val mutable = workingOrder.toMutableList()
                                                    val moved = mutable.removeAt(fromIndex)
                                                    mutable.add(toIndex, moved)
                                                    workingOrder = mutable
                                                    dragOrder = mutable
                                                }
                                                currentTargetSlotIndex = targetSlotIndex
                                            }
                                        }
                                    }
                                }
                            } finally {
                                if (hasDragged) {
                                    viewModel.onReorderChannels(workingOrder.map { it.id })
                                }
                                dragOrder = null
                                draggedChannelId = null
                                dragOffset = Offset.Zero
                                hasDragged = false
                            }
                        }
                    }
                    .testTag("channels_grid")
            ) {
                items(
                    items = displayChannels,
                    key = { it.id }
                ) { channel ->
                    val isDraggingThis = hasDragged && draggedChannelId == channel.id
                    ChannelTile(
                        channel = channel,
                        logoVersion = uiState.logoReloadVersions[channel.id] ?: 0,
                        isContextMenuOpen = contextMenuChannelId == channel.id,
                        onDismissContextMenu = {
                            if (contextMenuChannelId == channel.id) {
                                contextMenuChannelId = null
                            }
                        },
                        onEditChannel = {
                            viewModel.onOpenEditDialog(channel)
                        },
                        onRedownloadLogo = {
                            viewModel.onRedownloadLogo(channel)
                        },
                        onDeleteChannel = {
                            viewModel.onRequestDeleteChannel(channel)
                        },
                        onClick = {
                            viewModel.onChannelClicked(channel)
                            onChannelSelected(channel)
                        },
                        modifier = Modifier
                            .zIndex(if (isDraggingThis) 1f else 0f)
                            .graphicsLayer {
                                if (isDraggingThis) {
                                    val currentItem = gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == channel.id }
                                    val layoutShiftX = ((currentItem?.offset?.x ?: draggedItemInitialOffset.x.toInt()) - draggedItemInitialOffset.x)
                                    val layoutShiftY = ((currentItem?.offset?.y ?: draggedItemInitialOffset.y.toInt()) - draggedItemInitialOffset.y)
                                    translationX = dragOffset.x - layoutShiftX
                                    translationY = dragOffset.y - layoutShiftY
                                    scaleX = 1.05f
                                    scaleY = 1.05f
                                    shadowElevation = 8.dp.toPx()
                                }
                            }
                    )
                }
            }
        }

        val deletingChannel = uiState.deletingChannel
        if (deletingChannel != null) {
            AlertDialog(
                modifier = Modifier.testTag("delete_channel_dialog"),
                onDismissRequest = viewModel::onDismissDeleteDialog,
                title = {
                    Text(text = "Delete ${deletingChannel.name}?")
                },
                text = {
                    Text(text = "Are you sure you want to remove ${deletingChannel.name}?")
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::onConfirmDeleteChannel,
                        modifier = Modifier.testTag("confirm_delete_channel_button")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::onDismissDeleteDialog,
                        modifier = Modifier.testTag("cancel_delete_channel_button")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (uiState.isImportConfirmDialogOpen) {
            AlertDialog(
                modifier = Modifier.testTag("import_confirm_dialog"),
                onDismissRequest = viewModel::onDismissImportConfirmDialog,
                title = {
                    Text(text = "Replace all channels?")
                },
                text = {
                    Text(text = "This will replace all current channels. Proceed?")
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::onConfirmImport,
                        modifier = Modifier.testTag("confirm_import_button")
                    ) {
                        Text("Proceed")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = viewModel::onDismissImportConfirmDialog,
                        modifier = Modifier.testTag("cancel_import_button")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (uiState.isAddDialogOpen) {
            AddChannelDialog(
                uiState = uiState,
                onNameChange = viewModel::onNameChanged,
                onStreamUrlChange = viewModel::onStreamUrlChanged,
                onAddStreamUrl = viewModel::onAddStreamUrlField,
                onRemoveStreamUrl = viewModel::onRemoveStreamUrlField,
                onLogoUrlChange = viewModel::onLogoUrlChanged,
                onDismiss = viewModel::onDismissAddDialog,
                onConfirm = { viewModel.onSaveChannel() },
                qrScanner = qrScanner,
                onScanQr = viewModel::onOpenQrScanner
            )
        }

        if (uiState.isAddDialogOpen && uiState.isQrScannerOpen && qrScanner != null) {
            Dialog(
                onDismissRequest = viewModel::onCloseQrScanner,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    qrScanner.CameraScanner(
                        onQrDetected = viewModel::onQrPayloadScanned,
                        onClose = viewModel::onCloseQrScanner,
                        modifier = Modifier
                    )
                }
            }
        }

        if (isAboutDialogOpen) {
            AboutDialog(
                onDismissRequest = { isAboutDialogOpen = false }
            )
        }
    }
}

