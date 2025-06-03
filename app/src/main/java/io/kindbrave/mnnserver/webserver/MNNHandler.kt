package io.kindbrave.mnnserver.webserver

import android.content.Context
import com.elvishew.xlog.XLog
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnnserver.annotation.LogAfter
import io.kindbrave.mnnserver.engine.AsrSession
import io.kindbrave.mnnserver.engine.ChatSession
import io.kindbrave.mnnserver.engine.MNNAsr
import io.kindbrave.mnnserver.engine.MNNLlm
import io.kindbrave.mnnserver.service.LLMService
import io.kindbrave.mnnserver.utils.FileUtils
import io.kindbrave.mnnserver.webserver.response.Model
import kotlinx.io.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.io.Writer
import java.security.InvalidParameterException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MNNHandler @Inject constructor(
    private val llmService: LLMService,
    @ApplicationContext private val context: Context
) {
    private val tag = MNNHandler::class.java.simpleName

    fun getModels(): List<Model> {
        val modelList = mutableListOf<Model>()
        val sessions = llmService.getAllSessions()

        sessions.forEach { session ->
            modelList.add(Model(
                id = session.modelId,
                created = System.currentTimeMillis() / 1000,
            ))
        }

        return modelList
    }

    fun completions(jsonBody: JSONObject): JSONObject {
        val modelId = jsonBody.optString("model", "")
        XLog.tag(tag).d("completions: modelId:${modelId}")
        val messages = jsonBody.optJSONArray("messages")
        if (modelId.isEmpty() || messages == null || messages.length() == 0) {
            throw InvalidParameterException("please give modelId or messages params")
        }

        val chatSession = llmService.getChatSession(modelId)
        if (chatSession != null) {
            return chatSessionGenerate(messages, modelId, chatSession)
        }
        val asrSession = llmService.getAsrSession(modelId)
        if (asrSession != null) {
            return asrSessionGenerate(messages, modelId, asrSession)
        }
        throw InvalidParameterException("session is null")
    }

    fun completionsStreaming(jsonBody: JSONObject, writer: Writer) {
        val modelId = jsonBody.optString("model", "")
        XLog.tag(tag).d("completionsStreaming: modelId:${modelId}")
        val messages = jsonBody.optJSONArray("messages")

        if (modelId.isEmpty() || messages == null || messages.length() == 0) {
            throw InvalidParameterException("please give modelId or messages params")
        }

        val chatSession = llmService.getChatSession(modelId)
        if (chatSession != null) {
            chatSessionStreamingGenerate(messages, modelId, writer, chatSession)
            return
        }
        val asrSession = llmService.getAsrSession(modelId)
        if (asrSession != null) {
            asrSessionStreamingGenerate(messages, modelId, writer, asrSession)
            return
        }

        throw InvalidParameterException("session is null")
    }

    fun embeddings(requestJson: String): JSONObject {
        val jsonBody = JSONObject(requestJson)

        val modelId = jsonBody.optString("model", "")
        val input = jsonBody.optJSONArray("input")

        if (modelId.isEmpty() || input == null || input.length() == 0) {
            throw InvalidParameterException("please give modelId or input params")
        }

        val embeddingSession = llmService.getEmbeddingSession(modelId)
        if (embeddingSession == null) {
            throw InvalidParameterException("embeddingSession is null")
        }

        runCatching {
            val embedding = embeddingSession.embedding(input.getString(0))
            val result = JSONObject()
                .put("object", "list")
                .put("data", JSONArray().put(
                    JSONObject()
                        .put("object", "embedding")
                        .put("embedding", JSONArray(embedding))
                        .put("index", 0)
                ))
                .put("model", modelId)
            return result
        }.onFailure { e ->
            XLog.tag(tag).e("embeddings:onFailure:$e")
        }
        throw Exception("embedding error")
    }

    private fun buildChatHistory(messages: JSONArray): ArrayList<Pair<String, String>> {
        val history = ArrayList<Pair<String, String>>()

        for (i in 0 until messages.length()) {
            val message = messages.getJSONObject(i)
            val role = message.optString("role", "")
            val content = message.opt("content")

            val parsedContent = when (content) {
                is String -> content
                is JSONArray -> {
                    val sb = StringBuilder()
                    for (j in 0 until content.length()) {
                        val item = content.getJSONObject(j)
                        when (item.optString("type")) {
                            "text" -> sb.append(item.optString("text", ""))
                            "input_audio" -> {
                                val audioData = item.optJSONObject("input_audio")?.optString("data", "") ?: ""
                                val audioCachePath = FileUtils.saveBase64WavToCache(context, audioData)
                                sb.append("<audio>").append(audioCachePath).append("</audio>")
                            }
                            "input_image" -> {
                                val imageData = item.optJSONObject("input_image")?.optString("data", "") ?: ""
                                val imgCachePath = FileUtils.saveBase64JpgToCache(context, imageData)
                                sb.append("<img>").append(imgCachePath).append("</img>")
                            }
                            else -> sb.append("")
                        }
                    }
                    sb.toString()
                }
                else -> ""
            }

            history.add(Pair(role, parsedContent))
        }
        return history
    }

    private fun chatSessionGenerate(
        messages: JSONArray,
        modelId: String,
        chatSession: ChatSession
    ): JSONObject {
        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        val history = buildChatHistory(messages)
        val generateResponse = StringBuilder()
        val metrics = chatSession.generate(history, object : MNNLlm.GenerateProgressListener {
            override fun onProgress(progress: String?): Boolean {
                return try {
                    if (progress == null) {
                        true
                    } else {
                        generateResponse.append(progress)
                        false
                    }
                } catch (e: IOException) {
                    XLog.tag(tag).e("completions:onFailure:$e")
                    true
                }
            }
        })
        val promptLen = if (metrics.containsKey("prompt_len")) metrics["prompt_len"] as Long else 0L
        val decodeLen = if (metrics.containsKey("decode_len")) metrics["decode_len"] as Long else 0L
        val response = JSONObject()
            .put("id", "chatcmpl-$messageId")
            .put("object", "chat.completion")
            .put("created", createdTime)
            .put("model", modelId)
            .put("choices", JSONArray().put(
                JSONObject()
                    .put("index", 0)
                    .put("message", JSONObject()
                        .put("role", "assistant")
                        .put("content", generateResponse.toString()))
                    .put("finish_reason", "stop")
            ))
            .put("usage", JSONObject()
                .put("prompt_tokens", promptLen)
                .put("completion_tokens", decodeLen)
                .put("total_tokens", promptLen + decodeLen))
        XLog.tag(tag).d("chatSessionGenerate modelId:$modelId done")
        return response
    }

    private fun chatSessionStreamingGenerate(
        messages: JSONArray,
        modelId: String,
        writer: Writer,
        chatSession: ChatSession
    ) {
        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        val history = buildChatHistory(messages)
        // 首先发送空白内容防止回复过慢客户端断开连接
        writer.writeChunk(messageId, createdTime, modelId, "")
        val metrics = chatSession.generate(history, object : MNNLlm.GenerateProgressListener {
            override fun onProgress(progress: String?): Boolean {
                return try {
                    if (progress == null) {
                        true
                    } else {
                        writer.writeChunk(messageId, createdTime, modelId, progress)
                        false
                    }
                } catch (e: IOException) {
                    XLog.tag(tag).e("chatSessionStreamingGenerate:onProgress onFailure:$e")
                    true
                }
            }
        })
        writer.writeLastChunk(messageId, createdTime, modelId, metrics)
        XLog.tag(tag).d("chatSessionStreamingGenerate modelId:$modelId done")
    }

    private fun asrSessionStreamingGenerate(
        messages: JSONArray,
        modelId: String,
        writer: Writer,
        asrSession: AsrSession
    ) {
        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        val history = buildChatHistory(messages)
        if (history.size != 1 && history[0].second.contains(Regex("<audio>.*</audio>")).not()) {
            throw InvalidParameterException("Asr only support audio input")
        }
        val audioPath = parseAudioTag(history[0].second)
        if (audioPath == null) {
            throw InvalidParameterException("Audio is null")
        }

        // 首先发送空白内容防止回复过慢客户端断开连接
        writer.writeChunk(messageId, createdTime, modelId, "")

        asrSession.generate(audioPath, object : MNNAsr.AsrCallback {
            override fun onPartialResult(text: String?) {
                text?.let {
                    writer.writeChunk(messageId, createdTime, modelId, it)
                }
            }

            override fun onFinalResult(text: String?) {
                writer.writeLastChunk(messageId, createdTime, modelId)
            }

        })
        XLog.tag(tag).d("asrSessionStreamingGenerate modelId:$modelId done")
    }

    private fun asrSessionGenerate(
        messages: JSONArray,
        modelId: String,
        asrSession: AsrSession
    ): JSONObject {
        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        val history = buildChatHistory(messages)
        if (history.size != 1 && history[0].second.contains(Regex("<audio>.*</audio>")).not()) {
            throw InvalidParameterException("Asr only support audio input")
        }
        val audioPath = parseAudioTag(history[0].second)
        if (audioPath == null) {
            throw InvalidParameterException("Audio is null")
        }

        val generateResponse = StringBuilder()
        asrSession.generate(audioPath, object : MNNAsr.AsrCallback {
            override fun onPartialResult(text: String?) {

            }

            override fun onFinalResult(text: String?) {
                generateResponse.append(text)
            }

        })
        val promptLen = 0L
        val decodeLen = 0L
        val response = JSONObject()
            .put("id", "chatcmpl-$messageId")
            .put("object", "chat.completion")
            .put("created", createdTime)
            .put("model", modelId)
            .put("choices", JSONArray().put(
                JSONObject()
                    .put("index", 0)
                    .put("message", JSONObject()
                        .put("role", "assistant")
                        .put("content", generateResponse.toString()))
                    .put("finish_reason", "stop")
            ))
            .put("usage", JSONObject()
                .put("prompt_tokens", promptLen)
                .put("completion_tokens", decodeLen)
                .put("total_tokens", promptLen + decodeLen))
        XLog.tag(tag).d("asrSessionGenerate modelId:$modelId done")
        return response
    }

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