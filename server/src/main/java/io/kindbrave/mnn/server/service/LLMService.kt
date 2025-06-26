package io.kindbrave.mnn.server.service

import android.text.TextUtils
import com.alibaba.mls.api.ModelItem
import com.elvishew.xlog.XLog
import io.kindbrave.mnn.server.annotation.LogAfter
import io.kindbrave.mnn.server.engine.AsrSession
import io.kindbrave.mnn.server.engine.ChatSession
import io.kindbrave.mnn.server.engine.EmbeddingSession
import io.kindbrave.mnn.server.engine.Session
import io.kindbrave.mnn.server.engine.TTSSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMService @Inject constructor() {
    private val tag = LLMService::class.java.simpleName
    private val chatSessionMap = mutableMapOf<String, ChatSession>()
    private val embeddingSessionMap = mutableMapOf<String, EmbeddingSession>()
    private val asrSessionMap = mutableMapOf<String, AsrSession>()
    private val ttsSessionMap = mutableMapOf<String, TTSSession>()
    private val _loadedModelsState: MutableStateFlow<MutableMap<String, ModelItem>> = MutableStateFlow(mutableMapOf<String, ModelItem>())
    val loadedModelsState: StateFlow<Map<String, ModelItem>> = _loadedModelsState

    @LogAfter("")
    suspend fun createChatSession(
        modelId: String,
        modelDir: String,
        sessionId: String,
        modelItem: ModelItem,
    ): ChatSession {
        var finalSessionId = sessionId
        if (TextUtils.isEmpty(finalSessionId)) {
            finalSessionId = System.currentTimeMillis().toString()
        }

        val session = ChatSession(
            modelId = modelId,
            sessionId = finalSessionId,
            configPath = "$modelDir/config.json"
        )

        session.load()

        chatSessionMap[modelId] = session
        _loadedModelsState.update { currentMap ->
            currentMap.toMutableMap().apply {
                put(modelId, modelItem)
            }
        }
        return session
    }

    @LogAfter("")
    suspend fun createEmbeddingSession(
        modelId: String,
        modelDir: String,
        sessionId: String,
        modelItem: ModelItem,
    ): EmbeddingSession {
        var finalSessionId = sessionId
        if (TextUtils.isEmpty(finalSessionId)) {
            finalSessionId = System.currentTimeMillis().toString()
        }

        val session = EmbeddingSession(
            modelId = modelId,
            sessionId = finalSessionId,
            configPath = "$modelDir/config.json",
        )
        session.load()

        embeddingSessionMap[modelId] = session
        _loadedModelsState.update { currentMap ->
            currentMap.toMutableMap().apply {
                put(modelId, modelItem)
            }
        }
        return session
    }

    @LogAfter("")
    suspend fun createAsrSession(
        modelId: String,
        modelDir: String,
        sessionId: String,
        modelItem: ModelItem,
    ): AsrSession {
        var finalSessionId = sessionId
        if (TextUtils.isEmpty(finalSessionId)) {
            finalSessionId = System.currentTimeMillis().toString()
        }

        val session = AsrSession(
            modelId = modelId,
            sessionId = finalSessionId,
            configPath = "$modelDir/config.json",
        )
        session.load()

        asrSessionMap[modelId] = session
        _loadedModelsState.update { currentMap ->
            currentMap.toMutableMap().apply {
                put(modelId, modelItem)
            }
        }
        return session
    }

    @LogAfter("")
    suspend fun createTTSSession(
        modelId: String,
        modelDir: String,
        sessionId: String,
        modelItem: ModelItem,
    ): TTSSession {
        try {
            var finalSessionId = sessionId
            if (TextUtils.isEmpty(finalSessionId)) {
                finalSessionId = System.currentTimeMillis().toString()
            }

            val session = TTSSession(
                modelId = modelId,
                sessionId = finalSessionId,
                configPath = modelDir,
            )
            session.load()

            ttsSessionMap[modelId] = session
            _loadedModelsState.update { currentMap ->
                currentMap.toMutableMap().apply {
                    put(modelId, modelItem)
                }
            }
            return session
        } catch (e: Exception) {
            XLog.tag(tag).e("Failed to create tts session", e)
            throw e
        }
    }

    fun getChatSession(modelId: String): ChatSession? {
        return chatSessionMap[modelId]
    }

    fun getEmbeddingSession(modelId: String): EmbeddingSession? {
        return embeddingSessionMap[modelId]
    }

    fun getAsrSession(modelId: String): AsrSession? {
        return asrSessionMap[modelId]
    }

    fun getTTSSession(modelId: String): TTSSession? {
        return ttsSessionMap[modelId]
    }

    suspend fun removeChatSession(modelId: String) {
        chatSessionMap[modelId]?.release()
        chatSessionMap.remove(modelId)
        _loadedModelsState.update { currentMap ->
            currentMap.toMutableMap().apply {
                remove(modelId)
            }
        }
    }

    suspend fun removeEmbeddingSession(modelId: String) {
        embeddingSessionMap[modelId]?.release()
        embeddingSessionMap.remove(modelId)
        _loadedModelsState.update { currentMap ->
            currentMap.toMutableMap().apply {
                remove(modelId)
            }
        }
    }

    suspend fun removeAsrSession(modelId: String) {
        asrSessionMap[modelId]?.release()
        asrSessionMap.remove(modelId)
        _loadedModelsState.update { currentMap ->
            currentMap.toMutableMap().apply {
                remove(modelId)
            }
        }
    }

    suspend fun removeTTSSession(modelId: String) {
        ttsSessionMap[modelId]?.release()
        ttsSessionMap.remove(modelId)
        _loadedModelsState.update { currentMap ->
            currentMap.toMutableMap().apply {
                remove(modelId)
            }
        }
    }

    fun getAllSessions(): List<Session> {
        return chatSessionMap.values.toList() + embeddingSessionMap.values.toList() + asrSessionMap.values.toList() + ttsSessionMap.values.toList()
    }

    fun getAllChatSessions(): List<ChatSession> {
        return chatSessionMap.values.toList()
    }

    fun getAllEmbeddingSessions(): List<EmbeddingSession> {
        return embeddingSessionMap.values.toList()
    }

    fun getAllAsrSessions(): List<AsrSession> {
        return asrSessionMap.values.toList()
    }

    fun getAllTTSSessions(): List<TTSSession> {
        return ttsSessionMap.values.toList()
    }

    suspend fun releaseAllSessions() {
        chatSessionMap.values.forEach { it.release() }
        chatSessionMap.clear()
        embeddingSessionMap.values.forEach { it.release() }
        embeddingSessionMap.clear()
        asrSessionMap.values.forEach { it.release() }
        asrSessionMap.clear()
        ttsSessionMap.values.forEach { it.release() }
        ttsSessionMap.clear()
        _loadedModelsState.emit(mutableMapOf())
    }

    fun isModelLoaded(modelId: String): Boolean {
        return chatSessionMap.containsKey(modelId) || embeddingSessionMap.containsKey(modelId) || asrSessionMap.containsKey(modelId) || ttsSessionMap.containsKey(modelId)
    }
}