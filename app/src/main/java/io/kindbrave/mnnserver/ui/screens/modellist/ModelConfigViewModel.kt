package io.kindbrave.mnnserver.ui.screens.modellist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.kindbrave.mnnserver.utils.FileUtils
import io.kindbrave.mnnserver.utils.ModelConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ModelConfigViewModel(private val application: Application): AndroidViewModel(application) {

    private val _modelConfig: MutableStateFlow<ModelConfig> = MutableStateFlow(DEFAULT_CONFIG)
    val modelConfig: StateFlow<ModelConfig> = _modelConfig
    private var modelId = ""

    private fun getModelSettingsFile(modelId: String): String {
        this.modelId = modelId
        return FileUtils.getModelConfigDir(modelId) + "/custom_config.json"
    }

    fun initModelConfig(modelId: String, modelPath: String) {
        val originalModelSettingsFile = "$modelPath/config.json"
        val modelSettingsFile = getModelSettingsFile(modelId)
        ModelConfig.loadConfig(originalModelSettingsFile, modelSettingsFile)?.let {
            _modelConfig.value = it
        }
    }

    fun setThinkingMode(thinking: Boolean) {
        _modelConfig.value = _modelConfig.value.copy(thinkingMode = thinking)
        saveModelConfig()
    }

    fun saveModelConfig() {
        val modelSettingsFile = getModelSettingsFile(modelId)
        ModelConfig.saveConfig(modelSettingsFile, _modelConfig.value)
    }

    companion object {
        private val DEFAULT_CONFIG = ModelConfig(
            llmModel = null,
            llmWeight = null,
            backendType = "",
            threadNum = 0,
            precision = "",
            memory = "",
            systemPrompt = "You are a helpful assistant.",
            samplerType = "",
            mixedSamplers = mutableListOf(),
            temperature = 0.0f,
            topP = 0.9f,
            topK = 0,
            minP = 0.0f,
            tfsZ = 1.0f,
            typical = 1.0f,
            penalty = 1.5f,
            nGram = 8,
            nGramFactor = 1.02f,
            maxNewTokens = 2048,
            assistantPromptTemplate = null,
            thinkingMode = true,
        )
    }
}