package com.teledrive.sky.data.local.dao

import androidx.room.*
import com.teledrive.sky.data.local.entity.*
import kotlinx.coroutines.flow.Flow

// ─── Room DAOs ───────────────────────────────────────────────────────────────

@Dao
interface FileDao {
    @Query("SELECT * FROM files WHERE (folderId IS NULL AND :folderId IS NULL OR folderId = :folderId) AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getFilesInFolder(folderId: String?): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isStarred = 1 AND isDeleted = 0 ORDER BY createdAt DESC")
    fun getStarredFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE isDeleted = 1 ORDER BY createdAt DESC")
    fun getDeletedFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' AND isDeleted = 0 LIMIT 50")
    suspend fun searchFiles(query: String): List<FileEntity>

    @Query("SELECT * FROM files WHERE id = :id")
    suspend fun getFileById(id: String): FileEntity?

    @Query("SELECT SUM(size) FROM files WHERE isDeleted = 0")
    suspend fun getTotalSize(): Long?

    @Query("SELECT COUNT(*) FROM files WHERE isDeleted = 0")
    suspend fun getTotalCount(): Int

    @Upsert
    suspend fun upsertFiles(files: List<FileEntity>)

    @Upsert
    suspend fun upsertFile(file: FileEntity)

    @Query("DELETE FROM files WHERE id IN (:ids)")
    suspend fun deleteFiles(ids: List<String>)

    @Query("DELETE FROM files WHERE (folderId IS NULL AND :folderId IS NULL OR folderId = :folderId)")
    suspend fun clearFolderFiles(folderId: String?)

    @Query("DELETE FROM files")
    suspend fun clearAll()

    @Query("SELECT lastSyncedAt FROM files WHERE (folderId IS NULL AND :folderId IS NULL OR folderId = :folderId) ORDER BY lastSyncedAt DESC LIMIT 1")
    suspend fun getLastSyncTime(folderId: String?): Long?
}

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders WHERE (parentId IS NULL AND :parentId IS NULL OR parentId = :parentId) AND isDeleted = 0 ORDER BY name ASC")
    fun getFoldersInParent(parentId: String?): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getFolderById(id: String): FolderEntity?

    @Query("SELECT * FROM folders WHERE parentId = :parentId AND isDeleted = 0")
    suspend fun getSubfolders(parentId: String): List<FolderEntity>

    @Upsert
    suspend fun upsertFolders(folders: List<FolderEntity>)

    @Upsert
    suspend fun upsertFolder(folder: FolderEntity)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("DELETE FROM folders")
    suspend fun clearAll()
}

@Dao
interface TransferDao {
    @Query("SELECT * FROM transfers WHERE state NOT IN ('COMPLETED', 'CANCELLED') ORDER BY createdAt ASC")
    fun getActiveTransfers(): Flow<List<TransferEntity>>

    @Query("SELECT * FROM transfers WHERE id = :id")
    fun getTransferById(id: String): Flow<TransferEntity?>

    @Query("SELECT * FROM transfers WHERE state = 'IN_PROGRESS' OR state = 'QUEUED'")
    suspend fun getPendingTransfers(): List<TransferEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransfer(transfer: TransferEntity)

    @Update
    suspend fun updateTransfer(transfer: TransferEntity)

    @Query("UPDATE transfers SET state = :state, transferredBytes = :transferred, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateTransferProgress(id: String, state: String, transferred: Long, updatedAt: Long)

    @Query("UPDATE transfers SET completedChunks = :completed, chunksJson = :chunksJson, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateChunkProgress(id: String, completed: Int, chunksJson: String, updatedAt: Long)

    @Query("DELETE FROM transfers WHERE state IN ('COMPLETED', 'CANCELLED') AND updatedAt < :olderThan")
    suspend fun cleanupOldTransfers(olderThan: Long)

    @Query("DELETE FROM transfers WHERE id = :id")
    suspend fun deleteTransfer(id: String)
}

@Dao
interface ThumbnailCacheDao {
    @Query("SELECT * FROM thumbnail_cache WHERE fileId = :fileId")
    suspend fun getThumbnail(fileId: String): ThumbnailCacheEntity?

    @Query("SELECT SUM(sizeBytes) FROM thumbnail_cache")
    suspend fun getTotalCacheSize(): Long?

    @Query("SELECT * FROM thumbnail_cache ORDER BY lastAccessedAt ASC LIMIT :count")
    suspend fun getLruEntries(count: Int): List<ThumbnailCacheEntity>

    @Upsert
    suspend fun upsertThumbnail(thumbnail: ThumbnailCacheEntity)

    @Query("UPDATE thumbnail_cache SET lastAccessedAt = :time WHERE fileId = :fileId")
    suspend fun updateLastAccessed(fileId: String, time: Long)

    @Query("DELETE FROM thumbnail_cache WHERE fileId IN (:fileIds)")
    suspend fun deleteThumbnails(fileIds: List<String>)

    @Query("DELETE FROM thumbnail_cache")
    suspend fun clearAll()
}
