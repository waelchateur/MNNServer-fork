package io.kindbrave.mnnserver.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

object LogManager {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    fun addLog(message: String) {
        CoroutineScope(Dispatchers.Default).launch {
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                .format(Date())
            val logMessage = "[$timestamp] $message"
            _logs.value = _logs.value + logMessage
        }
    }

    fun getLatestLogs(count: Int = 10): List<String> {
        return _logs.value.takeLast(count)
    }

    fun clearLogs() {
        _logs.value = emptyList()
    }
}