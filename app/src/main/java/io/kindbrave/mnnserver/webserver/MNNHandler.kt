package io.kindbrave.mnnserver.webserver

import com.elvishew.xlog.XLog
import io.kindbrave.mnnserver.annotation.LogAfter
import io.kindbrave.mnnserver.engine.MNNLlm
import io.kindbrave.mnnserver.service.LLMService
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
    private val llmService: LLMService
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

    @LogAfter("")
    fun completions(jsonBody: JSONObject): JSONObject {
        val modelId = jsonBody.optString("model", "")
        val messages = jsonBody.optJSONArray("messages")
        if (modelId.isEmpty() || messages == null || messages.length() == 0) {
            throw InvalidParameterException("please give modelId or messages params")
        }

        val chatSession = llmService.getChatSession(modelId)
        if (chatSession == null) {
            throw InvalidParameterException("this model can not completion")
        }

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
        return response
    }

    fun completionsStreaming(jsonBody: JSONObject, writer: Writer) {
        val modelId = jsonBody.optString("model", "")
        val messages = jsonBody.optJSONArray("messages")

        if (modelId.isEmpty() || messages == null || messages.length() == 0) {
            throw InvalidParameterException("please give modelId or messages params")
        }

        val chatSession = llmService.getChatSession(modelId)
        if (chatSession == null) {
            throw InvalidParameterException("chatSession is null")
        }

        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        runCatching {
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
                        XLog.tag(tag).e("completions:onProgress onFailure:$e")
                        true
                    }
                }
            })
            writer.writeLastChunk(messageId, createdTime, modelId, metrics)
        }.onFailure { e ->
            XLog.tag(tag).e("completionsStreaming:onFailure:$e")
        }
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
            val content = message.optString("content", "")
            history.add(Pair(role, content))
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
    metrics: HashMap<String, Any>
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