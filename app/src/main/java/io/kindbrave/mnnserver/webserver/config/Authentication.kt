package io.kindbrave.mnnserver.webserver.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.bearer

fun Application.configureAuthentication() {
    install(Authentication) {
        bearer("auth-bearer") {
            authenticate { tokenCredential ->
                if (tokenCredential.token.isNotBlank() &&
                    tokenCredential.token.startsWith("sk-")) {
                    UserIdPrincipal(tokenCredential.token)
                } else {
                    null
                }
            }
        }
    }
}