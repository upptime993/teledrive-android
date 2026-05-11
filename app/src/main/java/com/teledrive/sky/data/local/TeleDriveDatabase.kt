package com.teledrive.sky.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.teledrive.sky.data.local.dao.*
import com.teledrive.sky.data.local.entity.*

@Database(
    entities = [
        FileEntity::class,
        FolderEntity::class,
        TransferEntity::class,
        ThumbnailCacheEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class TeleDriveDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun folderDao(): FolderDao
    abstract fun transferDao(): TransferDao
    abstract fun thumbnailCacheDao(): ThumbnailCacheDao

    companion object {
        const val DATABASE_NAME = "teledrive_sky.db"
    }
}
