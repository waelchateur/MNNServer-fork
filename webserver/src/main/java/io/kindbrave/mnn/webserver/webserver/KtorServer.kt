package io.kindbrave.mnn.webserver.webserver

import com.elvishew.xlog.XLog
import io.kindbrave.mnn.webserver.webserver.config.configureAuthentication
import io.kindbrave.mnn.webserver.webserver.config.configureCORS
import io.kindbrave.mnn.webserver.webserver.config.configureLogging
import io.kindbrave.mnn.webserver.webserver.config.configureRouting
import io.kindbrave.mnn.webserver.webserver.config.configureSerialization
import io.kindbrave.mnn.webserver.webserver.config.configureStatusPages
import io.kindbrave.mnn.webserver.webserver.config.configureWebSockets
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CompletableDeferred
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

    suspend fun start(exportWebPort: Boolean = false): Result<Unit> {
        if (serverJob?.isActive == true) return Result.success(Unit)

        val result = CompletableDeferred<Result<Unit>>()

        serverJob = serverScope.launch {
            runCatching {
                embeddedServer(Netty, port, host = if (exportWebPort) "0.0.0.0" else "localhost") {
                    configureSerialization()
                    configureAuthentication()
                    configureCORS()
                    configureLogging()
                    configureStatusPages()
                    configureWebSockets()
                    configureRouting(mnnHandler)
                }.start(true)
                result.complete(Result.success(Unit))
            }.onFailure {
                result.complete(Result.failure(it))
            }
        }
        return result.await()
    }

    fun stop() {
        serverJob?.cancel()
    }
}