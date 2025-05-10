package io.kindbrave.mnnserver.ui.screens.log

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.kindbrave.mnnserver.data.LogRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LogsViewModel(application: Application) : AndroidViewModel(application) {
    private val logRepository = LogRepository(application)

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            logRepository.logs.collect { logList ->
                _logs.value = logList
            }
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            logRepository.clearLogs()
        }
    }
}