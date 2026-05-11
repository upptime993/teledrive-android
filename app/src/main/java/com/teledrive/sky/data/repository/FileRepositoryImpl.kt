package com.teledrive.sky.data.repository

import com.teledrive.sky.data.local.dao.FileDao
import com.teledrive.sky.data.local.dao.FolderDao
import com.teledrive.sky.data.local.entity.FileEntity
import com.teledrive.sky.data.local.entity.FolderEntity
import com.teledrive.sky.data.local.preferences.UserPreferencesDataStore
import com.teledrive.sky.data.remote.api.TeleDriveApi
import com.teledrive.sky.data.remote.dto.*
import com.teledrive.sky.data.remote.mapper.FileMapper.toDomain
import com.teledrive.sky.domain.model.*
import com.teledrive.sky.domain.repository.FileRepository
import com.teledrive.sky.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    private val api: TeleDriveApi,
    private val fileDao: FileDao,
    private val prefs: UserPreferencesDataStore,
) : FileRepository {

    override fun getFiles(
        folderId: String?,
        page: Int,
        limit: Int,
        filter: FileFilter?,
    ): Flow<AppResult<PaginatedResult<TeleFile>>> = flow {
        emit(AppResult.Loading)

        // Emit cached data first for instant UI
        val cached = fileDao.getFilesInFolder(folderId)

        try {
            val filterParam = when (filter) {
                is FileFilter.Starred -> "starred"
                is FileFilter.Trash -> "trash"
                is FileFilter.ByCategory -> filter.category.name.lowercase()
                else -> null
            }

            val response = api.getFiles(
                folderId = folderId,
                filter = filterParam,
                page = page,
                limit = limit,
            )

            if (response.isSuccessful) {
                val body = response.body()
                val files = body?.files?.map { it.toDomain() } ?: emptyList()

                // Update cache
                val entities = files.map { file ->
                    FileEntity(
                        id = file.id,
                        name = file.name,
                        size = file.size,
                        mimeType = file.mimeType,
                        folderId = file.folderId,
                        isStarred = file.isStarred,
                        isDeleted = file.isDeleted,
                        isChunked = file.isChunked,
                        createdAt = file.createdAt,
                        lastSyncedAt = System.currentTimeMillis(),
                    )
                }
                fileDao.upsertFiles(entities)
                prefs.saveLastSyncTime(System.currentTimeMillis())

                emit(AppResult.Success(
                    PaginatedResult(
                        items = files,
                        total = body?.total ?: files.size,
                        page = page,
                        limit = limit,
                    )
                ))
            } else {
                val error = parseError(response.code(), response.message())
                emit(AppResult.Error(error, response.code()))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Network error"))
        }
    }

    override suspend fun updateFile(update: FileUpdate): AppResult<TeleFile> {
        return try {
            val request = UpdateFileRequest(
                fileId = update.fileId,
                isStarred = update.isStarred,
                isDeleted = update.isDeleted,
                folderId = update.folderId,
                name = update.newName,
            )
            val response = api.updateFile(request)
            if (response.isSuccessful) {
                val file = response.body()?.file?.toDomain()
                if (file != null) {
                    fileDao.upsertFile(file.toEntity())
                    AppResult.Success(file)
                } else {
                    AppResult.Error("Invalid response")
                }
            } else {
                AppResult.Error(parseError(response.code(), response.message()), response.code())
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Update failed")
        }
    }

    override suspend fun deleteFiles(fileIds: List<String>, permanent: Boolean): AppResult<BatchResult> {
        return try {
            val response = api.deleteFiles(DeleteFilesRequest(fileIds, permanent))
            if (response.isSuccessful) {
                if (permanent) {
                    fileDao.deleteFiles(fileIds)
                } else {
                    fileIds.forEach { id ->
                        fileDao.getFileById(id)?.let { entity ->
                            fileDao.upsertFile(entity.copy(isDeleted = true))
                        }
                    }
                }
                AppResult.Success(BatchResult(fileIds.size, 0, emptyList()))
            } else {
                AppResult.Error(parseError(response.code(), response.message()), response.code())
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Delete failed")
        }
    }

    override suspend fun searchFiles(query: String): AppResult<List<TeleFile>> {
        return try {
            val response = api.getFiles(limit = 50)
            if (response.isSuccessful) {
                val allFiles = response.body()?.files?.map { it.toDomain() } ?: emptyList()
                val filtered = allFiles.filter {
                    it.name.contains(query, ignoreCase = true) && !it.isDeleted
                }.take(50)
                AppResult.Success(filtered)
            } else {
                // Fall back to local search
                val localResults = fileDao.searchFiles(query).map { it.toDomain() }
                AppResult.Success(localResults)
            }
        } catch (e: Exception) {
            val localResults = fileDao.searchFiles(query).map { it.toDomain() }
            AppResult.Success(localResults)
        }
    }

    override fun getStorageInfo(): Flow<AppResult<StorageInfo>> = flow {
        emit(AppResult.Loading)
        try {
            val response = api.getFiles(aggregate = "storage")
            if (response.isSuccessful) {
                val body = response.body()
                val allFiles = body?.files ?: emptyList()

                var photoSize = 0L; var photoCount = 0
                var videoSize = 0L; var videoCount = 0
                var docSize = 0L; var docCount = 0
                var otherSize = 0L; var otherCount = 0

                allFiles.forEach { file ->
                    val mime = file.mimeType ?: "application/octet-stream"
                    when {
                        mime.startsWith("image/") -> { photoSize += file.size; photoCount++ }
                        mime.startsWith("video/") -> { videoSize += file.size; videoCount++ }
                        mime.startsWith("text/") || mime == "application/pdf" ||
                        mime.startsWith("application/msword") ||
                        mime.startsWith("application/vnd.") -> { docSize += file.size; docCount++ }
                        else -> { otherSize += file.size; otherCount++ }
                    }
                }

                val totalSize = body?.totalSize ?: (photoSize + videoSize + docSize + otherSize)
                val totalFiles = body?.totalFiles ?: allFiles.size

                emit(AppResult.Success(StorageInfo(
                    totalSize = totalSize,
                    totalFiles = totalFiles,
                    photoSize = photoSize, photoCount = photoCount,
                    videoSize = videoSize, videoCount = videoCount,
                    documentSize = docSize, documentCount = docCount,
                    otherSize = otherSize, otherCount = otherCount,
                )))
            } else {
                emit(AppResult.Error(parseError(response.code(), response.message())))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Failed to load storage info"))
        }
    }

    override fun getCachedFiles(folderId: String?): Flow<List<TeleFile>> =
        fileDao.getFilesInFolder(folderId).map { entities -> entities.map { it.toDomain() } }

    override fun getStarredFiles(): Flow<List<TeleFile>> =
        fileDao.getStarredFiles().map { entities -> entities.map { it.toDomain() } }

    override fun getDeletedFiles(): Flow<List<TeleFile>> =
        fileDao.getDeletedFiles().map { entities -> entities.map { it.toDomain() } }

    override suspend fun syncFiles(folderId: String?) {
        try {
            val response = api.getFiles(folderId = folderId, limit = 200)
            if (response.isSuccessful) {
                val files = response.body()?.files?.map { it.toDomain() } ?: return
                fileDao.upsertFiles(files.map { it.toEntity() })
            }
        } catch (e: Exception) { /* silent fail for background sync */ }
    }

    private fun TeleFile.toEntity() = FileEntity(
        id = id,
        name = name,
        size = size,
        mimeType = mimeType,
        folderId = folderId,
        isStarred = isStarred,
        isDeleted = isDeleted,
        isChunked = isChunked,
        createdAt = createdAt,
        lastSyncedAt = System.currentTimeMillis(),
    )

    private fun FileEntity.toDomain() = TeleFile(
        id = id,
        name = name,
        size = size,
        mimeType = mimeType,
        folderId = folderId,
        isStarred = isStarred,
        isDeleted = isDeleted,
        isChunked = isChunked,
        createdAt = createdAt,
    )

    private fun parseError(code: Int, message: String): String = when (code) {
        401 -> "Sesi habis. Silakan masuk kembali."
        403 -> "Akses ditolak."
        404 -> "File tidak ditemukan."
        500, 502, 503 -> "Server error. Coba beberapa saat lagi."
        else -> message.ifBlank { "Terjadi kesalahan ($code)" }
    }
}

@Singleton
class FolderRepositoryImpl @Inject constructor(
    private val api: TeleDriveApi,
    private val folderDao: FolderDao,
    private val fileDao: FileDao,
) : FolderRepository {

    override fun getFolders(parentId: String?): Flow<AppResult<List<TeleFolder>>> = flow {
        emit(AppResult.Loading)

        try {
            val response = api.getFolders(parentId = parentId)
            if (response.isSuccessful) {
                val folders = response.body()?.folders?.map { it.toDomain() } ?: emptyList()
                folderDao.upsertFolders(folders.map { it.toEntity() })
                emit(AppResult.Success(folders))
            } else {
                val cached = folderDao.getFoldersInParent(parentId)
                emit(AppResult.Error("Failed to load folders"))
            }
        } catch (e: Exception) {
            emit(AppResult.Error(e.message ?: "Network error"))
        }
    }

    override suspend fun createFolder(name: String, parentId: String?): AppResult<TeleFolder> {
        return try {
            val response = api.createFolder(CreateFolderRequest(name, parentId))
            if (response.isSuccessful) {
                val folder = response.body()?.folder?.toDomain()
                if (folder != null) {
                    folderDao.upsertFolder(folder.toEntity())
                    AppResult.Success(folder)
                } else AppResult.Error("Invalid response")
            } else {
                AppResult.Error("Failed to create folder (${response.code()})")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Create folder failed")
        }
    }

    override suspend fun renameFolder(folderId: String, newName: String): AppResult<TeleFolder> {
        return try {
            val response = api.updateFolder(UpdateFolderRequest(folderId = folderId, name = newName))
            if (response.isSuccessful) {
                val folder = response.body()?.folder?.toDomain()
                if (folder != null) {
                    folderDao.upsertFolder(folder.toEntity())
                    AppResult.Success(folder)
                } else AppResult.Error("Invalid response")
            } else AppResult.Error("Failed to rename folder")
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Rename failed")
        }
    }

    override suspend fun deleteFolder(folderId: String, permanent: Boolean): AppResult<Unit> {
        return try {
            val response = api.deleteFolder(folderId, permanent)
            if (response.isSuccessful) {
                if (permanent) folderDao.deleteFolder(folderId)
                else {
                    folderDao.getFolderById(folderId)?.let {
                        folderDao.upsertFolder(it.copy(isDeleted = true))
                    }
                }
                AppResult.Success(Unit)
            } else AppResult.Error("Failed to delete folder")
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Delete failed")
        }
    }

    override fun getCachedFolders(parentId: String?): Flow<List<TeleFolder>> =
        folderDao.getFoldersInParent(parentId).map { entities -> entities.map { it.toDomain() } }

    override suspend fun getBreadcrumbPath(folderId: String?): List<BreadcrumbItem> {
        if (folderId == null) return listOf(BreadcrumbItem(null, "Beranda"))
        val path = mutableListOf<BreadcrumbItem>()
        var currentId: String? = folderId

        while (currentId != null) {
            val folder = folderDao.getFolderById(currentId)
            if (folder != null) {
                path.add(0, BreadcrumbItem(folder.id, folder.name))
                currentId = folder.parentId
            } else break
        }

        return listOf(BreadcrumbItem(null, "Beranda")) + path
    }

    private fun FolderDto.toDomain() = TeleFolder(
        id = id,
        name = name,
        parentId = parentId,
        isDeleted = isDeleted ?: false,
        createdAt = System.currentTimeMillis(),
    )

    private fun TeleFolder.toEntity() = FolderEntity(
        id = id,
        name = name,
        parentId = parentId,
        isDeleted = isDeleted,
        createdAt = createdAt,
        lastSyncedAt = System.currentTimeMillis(),
    )

    private fun FolderEntity.toDomain() = TeleFolder(
        id = id,
        name = name,
        parentId = parentId,
        isDeleted = isDeleted,
        createdAt = createdAt,
    )
}
