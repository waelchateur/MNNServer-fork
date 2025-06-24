package io.kindbrave.mnn.mnnui.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.alibaba.mls.api.ModelItem
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnn.mnnui.utils.ModelUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ConfigRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private val LAST_RUNNING_MODELS_KEY = stringSetPreferencesKey("last_running_models")
    }

    suspend fun getLastRunningModels(): List<ModelItem> {
        return context.dataStore.data.map { preferences ->
            val runningSet = preferences[LAST_RUNNING_MODELS_KEY] ?: emptySet()
            runningSet.map { it.split("#$%") }.filter { it.size == 2 }.map {
                ModelItem().apply {
                    modelId = it[0]
                    addTag(it[1])
                }
            }
        }.first()
    }

    suspend fun setLastRunningModels(models: Map<String, ModelItem>) {
        val runningSet = models.map {
            "${it.key}#$%${ModelUtils.getModelTag(it.value)}"
        }.toSet()
        context.dataStore.edit { preferences ->
            preferences[LAST_RUNNING_MODELS_KEY] = runningSet
        }
    }
}