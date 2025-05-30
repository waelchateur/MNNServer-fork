package io.kindbrave.mnnserver.webserver.route

import com.elvishew.xlog.XLog
import io.kindbrave.mnnserver.webserver.MNNHandler
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.response.respondTextWriter
import io.ktor.server.routing.Route
import io.ktor.server.routing.options
import io.ktor.server.routing.post
import org.json.JSONObject

private const val tag = "CompletionsRoutes"

fun Route.completionsRoutes(mnnHandler: MNNHandler) {
    post("/chat/completions") {
        runCatching {
            val body = call.receiveText()
            val jsonBody = JSONObject(body)
            XLog.tag(tag).d("completions:body:$body")

            val stream = jsonBody.optBoolean("stream", false)
            if (stream) {
                call.respondTextWriter(contentType = ContentType.Text.EventStream) {
                    runCatching {
                        mnnHandler.completionsStreaming(jsonBody, this)
                    }.onFailure { e ->
                        XLog.tag(tag).e("completions:onFailure:$e", e)
                        call.response.status(HttpStatusCode(500, e.message ?: "Unknown error"))
                    }
                }
            } else {
                call.respondText(mnnHandler.completions(jsonBody).toString())
            }
        }.onFailure { e ->
            XLog.tag(tag).e("completions:onFailure:$e", e)
            call.response.status(HttpStatusCode(500, e.message ?: "Unknown error"))
        }
    }
    options("/chat/completions") {
        call.respond(HttpStatusCode.OK, null)
    }
}