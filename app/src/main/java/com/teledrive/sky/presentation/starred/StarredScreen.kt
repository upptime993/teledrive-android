package com.teledrive.sky.presentation.starred

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
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
import com.teledrive.sky.presentation.components.FileListItem
import com.teledrive.sky.presentation.components.FileListShimmer
import com.teledrive.sky.ui.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StarredViewModel @Inject constructor(private val fileRepository: FileRepository) : ViewModel() {
    val starredFiles = fileRepository.getStarredFiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unstar(fileId: String) {
        viewModelScope.launch {
            fileRepository.updateFile(FileUpdate(fileId = fileId, isStarred = false))
        }
    }
}

@Composable
fun StarredScreen(navController: NavController, viewModel: StarredViewModel = hiltViewModel()) {
    val files by viewModel.starredFiles.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize().background(SkyBackground)) {
        Surface(color = SkyBackground) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Star, null, tint = SkyWarning, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Favorit", style = MaterialTheme.typography.headlineSmall, color = SkyTextPrimary)
            }
        }
        HorizontalDivider(color = SkyDivider, thickness = 0.5.dp)

        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Star, null, tint = SkyTextTertiary, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(12.dp))
                    Text("Belum ada favorit", style = MaterialTheme.typography.titleMedium, color = SkyTextSecondary)
                    Text("Tandai file untuk akses cepat", style = MaterialTheme.typography.bodyMedium, color = SkyTextTertiary)
                }
            }
        } else {
            LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
                items(files, key = { it.id }) { file ->
                    FileListItem(
                        file = file, isSelected = false, isMultiSelect = false,
                        onClick = {}, onLongPress = {},
                        onStar = { viewModel.unstar(file.id) }, onDelete = {},
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}
