package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.arkSettingsDataStore by preferencesDataStore(name = "ark_social_prefs")

class ThemePreferences(private val context: Context) {
    private val darkModeKey = booleanPreferencesKey("dark_mode_enabled")

    val darkModeFlow: Flow<Boolean?> = context.arkSettingsDataStore.data.map { prefs ->
        if (prefs.contains(darkModeKey)) prefs[darkModeKey] else null
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.arkSettingsDataStore.edit { prefs ->
            prefs[darkModeKey] = enabled
        }
    }
}
