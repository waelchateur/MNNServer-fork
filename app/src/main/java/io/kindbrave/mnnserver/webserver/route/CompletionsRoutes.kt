package io.kindbrave.mnnserver.webserver.route

import com.elvishew.xlog.XLog
import io.kindbrave.mnnserver.webserver.MNNHandler
import io.kindbrave.mnnserver.webserver.request.ChatGenerateRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.options
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json
import org.json.JSONObject

private const val tag = "CompletionsRoutes"

fun Route.completionsRoutes(mnnHandler: MNNHandler) {
    post("/chat/completions") {
        runCatching {
            val textBody = call.receiveText()
            XLog.tag(tag).d("completionsRoutes:request:\n$textBody")
            val body = Json.decodeFromString(ChatGenerateRequest.serializer(), textBody)
            if (body.stream) {
                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                    runCatching {
                        mnnHandler.completionsStreaming(body, this)
                    }.onFailure { e ->
                        XLog.tag(tag).e("completions:onFailure:$e", e)
                        call.response.status(HttpStatusCode(500, e.message ?: "Unknown error"))
                    }
                }
            } else {
                call.respondText(mnnHandler.completions(body).toString())
            }
        }.onFailure { e ->
            XLog.tag(tag).e("completionsRoutes:onFailure:$e", e)
            call.response.status(HttpStatusCode(500, e.message ?: "Unknown error"))
        }
    }
    options("/chat/completions") {
        call.respond(HttpStatusCode.OK, null)
    }
}