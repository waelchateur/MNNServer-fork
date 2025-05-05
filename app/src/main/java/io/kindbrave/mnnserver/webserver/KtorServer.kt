package io.kindbrave.mnnserver.webserver

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

class KtorServer(port: Int) {
    private val mnnHandler = MNNHandler()
    private val server by lazy {
        embeddedServer(Netty, port) {
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
                        call.response.status(HttpStatusCode(400, e.message ?: "Unknown error"))
                    }
                }
            }
        }
    }

    fun start() {
        CoroutineScope(Dispatchers.IO).launch { server.start(true) }
    }

    fun stop() {
        server.stop(1000, 1000)
    }
}