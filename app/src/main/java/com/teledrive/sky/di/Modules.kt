package com.teledrive.sky.di

import android.content.Context
import androidx.room.Room
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.teledrive.sky.BuildConfig
import com.teledrive.sky.data.local.TeleDriveDatabase
import com.teledrive.sky.data.local.dao.*
import com.teledrive.sky.data.local.preferences.UserPreferencesDataStore
import com.teledrive.sky.data.remote.api.TeleDriveApi
import com.teledrive.sky.data.remote.interceptor.AuthInterceptor
import com.teledrive.sky.data.remote.interceptor.RetryInterceptor
import com.teledrive.sky.data.remote.interceptor.UserAgentInterceptor
import com.teledrive.sky.data.repository.*
import com.teledrive.sky.domain.repository.*
import com.teledrive.sky.util.NetworkMonitor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.CookieJar
import okhttp3.JavaNetCookieJar
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.net.CookieManager
import java.net.CookiePolicy
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideCookieJar(): CookieJar {
        val cookieManager = CookieManager().apply {
            setCookiePolicy(CookiePolicy.ACCEPT_ALL)
        }
        return JavaNetCookieJar(cookieManager)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        retryInterceptor: RetryInterceptor,
        userAgentInterceptor: UserAgentInterceptor,
        cookieJar: CookieJar,
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(retryInterceptor)
            .apply {
                if (BuildConfig.DEBUG_MODE) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                }
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    @Provides
    @Singleton
    fun provideTeleDriveApi(retrofit: Retrofit): TeleDriveApi =
        retrofit.create(TeleDriveApi::class.java)

    @Provides
    @Singleton
    fun provideAuthInterceptor(prefs: UserPreferencesDataStore): AuthInterceptor =
        AuthInterceptor(prefs)

    @Provides
    @Singleton
    fun provideRetryInterceptor(): RetryInterceptor = RetryInterceptor()

    @Provides
    @Singleton
    fun provideUserAgentInterceptor(): UserAgentInterceptor = UserAgentInterceptor()

    @Provides
    @Singleton
    fun provideNetworkMonitor(@ApplicationContext context: Context): NetworkMonitor =
        NetworkMonitor(context)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TeleDriveDatabase =
        Room.databaseBuilder(context, TeleDriveDatabase::class.java, TeleDriveDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFileDao(db: TeleDriveDatabase): FileDao = db.fileDao()

    @Provides
    fun provideFolderDao(db: TeleDriveDatabase): FolderDao = db.folderDao()

    @Provides
    fun provideTransferDao(db: TeleDriveDatabase): TransferDao = db.transferDao()

    @Provides
    fun provideThumbnailCacheDao(db: TeleDriveDatabase): ThumbnailCacheDao = db.thumbnailCacheDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindFileRepository(impl: FileRepositoryImpl): FileRepository

    @Binds
    @Singleton
    abstract fun bindFolderRepository(impl: FolderRepositoryImpl): FolderRepository

    @Binds
    @Singleton
    abstract fun bindTransferRepository(impl: TransferRepositoryImpl): TransferRepository

    @Binds
    @Singleton
    abstract fun bindShareRepository(impl: ShareRepositoryImpl): ShareRepository
}
