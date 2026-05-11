package com.teledrive.sky.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.teledrive.sky.domain.model.SortConfig
import com.teledrive.sky.domain.model.SortDirection
import com.teledrive.sky.domain.model.SortField
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "teledrive_prefs")

@Singleton
class UserPreferencesDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    // ── Keys ─────────────────────────────────────────────────────────────────
    companion object {
        val KEY_AUTH_TOKEN = stringPreferencesKey("auth_token")
        val KEY_USER_ID = stringPreferencesKey("user_id")
        val KEY_USER_NAME = stringPreferencesKey("user_name")
        val KEY_USER_EMAIL = stringPreferencesKey("user_email")
        val KEY_SORT_FIELD = stringPreferencesKey("sort_field")
        val KEY_SORT_DIRECTION = stringPreferencesKey("sort_direction")
        val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin_hash")
        val KEY_APP_LOCK_USE_BIOMETRIC = booleanPreferencesKey("app_lock_biometric")
        val KEY_LAST_BACKGROUND_TIME = longPreferencesKey("last_background_time")
        val KEY_LAST_SYNC_TIME = longPreferencesKey("last_sync_time")
        val KEY_VIEW_MODE_GRID = booleanPreferencesKey("view_mode_grid")
        val KEY_PIN_FAIL_COUNT = intPreferencesKey("pin_fail_count")
        val KEY_PIN_LOCKED_UNTIL = longPreferencesKey("pin_locked_until")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
    }

    // ── Auth Token ───────────────────────────────────────────────────────────
    fun getAuthToken(): Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_AUTH_TOKEN] }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { it[KEY_AUTH_TOKEN] = token }
    }

    suspend fun clearAuthToken() {
        dataStore.edit { it.remove(KEY_AUTH_TOKEN) }
    }

    // ── User Profile ─────────────────────────────────────────────────────────
    suspend fun saveUserProfile(id: String, name: String, email: String) {
        dataStore.edit {
            it[KEY_USER_ID] = id
            it[KEY_USER_NAME] = name
            it[KEY_USER_EMAIL] = email
        }
    }

    fun getUserName(): Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_USER_NAME] }

    fun getUserEmail(): Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_USER_EMAIL] }

    // ── Sort Preferences ─────────────────────────────────────────────────────
    fun getSortConfig(): Flow<SortConfig> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            SortConfig(
                field = prefs[KEY_SORT_FIELD]?.let { SortField.valueOf(it) } ?: SortField.NAME,
                direction = prefs[KEY_SORT_DIRECTION]?.let { SortDirection.valueOf(it) } ?: SortDirection.ASC,
            )
        }

    suspend fun saveSortConfig(config: SortConfig) {
        dataStore.edit {
            it[KEY_SORT_FIELD] = config.field.name
            it[KEY_SORT_DIRECTION] = config.direction.name
        }
    }

    // ── App Lock ─────────────────────────────────────────────────────────────
    fun isAppLockEnabled(): Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_APP_LOCK_ENABLED] ?: false }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_APP_LOCK_ENABLED] = enabled }
    }

    fun getPinHash(): Flow<String?> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_APP_LOCK_PIN] }

    suspend fun savePinHash(hash: String) {
        dataStore.edit { it[KEY_APP_LOCK_PIN] = hash }
    }

    suspend fun clearPin() {
        dataStore.edit { it.remove(KEY_APP_LOCK_PIN) }
    }

    fun isBiometricEnabled(): Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_APP_LOCK_USE_BIOMETRIC] ?: true }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_APP_LOCK_USE_BIOMETRIC] = enabled }
    }

    // ── Background Time (for app lock) ───────────────────────────────────────
    suspend fun saveLastBackgroundTime(time: Long) {
        dataStore.edit { it[KEY_LAST_BACKGROUND_TIME] = time }
    }

    suspend fun getLastBackgroundTime(): Long {
        return dataStore.data.first()[KEY_LAST_BACKGROUND_TIME] ?: 0L
    }

    // ── Sync Time ────────────────────────────────────────────────────────────
    fun getLastSyncTime(): Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_LAST_SYNC_TIME] ?: 0L }

    suspend fun saveLastSyncTime(time: Long) {
        dataStore.edit { it[KEY_LAST_SYNC_TIME] = time }
    }

    // ── View Mode ────────────────────────────────────────────────────────────
    fun isGridViewMode(): Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_VIEW_MODE_GRID] ?: false }

    suspend fun setGridViewMode(isGrid: Boolean) {
        dataStore.edit { it[KEY_VIEW_MODE_GRID] = isGrid }
    }

    // ── PIN Lockout ──────────────────────────────────────────────────────────
    fun getPinFailCount(): Flow<Int> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PIN_FAIL_COUNT] ?: 0 }

    suspend fun incrementPinFailCount() {
        dataStore.edit { it[KEY_PIN_FAIL_COUNT] = (it[KEY_PIN_FAIL_COUNT] ?: 0) + 1 }
    }

    suspend fun resetPinFailCount() {
        dataStore.edit {
            it[KEY_PIN_FAIL_COUNT] = 0
            it.remove(KEY_PIN_LOCKED_UNTIL)
        }
    }

    fun getPinLockedUntil(): Flow<Long> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_PIN_LOCKED_UNTIL] ?: 0L }

    suspend fun setPinLockedUntil(time: Long) {
        dataStore.edit { it[KEY_PIN_LOCKED_UNTIL] = time }
    }

    // ── Clear All ────────────────────────────────────────────────────────────
    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    // ── Onboarding ───────────────────────────────────────────────────────────
    fun isOnboardingDone(): Flow<Boolean> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[KEY_ONBOARDING_DONE] ?: false }

    suspend fun setOnboardingDone() {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = true }
    }
}
