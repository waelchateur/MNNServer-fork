// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnnserver.service

import android.text.TextUtils
import io.kindbrave.mnnserver.engine.ChatSession
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class LLMService private constructor() {
    private val chatSessionMap: MutableMap<String, ChatSession> = HashMap()
    // private val embeddingSessionMap: MutableMap<String, EmbeddingSession> = HashMap()

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
    
    @Synchronized
    fun getChatSession(modelId: String): ChatSession? {
        return chatSessionMap[modelId]
    }
    
//    @Synchronized
//    fun getEmbeddingSession(modelId: String): EmbeddingSession? {
//        return embeddingSessionMap[modelId]
//    }
    
    @Synchronized
    fun removeChatSession(modelId: String) {
        chatSessionMap[modelId]?.release()
        chatSessionMap.remove(modelId)
    }
    
//    @Synchronized
//    fun removeEmbeddingSession(modelId: String) {
//        embeddingSessionMap[modelId]?.release()
//        embeddingSessionMap.remove(modelId)
//    }
    
    @Synchronized
    fun getAllChatSessions(): List<ChatSession> {
        return chatSessionMap.values.toList()
    }
    
//    @Synchronized
//    fun getAllEmbeddingSessions(): List<EmbeddingSession> {
//        return embeddingSessionMap.values.toList()
//    }
    
    @Synchronized
    fun releaseAllSessions() {
        chatSessionMap.values.forEach { it.release() }
        // embeddingSessionMap.values.forEach { it.release() }
        chatSessionMap.clear()
        // embeddingSessionMap.clear()
    }
    
    companion object {
        @Volatile
        private var instance: LLMService? = null
        
        fun getInstance(): LLMService {
            return instance ?: synchronized(this) {
                instance ?: LLMService().also { instance = it }
            }
        }
    }
}