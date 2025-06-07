package io.kindbrave.mnn.webserver.webserver.response

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
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
    @SerialName("function_call") val functionCall: FunctionCall? = null,
    @SerialName("tool_calls") val toolCalls: List<ToolCall>? = null
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
    @Serializable(with = JsonElementAsStringSerializer::class)
    val arguments: String
)

@Serializable
data class ToolCall(
    val id: String,
    val index: Int? = null,
    @SerialName("type") val toolType: String,
    @SerialName("function") val functionCall: FunctionCall
)

@OptIn(ExperimentalSerializationApi::class)
object JsonElementAsStringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("JsonAsString", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        return when (val input = decoder as? JsonDecoder) {
            null -> decoder.decodeString()
            else -> {
                val element = input.decodeJsonElement()
                element.toString()
            }
        }
    }
}
