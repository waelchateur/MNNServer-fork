package io.kindbrave.mnnserver.webserver.route

import io.kindbrave.mnnserver.webserver.MNNHandler
import io.kindbrave.mnnserver.webserver.response.ModelsResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.modelsRoutes(mnnHandler: MNNHandler) {
    get("/models") {
        call.respond(ModelsResponse(data = mnnHandler.getModels()))
    }
}