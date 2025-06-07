package io.kindbrave.mnnserver.ui.screens.list

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.kindbrave.mnnserver.utils.FileUtils
import io.kindbrave.mnnserver.utils.ModelConfig
import io.kindbrave.mnnserver.utils.ModelUtils
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ModelConfigViewModel @Inject constructor(): ViewModel() {

    private val _modelConfig: MutableStateFlow<ModelConfig> = MutableStateFlow(DEFAULT_CONFIG)
    private var modelId = ""

    val thinkMode = mutableStateOf(false)
    val mmapEnabled = mutableStateOf(false)
    val backend = mutableStateOf("cpu")

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
        updateConfig()
    }

    fun isNeedShowThinkConfig(): Boolean {
        return ModelUtils.isNeedConfigThinkMode(modelId)
    }

    private fun updateConfig() {
        thinkMode.value = _modelConfig.value.thinkingMode == true
        mmapEnabled.value = _modelConfig.value.mmap == true
        backend.value = _modelConfig.value.backendType ?: "cpu"
    }

    fun setThinkingMode(thinking: Boolean) {
        thinkMode.value = thinking
        _modelConfig.value = _modelConfig.value.copy(thinkingMode = thinking)
        saveModelConfig()
    }

    fun setMMap(mmap: Boolean) {
        mmapEnabled.value = mmap
        _modelConfig.value = _modelConfig.value.copy(mmap = mmap)
        saveModelConfig()
    }

    fun setBackend(backend: String) {
        this.backend.value = backend
        _modelConfig.value = _modelConfig.value.copy(backendType = backend)
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
            thinkingMode = false,
            mmap = false
        )
    }
}