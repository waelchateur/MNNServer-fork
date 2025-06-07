package io.kindbrave.mnnserver.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import dagger.hilt.android.AndroidEntryPoint
import io.kindbrave.mnn.webserver.service.LLMService
import javax.inject.Inject

@AndroidEntryPoint
class AistService : Service() {

    @Inject lateinit var llmService: LLMService

    override fun onBind(intent: Intent?): IBinder? {
        return AistBinder()
    }

    inner class AistBinder : IAistService.Stub() {
        override fun startWebService() {
            val intent = Intent(this@AistService, WebServerService::class.java)
            startService(intent)
        }

        override fun loadModel(modelId: String?) {

        }
    }
}