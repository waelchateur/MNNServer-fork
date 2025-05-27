package io.kindbrave.mnnserver.webserver.route

import com.elvishew.xlog.XLog
import io.kindbrave.mnnserver.webserver.MNNHandler
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.options
import io.ktor.server.routing.post

private const val tag = "CompletionsRoutes"

fun Route.completionsRoutes(mnnHandler: MNNHandler) {
    post("/chat/completions") {
        runCatching {
            call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                runCatching {
                    mnnHandler.completions(call.receiveText(), this)
                }.onFailure { e ->
                    XLog.tag(tag).e("completions:onFailure:$e")
                    call.response.status(HttpStatusCode(500, e.message ?: "Unknown error"))
                }
            }
        }.onFailure { e ->
            call.response.status(HttpStatusCode(500, e.message ?: "Unknown error"))
        }
    }
    options("/chat/completions") {
        call.respond(HttpStatusCode.OK, null)
    }
}