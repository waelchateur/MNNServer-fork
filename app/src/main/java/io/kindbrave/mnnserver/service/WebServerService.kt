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
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.api.ApiHandler
import io.kindbrave.mnnserver.utils.LogManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.response.Response
import java.io.IOException

class WebServerService : Service() {
    
    private val TAG = "WebServerService"
    private val NOTIFICATION_ID = 1001
    private val CHANNEL_ID = "mnn_server_channel"
    
    private var webServer: MNNWebServer? = null
    private val binder = LocalBinder()
    private lateinit var llmService: LLMService
    private lateinit var apiHandler: ApiHandler
    
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
        llmService = LLMService.getInstance()
        apiHandler = ApiHandler(llmService)
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
        if (webServer != null) {
            addLog("服务已经在运行，端口: $port")
            return
        }
        
        try {
            webServer = MNNWebServer(port)
            webServer?.start()
            
            _serverStatus.value = ServerStatus.Running(port)
            addLog("服务已启动，端口: $port")
            
            startForeground(NOTIFICATION_ID, createNotification("服务运行中，端口: $port"))
        } catch (e: IOException) {
            _serverStatus.value = ServerStatus.Error("启动服务失败: ${e.message}")
            addLog("启动服务失败: ${e.message}")
            Log.e(TAG, "启动服务失败", e)
        }
    }
    
    fun stopServer() {
        webServer?.stop()
        webServer = null
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
        data class Running(val port: Int) : ServerStatus()
        data class Error(val message: String) : ServerStatus()
    }
    
    private inner class MNNWebServer(port: Int) : NanoHTTPD(port) {
        override fun serve(session: IHTTPSession): Response {
            val uri = session.uri
            val method = session.method

            Log.e(TAG, "收到请求: $method $uri")
            addLog("收到请求: $method $uri")
            
            // 使用 ApiHandler 处理 API 请求
            if (uri.startsWith("/v1")) {
                return apiHandler.handleRequest(session)
            }
            
            // 处理非 API 请求
            return Response.newFixedLengthResponse("MNN Server is running")
        }
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