package com.teledrive.sky.data.remote.api

import com.teledrive.sky.data.remote.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

// ─── Retrofit API Service ────────────────────────────────────────────────────
// Matches the TeleDrive Next.js API endpoints

interface TeleDriveApi {

    // ── Auth (NextAuth) ─────────────────────────────────────────────────────
    @GET("api/auth/session")
    suspend fun getSession(): Response<SessionResponse>

    @POST("api/auth/signin/credentials")
    suspend fun signIn(@Body body: Map<String, String>): Response<AuthResponse>

    @POST("api/auth/signout")
    suspend fun signOut(): Response<AuthResponse>

    // ── Register ────────────────────────────────────────────────────────────
    @POST("api/auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<SessionResponse>

    // ── Files ───────────────────────────────────────────────────────────────
    @GET("api/files")
    suspend fun getFiles(
        @Query("folderId") folderId: String? = null,
        @Query("filter") filter: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("aggregate") aggregate: String? = null,
    ): Response<FilesResponse>

    @PATCH("api/files")
    suspend fun updateFile(@Body request: UpdateFileRequest): Response<UpdateFileResponse>

    @HTTP(method = "DELETE", path = "api/files", hasBody = true)
    suspend fun deleteFiles(@Body request: DeleteFilesRequest): Response<DeleteResponse>

    // ── Folders ─────────────────────────────────────────────────────────────
    @GET("api/folders")
    suspend fun getFolders(
        @Query("parentId") parentId: String? = null,
        @Query("filter") filter: String? = null,
    ): Response<FoldersResponse>

    @POST("api/folders")
    suspend fun createFolder(@Body request: CreateFolderRequest): Response<CreateFolderResponse>

    @PATCH("api/folders")
    suspend fun updateFolder(@Body request: UpdateFolderRequest): Response<UpdateFolderResponse>

    @DELETE("api/folders")
    suspend fun deleteFolder(
        @Query("folderId") folderId: String,
        @Query("permanent") permanent: Boolean = false,
    ): Response<DeleteResponse>

    // ── Upload ──────────────────────────────────────────────────────────────
    // Single chunk upload (< 20MB)
    @Multipart
    @POST("api/upload-chunk")
    suspend fun uploadChunk(
        @Part chunk: MultipartBody.Part,
        @Part("part") part: RequestBody,
        @Part("totalParts") totalParts: RequestBody,
        @Part("fileName") fileName: RequestBody,
        @Part("workerIdx") workerIdx: RequestBody,
    ): Response<UploadChunkResponse>

    @POST("api/upload-complete")
    suspend fun uploadComplete(
        @Body request: UploadCompleteRequest,
    ): Response<UploadCompleteResponse>

    // ── Download ────────────────────────────────────────────────────────────
    @Streaming
    @GET("api/download")
    suspend fun downloadFile(
        @Query("fileId") fileId: String,
    ): Response<ResponseBody>

    // ── Share ───────────────────────────────────────────────────────────────
    @POST("api/share")
    suspend fun createShareLink(@Body request: CreateShareRequest): Response<ShareResponse>

    @GET("api/share/list")
    suspend fun getShareLinks(@Query("fileId") fileId: String? = null): Response<ShareResponse>

    @DELETE("api/share")
    suspend fun revokeShareLink(@Query("token") token: String): Response<DeleteResponse>

    // ── System ──────────────────────────────────────────────────────────────
    @GET("api/health")
    suspend fun getHealth(): Response<HealthResponse>

    @GET("api/worker-url")
    suspend fun getWorkerUrl(): Response<WorkerUrlResponse>

    @GET("api/worker-info")
    suspend fun getWorkerInfo(): Response<WorkerUrlResponse>
}
