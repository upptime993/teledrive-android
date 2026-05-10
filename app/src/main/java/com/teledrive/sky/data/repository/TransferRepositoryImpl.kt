package com.teledrive.sky.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.teledrive.sky.BuildConfig
import com.teledrive.sky.data.local.dao.TransferDao
import com.teledrive.sky.data.local.entity.TransferEntity
import com.teledrive.sky.data.remote.api.TeleDriveApi
import com.teledrive.sky.data.remote.dto.ChunkDto
import com.teledrive.sky.data.remote.dto.UploadCompleteRequest
import com.teledrive.sky.domain.model.*
import com.teledrive.sky.domain.repository.TransferRepository
import com.teledrive.sky.util.ChunkCalculator
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: TeleDriveApi,
    private val transferDao: TransferDao,
) : TransferRepository {

    override suspend fun uploadFile(
        uri: Uri,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        folderId: String?,
        onProgress: (TransferProgress) -> Unit,
    ): AppResult<TeleFile> = withContext(Dispatchers.IO) {
        if (ChunkCalculator.needsChunking(fileSize)) {
            uploadChunked(uri, fileName, fileSize, mimeType, folderId, onProgress)
        } else {
            // Still use chunk API for small files (1 chunk)
            uploadChunked(uri, fileName, fileSize, mimeType, folderId, onProgress)
        }
    }

    override suspend fun uploadChunked(
        uri: Uri,
        fileName: String,
        fileSize: Long,
        mimeType: String,
        folderId: String?,
        onProgress: (TransferProgress) -> Unit,
    ): AppResult<TeleFile> = withContext(Dispatchers.IO) {
        val transferId = UUID.randomUUID().toString()
        val chunks = ChunkCalculator.calculateChunks(fileSize)
        val totalChunks = chunks.size

        // Save initial transfer state
        val transferEntity = TransferEntity(
            id = transferId,
            fileName = fileName,
            fileUri = uri.toString(),
            fileId = null,
            folderId = folderId,
            totalBytes = fileSize,
            transferredBytes = 0,
            totalChunks = totalChunks,
            completedChunks = 0,
            chunksJson = "[]",
            mimeType = mimeType,
            type = "upload",
            state = TransferState.IN_PROGRESS.name,
            errorMessage = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
        )
        transferDao.insertTransfer(transferEntity)

        val completedChunks = mutableListOf<ChunkDto>()
        var transferredBytes = 0L
        val startTime = System.currentTimeMillis()

        // Get worker index
        var workerIdx = 0
        try {
            val workerResponse = api.getWorkerInfo()
            workerIdx = workerResponse.body()?.workerIdx ?: 0
        } catch (e: Exception) { /* use default 0 */ }

        try {
            for (chunk in chunks) {
                val chunkData = readChunk(uri, chunk.startByte, chunk.size)
                val tempFile = File(context.cacheDir, "chunk_${transferId}_${chunk.part}.tmp")
                tempFile.writeBytes(chunkData)

                var attempt = 0
                var success = false
                var lastError = ""

                while (attempt < 3 && !success) {
                    try {
                        val filePart = MultipartBody.Part.createFormData(
                            "file",
                            fileName,
                            tempFile.asRequestBody(mimeType.toMediaType())
                        )

                        val response = api.uploadChunk(
                            chunk = filePart,
                            part = chunk.part.toString().toRequestBody("text/plain".toMediaType()),
                            totalParts = totalChunks.toString().toRequestBody("text/plain".toMediaType()),
                            fileName = fileName.toRequestBody("text/plain".toMediaType()),
                            workerIdx = workerIdx.toString().toRequestBody("text/plain".toMediaType()),
                        )

                        if (response.isSuccessful) {
                            val body = response.body()
                            completedChunks.add(ChunkDto(
                                part = body?.part ?: chunk.part,
                                msgId = body?.msgId ?: 0L,
                                size = body?.size ?: chunk.size,
                            ))
                            success = true
                            transferredBytes += chunk.size

                            // Update progress
                            val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            val speed = (transferredBytes * 1000) / elapsed
                            onProgress(TransferProgress(
                                transferId = transferId,
                                fileName = fileName,
                                totalBytes = fileSize,
                                transferredBytes = transferredBytes,
                                speedBytesPerSec = speed,
                                state = TransferState.IN_PROGRESS,
                                isUpload = true,
                            ))

                            // Update DB
                            transferDao.updateChunkProgress(
                                id = transferId,
                                completed = completedChunks.size,
                                chunksJson = Json.encodeToString(completedChunks),
                                updatedAt = System.currentTimeMillis(),
                            )
                        } else {
                            lastError = "Chunk ${chunk.part} failed: ${response.code()}"
                            attempt++
                            if (attempt < 3) delay(5000L)
                        }
                    } catch (e: Exception) {
                        lastError = e.message ?: "Chunk upload error"
                        attempt++
                        if (attempt < 3) delay(5000L)
                    }
                }

                tempFile.delete()

                if (!success) {
                    transferDao.updateTransferProgress(
                        id = transferId,
                        state = TransferState.FAILED.name,
                        transferred = transferredBytes,
                        updatedAt = System.currentTimeMillis(),
                    )
                    return@withContext AppResult.Error("Upload gagal pada chunk ${chunk.part}: $lastError")
                }
            }

            // Complete upload
            val completeResponse = api.uploadComplete(
                UploadCompleteRequest(
                    fileName = fileName,
                    fileSize = fileSize,
                    mimeType = mimeType,
                    folderId = folderId,
                    chunks = completedChunks.sortedBy { it.part },
                )
            )

            if (completeResponse.isSuccessful) {
                val file = completeResponse.body()?.file
                transferDao.updateTransferProgress(
                    id = transferId,
                    state = TransferState.COMPLETED.name,
                    transferred = fileSize,
                    updatedAt = System.currentTimeMillis(),
                )
                onProgress(TransferProgress(
                    transferId = transferId,
                    fileName = fileName,
                    totalBytes = fileSize,
                    transferredBytes = fileSize,
                    speedBytesPerSec = 0,
                    state = TransferState.COMPLETED,
                    isUpload = true,
                ))

                if (file != null) {
                    AppResult.Success(TeleFile(
                        id = file.id,
                        name = file.name,
                        size = file.size,
                        mimeType = file.mimeType ?: mimeType,
                        folderId = file.folderId,
                        isStarred = false,
                        isDeleted = false,
                        isChunked = file.isChunked ?: true,
                        createdAt = System.currentTimeMillis(),
                    ))
                } else AppResult.Error("Upload complete tapi response tidak valid")
            } else {
                AppResult.Error("Upload complete gagal: ${completeResponse.code()}")
            }
        } catch (e: Exception) {
            transferDao.updateTransferProgress(
                id = transferId,
                state = TransferState.FAILED.name,
                transferred = transferredBytes,
                updatedAt = System.currentTimeMillis(),
            )
            AppResult.Error(e.message ?: "Upload gagal")
        }
    }

    override suspend fun downloadFile(
        fileId: String,
        fileName: String,
        onProgress: (TransferProgress) -> Unit,
    ): AppResult<Uri> = withContext(Dispatchers.IO) {
        val transferId = UUID.randomUUID().toString()

        try {
            val response = api.downloadFile(fileId)
            if (!response.isSuccessful || response.body() == null) {
                return@withContext when (response.code()) {
                    503 -> AppResult.Error("Server penyimpanan sedang offline.")
                    404 -> AppResult.Error("File tidak ditemukan.")
                    else -> AppResult.Error("Download gagal: ${response.code()}")
                }
            }

            val body = response.body()!!
            val totalBytes = body.contentLength()

            // Save to Downloads folder
            val savedUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, response.headers()["Content-Type"] ?: "application/octet-stream")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                ) ?: return@withContext AppResult.Error("Tidak bisa menyimpan file")

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    var downloadedBytes = 0L
                    val startTime = System.currentTimeMillis()
                    body.byteStream().use { inputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            val elapsed = (System.currentTimeMillis() - startTime).coerceAtLeast(1)
                            val speed = (downloadedBytes * 1000) / elapsed
                            onProgress(TransferProgress(
                                transferId = transferId,
                                fileName = fileName,
                                totalBytes = totalBytes,
                                transferredBytes = downloadedBytes,
                                speedBytesPerSec = speed,
                                state = TransferState.IN_PROGRESS,
                                isUpload = false,
                            ))
                        }
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
                uri
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                file.outputStream().use { outputStream ->
                    body.byteStream().copyTo(outputStream)
                }
                Uri.fromFile(file)
            }

            onProgress(TransferProgress(
                transferId = transferId,
                fileName = fileName,
                totalBytes = totalBytes,
                transferredBytes = totalBytes,
                speedBytesPerSec = 0,
                state = TransferState.COMPLETED,
                isUpload = false,
            ))

            AppResult.Success(savedUri)
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Download gagal")
        }
    }

    override fun getActiveTransfers(): Flow<List<TransferProgress>> =
        transferDao.getActiveTransfers().map { entities ->
            entities.map { entity ->
                TransferProgress(
                    transferId = entity.id,
                    fileName = entity.fileName,
                    totalBytes = entity.totalBytes,
                    transferredBytes = entity.transferredBytes,
                    speedBytesPerSec = 0,
                    state = TransferState.valueOf(entity.state),
                    isUpload = entity.type == "upload",
                    errorMessage = entity.errorMessage,
                )
            }
        }

    override suspend fun pauseTransfer(transferId: String) {
        transferDao.updateTransferProgress(
            id = transferId,
            state = TransferState.PAUSED.name,
            transferred = 0,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun resumeTransfer(transferId: String) {
        transferDao.updateTransferProgress(
            id = transferId,
            state = TransferState.QUEUED.name,
            transferred = 0,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override suspend fun cancelTransfer(transferId: String) {
        transferDao.updateTransferProgress(
            id = transferId,
            state = TransferState.CANCELLED.name,
            transferred = 0,
            updatedAt = System.currentTimeMillis(),
        )
    }

    override fun getTransfer(transferId: String): Flow<TransferProgress?> =
        transferDao.getTransferById(transferId).map { entity ->
            entity?.let {
                TransferProgress(
                    transferId = it.id,
                    fileName = it.fileName,
                    totalBytes = it.totalBytes,
                    transferredBytes = it.transferredBytes,
                    speedBytesPerSec = 0,
                    state = TransferState.valueOf(it.state),
                    isUpload = it.type == "upload",
                )
            }
        }

    override suspend fun cleanupCompletedTransfers() {
        val oneDayAgo = System.currentTimeMillis() - 24 * 60 * 60 * 1000
        transferDao.cleanupOldTransfers(oneDayAgo)
    }

    private fun readChunk(uri: Uri, startByte: Long, size: Long): ByteArray {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Cannot open file")
        return inputStream.use { stream ->
            stream.skip(startByte)
            val buffer = ByteArray(size.toInt())
            stream.read(buffer)
            buffer
        }
    }
}
