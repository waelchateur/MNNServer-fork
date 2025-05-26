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
import android.provider.Settings
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
import io.kindbrave.mnnserver.utils.ServiceUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.core.net.toUri
import com.elvishew.xlog.XLog

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmService: LLMService
) : ViewModel() {

    private val tag = MainViewModel::class.java.simpleName
    private val settingsRepository = SettingsRepository(context)

    private val intent = Intent(context, WebServerService::class.java)

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
        viewModelScope.launch {
            llmService.loadedModelsState.collect { models ->
                _loadedModelsCount.value = models.size
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
        viewModelScope.launch {
            _serverPort.emit(settingsRepository.getServerPort())
            context.startService(intent)
            _serverStatus.emit(WebServerService.ServerStatus.Running)
        }
    }

    fun stopServer() {
        viewModelScope.launch {
            webServerService?.stopServer()
            context.stopService(intent)
            _serverStatus.emit(WebServerService.ServerStatus.Stopped)
        }
    }

    private fun checkServiceStatus() {
        viewModelScope.launch {
            val isRunning = ServiceUtils.isServiceRunning(context, WebServerService::class.java)
            if (isRunning) {
                _serverStatus.emit(WebServerService.ServerStatus.Running)
                _isServiceRunning.emit(true)
            } else {
                _serverStatus.emit(WebServerService.ServerStatus.Stopped)
                _isServiceRunning.emit(false)
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