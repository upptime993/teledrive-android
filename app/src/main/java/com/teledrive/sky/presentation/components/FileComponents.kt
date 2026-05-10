package com.teledrive.sky.presentation.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.teledrive.sky.domain.model.*
import com.teledrive.sky.ui.theme.*
import com.teledrive.sky.util.FileSizeFormatter
import com.teledrive.sky.util.MimeTypeUtils
import com.teledrive.sky.util.RelativeDateFormatter

// ─── File/Folder List Items ───────────────────────────────────────────────────

@Composable
fun FileListItem(
    file: TeleFile,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onStar: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.96f else 1f,
        animationSpec = SkyMotion.springSnappy(),
        label = "itemScale"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SkyBlueContainer else Color.Transparent,
        animationSpec = tween(200),
        label = "itemBg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() },
                )
            },
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Checkbox or Icon
            if (isMultiSelect) {
                AnimatedContent(targetState = isSelected, label = "check") { selected ->
                    if (selected) {
                        Icon(Icons.Default.CheckCircle, null, tint = SkyBlue, modifier = Modifier.size(36.dp))
                    } else {
                        Icon(Icons.Outlined.RadioButtonUnchecked, null, tint = SkyTextTertiary, modifier = Modifier.size(36.dp))
                    }
                }
            } else {
                FileIcon(file = file, size = 40.dp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = FileSizeFormatter.format(file.size),
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyTextTertiary,
                    )
                    Text("•", style = MaterialTheme.typography.labelSmall, color = SkyTextTertiary)
                    Text(
                        text = RelativeDateFormatter.format(file.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyTextTertiary,
                    )
                }
            }

            if (!isMultiSelect) {
                Row {
                    if (file.isStarred) {
                        Icon(Icons.Default.Star, null, tint = SkyWarning, modifier = Modifier.size(18.dp))
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, null, tint = SkyTextTertiary, modifier = Modifier.size(18.dp))
                        }
                        FileContextMenu(
                            expanded = showMenu,
                            onDismiss = { showMenu = false },
                            isStarred = file.isStarred,
                            onStar = { showMenu = false; onStar() },
                            onDelete = { showMenu = false; onDelete() },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FolderListItem(
    folder: TeleFolder,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) SkyBlueContainer else Color.Transparent,
        animationSpec = tween(200), label = "folderBg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            },
        color = bgColor,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (isMultiSelect) {
                AnimatedContent(targetState = isSelected, label = "folderCheck") { selected ->
                    Icon(
                        if (selected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        null,
                        tint = if (selected) SkyBlue else SkyTextTertiary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SkyBlueContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Folder, null, tint = SkyBlue, modifier = Modifier.size(22.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = SkyTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text("Folder", style = MaterialTheme.typography.labelSmall, color = SkyTextTertiary)
            }

            if (!isMultiSelect) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChevronRight, null, tint = SkyTextTertiary, modifier = Modifier.size(18.dp))
                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.MoreVert, null, tint = SkyTextTertiary, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu, onDismissRequest = { showMenu = false },
                            containerColor = SkySurfaceHighest,
                        ) {
                            DropdownMenuItem(
                                text = { Text("Hapus", color = SkyError) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = SkyError) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FileGridItem(
    file: TeleFile,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 0.94f else 1f,
        animationSpec = SkyMotion.springSnappy(), label = "gridScale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .aspectRatio(0.85f)
            .clip(SkyShapes.FileCard)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            },
        color = SkySurfaceElevated,
        border = if (isSelected) BorderStroke(2.dp, SkyBlue) else null,
        shape = SkyShapes.FileCard,
    ) {
        Box {
            Column {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f).background(SkySurface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (MimeTypeUtils.isImage(file.mimeType)) {
                        AsyncImage(model = "thumbnail_url", contentDescription = file.name,
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        FileIcon(file = file, size = 40.dp)
                    }
                }
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(file.name, style = MaterialTheme.typography.labelMedium, color = SkyTextPrimary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(FileSizeFormatter.format(file.size), style = MaterialTheme.typography.labelSmall, color = SkyTextTertiary)
                }
            }

            if (isMultiSelect) {
                Box(modifier = Modifier.padding(6.dp).align(Alignment.TopEnd)) {
                    Icon(
                        if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                        null, tint = if (isSelected) SkyBlue else SkyTextTertiary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun FolderGridItem(
    folder: TeleFolder,
    isSelected: Boolean,
    isMultiSelect: Boolean,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(SkyShapes.FileCard)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() }, onLongPress = { onLongPress() })
            },
        color = SkySurfaceElevated,
        border = if (isSelected) BorderStroke(2.dp, SkyBlue) else null,
        shape = SkyShapes.FileCard,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Default.Folder, null, tint = SkyBlue, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(8.dp))
            Text(folder.name, style = MaterialTheme.typography.labelMedium, color = SkyTextPrimary,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun FileIcon(file: TeleFile, size: androidx.compose.ui.unit.Dp) {
    val (icon, color) = when (MimeTypeUtils.categorize(file.mimeType)) {
        FileCategory.PHOTO -> Icons.Default.Image to ColorPhoto
        FileCategory.VIDEO -> Icons.Default.PlayCircle to ColorVideo
        FileCategory.DOCUMENT -> Icons.Default.Description to ColorDocument
        FileCategory.AUDIO -> Icons.Default.AudioFile to ColorAudio
        FileCategory.ARCHIVE -> Icons.Default.Archive to ColorArchive
        FileCategory.OTHER -> Icons.Default.InsertDriveFile to ColorOther
    }

    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(10.dp)).background(color.copy(0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(size * 0.55f))
    }
}

@Composable
fun FileContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isStarred: Boolean,
    onStar: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = SkySurfaceHighest) {
        DropdownMenuItem(
            text = { Text(if (isStarred) "Hapus dari Favorit" else "Tandai Favorit", color = SkyTextPrimary) },
            onClick = onStar,
            leadingIcon = { Icon(if (isStarred) Icons.Default.StarBorder else Icons.Default.Star, null, tint = SkyWarning) }
        )
        HorizontalDivider(color = SkyDivider)
        DropdownMenuItem(
            text = { Text("Pindah ke Sampah", color = SkyError) },
            onClick = onDelete,
            leadingIcon = { Icon(Icons.Default.Delete, null, tint = SkyError) }
        )
    }
}

@Composable
fun FileFAB(
    onUpload: () -> Unit,
    onNewFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = SkyMotion.springBouncy(), label = "fabRotate"
    )

    Column(modifier = modifier, horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it },
            exit = fadeOut(tween(150)) + slideOutVertically(tween(150)) { it },
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SmallFABItem("Unggah File", Icons.Default.Upload, SkyBlue) {
                    expanded = false; onUpload()
                }
                SmallFABItem("Folder Baru", Icons.Default.CreateNewFolder, SkyAccentDim) {
                    expanded = false; onNewFolder()
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = SkyBlue,
            contentColor = Color.White,
            shape = SkyShapes.FAB,
        ) {
            Icon(
                Icons.Default.Add, "Tambah",
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
            )
        }
    }
}

@Composable
private fun SmallFABItem(label: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            shape = SkyShapes.Chip,
            color = SkySurfaceHighest,
            shadowElevation = 2.dp,
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = SkyTextPrimary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
        }
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = color,
            contentColor = Color.White,
            shape = SkyShapes.FAB,
        ) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun CreateFolderDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onCreate: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Folder Baru", color = SkyTextPrimary) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                placeholder = { Text("Nama folder", color = SkyTextTertiary) },
                singleLine = true,
                shape = SkyShapes.TextField,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SkyBorderFocused, unfocusedBorderColor = SkyBorder,
                    focusedTextColor = SkyTextPrimary, unfocusedTextColor = SkyTextPrimary,
                    focusedContainerColor = SkySurface, unfocusedContainerColor = SkySurface,
                    cursorColor = SkyBlue, focusedLabelColor = SkyBlueLight,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = onCreate, enabled = name.isNotBlank()) {
                Text("Buat", color = SkyBlue)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal", color = SkyTextSecondary) } },
        containerColor = SkySurfaceElevated,
        shape = SkyShapes.Dialog,
    )
}

@Composable
fun SortBottomSheet(
    current: SortConfig,
    onSelect: (SortConfig) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        Triple("Nama A–Z", SortField.NAME, SortDirection.ASC),
        Triple("Nama Z–A", SortField.NAME, SortDirection.DESC),
        Triple("Terbaru", SortField.DATE, SortDirection.DESC),
        Triple("Terlama", SortField.DATE, SortDirection.ASC),
        Triple("Terbesar", SortField.SIZE, SortDirection.DESC),
        Triple("Terkecil", SortField.SIZE, SortDirection.ASC),
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Urutkan", color = SkyTextPrimary) },
        text = {
            Column {
                options.forEach { (label, field, dir) ->
                    val isSelected = current.field == field && current.direction == dir
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(SkyShapes.NavItem)
                            .background(if (isSelected) SkyBlueContainer else Color.Transparent)
                            .clickable { onSelect(SortConfig(field, dir)) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium,
                            color = if (isSelected) SkyBlueLight else SkyTextPrimary)
                        if (isSelected) Icon(Icons.Default.Check, null, tint = SkyBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
        },
        confirmButton = {},
        containerColor = SkySurfaceElevated,
        shape = SkyShapes.Dialog,
    )
}

@Composable
fun FileListShimmer(isGrid: Boolean) {
    // Animated shimmer placeholder
    val shimmer by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "shimmerAnim"
    )
    val brush = Brush.horizontalGradient(
        colors = listOf(SkySurfaceElevated, SkySurface.copy(shimmer * 0.5f + 0.5f), SkySurfaceElevated),
    )
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(6) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(brush))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Box(Modifier.fillMaxWidth(0.35f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
            }
        }
    }
}
