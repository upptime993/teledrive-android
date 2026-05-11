package com.teledrive.sky.presentation.files

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.teledrive.sky.domain.model.*
import com.teledrive.sky.presentation.components.*
import com.teledrive.sky.ui.theme.*
import com.teledrive.sky.util.FileSizeFormatter
import com.teledrive.sky.util.MimeTypeUtils
import com.teledrive.sky.util.RelativeDateFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileBrowserScreen(
    folderId: String?,
    navController: NavController,
    viewModel: FileBrowserViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    LaunchedEffect(folderId) { viewModel.init(folderId) }

    // File picker
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        uris.forEach { uri ->
            val name = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                c.moveToFirst()
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0) c.getString(idx) else "file"
            } ?: "file"
            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"
            val size = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                c.moveToFirst()
                val idx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                if (idx >= 0) c.getLong(idx) else 0L
            } ?: 0L
        }
    }

    val pullState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(SkyBackground)) {
        PullToRefreshBox(
            state = pullState,
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refresh()
                isRefreshing = false
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar
                FileBrowserTopBar(
                    breadcrumbs = uiState.breadcrumbs,
                    isMultiSelect = uiState.isMultiSelectMode,
                    selectedCount = uiState.selectedIds.size,
                    isGridView = uiState.isGridView,
                    onNavigateUp = { navController.navigateUp() },
                    onToggleView = viewModel::toggleViewMode,
                    onSort = viewModel::toggleSortSheet,
                    onSelectAll = viewModel::selectAll,
                    onClearSelection = viewModel::clearSelection,
                    onDeleteSelected = viewModel::deleteSelectedItems,
                )

                // Active transfers bar
                AnimatedVisibility(visible = uiState.activeTransfers.isNotEmpty()) {
                    TransferProgressBar(transfers = uiState.activeTransfers)
                }

                // Content
                if (uiState.isLoading && uiState.files.isEmpty() && uiState.folders.isEmpty()) {
                    FileListShimmer(isGrid = uiState.isGridView)
                } else if (uiState.files.isEmpty() && uiState.folders.isEmpty() && !uiState.isLoading) {
                    EmptyFolderState(onUpload = { filePicker.launch("*/*") })
                } else {
                    if (uiState.isGridView) {
                        FileGridContent(
                            folders = uiState.folders,
                            files = uiState.files,
                            selectedIds = uiState.selectedIds,
                            isMultiSelect = uiState.isMultiSelectMode,
                            onFolderClick = { folder ->
                                navController.navigate("folder/${folder.id}")
                            },
                            onFileClick = { /* open preview */ },
                            onLongPress = { id ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleSelectFile(id)
                            },
                            onToggleSelect = viewModel::toggleSelectFile,
                            onStar = { id, starred -> viewModel.starFile(id, starred) },
                            onDelete = viewModel::moveToTrash,
                        )
                    } else {
                        FileListContent(
                            folders = uiState.folders,
                            files = uiState.files,
                            selectedIds = uiState.selectedIds,
                            isMultiSelect = uiState.isMultiSelectMode,
                            onFolderClick = { folder ->
                                navController.navigate("folder/${folder.id}")
                            },
                            onFileClick = { /* open preview */ },
                            onLongPress = { id ->
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.toggleSelectFile(id)
                            },
                            onToggleSelect = viewModel::toggleSelectFile,
                            onStar = { id, starred -> viewModel.starFile(id, starred) },
                            onDelete = viewModel::moveToTrash,
                        )
                    }
                }
            }
        }

        // FAB
        if (!uiState.isMultiSelectMode) {
            FileFAB(
                onUpload = { filePicker.launch("*/*") },
                onNewFolder = viewModel::showCreateFolderDialog,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp),
            )
        }

        // Dialogs
        if (uiState.showCreateFolderDialog) {
            CreateFolderDialog(
                name = uiState.newFolderName,
                onNameChange = viewModel::onNewFolderNameChange,
                onCreate = viewModel::createFolder,
                onDismiss = viewModel::dismissCreateFolderDialog,
            )
        }

        if (uiState.showSortSheet) {
            SortBottomSheet(
                current = uiState.sortConfig,
                onSelect = viewModel::setSortConfig,
                onDismiss = viewModel::toggleSortSheet,
            )
        }

        // Error snackbar
        uiState.error?.let { error ->
            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 72.dp, start = 16.dp, end = 16.dp)) {
                Snackbar(
                    action = {
                        TextButton(onClick = viewModel::dismissError) {
                            Text("Tutup", color = SkyBlueLight)
                        }
                    },
                    containerColor = SkySurfaceHighest,
                    contentColor = SkyTextPrimary,
                ) { Text(error) }
            }
        }
    }
}

@Composable
private fun FileBrowserTopBar(
    breadcrumbs: List<BreadcrumbItem>,
    isMultiSelect: Boolean,
    selectedCount: Int,
    isGridView: Boolean,
    onNavigateUp: () -> Unit,
    onToggleView: () -> Unit,
    onSort: () -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
) {
    Surface(
        color = SkyBackground,
        shadowElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedContent(targetState = isMultiSelect, label = "topBar") { multiSelect ->
                    if (multiSelect) {
                        // Multi-select mode
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            IconButton(onClick = onClearSelection) {
                                Icon(Icons.Default.Close, "Batal", tint = SkyTextSecondary)
                            }
                            Text(
                                "$selectedCount dipilih",
                                style = MaterialTheme.typography.titleMedium,
                                color = SkyTextPrimary,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = onSelectAll) {
                                Icon(Icons.Default.SelectAll, "Pilih Semua", tint = SkyBlue)
                            }
                            IconButton(onClick = onDeleteSelected) {
                                Icon(Icons.Default.Delete, "Hapus", tint = SkyError)
                            }
                        }
                    } else {
                        // Normal mode
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            if (breadcrumbs.size > 1) {
                                IconButton(onClick = onNavigateUp) {
                                    Icon(Icons.Default.ArrowBack, "Kembali", tint = SkyTextSecondary)
                                }
                            }
                            // Title from breadcrumb
                            Text(
                                text = breadcrumbs.lastOrNull()?.name ?: "Beranda",
                                style = MaterialTheme.typography.titleLarge,
                                color = SkyTextPrimary,
                                modifier = Modifier.weight(1f).padding(start = if (breadcrumbs.size > 1) 0.dp else 8.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(onClick = onSort) {
                                Icon(Icons.Default.Sort, "Urutkan", tint = SkyTextSecondary)
                            }
                            IconButton(onClick = onToggleView) {
                                Icon(
                                    if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                    "Tampilan",
                                    tint = SkyTextSecondary,
                                )
                            }
                        }
                    }
                }
            }

            // Breadcrumb trail
            if (breadcrumbs.size > 2) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    items(breadcrumbs.dropLast(1)) { crumb ->
                        Text(
                            text = crumb.name,
                            style = MaterialTheme.typography.labelMedium,
                            color = SkyTextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Icon(Icons.Default.ChevronRight, null, tint = SkyTextTertiary, modifier = Modifier.size(14.dp))
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            HorizontalDivider(color = SkyDivider, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun FileListContent(
    folders: List<TeleFolder>,
    files: List<TeleFile>,
    selectedIds: Set<String>,
    isMultiSelect: Boolean,
    onFolderClick: (TeleFolder) -> Unit,
    onFileClick: (TeleFile) -> Unit,
    onLongPress: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onStar: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        if (folders.isNotEmpty()) {
            item {
                SectionHeader("Folder", folders.size)
            }
            items(folders, key = { it.id }) { folder ->
                FolderListItem(
                    folder = folder,
                    isSelected = folder.id in selectedIds,
                    isMultiSelect = isMultiSelect,
                    onClick = { if (isMultiSelect) onToggleSelect(folder.id) else onFolderClick(folder) },
                    onLongPress = { onLongPress(folder.id) },
                    onDelete = { onDelete(folder.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        if (files.isNotEmpty()) {
            item {
                SectionHeader("File", files.size)
            }
            items(files, key = { it.id }) { file ->
                FileListItem(
                    file = file,
                    isSelected = file.id in selectedIds,
                    isMultiSelect = isMultiSelect,
                    onClick = { if (isMultiSelect) onToggleSelect(file.id) else onFileClick(file) },
                    onLongPress = { onLongPress(file.id) },
                    onStar = { onStar(file.id, !file.isStarred) },
                    onDelete = { onDelete(file.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun FileGridContent(
    folders: List<TeleFolder>,
    files: List<TeleFile>,
    selectedIds: Set<String>,
    isMultiSelect: Boolean,
    onFolderClick: (TeleFolder) -> Unit,
    onFileClick: (TeleFile) -> Unit,
    onLongPress: (String) -> Unit,
    onToggleSelect: (String) -> Unit,
    onStar: (String, Boolean) -> Unit,
    onDelete: (String) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (folders.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) { SectionHeader("Folder", folders.size) }
            items(folders, key = { it.id }) { folder ->
                FolderGridItem(
                    folder = folder,
                    isSelected = folder.id in selectedIds,
                    isMultiSelect = isMultiSelect,
                    onClick = { if (isMultiSelect) onToggleSelect(folder.id) else onFolderClick(folder) },
                    onLongPress = { onLongPress(folder.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        if (files.isNotEmpty()) {
            item(span = { GridItemSpan(2) }) { SectionHeader("File", files.size) }
            items(files, key = { it.id }) { file ->
                FileGridItem(
                    file = file,
                    isSelected = file.id in selectedIds,
                    isMultiSelect = isMultiSelect,
                    onClick = { if (isMultiSelect) onToggleSelect(file.id) else onFileClick(file) },
                    onLongPress = { onLongPress(file.id) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
        item(span = { GridItemSpan(2) }) { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int) {
    Text(
        text = "$title ($count)",
        style = MaterialTheme.typography.labelMedium,
        color = SkyTextTertiary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun EmptyFolderState(onUpload: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.FolderOpen,
            null,
            tint = SkyTextTertiary,
            modifier = Modifier.size(80.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text("Folder ini kosong", style = MaterialTheme.typography.titleMedium, color = SkyTextSecondary)
        Spacer(Modifier.height(8.dp))
        Text("Unggah file untuk memulai", style = MaterialTheme.typography.bodyMedium, color = SkyTextTertiary)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onUpload,
            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue),
            shape = SkyShapes.Button,
        ) {
            Icon(Icons.Default.Upload, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Unggah File")
        }
    }
}

@Composable
private fun TransferProgressBar(transfers: List<TransferProgress>) {
    val active = transfers.filter { it.state == TransferState.IN_PROGRESS }.firstOrNull()
    active?.let { transfer ->
        Surface(color = SkyBlueContainer) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    if (transfer.isUpload) Icons.Default.Upload else Icons.Default.Download,
                    null, tint = SkyBlueLight, modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transfer.fileName,
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyTextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    LinearProgressIndicator(
                        progress = { transfer.progressPercent },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(SkyShapes.ProgressBar),
                        color = SkyBlue,
                        trackColor = SkyProgressTrack,
                    )
                }
                Text(
                    "${(transfer.progressPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = SkyBlueLight,
                )
            }
        }
    }
}
