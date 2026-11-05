/**
 * ThemePreferences.kt
 *
 * Preference store for managing and persisting the application's UI theme mode (SYSTEM, LIGHT, DARK).
 * Uses Jetpack DataStore to store theme choices across app restarts.
 */
package com.maryhairsalon.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "theme_prefs")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Persists the user's manual light/dark choice from Settings. Defaults to SYSTEM until they pick one. */
class ThemePreferences(private val context: Context) {
    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeMode: Flow<ThemeMode> = context.themeDataStore.data.map { prefs ->
        ThemeMode.entries.find { it.name == prefs[themeModeKey] } ?: ThemeMode.SYSTEM
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.themeDataStore.edit { it[themeModeKey] = mode.name }
    }
}
