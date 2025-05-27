package io.kindbrave.mnnserver.webserver.config

import com.elvishew.xlog.XLog
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import kotlinx.serialization.SerializationException
import io.ktor.server.response.respond

private val tag = "StatusPage"

fun Application.configureStatusPages() {
    install(StatusPages) {
        exception<SerializationException> { call, cause ->
            XLog.tag(tag).e("SerializationException:${cause.message}")
            call.respond(
                HttpStatusCode.BadRequest,
                "Invalid request body"
            )
        }

        exception<Throwable> { call, cause ->
            XLog.tag(tag).e("Exception:${cause.message}")
            call.respond(
                HttpStatusCode.InternalServerError,
                "An unexpected error occurred"
            )
        }
    }
}