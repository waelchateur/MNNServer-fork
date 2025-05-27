package io.kindbrave.mnnserver.webserver.route

import io.kindbrave.mnnserver.webserver.MNNHandler
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.post

fun Route.embeddingsRoutes(mnnHandler: MNNHandler) {
    post("/embeddings") {
        runCatching {
            call.respondText(mnnHandler.embeddings(call.receiveText()).toString())
        }.onFailure {
            call.response.status(HttpStatusCode(500, it.message ?: "Unknown error"))
        }
    }
}