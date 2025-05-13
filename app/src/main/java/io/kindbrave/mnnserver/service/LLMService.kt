// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnnserver.service

import android.text.TextUtils
import io.kindbrave.mnnserver.engine.ChatSession
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LLMService @Inject constructor() {
    private val chatSessionMap = mutableMapOf<String, ChatSession>()
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
    
//    @Synchronized
//    fun createEmbeddingSession(
//        modelId: String,
//        modelDir: String,
//        useTmpPath: Boolean,
//        sessionId: String
//    ): EmbeddingSession {
//        var finalSessionId = sessionId
//        if (TextUtils.isEmpty(finalSessionId)) {
//            finalSessionId = System.currentTimeMillis().toString()
//        }
//
//        val session = EmbeddingSession(
//            modelId = modelId,
//            sessionId = finalSessionId,
//            configPath = "$modelDir/config.json",
//            useTmpPath = useTmpPath
//        )
//        session.load()
//
//        embeddingSessionMap[modelId] = session
//        return session
//    }

    fun getChatSession(modelId: String): ChatSession? {
        return chatSessionMap[modelId]
    }
    
//    @Synchronized
//    fun getEmbeddingSession(modelId: String): EmbeddingSession? {
//        return embeddingSessionMap[modelId]
//    }

    suspend fun removeChatSession(modelId: String) {
        chatSessionMap[modelId]?.release()
        chatSessionMap.remove(modelId)
        _loadedModelsState.emit(_loadedModelsState.value.toMutableSet().apply { remove(modelId) })
    }
    
//    @Synchronized
//    fun removeEmbeddingSession(modelId: String) {
//        embeddingSessionMap[modelId]?.release()
//        embeddingSessionMap.remove(modelId)
//    }

    fun getAllChatSessions(): List<ChatSession> {
        return chatSessionMap.values.toList()
    }
    
//    @Synchronized
//    fun getAllEmbeddingSessions(): List<EmbeddingSession> {
//        return embeddingSessionMap.values.toList()
//    }

    suspend fun releaseAllSessions() {
        chatSessionMap.values.forEach { it.release() }
        chatSessionMap.clear()
        _loadedModelsState.emit(emptySet())
        // embeddingSessionMap.values.forEach { it.release() }
        // embeddingSessionMap.clear()
    }

    fun isModelLoaded(modelId: String): Boolean {
        return chatSessionMap.containsKey(modelId)
    }
}