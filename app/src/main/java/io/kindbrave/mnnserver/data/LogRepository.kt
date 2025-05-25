package io.kindbrave.mnnserver.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogRepository(private val context: Context) {
    companion object {
        private const val LOG_FILE_NAME = "log.txt"
    }
    
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: Flow<List<String>> = _logs.asStateFlow()
    
    private val logFile: File
        get() = File("${context.filesDir}/logs", LOG_FILE_NAME)
    
    init {
        loadLogs()
    }
    
    private fun loadLogs() {
        if (logFile.exists()) {
            val logContent = logFile.readText()
            _logs.value = logContent.lines().filter { it.isNotBlank() }
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        logFile.writeText("")
    }
} 