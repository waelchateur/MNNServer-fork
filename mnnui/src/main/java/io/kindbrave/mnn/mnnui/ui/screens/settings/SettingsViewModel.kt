package io.kindbrave.mnn.mnnui.ui.screens.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnn.mnnui.repository.SettingsRepository
import io.kindbrave.mnn.server.engine.TTSSession
import io.kindbrave.mnn.server.service.LLMService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext context: Context,
    private val llmService: LLMService
) : ViewModel() {
    private val settingsRepository = SettingsRepository(context)

    private val _serverPort = MutableStateFlow(0)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _exportWebPort = MutableStateFlow(false)
    val exportWebPort: StateFlow<Boolean> = _exportWebPort.asStateFlow()

    private val _startLastRunningModels = MutableStateFlow(false)
    val startLastRunningModels: StateFlow<Boolean> = _startLastRunningModels.asStateFlow()

    private val _defaultTTSModelId = MutableStateFlow("")
    val defaultTTSModelId: StateFlow<String> = _defaultTTSModelId.asStateFlow()

    init {
        viewModelScope.launch {
            _serverPort.emit(settingsRepository.getServerPort())
            _exportWebPort.emit(settingsRepository.getExportWebPort())
            _startLastRunningModels.emit(settingsRepository.getStartLastRunningModels())
            _defaultTTSModelId.emit(settingsRepository.getDefaultTTSModelId())
        }
    }

    fun updateServerPort(port: Int) {
        viewModelScope.launch {
            settingsRepository.setServerPort(port)
            _serverPort.value = port
        }
    }

    fun getExportWebPort() {
        viewModelScope.launch {
            _exportWebPort.emit(settingsRepository.getExportWebPort())
        }
    }

    fun setExportWebPort(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setExportWebPort(enable)
            _exportWebPort.emit(enable)
        }
    }

    fun setStartLastRunningModels(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setStartLastRunningModels(enable)
            _startLastRunningModels.emit(enable)
        }
    }

    fun setDefaultTTSModelId(modelId: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultTTSModelId(modelId)
            _defaultTTSModelId.emit(modelId)
        }
    }

    fun getAllTTSSession(): List<TTSSession> {
        return llmService.getAllTTSSessions()
    }
}