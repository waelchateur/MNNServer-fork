package io.kindbrave.mnnserver.webserver

import android.util.Log
import io.kindbrave.mnnserver.model.ChatDataItem
import io.kindbrave.mnnserver.service.LLMService
import io.kindbrave.mnnserver.session.ChatSession
import io.ktor.server.routing.RoutingCall
import kotlinx.coroutines.isActive
import kotlinx.io.IOException
import org.json.JSONArray
import org.json.JSONObject
import java.io.Writer
import java.security.InvalidParameterException
import java.util.UUID

class MNNHandler {
    private val tag = MNNHandler::class.java.simpleName
    private val llmService = LLMService.getInstance()

    fun getModels(): JSONObject {
        val response = JSONObject()
        val modelsArray = JSONArray()

        // 获取所有已加载的模型
        val chatSessions = llmService.getAllChatSessions()
        chatSessions.forEach { session ->
            val modelJson = JSONObject()
            modelJson.put("id", session.modelId)
            modelJson.put("object", "model")
            modelJson.put("created", System.currentTimeMillis() / 1000)
            modelJson.put("owned_by", "organization-owner")

            val permissionJson = JSONObject()
            permissionJson.put("id", "modelperm-${UUID.randomUUID()}")
            permissionJson.put("object", "model_permission")
            permissionJson.put("created", System.currentTimeMillis() / 1000)
            permissionJson.put("allow_create_engine", false)
            permissionJson.put("allow_sampling", true)
            permissionJson.put("allow_logprobs", true)
            permissionJson.put("allow_search_indices", false)
            permissionJson.put("allow_view", true)
            permissionJson.put("allow_fine_tuning", false)
            permissionJson.put("organization", "*")
            permissionJson.put("group", null)
            permissionJson.put("is_blocking", false)

            val permissionsArray = JSONArray()
            permissionsArray.put(permissionJson)

            modelJson.put("permission", permissionsArray)
            modelJson.put("root", session.modelId)
            modelJson.put("parent", null)

            modelsArray.put(modelJson)
        }

        response.put("object", "list")
        response.put("data", modelsArray)

        return response
    }

    fun completions(requestJson: String, writer: Writer) {
        val jsonBody = JSONObject(requestJson)

        val modelId = jsonBody.optString("model", "")
        val messages = jsonBody.optJSONArray("messages")

        if (modelId.isEmpty() || messages == null || messages.length() == 0) {
            throw InvalidParameterException("ModelId is null")
        }

        val chatSession = llmService.getChatSession(modelId)
        if (chatSession == null) {
            throw InvalidParameterException("ChatSession is null")
        }

        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        runCatching {
            val history = buildChatHistory(messages)
            chatSession.generate(history, object : ChatSession.GenerateProgressListener {
                override fun onProgress(progress: String?): Boolean {
                    return try {
                        if (progress == null) {
                            // Send final chunk
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
            Log.e(tag, e.toString())
        }
    }

    private fun buildChatHistory(messages: JSONArray): java.util.ArrayList<ChatDataItem> {
        val history = ArrayList<ChatDataItem>()

        for (i in 0 until messages.length()) {
            val message = messages.getJSONObject(i)
            val role = message.optString("role", "")
            val content = message.optString("content", "")
            history.add(ChatDataItem(role, content))
        }

        return history
    }
}