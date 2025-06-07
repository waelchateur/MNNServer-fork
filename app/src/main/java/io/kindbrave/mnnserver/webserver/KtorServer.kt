package io.kindbrave.mnnserver.webserver

import io.kindbrave.mnnserver.repository.SettingsRepository
import io.kindbrave.mnnserver.webserver.config.configureAuthentication
import io.kindbrave.mnnserver.webserver.config.configureCORS
import io.kindbrave.mnnserver.webserver.config.configureLogging
import io.kindbrave.mnnserver.webserver.config.configureRouting
import io.kindbrave.mnnserver.webserver.config.configureSerialization
import io.kindbrave.mnnserver.webserver.config.configureStatusPages
import io.kindbrave.mnnserver.webserver.config.configureWebSockets
import io.ktor.client.plugins.HttpTimeout
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KtorServer @Inject constructor(
    private val mnnHandler: MNNHandler,
    private val settingsRepository: SettingsRepository
) {
    val serverScope = CoroutineScope(Dispatchers.IO)
    var port = 8080

    private var serverJob: kotlinx.coroutines.Job? = null

    suspend fun start() {
        if (serverJob?.isActive == true) return

        val exportWebPort = settingsRepository.getExportWebPort()

        serverJob = serverScope.launch {
            embeddedServer(Netty, port, host = if (exportWebPort) "0.0.0.0" else "localhost") {
                configureSerialization()
                configureAuthentication()
                configureCORS()
                configureLogging()
                configureStatusPages()
                configureWebSockets()
                configureRouting(mnnHandler)
            }.start(true)
        }
    }

    fun stop() {
        serverJob?.cancel()
    }
}