package io.kindbrave.mnnserver.api

import android.util.Log
import io.kindbrave.mnnserver.api.ChunkedInputStream
import io.kindbrave.mnnserver.model.ChatDataItem
import io.kindbrave.mnnserver.service.LLMService
import io.kindbrave.mnnserver.session.ChatSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import org.nanohttpd.protocols.http.IHTTPSession
import org.nanohttpd.protocols.http.NanoHTTPD
import org.nanohttpd.protocols.http.request.Method
import org.nanohttpd.protocols.http.response.ChunkedOutputStream
import org.nanohttpd.protocols.http.response.Response
import org.nanohttpd.protocols.http.response.Status
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.*

class ApiHandler(private val llmService: LLMService) {
    
    private val TAG = "ApiHandler"
    private val scope = MainScope()
    
    fun handleRequest(session: IHTTPSession): Response {
        val uri = session.uri
        val method = session.method
        
        return try {
            when {
                uri == "/v1/models" && method == Method.GET -> {
                    handleGetModels()
                }
                uri == "/v1/chat/completions" && method == Method.POST -> {
                    handleChatCompletions(session)
                }
                uri == "/v1/embeddings" && method == Method.POST -> {
                    handleEmbeddings(session)
                }
                else -> {
                    Response.newFixedLengthResponse(
                        Status.NOT_FOUND,
                        "application/json",
                        "{\"error\":{\"message\":\"Not found\",\"type\":\"invalid_request_error\",\"code\":\"404\"}}"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "处理请求失败", e)
            Response.newFixedLengthResponse(
                Status.INTERNAL_ERROR,
                "application/json",
                "{\"error\":{\"message\":\"${e.message}\",\"type\":\"server_error\",\"code\":\"500\"}}"
            )
        }
    }
    
    private fun handleGetModels(): Response {
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
        
        return Response.newFixedLengthResponse(
            Status.OK,
            "application/json",
            response.toString()
        )
    }
    
    private fun handleChatCompletions(session: IHTTPSession): Response {
        val requestBody = readRequestBody(session)
        val jsonBody = JSONObject(requestBody)
        
        val modelId = jsonBody.optString("model", "")
        val messages = jsonBody.optJSONArray("messages")

        if (modelId.isEmpty() || messages == null || messages.length() == 0) {
            return Response.newFixedLengthResponse(
                Status.BAD_REQUEST,
                "application/json",
                "{\"error\":{\"message\":\"Missing required parameters\",\"type\":\"invalid_request_error\",\"code\":\"400\"}}"
            )
        }
        
        // 获取会话
        val chatSession = llmService.getChatSession(modelId)
            ?: return Response.newFixedLengthResponse(
                Status.BAD_REQUEST,
                "application/json",
                "{\"error\":{\"message\":\"Session Not Found\",\"type\":\"invalid_request_error\",\"code\":\"400\"}}"
            )

        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        val chunkedInputStream = PipedInputStream()
        val chunkedOutputStream = ChunkedOutputStream(PipedOutputStream(chunkedInputStream))

        val response = Response.newChunkedResponse(Status.OK, "what/ever", chunkedInputStream)
        response.setChunkedTransfer(true)

        scope.launch(Dispatchers.IO) {
            try {
                val history = buildChatHistory(messages)
                chatSession.generate(
                    history,
                    object : ChatSession.GenerateProgressListener {
                        override fun onProgress(progress: String?): Boolean {
                            try {
                                if (progress == null) {
                                    // 发送最后一个事件，表示完成
                                    val lastChunk = JSONObject()
                                    lastChunk.put("id", "chatcmpl-$messageId")
                                    lastChunk.put("object", "chat.completion.chunk")
                                    lastChunk.put("created", createdTime)
                                    lastChunk.put("model", modelId)

                                    val lastChoicesArray = JSONArray()
                                    val lastChoiceObj = JSONObject()
                                    lastChoiceObj.put("index", 0)
                                    lastChoiceObj.put("delta", JSONObject())
                                    lastChoiceObj.put("finish_reason", "stop")
                                    lastChoicesArray.put(lastChoiceObj)

                                    lastChunk.put("choices", lastChoicesArray)

                                    val lastEventData = "data: ${lastChunk}\n\n"
                                    chunkedOutputStream.write(lastEventData.toByteArray())
                                    chunkedOutputStream.flush()

                                    // 发送完成事件
                                    val doneEvent = "data: [DONE]\n\n"
                                    chunkedOutputStream.write(doneEvent.toByteArray())
                                    chunkedOutputStream.flush()
                                    chunkedOutputStream.close()
                                    return true
                                } else {
                                    // 构建流式响应格式
                                    val chunk = JSONObject()
                                    chunk.put("id", "chatcmpl-$messageId")
                                    chunk.put("object", "chat.completion.chunk")
                                    chunk.put("created", createdTime)
                                    chunk.put("model", modelId)

                                    val choicesArray = JSONArray()
                                    val choiceObj = JSONObject()
                                    choiceObj.put("index", 0)

                                    val deltaObj = JSONObject()
                                    deltaObj.put("content", progress)
                                    choiceObj.put("delta", deltaObj)

                                    choiceObj.put("finish_reason", null)
                                    choicesArray.put(choiceObj)

                                    chunk.put("choices", choicesArray)

                                    val eventData = "data: ${chunk}\n\n"
                                    chunkedOutputStream.write(eventData.toByteArray())
                                    chunkedOutputStream.flush()
                                    return false
                                }
                            } catch (e: IOException) {
                                Log.e(TAG, "发送流式响应失败", e)
                                chunkedOutputStream.close()
                                return true
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "生成响应失败", e)
                chunkedOutputStream.close()
            }
        }

        return response
    }
    
    private fun handleEmbeddings(session: IHTTPSession): Response {
        val requestBody = readRequestBody(session)
        val jsonBody = JSONObject(requestBody)
        
        val modelId = jsonBody.optString("model", "")
        val input = jsonBody.opt("input")
        
        if (modelId.isEmpty() || input == null) {
            return Response.newFixedLengthResponse(
                Status.BAD_REQUEST,
                "application/json",
                "{\"error\":{\"message\":\"Missing required parameters\",\"type\":\"invalid_request_error\",\"code\":\"400\"}}"
            )
        }
        
        // 获取或创建嵌入会话
        val sessionId = System.currentTimeMillis().toString()
        val embeddingSession = llmService.getEmbeddingSession(sessionId) ?: run {
            return Response.newFixedLengthResponse(
                Status.BAD_REQUEST,
                "application/json",
                "{\"error\":{\"message\":\"Missing required parameters\",\"type\":\"invalid_request_error\",\"code\":\"400\"}}"
            )
        }
        
        val jsonResponse = JSONObject()
        jsonResponse.put("object", "list")
        val dataArray = JSONArray()
        
        // 处理单个文本或文本数组
        when (input) {
            is String -> {
                val embedding = embeddingSession.getEmbedding(input)
                if (embedding != null) {
                    val embeddingObject = JSONObject()
                    embeddingObject.put("object", "embedding")
                    embeddingObject.put("embedding", JSONArray(embedding.toList()))
                    embeddingObject.put("index", 0)
                    dataArray.put(embeddingObject)
                }
            }
            is JSONArray -> {
                for (i in 0 until input.length()) {
                    val text = input.optString(i)
                    val embedding = embeddingSession.getEmbedding(text)
                    if (embedding != null) {
                        val embeddingObject = JSONObject()
                        embeddingObject.put("object", "embedding")
                        embeddingObject.put("embedding", JSONArray(embedding.toList()))
                        embeddingObject.put("index", i)
                        dataArray.put(embeddingObject)
                    }
                }
            }
        }
        
        jsonResponse.put("data", dataArray)
        jsonResponse.put("model", modelId)
        jsonResponse.put("usage", JSONObject().put("prompt_tokens", estimateTokenCount(input.toString())))
        
        return Response.newFixedLengthResponse(
            Status.OK,
            "application/json",
            jsonResponse.toString()
        )
    }
    
    private fun buildChatHistory(messages: JSONArray): ArrayList<ChatDataItem> {
        val history = ArrayList<ChatDataItem>()
        
        for (i in 0 until messages.length()) {
            val message = messages.getJSONObject(i)
            val role = message.optString("role", "")
            val content = message.optString("content", "")
            history.add(ChatDataItem(role, content))
        }
        
        return history
    }
    
    private fun estimateTokenCount(text: String): Int {
        // 简单估算：平均每个单词约为1.3个token
        return (text.split("\\s+".toRegex()).size * 1.3).toInt()
    }
    
    private fun readRequestBody(session: IHTTPSession): String {
        val contentLength = session.headers["content-length"]?.toIntOrNull() ?: 0
        val buffer = ByteArray(contentLength)
        
        try {
            session.inputStream.read(buffer, 0, contentLength)
            return String(buffer)
        } catch (e: IOException) {
            Log.e(TAG, "读取请求体失败", e)
            throw e
        }
    }
}