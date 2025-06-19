package io.kindbrave.mnn.webserver.webserver.utils

import android.content.Context
import io.kindbrave.mnn.server.engine.ChatSession
import io.kindbrave.mnn.server.utils.FileUtils
import io.kindbrave.mnn.webserver.webserver.request.Content
import io.kindbrave.mnn.webserver.webserver.request.Message
import io.kindbrave.mnn.webserver.webserver.response.ChatChoice
import io.kindbrave.mnn.webserver.webserver.response.ChatCompletionResponse
import io.kindbrave.mnn.webserver.webserver.response.ChatMessage
import io.kindbrave.mnn.webserver.webserver.response.ToolCall
import io.kindbrave.mnn.webserver.webserver.response.Usage
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.json.JSONArray
import org.json.JSONObject
import java.io.Writer

object MNNHandlerUtils {
    fun parseAudioTag(input: String): String? {
        val pattern = "<audio>(.*?)</audio>"
        val regex = Regex(pattern, RegexOption.DOT_MATCHES_ALL)
        val matches = regex.find(input)
        return matches?.value
    }

    fun parseImgTag(input: String): String? {
        val pattern = "<image>(.*?)</image>"
        val regex = Regex(pattern, RegexOption.DOT_MATCHES_ALL)
        val matches = regex.find(input)
        return matches?.value
    }

    fun buildChatHistory(messages: List<Message>, context: Context, chatSession: ChatSession? = null): ArrayList<Pair<String, String>> {
        val history = ArrayList<Pair<String, String>>()

        messages.forEach { message ->
            // toolCalls结果不参与构建历史
            if (message.toolCalls != null) return@forEach

            if (message.role == "system") {
                chatSession?.updateSystemPrompt(message.content.toString())
                return@forEach
            }

            val role = message.role
            val content = message.content

            val parsedContent = when (content) {
                is Content.ContentString -> content.value
                is Content.ContentArray -> {
                    val sb = StringBuilder()
                    content.items.forEach { item ->
                        when (item.type) {
                            "text" -> sb.append(item.text)
                            "input_audio" -> {
                                val audioData = item.inputAudio!!.data
                                val audioCachePath = FileUtils.saveBase64WavToCache(context, audioData)
                                sb.append("<audio>").append(audioCachePath).append("</audio>")
                            }
                            "input_image" -> {
                                val imageData = item.inputImage!!.data
                                val imgCachePath = FileUtils.saveBase64JpgToCache(context, imageData)
                                sb.append("<img>").append(imgCachePath).append("</img>")
                            }
                            else -> sb.append("")
                        }
                    }
                    sb.toString()
                }
            }

            history.add(Pair(role, parsedContent))
        }
        return history
    }

    fun buildChatCompletionResponse(
        id: String,
        created: Long,
        model: String,
        content: String,
        finishReason: String = "stop",
        toolCalls: List<ToolCall>? = null,
        promptTokens: Long?,
        completionTokens: Long?
    ): ChatCompletionResponse {
        return ChatCompletionResponse(
            id = id,
            created = created,
            model = model,
            choices = listOf(
                ChatChoice(
                    index = 0,
                    message = ChatMessage(
                        role = "assistant",
                        content = content,
                        toolCalls = toolCalls
                    ),
                    finishReason = finishReason
                )
            ),
            usage = if (promptTokens != null && completionTokens != null) Usage(
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                totalTokens = (promptTokens + completionTokens)
            ) else null
        )
    }
}

fun Writer.writeChunk(messageId: String, createdTime: Long, modelId: String, progress: String) {
    val chunk = JSONObject()
        .put("id", "chatcmpl-$messageId")
        .put("object", "chat.completion.chunk")
        .put("created", createdTime)
        .put("model", modelId)
        .put("choices", JSONArray().put(
            JSONObject()
                .put("index", 0)
                .put("delta", JSONObject().put("content", progress))
                .put("finish_reason", JSONObject.NULL)
        ))

    write("data: $chunk\n\n")
    flush()
}

fun Writer.writeToolCallsChunk(
    messageId: String,
    createdTime: Long,
    modelId: String,
    toolCalls: List<ToolCall>
) {
    val json = Json { encodeDefaults = true }
    val toolCallsJsonStr = json.encodeToString(ListSerializer(ToolCall.serializer()), toolCalls)
    val toolCallsJsonArray = JSONArray(toolCallsJsonStr)

    val chunk = JSONObject()
        .put("id", "chatcmpl-$messageId")
        .put("object", "chat.completion.chunk")
        .put("created", createdTime)
        .put("model", modelId)
        .put("choices", JSONArray().put(
            JSONObject()
               .put("index", 0)
               .put("delta", JSONObject().put("tool_calls", toolCallsJsonArray))
               .put("finish_reason", JSONObject.NULL)
        ))
    write("data: $chunk\n\n")
    flush()
}

fun Writer.writeLastChunk(
    messageId: String,
    createdTime: Long,
    modelId: String,
    metrics: HashMap<String, Any> = hashMapOf<String, Any>(),
    finishReason: String = "stop"
) {
    val promptLen = if (metrics.containsKey("prompt_len")) metrics["prompt_len"] as Long else 0L
    val decodeLen = if (metrics.containsKey("decode_len")) metrics["decode_len"] as Long else 0L
    val lastChunk = JSONObject()
        .put("id", "chatcmpl-$messageId")
        .put("object", "chat.completion.chunk")
        .put("created", createdTime)
        .put("model", modelId)
        .put("choices", JSONArray().put(
            JSONObject()
                .put("index", 0)
                .put("delta", JSONObject())
                .put("finish_reason", finishReason)
        ))
        .put("usage", JSONObject()
            .put("prompt_tokens", promptLen)
            .put("completion_tokens", decodeLen)
            .put("total_tokens", promptLen + decodeLen))

    write("data: $lastChunk\n\n")
    write("data: [DONE]\n\n")
    flush()
}