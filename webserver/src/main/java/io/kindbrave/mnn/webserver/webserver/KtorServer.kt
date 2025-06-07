package io.kindbrave.mnn.webserver.webserver

import io.kindbrave.mnn.webserver.webserver.config.configureAuthentication
import io.kindbrave.mnn.webserver.webserver.config.configureCORS
import io.kindbrave.mnn.webserver.webserver.config.configureLogging
import io.kindbrave.mnn.webserver.webserver.config.configureRouting
import io.kindbrave.mnn.webserver.webserver.config.configureSerialization
import io.kindbrave.mnn.webserver.webserver.config.configureStatusPages
import io.kindbrave.mnn.webserver.webserver.config.configureWebSockets
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KtorServer @Inject constructor(
    private val mnnHandler: MNNHandler
) {
    val serverScope = CoroutineScope(Dispatchers.IO)
    var port = 8080

    private var serverJob: kotlinx.coroutines.Job? = null

    suspend fun start(exportWebPort: Boolean = false) {
        if (serverJob?.isActive == true) return

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