package com.teledrive.sky.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── API Request DTOs ────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(
    val email: String,
    val password: String,
    val callbackUrl: String? = null,
)

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
)

@Serializable
data class UpdateFileRequest(
    val fileId: String,
    val isStarred: Boolean? = null,
    val isDeleted: Boolean? = null,
    val folderId: String? = null,
    val name: String? = null,
)

@Serializable
data class DeleteFilesRequest(
    val fileIds: List<String>,
    val permanent: Boolean = false,
)

@Serializable
data class CreateFolderRequest(
    val name: String,
    val parentId: String? = null,
)

@Serializable
data class UpdateFolderRequest(
    val folderId: String,
    val name: String? = null,
    val isDeleted: Boolean? = null,
    val parentId: String? = null,
)

@Serializable
data class UploadCompleteRequest(
    val fileName: String,
    val fileSize: Long,
    val mimeType: String,
    val folderId: String? = null,
    val chunks: List<ChunkDto>,
)

@Serializable
data class ChunkDto(
    val part: Int,
    val msgId: Long,
    val size: Long,
)

@Serializable
data class CreateShareRequest(
    val fileId: String,
    val expiresInDays: Int? = null,
)

// ─── API Response DTOs ───────────────────────────────────────────────────────

@Serializable
data class AuthResponse(
    val ok: Boolean? = null,
    val error: String? = null,
    val url: String? = null,
)

@Serializable
data class SessionResponse(
    val user: UserDto? = null,
    val expires: String? = null,
)

@Serializable
data class UserDto(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("email") val email: String? = null,
    @SerialName("image") val image: String? = null,
)

@Serializable
data class FilesResponse(
    val files: List<FileDto>? = null,
    val total: Int? = null,
    val page: Int? = null,
    val limit: Int? = null,
    // Storage aggregate response
    val totalSize: Long? = null,
    val totalFiles: Int? = null,
)

@Serializable
data class FileDto(
    @SerialName("_id") val id: String,
    val name: String,
    val size: Long = 0,
    val mimeType: String? = null,
    val type: String? = null,
    val folderId: String? = null,
    val isStarred: Boolean? = false,
    val isDeleted: Boolean? = false,
    val isChunked: Boolean? = false,
    val createdAt: String? = null,
)

@Serializable
data class FoldersResponse(
    val folders: List<FolderDto>? = null,
)

@Serializable
data class FolderDto(
    @SerialName("_id") val id: String,
    val name: String,
    val parentId: String? = null,
    val isDeleted: Boolean? = false,
    val createdAt: String? = null,
)

@Serializable
data class UploadChunkResponse(
    val part: Int? = null,
    val msgId: Long? = null,
    val size: Long? = null,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class UploadCompleteResponse(
    @SerialName("file") val file: FileDto? = null,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class ShareResponse(
    val share: ShareDto? = null,
    val links: List<ShareDto>? = null,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class ShareDto(
    @SerialName("_id") val id: String? = null,
    val token: String? = null,
    val fileId: String? = null,
    val fileName: String? = null,
    val fileSize: Long? = null,
    val mimeType: String? = null,
    val downloadCount: Int? = 0,
    val expiresAt: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class DeleteResponse(
    val message: String? = null,
    val error: String? = null,
    val count: Int? = null,
)

@Serializable
data class CreateFolderResponse(
    val folder: FolderDto? = null,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class UpdateFolderResponse(
    val folder: FolderDto? = null,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class UpdateFileResponse(
    val file: FileDto? = null,
    val message: String? = null,
    val error: String? = null,
)

@Serializable
data class WorkerUrlResponse(
    val url: String? = null,
    val workerIdx: Int? = null,
)

@Serializable
data class HealthResponse(
    val status: String? = null,
    val worker: Boolean? = null,
)

@Serializable
data class ErrorResponse(
    val error: String? = null,
    val message: String? = null,
)
