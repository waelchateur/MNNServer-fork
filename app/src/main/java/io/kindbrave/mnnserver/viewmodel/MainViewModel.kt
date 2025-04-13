package io.kindbrave.mnnserver.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Environment
import android.os.IBinder
import android.os.StatFs
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.kindbrave.mnnserver.data.LogRepository
import io.kindbrave.mnnserver.data.SettingsRepository
import io.kindbrave.mnnserver.model.ModelManager
import io.kindbrave.mnnserver.service.LLMService
import io.kindbrave.mnnserver.service.WebServerService
import io.kindbrave.mnnserver.ui.screens.DeviceInfo
import io.kindbrave.mnnserver.utils.LogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    private val modelManager = ModelManager(context)
    private val llmService = LLMService.getInstance()
    private val settingsRepository = SettingsRepository(application)
    private val logRepository = LogRepository(application)
    
    // WebServerService 绑定
    private var webServerService: WebServerService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebServerService.LocalBinder
            webServerService = binder.getService()
            
            // 监听服务状态变化
            viewModelScope.launch {
                webServerService?.serverStatus?.collect { status ->
                    _serverStatus.value = status
                    _isServiceRunning.value = status is WebServerService.ServerStatus.Running
                }
            }
            
            // 监听日志变化
            viewModelScope.launch {
                LogManager.logs.collect { logList ->
                    _logs.value = logList
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            webServerService = null
            _serverStatus.value = WebServerService.ServerStatus.Stopped
            _isServiceRunning.value = false
        }
    }
    
    // UI状态
    private val _serverStatus = MutableStateFlow<WebServerService.ServerStatus>(WebServerService.ServerStatus.Stopped)
    val serverStatus: StateFlow<WebServerService.ServerStatus> = _serverStatus.asStateFlow()
    
    private val _modelList = MutableStateFlow<List<ModelManager.ModelInfo>>(emptyList())
    val modelList: StateFlow<List<ModelManager.ModelInfo>> = _modelList.asStateFlow()

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()
    
    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()
    
    // 对话框状态
    val showModelNameDialog = mutableStateOf(false)
    val modelNameInput = mutableStateOf("")
    private var pendingModelUri: Uri? = null
    
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()
    
    private val _loadedModelsCount = MutableStateFlow(0)
    val loadedModelsCount: StateFlow<Int> = _loadedModelsCount.asStateFlow()
    
    private val _deviceInfo = MutableStateFlow(DeviceInfo(0, 0, 0, 0))
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()
    
    init {
        // 监听模型列表变化
        viewModelScope.launch {
            modelManager.modelList.collect { models ->
                _modelList.value = models
                _loadedModelsCount.value = models.count { it.isLoaded }
            }
        }

        viewModelScope.launch {
            settingsRepository.serverPort.collect { port ->
                _serverPort.value = port
            }
        }

        // 绑定WebServerService
        bindService()
        
        checkServiceStatus()
        
        updateSystemInfo()
    }
    
    private fun bindService() {
        val intent = Intent(context, WebServerService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    private fun unbindService() {
        context.unbindService(serviceConnection)
    }
    
    fun startServer() {
        viewModelScope.launch {
            WebServerService.startService(context, _serverPort.value)
        }
    }
    
    fun stopServer() {
        viewModelScope.launch {
            WebServerService.stopService(context)
        }
    }
    
    fun showModelNameDialog(uri: Uri) {
        pendingModelUri = uri
        modelNameInput.value = ""
        showModelNameDialog.value = true
    }
    
    fun importModel() {
        // modelManager.importModel()
    }
    
    fun importModelWithName(name: String) {
        viewModelScope.launch {
            //modelManager.importModelWithName(name)
            logRepository.addLog("Model imported: $name")
        }
    }
    
    fun loadModel(model: ModelManager.ModelInfo) {
        // modelManager.loadModel(model)
    }
    
    fun unloadModel(model: ModelManager.ModelInfo) {
        // modelManager.unloadModel(model)
    }
    
    fun deleteModel(model: ModelManager.ModelInfo) {
        // modelManager.deleteModel(model)
    }
    
    fun addLog(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val logMessage = "[$timestamp] $message"
        LogManager.addLog(logMessage)
    }
    
    fun clearLogs() {
        viewModelScope.launch {
            LogManager.clearLogs()
        }
    }
    
    private fun checkServiceStatus() {
        viewModelScope.launch {
            _isServiceRunning.value = _serverStatus.value is WebServerService.ServerStatus.Running
        }
    }
    
    fun toggleService() {
        viewModelScope.launch {
            if (_isServiceRunning.value) {
                stopService()
            } else {
                startService()
            }
        }
    }
    
    private suspend fun startService() {
        val port = _serverPort.value
        startServer()
        logRepository.addLog("Service started on port $port")
    }
    
    private suspend fun stopService() {
        stopServer()
        logRepository.addLog("Service stopped")
    }
    
    private fun updateSystemInfo() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            
            // Get memory info
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)
            
            val availableMemory = memoryInfo.availMem / (1024 * 1024) // Convert to MB
            val totalMemory = memoryInfo.totalMem / (1024 * 1024) // Convert to MB
            
            // Get storage info
            val stat = StatFs(Environment.getDataDirectory().path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            
            val totalStorage = (totalBlocks * blockSize) / (1024 * 1024 * 1024) // Convert to GB
            val availableStorage = (availableBlocks * blockSize) / (1024 * 1024 * 1024) // Convert to GB
            
            _deviceInfo.value = DeviceInfo(
                availableMemory = availableMemory,
                totalMemory = totalMemory,
                availableStorage = availableStorage,
                totalStorage = totalStorage
            )
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        unbindService()
    }
}