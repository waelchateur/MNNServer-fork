package io.kindbrave.mnnserver.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.kindbrave.mnnserver.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepository = SettingsRepository(application)

    private val _serverPort = MutableStateFlow(0)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _exportWebPort = MutableStateFlow(false)
    val exportWebPort: StateFlow<Boolean> = _exportWebPort.asStateFlow()

    private val _startLastRunningModels = MutableStateFlow(false)
    val startLastRunningModels: StateFlow<Boolean> = _startLastRunningModels.asStateFlow()

    init {
        viewModelScope.launch {
            _serverPort.emit(settingsRepository.getServerPort())
            _exportWebPort.emit(settingsRepository.getExportWebPort())
            _startLastRunningModels.emit(settingsRepository.getStartLastRunningModels())
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
}