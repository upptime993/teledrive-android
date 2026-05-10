package com.teledrive.sky.presentation.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.teledrive.sky.domain.model.*
import com.teledrive.sky.domain.repository.FileRepository
import com.teledrive.sky.presentation.components.FileListShimmer
import com.teledrive.sky.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(private val fileRepository: FileRepository) : ViewModel() {
    val deletedFiles = fileRepository.getDeletedFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(fileId: String) {
        viewModelScope.launch {
            fileRepository.updateFile(FileUpdate(fileId = fileId, isDeleted = false))
        }
    }

    fun deletePermanent(fileId: String) {
        viewModelScope.launch {
            fileRepository.deleteFiles(listOf(fileId), permanent = true)
        }
    }
}

@Composable
fun TrashScreen(navController: NavController, viewModel: TrashViewModel = hiltViewModel()) {
    val files by viewModel.deletedFiles.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(SkyBackground)) {
        Surface(color = SkyBackground) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Delete, null, tint = SkyError, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Sampah", style = MaterialTheme.typography.headlineSmall, color = SkyTextPrimary)
                Spacer(Modifier.weight(1f))
                if (files.isNotEmpty()) {
                    TextButton(onClick = { files.forEach { viewModel.deletePermanent(it.id) } }) {
                        Text("Kosongkan", color = SkyError, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        HorizontalDivider(color = SkyDivider, thickness = 0.5.dp)

        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Delete, null, tint = SkyTextTertiary, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Tempat sampah kosong", style = MaterialTheme.typography.titleMedium, color = SkyTextSecondary)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(files, key = { it.id }) { file ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        color = SkySurfaceElevated, shape = SkyShapes.FileCard,
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(Icons.Default.InsertDriveFile, null, tint = SkyTextTertiary, modifier = Modifier.size(36.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(file.name, style = MaterialTheme.typography.bodyMedium, color = SkyTextPrimary, maxLines = 1)
                            }
                            IconButton(onClick = { viewModel.restore(file.id) }) {
                                Icon(Icons.Default.Restore, "Pulihkan", tint = SkyBlue)
                            }
                            IconButton(onClick = { viewModel.deletePermanent(file.id) }) {
                                Icon(Icons.Default.DeleteForever, "Hapus Permanen", tint = SkyError)
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
