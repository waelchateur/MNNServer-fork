package io.kindbrave.mnn.webserver.webserver.config

import io.kindbrave.mnn.webserver.webserver.MNNHandler
import io.kindbrave.mnn.webserver.webserver.MNNTTSHandler
import io.kindbrave.mnn.webserver.webserver.route.audioRoutes
import io.kindbrave.mnn.webserver.webserver.route.completionsRoutes
import io.kindbrave.mnn.webserver.webserver.route.embeddingsRoutes
import io.kindbrave.mnn.webserver.webserver.route.modelsRoutes
import io.kindbrave.mnn.webserver.webserver.route.ttsRoutes
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(mnnHandler: MNNHandler, mnnTtsHandler: MNNTTSHandler) {
    routing {
        route("/v1") {
            modelsRoutes(mnnHandler)
            completionsRoutes(mnnHandler)
            embeddingsRoutes(mnnHandler)
            audioRoutes(mnnHandler)
            ttsRoutes(mnnTtsHandler)
        }
        get("/") {
            call.respondText("MNN Server is Running :)")
        }
    }
}