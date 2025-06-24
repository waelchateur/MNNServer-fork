// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnn.server.engine

import com.taobao.meta.avatar.tts.TtsService

class TTSSession(
    override val modelId: String,
    override var sessionId: String,
    override val configPath: String,
): Session(modelId, sessionId, configPath) {
    private val tag = TTSSession::class.java.simpleName

    private val ttsService = TtsService()

    suspend fun load() {
        ttsService.init(configPath)
    }

    fun process(text: String, id: Int): ShortArray {
        return ttsService.process(text, id)
    }

    fun release() {
        ttsService.destroy()
    }

    protected fun finalize() {
        release()
    }
}