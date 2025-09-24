package com.example.xiaomiwallet.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val LICENSE_KEY = stringPreferencesKey("license_key")
        val PUSH_PLUS_TOKEN = stringPreferencesKey("push_plus_token")
        val AUTO_RUN_ENABLED = booleanPreferencesKey("auto_run_enabled")
    }

    val licenseKeyFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.LICENSE_KEY] ?: ""
        }

    val pushPlusTokenFlow: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.PUSH_PLUS_TOKEN] ?: ""
        }

    val autoRunEnabledFlow: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_RUN_ENABLED] ?: false
        }

    suspend fun saveSettings(licenseKey: String, pushPlusToken: String, autoRunEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.LICENSE_KEY] = licenseKey
            settings[PreferencesKeys.PUSH_PLUS_TOKEN] = pushPlusToken
            settings[PreferencesKeys.AUTO_RUN_ENABLED] = autoRunEnabled
        }
    }

    suspend fun saveLicenseKey(licenseKey: String) {
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.LICENSE_KEY] = licenseKey
        }
    }

    suspend fun savePushPlusToken(pushPlusToken: String) {
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.PUSH_PLUS_TOKEN] = pushPlusToken
        }
    }

    suspend fun saveAutoRunEnabled(autoRunEnabled: Boolean) {
        context.dataStore.edit { settings ->
            settings[PreferencesKeys.AUTO_RUN_ENABLED] = autoRunEnabled
        }
    }
}
