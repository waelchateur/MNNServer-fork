package io.kindbrave.mnnserver.ui.screens.main

import android.app.ActivityManager
import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Environment
import android.os.IBinder
import android.os.StatFs
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnnserver.data.LogRepository
import io.kindbrave.mnnserver.repository.model.UserUploadModelRepository
import io.kindbrave.mnnserver.repository.SettingsRepository
import io.kindbrave.mnnserver.service.LLMService
import io.kindbrave.mnnserver.service.WebServerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmService: LLMService
) : ViewModel() {

    private val tag = MainViewModel::class.java.simpleName
    private val settingsRepository = SettingsRepository(context)
    private val logRepository = LogRepository(context)

    private val _serverStatus =
        MutableStateFlow<WebServerService.ServerStatus>(WebServerService.ServerStatus.Stopped)
    val serverStatus: StateFlow<WebServerService.ServerStatus> = _serverStatus.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)

    private val _loadedModelsCount = MutableStateFlow(0)
    val loadedModelsCount: StateFlow<Int> = _loadedModelsCount.asStateFlow()

    private val _deviceInfo = MutableStateFlow(DeviceInfo(0, 0, 0, 0))
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

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
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            webServerService = null
            _serverStatus.value = WebServerService.ServerStatus.Stopped
            _isServiceRunning.value = false
        }
    }

    init {
        // 监听模型列表变化
        viewModelScope.launch {
            llmService.loadedModelsState.collect { models ->
                _loadedModelsCount.value = models.size
            }
        }

        viewModelScope.launch {
            _serverPort.value = settingsRepository.getServerPort()
        }

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
            WebServerService.Companion.startService(context, _serverPort.value)
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            WebServerService.Companion.stopService(context)
        }
    }

    private fun checkServiceStatus() {
        viewModelScope.launch {
            _isServiceRunning.value = _serverStatus.value is WebServerService.ServerStatus.Running
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
            // Get memory info
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
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