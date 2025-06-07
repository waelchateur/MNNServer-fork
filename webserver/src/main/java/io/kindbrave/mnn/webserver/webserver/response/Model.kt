package io.kindbrave.mnn.webserver.webserver.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ModelsResponse(
    val data: List<Model>,
    @SerialName("object") val objectType: String = "list"
)

@Serializable
data class Model(
    val id: String,
    @SerialName("object") val objectType: String = "model",
    val created: Long = System.currentTimeMillis() / 1000,
    val owned_by: String = "organization"
)
