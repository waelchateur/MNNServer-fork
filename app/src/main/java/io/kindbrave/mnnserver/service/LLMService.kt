// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnnserver.service

import android.text.TextUtils
import com.elvishew.xlog.XLog
import io.kindbrave.mnnserver.engine.ChatSession
import io.kindbrave.mnnserver.engine.EmbeddingSession
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Arrays
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMService @Inject constructor() {
    private val chatSessionMap = mutableMapOf<String, ChatSession>()
    private val embeddingSessionMap = mutableMapOf<String, EmbeddingSession>()
    private val _loadedModelsState: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val loadedModelsState: StateFlow<Set<String>> = _loadedModelsState

    suspend fun createChatSession(
        modelId: String,
        modelDir: String,
        sessionId: String,
    ): ChatSession {
        var finalSessionId = sessionId
        if (TextUtils.isEmpty(finalSessionId)) {
            finalSessionId = System.currentTimeMillis().toString()
        }

        val session = ChatSession(
            modelId = modelId,
            sessionId = finalSessionId,
            configPath = "$modelDir/config.json",
        )

        session.load()

        chatSessionMap[modelId] = session
        _loadedModelsState.emit(_loadedModelsState.value.toMutableSet().apply { add(modelId) })
        return session
    }

    suspend fun createEmbeddingSession(
        modelId: String,
        modelDir: String,
        sessionId: String
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
        _loadedModelsState.emit(_loadedModelsState.value.toMutableSet().apply { add(modelId) })
        return session
    }

    fun getChatSession(modelId: String): ChatSession? {
        return chatSessionMap[modelId]
    }

    fun getEmbeddingSession(modelId: String): EmbeddingSession? {
        return embeddingSessionMap[modelId]
    }

    suspend fun removeChatSession(modelId: String) {
        chatSessionMap[modelId]?.release()
        chatSessionMap.remove(modelId)
        _loadedModelsState.emit(_loadedModelsState.value.toMutableSet().apply { remove(modelId) })
    }

    suspend fun removeEmbeddingSession(modelId: String) {
        embeddingSessionMap[modelId]?.release()
        embeddingSessionMap.remove(modelId)
        _loadedModelsState.emit(_loadedModelsState.value.toMutableSet().apply { remove(modelId) })
    }

    fun getAllChatSessions(): List<ChatSession> {
        return chatSessionMap.values.toList()
    }

    fun getAllEmbeddingSessions(): List<EmbeddingSession> {
        return embeddingSessionMap.values.toList()
    }

    suspend fun releaseAllSessions() {
        chatSessionMap.values.forEach { it.release() }
        chatSessionMap.clear()
        embeddingSessionMap.values.forEach { it.release() }
        embeddingSessionMap.clear()
        _loadedModelsState.emit(emptySet())
    }

    fun isModelLoaded(modelId: String): Boolean {
        return chatSessionMap.containsKey(modelId) || embeddingSessionMap.containsKey(modelId)
    }
}