package com.teledrive.sky.domain.model

// ─── Core Domain Models ──────────────────────────────────────────────────────

data class TeleFile(
    val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val folderId: String?,
    val isStarred: Boolean = false,
    val isDeleted: Boolean = false,
    val isChunked: Boolean = false,
    val createdAt: Long, // epoch millis
    val type: String? = null, // optional type from API
)

data class TeleFolder(
    val id: String,
    val name: String,
    val parentId: String?,
    val isDeleted: Boolean = false,
    val createdAt: Long, // epoch millis
)

data class AuthToken(
    val accessToken: String,
    val expiresAt: Long, // epoch millis
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
}

data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
)

data class StorageInfo(
    val totalSize: Long,
    val totalFiles: Int,
    val photoSize: Long,
    val photoCount: Int,
    val videoSize: Long,
    val videoCount: Int,
    val documentSize: Long,
    val documentCount: Int,
    val otherSize: Long,
    val otherCount: Int,
)

data class ShareLink(
    val id: String,
    val token: String,
    val url: String,
    val fileId: String,
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val downloadCount: Int,
    val expiresAt: Long?, // epoch millis, null = no expiry
    val createdAt: Long,
)

data class ChunkInfo(
    val part: Int,
    val msgId: Long,
    val size: Long,
)

data class TransferProgress(
    val transferId: String,
    val fileName: String,
    val totalBytes: Long,
    val transferredBytes: Long,
    val speedBytesPerSec: Long,
    val state: TransferState,
    val isUpload: Boolean,
    val errorMessage: String? = null,
) {
    val progressPercent: Float get() =
        if (totalBytes > 0) (transferredBytes.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
        else 0f
}

enum class TransferState {
    QUEUED, IN_PROGRESS, PAUSED, COMPLETED, FAILED, CANCELLED, RETRYING
}

enum class FileCategory {
    PHOTO, VIDEO, DOCUMENT, AUDIO, ARCHIVE, OTHER
}

sealed class FileFilter {
    object All : FileFilter()
    object Starred : FileFilter()
    object Trash : FileFilter()
    object Recent : FileFilter()
    data class ByCategory(val category: FileCategory) : FileFilter()
}

data class SortConfig(
    val field: SortField = SortField.NAME,
    val direction: SortDirection = SortDirection.ASC,
)

enum class SortField { NAME, DATE, SIZE }
enum class SortDirection { ASC, DESC }

data class PaginatedResult<T>(
    val items: List<T>,
    val total: Int,
    val page: Int,
    val limit: Int,
) {
    val hasMore: Boolean get() = (page * limit) < total
}

data class BatchResult(
    val successCount: Int,
    val failedCount: Int,
    val failedIds: List<String>,
)

data class FileUpdate(
    val fileId: String,
    val isStarred: Boolean? = null,
    val isDeleted: Boolean? = null,
    val folderId: String? = null,
    val newName: String? = null,
)



data class BreadcrumbItem(
    val id: String?,   // null = root
    val name: String,
)

sealed class AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>()
    data class Error(val message: String, val code: Int? = null) : AppResult<Nothing>()
    object Loading : AppResult<Nothing>()
}
