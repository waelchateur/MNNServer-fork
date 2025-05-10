package io.kindbrave.mnnserver.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.alibaba.mls.api.source.ModelSources
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnnserver.utils.DiffusionMemoryMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val SERVER_PORT_KEY = intPreferencesKey("server_port")
        private val DIFFUSION_MEMORY_MODE_KEY = stringPreferencesKey("diffusion_memory_mode")
        private val DOWNLOAD_PROVIDER_KEY = stringPreferencesKey("download_provider")
        private const val DEFAULT_SERVER_PORT = 8080
    }

    suspend fun setServerPort(port: Int) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_PORT_KEY] = port
        }
    }

    suspend fun getServerPort() = context.dataStore.data.map { preferences ->
            preferences[SERVER_PORT_KEY] ?: DEFAULT_SERVER_PORT
        }.first()

    suspend fun setDiffusionMemoryMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[DIFFUSION_MEMORY_MODE_KEY] = mode
        }
    }

    suspend fun getDiffusionMemoryMode() = context.dataStore.data.map { preferences ->
            preferences[DIFFUSION_MEMORY_MODE_KEY] ?: DiffusionMemoryMode.MEMORY_MODE_SAVING.value
        }.first()

    suspend fun setDownloadProvider(provider: String) {
        context.dataStore.edit { preferences ->
            preferences[DOWNLOAD_PROVIDER_KEY] = provider
        }
    }

    suspend fun getDownloadProvider() = context.dataStore.data.map { preferences ->
            preferences[DOWNLOAD_PROVIDER_KEY] ?: "ModelScope"
        }.first()
}