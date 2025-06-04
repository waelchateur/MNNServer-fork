package io.kindbrave.mnnserver.webserver.request

import kotlinx.serialization.*
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

@Serializable
data class ChatGenerateRequest(
    val messages: List<Message> = emptyList<Message>(),
    val model: String = "",
    val stream: Boolean = false,
    @SerialName("stream_options") val streamOptions: StreamOptions? = null,
    val temperature: Double? = null,
    val tools: List<FunctionTool>? = null,
    @SerialName("top_p") val topP: Double? = null,
)

@Serializable
data class Message(
    @Serializable(with = ContentSerializer::class)
    val content: Content,
    val role: String
)

@Serializable
data class StreamOptions(
    @SerialName("include_usage") val includeUsage: Boolean
)

@Serializable
data class FunctionTool(
    val type: String,
    val function: FunctionDetail
)

@Serializable
data class FunctionDetail(
    val name: String,
    val description: String,
    val parameters: JsonObject
)

@Serializable
sealed class Content {
    @Serializable
    @SerialName("string")
    data class ContentString(val value: String) : Content()

    @Serializable
    @SerialName("array")
    data class ContentArray(val items: List<ContentItem>) : Content()
}

@Serializable
data class ContentItem(
    val type: String,
    val data: JsonElement?,
    val text: String?
)

object ContentSerializer : KSerializer<Content> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Content")

    override fun deserialize(decoder: Decoder): Content {
        val input = decoder as? JsonDecoder ?: error("This class can be loaded only by JSON")
        val element = input.decodeJsonElement()
        return when {
            element is JsonPrimitive && element.isString -> {
                Content.ContentString(element.content)
            }
            element is JsonArray -> {
                val list = element.map { jsonElement ->
                    input.json.decodeFromJsonElement<ContentItem>(jsonElement)
                }
                Content.ContentArray(list)
            }
            else -> error("Unsupported content format: $element")
        }
    }

    override fun serialize(encoder: Encoder, value: Content) {
        val output = encoder as? JsonEncoder ?: error("This class can be saved only by JSON")
        when (value) {
            is Content.ContentString -> output.encodeString(value.value)
            is Content.ContentArray -> output.encodeSerializableValue(ListSerializer(ContentItem.serializer()), value.items)
        }
    }
}
