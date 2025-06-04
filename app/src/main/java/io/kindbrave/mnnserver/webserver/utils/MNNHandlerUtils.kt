package io.kindbrave.mnnserver.webserver.utils

import android.content.Context
import io.kindbrave.mnnserver.utils.FileUtils
import io.kindbrave.mnnserver.webserver.request.Content
import io.kindbrave.mnnserver.webserver.request.Message
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

    fun buildChatHistory(messages: List<Message>, context: Context): ArrayList<Pair<String, String>> {
        val history = ArrayList<Pair<String, String>>()

        messages.forEach { message ->
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
                                val audioData = item.data.toString()
                                val audioCachePath = FileUtils.saveBase64WavToCache(context, audioData)
                                sb.append("<audio>").append(audioCachePath).append("</audio>")
                            }
                            "input_image" -> {
                                val imageData = item.data.toString()
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

fun Writer.writeLastChunk(
    messageId: String,
    createdTime: Long,
    modelId: String,
    metrics: HashMap<String, Any> = hashMapOf<String, Any>()
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
                .put("finish_reason", "stop")
        ))
        .put("usage", JSONObject()
            .put("prompt_tokens", promptLen)
            .put("completion_tokens", decodeLen)
            .put("total_tokens", promptLen + decodeLen))

    write("data: $lastChunk\n\n")
    write("data: [DONE]\n\n")
    flush()
}