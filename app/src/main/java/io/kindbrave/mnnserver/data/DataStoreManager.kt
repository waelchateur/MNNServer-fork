package io.kindbrave.mnnserver.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mnn_server_preferences")

class DataStoreManager(private val context: Context) {
    companion object {
        private val PORT_KEY = intPreferencesKey("server_port")
        private const val DEFAULT_PORT = 8080
    }

    val port: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[PORT_KEY] ?: DEFAULT_PORT
        }

    suspend fun updatePort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[PORT_KEY] = port
        }
    }
} 