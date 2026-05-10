package com.teledrive.sky.data.repository

import com.teledrive.sky.BuildConfig
import com.teledrive.sky.data.remote.api.TeleDriveApi
import com.teledrive.sky.data.remote.dto.CreateShareRequest
import com.teledrive.sky.data.remote.mapper.FileMapper.toDomain
import com.teledrive.sky.domain.model.AppResult
import com.teledrive.sky.domain.model.ShareLink
import com.teledrive.sky.domain.repository.ShareRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRepositoryImpl @Inject constructor(
    private val api: TeleDriveApi,
) : ShareRepository {

    private val baseUrl = BuildConfig.BASE_URL.trimEnd('/')

    override suspend fun createShareLink(
        fileId: String,
        expiresInDays: Int?,
    ): AppResult<ShareLink> {
        return try {
            val response = api.createShareLink(
                CreateShareRequest(fileId = fileId, expiresInDays = expiresInDays)
            )
            if (response.isSuccessful) {
                val share = response.body()?.share
                if (share != null) {
                    AppResult.Success(share.toDomain(baseUrl))
                } else AppResult.Error("Invalid response")
            } else {
                when (response.code()) {
                    401 -> AppResult.Error("Sesi habis. Silakan masuk kembali.")
                    404 -> AppResult.Error("File tidak ditemukan.")
                    else -> AppResult.Error("Gagal membuat link (${response.code()})")
                }
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Gagal membuat link")
        }
    }

    override suspend fun getShareLinksForFile(fileId: String): AppResult<List<ShareLink>> {
        return try {
            val response = api.getShareLinks(fileId = fileId)
            if (response.isSuccessful) {
                val links = response.body()?.links?.map { it.toDomain(baseUrl) }
                    ?: response.body()?.share?.let { listOf(it.toDomain(baseUrl)) }
                    ?: emptyList()
                AppResult.Success(links)
            } else AppResult.Error("Gagal memuat link (${response.code()})")
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Gagal memuat link")
        }
    }

    override suspend fun getAllShareLinks(): AppResult<List<ShareLink>> {
        return try {
            val response = api.getShareLinks()
            if (response.isSuccessful) {
                val links = response.body()?.links?.map { it.toDomain(baseUrl) } ?: emptyList()
                AppResult.Success(links)
            } else AppResult.Error("Gagal memuat link")
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Gagal memuat link")
        }
    }

    override suspend fun revokeShareLink(token: String): AppResult<Unit> {
        return try {
            val response = api.revokeShareLink(token)
            if (response.isSuccessful) AppResult.Success(Unit)
            else AppResult.Error("Gagal menghapus link (${response.code()})")
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Gagal menghapus link")
        }
    }
}
