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
        const val LOG_RESET_MARKER = "=== LOG RESET ===\n"
        private const val LOG_FILE_NAME = "mnn_server_logs.txt"
        private const val MAX_LOG_SIZE = 1024 * 1024 // 1MB
    }
    
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: Flow<List<String>> = _logs.asStateFlow()
    
    private val logFile: File
        get() = File(context.filesDir, LOG_FILE_NAME)
    
    init {
        loadLogs()
    }
    
    private fun loadLogs() {
        if (logFile.exists()) {
            val logContent = logFile.readText()
            _logs.value = logContent.lines().filter { it.isNotBlank() }
        }
    }
    
    suspend fun addLog(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            .format(Date())
        val logMessage = "[$timestamp] $message"
        
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(logMessage)
        
        // 限制日志大小
        if (currentLogs.size > MAX_LOG_SIZE) {
            currentLogs.removeAt(0)
        }
        
        _logs.value = currentLogs
        
        // 写入文件
        logFile.writeText(currentLogs.joinToString("\n"))
    }
    
    suspend fun clearLogs() {
        _logs.value = emptyList()
        logFile.writeText(LOG_RESET_MARKER)
    }
} 