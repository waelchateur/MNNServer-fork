package io.kindbrave.mnn.webserver.webserver.request

import kotlinx.serialization.Serializable

@Serializable
data class TTSTextRequest(
    val model: String,
    val text: String
)