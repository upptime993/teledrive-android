package com.teledrive.sky.util

import com.teledrive.sky.domain.model.FileCategory

// ─── Utility Functions ───────────────────────────────────────────────────────

object FileSizeFormatter {
    private val UNITS = listOf("bytes", "KB", "MB", "GB", "TB")

    fun format(bytes: Long): String {
        if (bytes < 0) return "0 bytes"
        if (bytes < 1024) return "$bytes bytes"
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < UNITS.size - 1) {
            value /= 1024.0
            unitIndex++
        }
        return "%.1f %s".format(value, UNITS[unitIndex])
    }

    fun formatSpeed(bytesPerSec: Long): String = "${format(bytesPerSec)}/s"
}

object RelativeDateFormatter {
    fun format(epochMillis: Long): String {
        val now = System.currentTimeMillis()
        val diffMs = now - epochMillis
        if (diffMs < 0) return "Baru saja"

        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHour = diffMin / 60
        val diffDay = diffHour / 24

        return when {
            diffSec < 60 -> "Baru saja"
            diffMin < 60 -> "${diffMin} menit lalu"
            diffHour < 24 -> "${diffHour} jam lalu"
            diffDay < 7 -> "${diffDay} hari lalu"
            else -> {
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = epochMillis
                "%02d/%02d/%d".format(
                    cal.get(java.util.Calendar.DAY_OF_MONTH),
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.YEAR)
                )
            }
        }
    }
}

object MimeTypeUtils {
    fun categorize(mimeType: String): FileCategory = when {
        mimeType.startsWith("image/") -> FileCategory.PHOTO
        mimeType.startsWith("video/") -> FileCategory.VIDEO
        mimeType.startsWith("audio/") -> FileCategory.AUDIO
        mimeType.startsWith("text/") ||
        mimeType == "application/pdf" ||
        mimeType.startsWith("application/msword") ||
        mimeType.startsWith("application/vnd.openxmlformats-officedocument") ||
        mimeType.startsWith("application/vnd.ms-") ||
        mimeType == "application/rtf" -> FileCategory.DOCUMENT
        mimeType == "application/zip" ||
        mimeType == "application/x-rar-compressed" ||
        mimeType == "application/x-7z-compressed" ||
        mimeType == "application/x-tar" ||
        mimeType == "application/gzip" -> FileCategory.ARCHIVE
        else -> FileCategory.OTHER
    }

    fun isPreviewable(mimeType: String): Boolean = when {
        mimeType.startsWith("image/") -> true
        mimeType.startsWith("video/") -> true
        mimeType == "application/pdf" -> true
        mimeType.startsWith("text/") -> true
        else -> false
    }

    fun isImage(mimeType: String) = mimeType.startsWith("image/")
    fun isVideo(mimeType: String) = mimeType.startsWith("video/")
    fun isPdf(mimeType: String) = mimeType == "application/pdf"
    fun isText(mimeType: String) = mimeType.startsWith("text/")
    fun isAudio(mimeType: String) = mimeType.startsWith("audio/")
}

object ChunkCalculator {
    const val CHUNK_SIZE_BYTES = 20L * 1024 * 1024  // 20MB
    const val MAX_FILE_SIZE_BYTES = 2L * 1024 * 1024 * 1024  // 2GB

    fun needsChunking(fileSize: Long): Boolean = fileSize >= CHUNK_SIZE_BYTES

    fun calculateChunks(fileSize: Long): List<ChunkRange> {
        if (fileSize <= 0) return emptyList()
        require(fileSize <= MAX_FILE_SIZE_BYTES) {
            "File size ${FileSizeFormatter.format(fileSize)} exceeds maximum ${FileSizeFormatter.format(MAX_FILE_SIZE_BYTES)}"
        }
        val totalChunks = ((fileSize + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES).toInt()
        return (0 until totalChunks).map { part ->
            val start = part.toLong() * CHUNK_SIZE_BYTES
            val end = minOf(start + CHUNK_SIZE_BYTES, fileSize)
            ChunkRange(part = part, startByte = start, endByte = end, size = end - start)
        }
    }

    fun totalChunks(fileSize: Long): Int =
        ((fileSize + CHUNK_SIZE_BYTES - 1) / CHUNK_SIZE_BYTES).toInt().coerceAtLeast(1)
}

data class ChunkRange(
    val part: Int,
    val startByte: Long,
    val endByte: Long,
    val size: Long,
)

object ValidationUtils {
    fun isValidEmail(email: String): Boolean =
        android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    fun isValidPassword(password: String): Boolean =
        password.length >= 6 && password.any { it.isLetter() } && password.any { it.isDigit() }

    fun isValidName(name: String): Boolean =
        name.trim().isNotEmpty() && name.trim().length <= 100

    fun isValidFolderName(name: String): Boolean {
        val trimmed = name.trim().replace("../", "").replace("..\\", "")
        return trimmed.isNotEmpty() && trimmed.length <= 100
    }

    fun isValidSearchQuery(query: String): Boolean =
        query.trim().length in 2..100

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val message: String) : ValidationResult()
    }

    fun validateLoginForm(email: String, password: String): ValidationResult {
        if (!isValidEmail(email)) return ValidationResult.Invalid("Format email tidak valid")
        if (password.isBlank()) return ValidationResult.Invalid("Kata sandi tidak boleh kosong")
        return ValidationResult.Valid
    }

    fun validateRegisterForm(name: String, email: String, password: String): ValidationResult {
        if (!isValidName(name)) return ValidationResult.Invalid("Nama tidak boleh kosong (maks 100 karakter)")
        if (!isValidEmail(email)) return ValidationResult.Invalid("Format email tidak valid")
        if (password.length < 6) return ValidationResult.Invalid("Kata sandi minimal 6 karakter")
        if (!password.any { it.isLetter() }) return ValidationResult.Invalid("Kata sandi harus mengandung huruf")
        if (!password.any { it.isDigit() }) return ValidationResult.Invalid("Kata sandi harus mengandung angka")
        return ValidationResult.Valid
    }
}
