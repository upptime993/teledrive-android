package com.teledrive.sky.data.repository

import com.teledrive.sky.data.local.preferences.UserPreferencesDataStore
import com.teledrive.sky.data.remote.api.TeleDriveApi
import com.teledrive.sky.data.remote.dto.RegisterRequest
import com.teledrive.sky.domain.model.AppResult
import com.teledrive.sky.domain.model.AuthToken
import com.teledrive.sky.domain.model.UserProfile
import com.teledrive.sky.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: TeleDriveApi,
    private val prefs: UserPreferencesDataStore,
) : AuthRepository {

    override suspend fun login(email: String, password: String): AppResult<UserProfile> {
        return try {
            // Step 1: Get CSRF token
            val csrfResponse = api.getSession()

            // Step 2: Sign in with credentials
            val signInBody = mapOf(
                "email" to email,
                "password" to password,
                "csrfToken" to "", // NextAuth will handle CSRF
                "callbackUrl" to "",
                "json" to "true",
            )

            val response = api.signIn(signInBody)

            if (response.isSuccessful || response.code() == 302) {
                // Step 3: Get session to confirm login
                val sessionResponse = api.getSession()
                val user = sessionResponse.body()?.user

                if (user != null || sessionResponse.isSuccessful) {
                    // Try to get auth token from cookie headers
                    val cookies = response.headers().values("Set-Cookie")
                    val sessionToken = cookies
                        .firstOrNull { it.contains("next-auth.session-token") }
                        ?.substringAfter("next-auth.session-token=")
                        ?.substringBefore(";")

                    if (!sessionToken.isNullOrBlank()) {
                        prefs.saveAuthToken(sessionToken)
                    }

                    val profile = UserProfile(
                        id = user?.id ?: email,
                        name = user?.name ?: email.substringBefore("@"),
                        email = user?.email ?: email,
                    )
                    prefs.saveUserProfile(profile.id, profile.name, profile.email)
                    AppResult.Success(profile)
                } else {
                    AppResult.Error("Login gagal. Periksa email dan kata sandi Anda.")
                }
            } else {
                when (response.code()) {
                    401 -> AppResult.Error("Email atau kata sandi salah.")
                    429 -> AppResult.Error("Terlalu banyak percobaan. Coba beberapa saat lagi.")
                    else -> AppResult.Error("Login gagal (${response.code()})")
                }
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Koneksi gagal. Periksa internet Anda.")
        }
    }

    override suspend fun register(name: String, email: String, password: String): AppResult<UserProfile> {
        return try {
            val response = api.register(RegisterRequest(name, email, password))
            if (response.isSuccessful) {
                val user = response.body()?.user
                val profile = UserProfile(
                    id = user?.id ?: email,
                    name = user?.name ?: name,
                    email = user?.email ?: email,
                )
                prefs.saveUserProfile(profile.id, profile.name, profile.email)

                // Auto-login after register
                login(email, password)
            } else {
                when (response.code()) {
                    409 -> AppResult.Error("Email sudah terdaftar.")
                    400 -> AppResult.Error("Data tidak valid. Periksa kembali.")
                    else -> AppResult.Error("Pendaftaran gagal (${response.code()})")
                }
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Koneksi gagal.")
        }
    }

    override suspend fun logout() {
        try {
            api.signOut()
        } catch (e: Exception) { /* ignore */ }
        prefs.clearAuthToken()
        prefs.clearAll()
    }

    override fun getAuthToken(): Flow<AuthToken?> = prefs.getAuthToken().map { token ->
        if (token.isNullOrBlank()) null
        else AuthToken(
            accessToken = token,
            expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000 // 30 days
        )
    }

    override fun isLoggedIn(): Flow<Boolean> = prefs.getAuthToken().map { !it.isNullOrBlank() }

    override suspend fun refreshToken(): AppResult<AuthToken> {
        return try {
            val response = api.getSession()
            if (response.isSuccessful && response.body()?.user != null) {
                val token = prefs.getAuthToken().first()
                if (!token.isNullOrBlank()) {
                    AppResult.Success(AuthToken(
                        accessToken = token,
                        expiresAt = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
                    ))
                } else AppResult.Error("No valid session")
            } else {
                AppResult.Error("Session expired")
            }
        } catch (e: Exception) {
            AppResult.Error(e.message ?: "Refresh failed")
        }
    }

    override fun getUserProfile(): Flow<UserProfile?> =
        combine(prefs.getUserName(), prefs.getUserEmail()) { name, email ->
            if (name != null && email != null) {
                UserProfile(id = email, name = name, email = email)
            } else null
        }
}
