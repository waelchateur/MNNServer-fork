package io.kindbrave.mnn.webserver.webserver.config

import com.elvishew.xlog.XLog
import io.ktor.server.application.Application
import io.ktor.server.application.createRouteScopedPlugin
import io.ktor.server.application.install
import io.ktor.server.logging.toLogString

private const val tag = "WebServer"

fun Application.configureLogging() {
    install(createRouteScopedPlugin("RequestTracePlugin") {
        onCall { call ->
            XLog.tag(tag).d(call.request.toLogString())
        }
    })
}