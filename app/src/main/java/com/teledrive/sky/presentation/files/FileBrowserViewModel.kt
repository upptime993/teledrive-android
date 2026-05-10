package com.teledrive.sky.presentation.files

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teledrive.sky.domain.model.*
import com.teledrive.sky.domain.repository.FileRepository
import com.teledrive.sky.domain.repository.FolderRepository
import com.teledrive.sky.domain.repository.TransferRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FileBrowserUiState(
    val files: List<TeleFile> = emptyList(),
    val folders: List<TeleFolder> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val breadcrumbs: List<BreadcrumbItem> = emptyList(),
    val sortConfig: SortConfig = SortConfig(),
    val selectedIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val isGridView: Boolean = false,
    val showCreateFolderDialog: Boolean = false,
    val newFolderName: String = "",
    val showSortSheet: Boolean = false,
    val activeTransfers: List<TransferProgress> = emptyList(),
)

@HiltViewModel
class FileBrowserViewModel @Inject constructor(
    private val fileRepository: FileRepository,
    private val folderRepository: FolderRepository,
    private val transferRepository: TransferRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(FileBrowserUiState())
    val uiState: StateFlow<FileBrowserUiState> = _uiState.asStateFlow()

    private var currentFolderId: String? = null

    fun init(folderId: String?) {
        currentFolderId = folderId
        loadContent()
        loadBreadcrumbs(folderId)
        observeTransfers()
    }

    private fun observeTransfers() {
        transferRepository.getActiveTransfers()
            .onEach { transfers ->
                _uiState.update { it.copy(activeTransfers = transfers) }
            }
            .launchIn(viewModelScope)
    }

    private fun loadBreadcrumbs(folderId: String?) {
        viewModelScope.launch {
            val crumbs = folderRepository.getBreadcrumbPath(folderId)
            _uiState.update { it.copy(breadcrumbs = crumbs) }
        }
    }

    fun loadContent(refresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Load folders
            folderRepository.getFolders(currentFolderId)
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            _uiState.update { it.copy(
                                folders = result.data,
                                isLoading = false,
                            )}
                        }
                        is AppResult.Error -> {
                            _uiState.update { it.copy(error = result.message, isLoading = false) }
                        }
                        else -> {}
                    }
                }
        }

        viewModelScope.launch {
            fileRepository.getFiles(folderId = currentFolderId)
                .collect { result ->
                    when (result) {
                        is AppResult.Success -> {
                            val sorted = sortItems(result.data.items, _uiState.value.sortConfig)
                            _uiState.update { it.copy(
                                files = sorted,
                                isLoading = false,
                            )}
                        }
                        is AppResult.Error -> {
                            _uiState.update { it.copy(error = result.message, isLoading = false) }
                        }
                        else -> {}
                    }
                }
        }
    }

    fun refresh() = loadContent(refresh = true)

    fun toggleViewMode() {
        _uiState.update { it.copy(isGridView = !it.isGridView) }
    }

    fun toggleSortSheet() {
        _uiState.update { it.copy(showSortSheet = !it.showSortSheet) }
    }

    fun setSortConfig(config: SortConfig) {
        val sorted = sortItems(_uiState.value.files, config)
        _uiState.update { it.copy(sortConfig = config, files = sorted, showSortSheet = false) }
    }

    private fun sortItems(files: List<TeleFile>, config: SortConfig): List<TeleFile> {
        return when (config.field) {
            SortField.NAME -> if (config.direction == SortDirection.ASC)
                files.sortedBy { it.name.lowercase() }
            else files.sortedByDescending { it.name.lowercase() }
            SortField.DATE -> if (config.direction == SortDirection.ASC)
                files.sortedBy { it.createdAt }
            else files.sortedByDescending { it.createdAt }
            SortField.SIZE -> if (config.direction == SortDirection.ASC)
                files.sortedBy { it.size }
            else files.sortedByDescending { it.size }
        }
    }

    // ── Multi-select ─────────────────────────────────────────────────────────
    fun toggleSelectFile(fileId: String) {
        val current = _uiState.value.selectedIds
        val newSelected = if (fileId in current) current - fileId else current + fileId
        _uiState.update { it.copy(
            selectedIds = newSelected,
            isMultiSelectMode = newSelected.isNotEmpty(),
        )}
    }

    fun selectAll() {
        val allIds = (_uiState.value.files.map { it.id } +
                _uiState.value.folders.map { it.id }).toSet()
        _uiState.update { it.copy(selectedIds = allIds, isMultiSelectMode = true) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet(), isMultiSelectMode = false) }
    }

    // ── CRUD Operations ──────────────────────────────────────────────────────
    fun createFolder() {
        val name = _uiState.value.newFolderName.trim()
        if (name.isBlank()) return

        viewModelScope.launch {
            when (val result = folderRepository.createFolder(name, currentFolderId)) {
                is AppResult.Success -> {
                    _uiState.update { it.copy(
                        folders = it.folders + result.data,
                        showCreateFolderDialog = false,
                        newFolderName = "",
                    )}
                }
                is AppResult.Error -> {
                    _uiState.update { it.copy(error = result.message) }
                }
                else -> {}
            }
        }
    }

    fun onNewFolderNameChange(name: String) {
        _uiState.update { it.copy(newFolderName = name) }
    }

    fun showCreateFolderDialog() {
        _uiState.update { it.copy(showCreateFolderDialog = true, newFolderName = "") }
    }

    fun dismissCreateFolderDialog() {
        _uiState.update { it.copy(showCreateFolderDialog = false, newFolderName = "") }
    }

    fun starFile(fileId: String, isStarred: Boolean) {
        viewModelScope.launch {
            fileRepository.updateFile(FileUpdate(fileId = fileId, isStarred = isStarred))
            // Optimistic update
            _uiState.update { state ->
                state.copy(files = state.files.map { file ->
                    if (file.id == fileId) file.copy(isStarred = isStarred) else file
                })
            }
        }
    }

    fun moveToTrash(fileId: String) {
        viewModelScope.launch {
            fileRepository.deleteFiles(listOf(fileId), permanent = false)
            _uiState.update { state ->
                state.copy(files = state.files.filter { it.id != fileId })
            }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folderId, permanent = false)
            _uiState.update { state ->
                state.copy(folders = state.folders.filter { it.id != folderId })
            }
        }
    }

    fun deleteSelectedItems() {
        val selected = _uiState.value.selectedIds.toList()
        if (selected.isEmpty()) return

        viewModelScope.launch {
            val fileIds = selected.filter { id -> _uiState.value.files.any { it.id == id } }
            val folderIds = selected.filter { id -> _uiState.value.folders.any { it.id == id } }

            if (fileIds.isNotEmpty()) {
                fileRepository.deleteFiles(fileIds, permanent = false)
            }
            folderIds.forEach { folderRepository.deleteFolder(it, permanent = false) }

            _uiState.update { state ->
                state.copy(
                    files = state.files.filter { it.id !in selected },
                    folders = state.folders.filter { it.id !in selected },
                    selectedIds = emptySet(),
                    isMultiSelectMode = false,
                )
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
