// En: com/example/juego/ui/utils/SettingsManager.kt
package com.example.juego.ui.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Crea el DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(context: Context) {

    private val dataStore = context.dataStore

    companion object {
        val PREFERRED_SAVE_FORMAT = stringPreferencesKey("preferred_save_format")
    }

    // Flujo para LEER la preferencia
    val preferredFormatFlow: Flow<SaveFormat> = dataStore.data.map { preferences ->
        val formatName = preferences[PREFERRED_SAVE_FORMAT] ?: SaveFormat.JSON.name
        SaveFormat.valueOf(formatName)
    }

    // Función para GUARDAR la preferencia
    suspend fun setPreferredFormat(format: SaveFormat) {
        dataStore.edit { preferences ->
            preferences[PREFERRED_SAVE_FORMAT] = format.name
        }
    }
}