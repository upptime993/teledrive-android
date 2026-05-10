package com.teledrive.sky.domain.repository

import android.net.Uri
import com.teledrive.sky.domain.model.*
import kotlinx.coroutines.flow.Flow

// ─── Repository Interfaces (Domain Layer) ────────────────────────────────────

interface AuthRepository {
    suspend fun login(email: String, password: String): AppResult<UserProfile>
    suspend fun register(name: String, email: String, password: String): AppResult<UserProfile>
    suspend fun logout()
    fun getAuthToken(): Flow<AuthToken?>
    fun isLoggedIn(): Flow<Boolean>
    suspend fun refreshToken(): AppResult<AuthToken>
    fun getUserProfile(): Flow<UserProfile?>
}

interface FileRepository {
    fun getFiles(
        folderId: String?,
        page: Int = 1,
        limit: Int = 100,
        filter: FileFilter? = null,
    ): Flow<AppResult<PaginatedResult<TeleFile>>>

    suspend fun updateFile(update: FileUpdate): AppResult<TeleFile>

    suspend fun deleteFiles(
        fileIds: List<String>,
        permanent: Boolean = false
    ): AppResult<BatchResult>

    suspend fun searchFiles(query: String): AppResult<List<TeleFile>>

    fun getStorageInfo(): Flow<AppResult<StorageInfo>>

    fun getCachedFiles(folderId: String?): Flow<List<TeleFile>>

    fun getStarredFiles(): Flow<List<TeleFile>>

    fun getDeletedFiles(): Flow<List<TeleFile>>

    suspend fun syncFiles(folderId: String?)
}

interface FolderRepository {
    fun getFolders(parentId: String?): Flow<AppResult<List<TeleFolder>>>

    suspend fun createFolder(name: String, parentId: String?): AppResult<TeleFolder>

    suspend fun renameFolder(folderId: String, newName: String): AppResult<TeleFolder>

    suspend fun deleteFolder(folderId: String, permanent: Boolean = false): AppResult<Unit>

    fun getCachedFolders(parentId: String?): Flow<List<TeleFolder>>

    suspend fun getBreadcrumbPath(folderId: String?): List<BreadcrumbItem>
}

interface TransferRepository {
    suspend fun uploadFile(
        uri: Uri,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        folderId: String?,
        onProgress: (TransferProgress) -> Unit
    ): AppResult<TeleFile>

    suspend fun uploadChunked(
        uri: Uri,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        folderId: String?,
        onProgress: (TransferProgress) -> Unit
    ): AppResult<TeleFile>

    suspend fun downloadFile(
        fileId: String,
        fileName: String,
        onProgress: (TransferProgress) -> Unit
    ): AppResult<Uri>

    fun getActiveTransfers(): Flow<List<TransferProgress>>

    suspend fun pauseTransfer(transferId: String)

    suspend fun resumeTransfer(transferId: String)

    suspend fun cancelTransfer(transferId: String)

    fun getTransfer(transferId: String): Flow<TransferProgress?>

    suspend fun cleanupCompletedTransfers()
}

interface ShareRepository {
    suspend fun createShareLink(
        fileId: String,
        expiresInDays: Int? = null
    ): AppResult<ShareLink>

    suspend fun getShareLinksForFile(fileId: String): AppResult<List<ShareLink>>

    suspend fun getAllShareLinks(): AppResult<List<ShareLink>>

    suspend fun revokeShareLink(token: String): AppResult<Unit>
}
