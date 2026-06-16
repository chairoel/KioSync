package com.mascill.kiosync.core.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.kioskDataStore: DataStore<Preferences> by preferencesDataStore(
    name = KioskPreferencesDataSource.PREF_NAME,
    produceMigrations = { context ->
        listOf(
            SharedPreferencesMigration(
                context = context,
                sharedPreferencesName = KioskPreferencesDataSource.PREF_NAME
            )
        )
    }
)

/**
 * Persists kiosk settings in Jetpack DataStore.
 *
 * SharedPreferencesMigration keeps older installations compatible if they previously used the same
 * preference file name.
 */
class KioskPreferencesDataSource(context: Context) {

    private val dataStore = context.applicationContext.kioskDataStore

    /** Emits whether kiosk mode should be active. Defaults to false when data is missing. */
    val kioskEnabled: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_KIOSK_ENABLED] ?: false
        }

    /** Emits package names selected by the admin as launchable inside kiosk mode. */
    val allowedPackages: Flow<Set<String>> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_ALLOWED_APP_PACKAGES].orEmpty()
        }

    /** Saves the desired kiosk enabled state. */
    suspend fun setKioskEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_KIOSK_ENABLED] = enabled
        }
    }

    /** Saves the complete allowlist of package names. */
    suspend fun setAllowedPackages(packageNames: Set<String>) {
        dataStore.edit { preferences ->
            preferences[KEY_ALLOWED_APP_PACKAGES] = packageNames
        }
    }

    companion object {
        const val PREF_NAME = "kiosync_settings"

        private val KEY_KIOSK_ENABLED = booleanPreferencesKey("kiosk_enabled")
        private val KEY_ALLOWED_APP_PACKAGES = stringSetPreferencesKey("allowed_app_packages")
    }
}
