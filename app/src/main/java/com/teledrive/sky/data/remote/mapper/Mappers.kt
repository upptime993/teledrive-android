package com.teledrive.sky.data.remote.mapper

import com.teledrive.sky.data.remote.dto.*
import com.teledrive.sky.domain.model.*
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

// ─── DTO to Domain Mappers ───────────────────────────────────────────────────

object FileMapper {
    private val isoDateFormats = listOf(
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") },
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US),
    )

    private fun parseDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        for (format in isoDateFormats) {
            try {
                return format.parse(dateStr)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) { /* try next */ }
        }
        return System.currentTimeMillis()
    }

    fun FileDto.toDomain(): TeleFile = TeleFile(
        id = id,
        name = name,
        size = size,
        mimeType = mimeType ?: type ?: "application/octet-stream",
        folderId = folderId,
        isStarred = isStarred ?: false,
        isDeleted = isDeleted ?: false,
        isChunked = isChunked ?: false,
        createdAt = parseDate(createdAt),
        type = type,
    )

    fun FolderDto.toDomain(): TeleFolder = TeleFolder(
        id = id,
        name = name,
        parentId = parentId,
        isDeleted = isDeleted ?: false,
        createdAt = parseDate(createdAt),
    )

    fun ShareDto.toDomain(baseUrl: String): ShareLink = ShareLink(
        id = id ?: "",
        token = token ?: "",
        url = "$baseUrl/api/share/download?token=${token}",
        fileId = fileId ?: "",
        fileName = fileName ?: "",
        fileSize = fileSize ?: 0,
        mimeType = mimeType ?: "application/octet-stream",
        downloadCount = downloadCount ?: 0,
        expiresAt = if (expiresAt != null) parseDate(expiresAt) else null,
        createdAt = parseDate(createdAt),
    )

    fun FilesResponse.toStorageInfo(): StorageInfo {
        return StorageInfo(
            totalSize = totalSize ?: 0L,
            totalFiles = totalFiles ?: files?.size ?: 0,
            photoSize = 0L,
            photoCount = 0,
            videoSize = 0L,
            videoCount = 0,
            documentSize = 0L,
            documentCount = 0,
            otherSize = 0L,
            otherCount = 0,
        )
    }
}
