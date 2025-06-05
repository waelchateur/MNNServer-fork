package io.kindbrave.mnnserver.webserver

import android.content.Context
import com.elvishew.xlog.XLog
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnnserver.engine.AsrSession
import io.kindbrave.mnnserver.engine.ChatSession
import io.kindbrave.mnnserver.engine.MNNAsr
import io.kindbrave.mnnserver.engine.MNNLlm
import io.kindbrave.mnnserver.service.LLMService
import io.kindbrave.mnnserver.webserver.request.ChatGenerateRequest
import io.kindbrave.mnnserver.webserver.request.FunctionTool
import io.kindbrave.mnnserver.webserver.request.Message
import io.kindbrave.mnnserver.webserver.response.ChatCompletionResponse
import io.kindbrave.mnnserver.webserver.response.Model
import io.kindbrave.mnnserver.webserver.utils.FunctionCallUtils
import io.kindbrave.mnnserver.webserver.utils.MNNHandlerUtils
import io.kindbrave.mnnserver.webserver.utils.writeChunk
import io.kindbrave.mnnserver.webserver.utils.writeLastChunk
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

    fun completions(body: ChatGenerateRequest): ChatCompletionResponse {
        val modelId = body.model
        XLog.tag(tag).d("completions: modelId:${modelId}")
        val messages = body.messages
        if (modelId.isEmpty() || messages.isEmpty()) {
            throw InvalidParameterException("please give modelId or messages params")
        }

        val chatSession = llmService.getChatSession(modelId)
        if (chatSession != null) {
            return chatSessionGenerate(messages, body.tools, modelId, chatSession)
        }
        val asrSession = llmService.getAsrSession(modelId)
        if (asrSession != null) {
            return asrSessionGenerate(messages, modelId, asrSession)
        }
        throw InvalidParameterException("session is null")
    }

    fun completionsStreaming(body: ChatGenerateRequest, writer: Writer) {
        val modelId = body.model
        XLog.tag(tag).d("completionsStreaming: modelId:${modelId}")
        val messages = body.messages

        if (modelId.isEmpty() || messages.isEmpty()) {
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

    private fun chatSessionGenerate(
        messages: List<Message>,
        tools: List<FunctionTool>?,
        modelId: String,
        chatSession: ChatSession
    ): ChatCompletionResponse {
        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        val isFunctionCall = tools.isNullOrEmpty().not()

        if (isFunctionCall) {
            val systemPrompt = FunctionCallUtils.buildFunctionCallPrompt(tools)
            chatSession.updateSystemPrompt(systemPrompt)
        }

        val history = MNNHandlerUtils.buildChatHistory(messages, context)
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

        // function call
        val functionCall = if (isFunctionCall) {
            FunctionCallUtils.tryToParseFunctionCall(generateResponse.toString())
        } else null

        val response = MNNHandlerUtils.buildChatCompletionResponse(
            id = messageId,
            created = createdTime,
            model = modelId,
            content = generateResponse.toString(),
            functionCall = functionCall,
            finishReason = if (functionCall != null) "function_call" else "stop",
            promptTokens = promptLen,
            completionTokens = decodeLen
        )
        XLog.tag(tag).d("chatSessionGenerate modelId:$modelId done")
        return response
    }

    private fun chatSessionStreamingGenerate(
        messages: List<Message>,
        modelId: String,
        writer: Writer,
        chatSession: ChatSession
    ) {
        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        val history = MNNHandlerUtils.buildChatHistory(messages, context)
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
        messages: List<Message>,
        modelId: String,
        writer: Writer,
        asrSession: AsrSession
    ) {
        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        val history = MNNHandlerUtils.buildChatHistory(messages, context)
        if (history.size != 1 && history[0].second.contains(Regex("<audio>.*</audio>")).not()) {
            throw InvalidParameterException("Asr only support audio input")
        }
        val audioPath = MNNHandlerUtils.parseAudioTag(history[0].second)
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
        messages: List<Message>,
        modelId: String,
        asrSession: AsrSession
    ): ChatCompletionResponse {
        val messageId = UUID.randomUUID().toString()
        val createdTime = System.currentTimeMillis() / 1000

        val history = MNNHandlerUtils.buildChatHistory(messages, context)
        if (history.size != 1 && history[0].second.contains(Regex("<audio>.*</audio>")).not()) {
            throw InvalidParameterException("Asr only support audio input")
        }
        val audioPath = MNNHandlerUtils.parseAudioTag(history[0].second)
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
        val response = MNNHandlerUtils.buildChatCompletionResponse(
            id = "chatcmpl-$messageId",
            created = createdTime,
            model = modelId,
            content = generateResponse.toString(),
            promptTokens = promptLen,
            completionTokens = decodeLen
        )
        XLog.tag(tag).d("asrSessionGenerate modelId:$modelId done")
        return response
    }
}