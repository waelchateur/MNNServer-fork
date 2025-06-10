package io.kindbrave.mnnserver.ui.screens.main

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Environment
import android.os.IBinder
import android.os.StatFs
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.mls.api.ModelItem
import com.elvishew.xlog.XLog
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnn.webserver.repository.MNNModelRepository
import io.kindbrave.mnn.webserver.service.LLMService
import io.kindbrave.mnnserver.repository.ConfigRepository
import io.kindbrave.mnnserver.repository.SettingsRepository
import io.kindbrave.mnnserver.service.WebServerService
import io.kindbrave.mnnserver.utils.ServiceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmService: LLMService,
    private val settingsRepository: SettingsRepository,
    private val configRepository: ConfigRepository,
    private val mnnModelRepository: MNNModelRepository
) : ViewModel() {

    private val tag = MainViewModel::class.java.simpleName

    private val intent = Intent(context, WebServerService::class.java)

    private val _serverStatus =
        MutableStateFlow<ServerStatus>(ServerStatus.Stopped)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus.asStateFlow()

    private val _serverPort = MutableStateFlow(8080)
    val serverPort: StateFlow<Int> = _serverPort.asStateFlow()

    private val _loadedModels = MutableStateFlow<List<ModelItem>>(emptyList())
    val loadedModels: StateFlow<List<ModelItem>> = _loadedModels.asStateFlow()

    private val _deviceInfo = MutableStateFlow(DeviceInfo(0, 0, 0, 0))
    val deviceInfo: StateFlow<DeviceInfo> = _deviceInfo.asStateFlow()

    private var webServerService: WebServerService? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as WebServerService.LocalBinder
            webServerService = binder.getService()

            viewModelScope.launch {
                webServerService?.serverStatus?.collect { status ->
                    when (status) {
                        is WebServerService.ServerStatus.Error -> _serverStatus.value = ServerStatus.Error(status.message)
                        WebServerService.ServerStatus.Stopped -> _serverStatus.value = ServerStatus.Stopped
                        else -> Unit
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            webServerService = null
            _serverStatus.value = ServerStatus.Stopped
        }
    }

    init {
        viewModelScope.launch {
            llmService.loadedModelsState.collect { models ->
                _loadedModels.value = models.values.toList()
            }
            _serverPort.emit(settingsRepository.getServerPort())
        }
        bindService()
        checkServiceStatus()
        updateSystemInfo()
    }

    private fun bindService() {
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun unbindService() {
        context.unbindService(serviceConnection)
    }

    fun isBatteryOptimizationDisabled(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

   fun requestIgnoreBatteryOptimizations() {
       runCatching {
           val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
               data = "package:${context.packageName}".toUri()
               flags = Intent.FLAG_ACTIVITY_NEW_TASK
           }
           context.startActivity(intent)
       }.onFailure {
           XLog.tag(tag).e("requestIgnoreBatteryOptimizations:onFailure:${it.message}")
       }
    }

    fun startServer() {
        viewModelScope.launch(Dispatchers.IO) {
            _serverStatus.emit(ServerStatus.Starting)
            _serverPort.emit(settingsRepository.getServerPort())
            context.startService(intent)
            startLastRunningModels()
            _serverStatus.emit(ServerStatus.Running)
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            webServerService?.stopServer()
            context.stopService(intent)
            _serverStatus.emit(ServerStatus.Stopped)
        }
    }

    private suspend fun startLastRunningModels() {
        if (settingsRepository.getStartLastRunningModels()) {
            val lastRunningModels = configRepository.getLastRunningModels()
            lastRunningModels.forEach { model ->
                runCatching {
                    mnnModelRepository.loadModel(model)
                }.onFailure { e ->
                    XLog.tag(tag).e("startLastRunningModels:onFailure:${e.message}", e)
                }
            }
        }
    }

    private fun checkServiceStatus() {
        viewModelScope.launch {
            val isRunning = ServiceUtils.isServiceRunning(context, WebServerService::class.java)
            if (isRunning) {
                _serverStatus.emit(ServerStatus.Running)
            } else {
                _serverStatus.emit(ServerStatus.Stopped)
            }
        }
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

sealed class ServerStatus {
    object Stopped : ServerStatus()
    object Starting : ServerStatus()
    object Running : ServerStatus()
    data class Error(val message: String) : ServerStatus()
}