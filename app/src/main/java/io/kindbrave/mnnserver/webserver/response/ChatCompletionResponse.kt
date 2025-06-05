package io.kindbrave.mnnserver.webserver.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatCompletionResponse(
    val id: String,
    @SerialName("object") val objectType: String = "chat.completion",
    val created: Long,
    val model: String,
    val choices: List<ChatChoice>,
    val usage: Usage? = null
)

@Serializable
data class ChatChoice(
    val index: Int,
    val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String,
    @SerialName("function_call") val functionCall: List<FunctionCall>? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Long,
    @SerialName("completion_tokens") val completionTokens: Long,
    @SerialName("total_tokens") val totalTokens: Long
)

@Serializable
data class FunctionCall(
    val name: String,
    val arguments: JsonElement
)