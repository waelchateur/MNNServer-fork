package io.kindbrave.mnnserver.webserver

import android.util.Log
import com.elvishew.xlog.XLog
import io.kindbrave.mnnserver.annotation.LogAfter
import io.kindbrave.mnnserver.annotation.LogBefore
import io.kindbrave.mnnserver.engine.MNNLlm
import io.kindbrave.mnnserver.service.LLMService
import io.kindbrave.mnnserver.webserver.response.Model
import io.kindbrave.mnnserver.webserver.response.ModelsResponse
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

    fun completions(requestJson: String, writer: Writer) {
        val jsonBody = JSONObject(requestJson)

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

        runCatching {
            val history = buildChatHistory(messages)
            chatSession.generate(history, object : MNNLlm.GenerateProgressListener {
                override fun onProgress(progress: String?): Boolean {
                    return try {
                        if (progress == null) {
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

                            writer.write("data: $lastChunk\n\n")
                            writer.write("data: [DONE]\n\n")
                            writer.flush()
                            true
                        } else {
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

                            writer.write("data: $chunk\n\n")
                            writer.flush()
                            false
                        }
                    } catch (e: IOException) {
                        Log.e(tag, e.toString())
                        true
                    }
                }
            })
        }.onFailure { e ->
            XLog.tag(tag).e("completions:onFailure:$e")
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
            throw InvalidParameterException("this model can not embedding")
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