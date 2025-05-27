package io.kindbrave.mnnserver.webserver.config

import io.kindbrave.mnnserver.webserver.MNNHandler
import io.kindbrave.mnnserver.webserver.route.completionsRoutes
import io.kindbrave.mnnserver.webserver.route.embeddingsRoutes
import io.kindbrave.mnnserver.webserver.route.modelsRoutes
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(mnnHandler: MNNHandler) {
    routing {
        route("/v1") {
            modelsRoutes(mnnHandler)
            completionsRoutes(mnnHandler)
            embeddingsRoutes(mnnHandler)
        }
    }
}