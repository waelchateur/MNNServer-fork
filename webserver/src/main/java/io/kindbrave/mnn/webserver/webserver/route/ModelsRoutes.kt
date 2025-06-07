package io.kindbrave.mnn.webserver.webserver.route

import io.kindbrave.mnn.webserver.webserver.MNNHandler
import io.kindbrave.mnn.webserver.webserver.response.ModelsResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.modelsRoutes(mnnHandler: MNNHandler) {
    get("/models") {
        call.respond(ModelsResponse(data = mnnHandler.getModels()))
    }
}