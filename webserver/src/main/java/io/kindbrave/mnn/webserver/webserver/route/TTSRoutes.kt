package io.kindbrave.mnn.webserver.webserver.route

import com.elvishew.xlog.XLog
import io.kindbrave.mnn.webserver.webserver.MNNTTSHandler
import io.kindbrave.mnn.webserver.webserver.request.TTSTextRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import kotlinx.serialization.json.Json

private const val tag = "TTSRoutes"

fun Route.ttsRoutes(mnnTtsHandler: MNNTTSHandler) {
    post("/tts") {
        runCatching {
            val textBody = call.receiveText()
            XLog.tag(tag).d("ttsRoutes:request:\n$textBody")
            val body = Json.decodeFromString(TTSTextRequest.serializer(), textBody)
            call.respondBytes(mnnTtsHandler.generateAudio(body))
        }.onFailure { e ->
            XLog.tag(tag).e("ttsRoutes:onFailure:$e", e)
            call.response.status(HttpStatusCode(500, e.message ?: "Unknown error"))
        }
    }
    get("/tts") {
        call.respond(HttpStatusCode.OK, null)
    }
}