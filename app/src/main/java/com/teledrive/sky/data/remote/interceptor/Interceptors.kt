package com.teledrive.sky.data.remote.interceptor

import com.teledrive.sky.data.local.preferences.UserPreferencesDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * AuthInterceptor — Attaches NextAuth session token to every API request
 *
 * TeleDrive uses NextAuth cookie-based sessions. The session token is stored
 * as `next-auth.session-token` cookie by the browser, but for mobile we
 * store the Bearer token returned after sign-in.
 */
class AuthInterceptor(
    private val preferencesDataStore: UserPreferencesDataStore
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking {
            preferencesDataStore.getAuthToken().firstOrNull()
        }

        val request = chain.request()

        // Build new request with auth headers
        val authenticatedRequest = if (!token.isNullOrBlank()) {
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Cookie", "next-auth.session-token=$token")
                .build()
        } else {
            request
        }

        val response = chain.proceed(authenticatedRequest)

        // Extract session token from Set-Cookie header if present
        val setCookieHeader = response.headers("Set-Cookie")
        val sessionToken = setCookieHeader
            .firstOrNull { it.startsWith("next-auth.session-token=") }
            ?.substringAfter("next-auth.session-token=")
            ?.substringBefore(";")

        if (!sessionToken.isNullOrBlank() && sessionToken != token) {
            runBlocking {
                preferencesDataStore.saveAuthToken(sessionToken)
            }
        }

        return response
    }
}

/**
 * RetryInterceptor — Retries failed requests with exponential backoff
 */
class RetryInterceptor(
    private val maxRetries: Int = 3,
    private val retryDelayMs: Long = 5000L,
    private val retryOnStatusCodes: Set<Int> = setOf(500, 502, 503, 504),
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var response: Response? = null
        var lastException: Exception? = null
        val request = chain.request()

        for (attempt in 0..maxRetries) {
            try {
                response?.close()
                response = chain.proceed(request)

                if (response.isSuccessful || response.code !in retryOnStatusCodes) {
                    return response
                }

                if (attempt < maxRetries) {
                    response.close()
                    Thread.sleep(retryDelayMs * (attempt + 1))
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelayMs * (attempt + 1))
                }
            }
        }

        return response ?: throw lastException ?: RuntimeException("Request failed after $maxRetries retries")
    }
}

/**
 * LoggingInterceptor wrapper — only in debug mode
 */
class UserAgentInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .addHeader("User-Agent", "TeleDrive-Sky/1.0 Android/${android.os.Build.VERSION.RELEASE}")
            .addHeader("Accept", "application/json")
            .build()
        return chain.proceed(request)
    }
}
