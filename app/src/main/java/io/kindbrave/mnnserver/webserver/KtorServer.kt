package io.kindbrave.mnnserver.webserver

import com.elvishew.xlog.XLog
import io.kindbrave.mnnserver.repository.SettingsRepository
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.request.receiveText
import io.ktor.server.response.cacheControl
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KtorServer @Inject constructor(
    private val mnnHandler: MNNHandler,
    private val settingsRepository: SettingsRepository
) {
    private val tag = KtorServer::class.simpleName
    var port = 8080

    private val server by lazy {
        val exportWebPort = runBlocking {
            settingsRepository.getExportWebPort()
        }
        embeddedServer(Netty, port, host = if (exportWebPort) "0.0.0.0" else "localhost") {
            install(CallLogging)

            routing {
                get("/") {
                    call.respondText("MNN Server is running")
                }
                get("/v1/models") {
                    call.respondText(mnnHandler.getModels().toString())
                }
                post("/v1/chat/completions") {
                    call.response.cacheControl(CacheControl.NoCache(null))
                    runCatching {
                        call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                            mnnHandler.completions(call.receiveText(), this)
                        }
                    }.onFailure { e ->
                        call.response.status(HttpStatusCode(500, e.message ?: "Unknown error"))
                    }
                }
                post("/v1/embeddings") {
                    runCatching {
                        call.respondText(mnnHandler.embeddings(call.receiveText()).toString())
                    }.onFailure {
                        call.response.status(HttpStatusCode(500, it.message ?: "Unknown error"))
                    }
                }
            }
        }
    }

    fun start() {
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                server.start(true)
            }.onFailure { e ->
                XLog.tag(tag).e("start:onFailure:$e")
            }
        }
    }

    fun stop() {
        server.stop(1000, 1000)
    }
}