package com.teledrive.sky.presentation.search

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val fileRepository: FileRepository,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _results = MutableStateFlow<List<TeleFile>>(emptyList())
    val results = _results.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChange(q: String) {
        _query.value = q
        searchJob?.cancel()
        if (q.length < 2) { _results.value = emptyList(); return }
        searchJob = viewModelScope.launch {
            delay(300)
            _isLoading.value = true
            when (val r = fileRepository.searchFiles(q)) {
                is AppResult.Success -> _results.value = r.data
                else -> _results.value = emptyList()
            }
            _isLoading.value = false
        }
    }
}

@Composable
fun SearchScreen(navController: NavController, viewModel: SearchViewModel = hiltViewModel()) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier.fillMaxSize().background(SkyBackground)
            .windowInsetsPadding(WindowInsets.systemBars)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                placeholder = { Text("Cari file...", color = SkyTextTertiary) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = SkyTextSecondary) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Default.Close, null, tint = SkyTextSecondary)
                    }
                },
                shape = SkyShapes.TextField,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = SkyBorderFocused, unfocusedBorderColor = SkyBorder,
                    focusedTextColor = SkyTextPrimary, unfocusedTextColor = SkyTextPrimary,
                    focusedContainerColor = SkySurface, unfocusedContainerColor = SkySurface,
                    cursorColor = SkyBlue,
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            )
        }

        HorizontalDivider(color = SkyDivider, thickness = 0.5.dp)

        when {
            query.length < 2 -> SearchPlaceholder()
            isLoading -> FileListShimmer(isGrid = false)
            results.isEmpty() -> SearchEmpty(query)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                item {
                    Text("${results.size} hasil untuk \"$query\"",
                        style = MaterialTheme.typography.labelMedium, color = SkyTextTertiary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
                items(results, key = { it.id }) { file ->
                    FileListItem(
                        file = file, isSelected = false, isMultiSelect = false,
                        onClick = {}, onLongPress = {}, onStar = {}, onDelete = {},
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable private fun SearchPlaceholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Search, null, tint = SkyTextTertiary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text("Ketik untuk mencari file", style = MaterialTheme.typography.bodyMedium, color = SkyTextSecondary)
            Text("Minimal 2 karakter", style = MaterialTheme.typography.labelMedium, color = SkyTextTertiary)
        }
    }
}

@Composable private fun SearchEmpty(query: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.SearchOff, null, tint = SkyTextTertiary, modifier = Modifier.size(64.dp))
            Spacer(Modifier.height(12.dp))
            Text("Tidak ada hasil", style = MaterialTheme.typography.titleMedium, color = SkyTextSecondary)
            Text("Coba kata kunci lain", style = MaterialTheme.typography.bodyMedium, color = SkyTextTertiary)
        }
    }
}
