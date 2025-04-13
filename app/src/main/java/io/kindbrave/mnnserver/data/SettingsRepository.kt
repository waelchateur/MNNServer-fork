package io.kindbrave.mnnserver.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    companion object {
        private val SERVER_PORT_KEY = intPreferencesKey("server_port")
        private const val DEFAULT_SERVER_PORT = 8080
    }
    
    val serverPort: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[SERVER_PORT_KEY] ?: DEFAULT_SERVER_PORT
        }
    
    suspend fun setServerPort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_PORT_KEY] = port
        }
    }
    
    suspend fun getServerPort(): Int {
        return context.dataStore.data.map { preferences ->
            preferences[SERVER_PORT_KEY] ?: DEFAULT_SERVER_PORT
        }.first()
    }
} 