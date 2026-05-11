package com.teledrive.sky.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// ─── Room Entities ───────────────────────────────────────────────────────────

@Entity(
    tableName = "files",
    indices = [
        Index(value = ["folderId"]),
        Index(value = ["isStarred"]),
        Index(value = ["isDeleted"]),
        Index(value = ["createdAt"]),
    ]
)
data class FileEntity(
    @PrimaryKey val id: String,
    val name: String,
    val size: Long,
    val mimeType: String,
    val folderId: String?,
    val isStarred: Boolean,
    val isDeleted: Boolean,
    val isChunked: Boolean,
    val createdAt: Long,
    val lastSyncedAt: Long,
)

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["parentId"]),
        Index(value = ["isDeleted"]),
    ]
)
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val parentId: String?,
    val isDeleted: Boolean,
    val createdAt: Long,
    val lastSyncedAt: Long,
)

@Entity(
    tableName = "transfers",
    indices = [
        Index(value = ["state"]),
        Index(value = ["type"]),
    ]
)
data class TransferEntity(
    @PrimaryKey val id: String,
    val fileName: String,
    val fileUri: String?,
    val fileId: String?,
    val folderId: String?,
    val totalBytes: Long,
    val transferredBytes: Long,
    val totalChunks: Int,
    val completedChunks: Int,
    val chunksJson: String, // serialized List<ChunkInfo>
    val mimeType: String,
    val type: String, // "upload" or "download"
    val state: String, // TransferState name
    val errorMessage: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "thumbnail_cache",
    indices = [Index(value = ["lastAccessedAt"])]
)
data class ThumbnailCacheEntity(
    @PrimaryKey val fileId: String,
    val localPath: String,
    val sizeBytes: Long,
    val lastAccessedAt: Long,
)
