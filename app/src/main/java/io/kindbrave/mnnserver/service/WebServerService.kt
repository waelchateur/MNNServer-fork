package io.kindbrave.mnnserver.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.utils.LogManager
import io.kindbrave.mnnserver.webserver.KtorServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class WebServerService : Service() {
    private val TAG = "WebServerService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "mnn_server_channel"

    @Inject lateinit var server: KtorServer

    private val binder = LocalBinder()
    
    private val _serverStatus = MutableStateFlow<ServerStatus>(ServerStatus.Stopped)
    val serverStatus: StateFlow<ServerStatus> = _serverStatus
    
    // 修改addLog方法
    private fun addLog(message: String) {
        LogManager.addLog(message)
    }
    
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
        when (intent?.action) {
            ACTION_START_SERVER -> {
                val port = intent.getIntExtra(EXTRA_PORT, DEFAULT_PORT)
                startServer(port)
            }
            ACTION_STOP_SERVER -> {
                stopServer()
            }
        }
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
    
    fun startServer(port: Int) {
        try {
            server.port = port
            server.start()

            _serverStatus.value = ServerStatus.Running
            addLog("服务已启动，端口: $port")

            startForeground(NOTIFICATION_ID, createNotification("服务运行中，端口: $port"))
        } catch (e: IOException) {
            _serverStatus.value = ServerStatus.Error("启动服务失败: ${e.message}")
            addLog("启动服务失败: ${e.message}")
            Log.e(TAG, "启动服务失败", e)
        }
    }
    
    fun stopServer() {
        if (::server.isInitialized) {
            server.stop()
        }
        _serverStatus.value = ServerStatus.Stopped
        addLog("服务已停止")
        stopForeground(true)
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
    
    companion object {
        const val ACTION_START_SERVER = "io.kindbrave.mnnserver.action.START_SERVER"
        const val ACTION_STOP_SERVER = "io.kindbrave.mnnserver.action.STOP_SERVER"
        const val EXTRA_PORT = "io.kindbrave.mnnserver.extra.PORT"
        const val DEFAULT_PORT = 8080
        
        fun startService(context: Context, port: Int) {
            val intent = Intent(context, WebServerService::class.java).apply {
                action = ACTION_START_SERVER
                putExtra(EXTRA_PORT, port)
            }
            context.startService(intent)
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, WebServerService::class.java).apply {
                action = ACTION_STOP_SERVER
            }
            context.startService(intent)
        }
    }
}