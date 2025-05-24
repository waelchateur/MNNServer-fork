package io.kindbrave.mnnserver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.elvishew.xlog.XLog
import dagger.hilt.android.AndroidEntryPoint
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.annotation.LogAfter
import io.kindbrave.mnnserver.di.ApplicationScope
import io.kindbrave.mnnserver.repository.SettingsRepository
import io.kindbrave.mnnserver.webserver.KtorServer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WebServerService : Service() {
    private val tag = WebServerService::class.simpleName
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "mnn_server_channel"

    @Inject lateinit var server: KtorServer
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject
    @ApplicationScope
    lateinit var scope: CoroutineScope

    private val binder = LocalBinder()
    
    private val _serverStatus = MutableStateFlow<ServerStatus>(ServerStatus.Stopped)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus
    
    inner class LocalBinder : Binder() {
        fun getService(): WebServerService = this@WebServerService
    }
    
    override fun onBind(intent: Intent): IBinder {
        return binder
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startServer()
        return START_NOT_STICKY
    }
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MNN Server Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }
    
    private fun createNotification(status: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MNN Server")
            .setContentText(status)
            .setSmallIcon(R.drawable.ic_notification)
            .build()
    }

    @LogAfter("")
    private fun startServer() {
        runCatching {
            scope.launch(Dispatchers.IO) {
                val port = settingsRepository.getServerPort()
                launch(Dispatchers.Default) {
                    server.port = port
                    server.start()
                    _serverStatus.value = ServerStatus.Running
                    startForeground(NOTIFICATION_ID, createNotification("服务运行中，端口: $port"))
                }
            }
        }.onFailure { e ->
            _serverStatus.value = ServerStatus.Error("启动服务失败: ${e.message}")
            XLog.tag(tag).e("startServer=>error", e)
        }
    }
    
    fun stopServer() {
        server.stop()
        _serverStatus.value = ServerStatus.Stopped
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    override fun onDestroy() {
        stopServer()
        super.onDestroy()
    }
    
    sealed class ServerStatus {
        object Stopped : ServerStatus()
        object Running : ServerStatus()
        data class Error(val message: String) : ServerStatus()
    }
}