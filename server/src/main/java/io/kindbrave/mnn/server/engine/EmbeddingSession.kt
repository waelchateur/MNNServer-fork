// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnn.server.engine

import android.util.Log
import com.google.gson.Gson
import io.kindbrave.mnn.server.utils.FileUtils
import io.kindbrave.mnn.base.utils.ModelConfig
import java.io.File

class EmbeddingSession(
    override val modelId: String,
    override var sessionId: String,
    override val configPath: String,
): Session(modelId, sessionId, configPath) {
    private val tag = EmbeddingSession::class.java.simpleName

    private var nativePtr: Long = 0

    @Volatile
    private var modelLoading = false

    @Volatile
    private var generating = false

    @Volatile
    private var releaseRequeted = false

    fun load() {
        modelLoading = true


        val extraConfig = ModelConfig.loadConfig(configPath, getModelSettingsFile())

        var rootCacheDir: String? = ""
        if (extraConfig?.mmap == true) {
            rootCacheDir = FileUtils.getMmapDir(modelId, configPath.contains("modelscope"))
            File(rootCacheDir).mkdirs()
        }

        val configMap = HashMap<String, Any>().apply {
            put("mmap_dir", rootCacheDir ?: "")
        }

        Log.d(tag, "MNN_DEBUG load initNative")
        nativePtr = MNNEmbedding.initNative(
            configPath,
            if (extraConfig != null) {
                Gson().toJson(extraConfig)
            } else {
                "{}"
            },
            Gson().toJson(configMap)
        )
        Log.d(tag, "MNN_DEBUG load initNative end")
        modelLoading = false
        if (releaseRequeted) {
            release()
        }
    }

    fun generateNewSession(): String {
        this.sessionId = System.currentTimeMillis().toString()
        return this.sessionId
    }

    fun reset() {

    }

    fun release() {
        synchronized(this) {
            Log.d(
                tag,
                "MNN_DEBUG release nativePtr: $nativePtr mGenerating: $generating"
            )
            if (!generating && !modelLoading) {
                releaseInner()
            } else {
                releaseRequeted = true
                while (generating || modelLoading) {
                    try {
                        (this as Object).wait()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        Log.e(tag, "Thread interrupted while waiting for release", e)
                    }
                }
                releaseInner()
            }
        }
    }

    fun loadConfig(): ModelConfig? {
        return ModelConfig.loadConfig(configPath, getModelSettingsFile())
    }

    fun getModelSettingsFile():String {
        return FileUtils.getModelConfigDir(modelId) + "/custom_config.json"
    }

    private fun releaseInner() {
        if (nativePtr != 0L) {
            MNNEmbedding.releaseNative(nativePtr)
            nativePtr = 0
            (this as Object).notifyAll()
        }
    }

    fun clearMmapCache() {
        FileUtils.clearMmapCache(modelId)
    }

    fun updateMaxNewTokens(maxNewTokens: Int) {
        MNNEmbedding.updateMaxNewTokensNative(nativePtr, maxNewTokens)
    }

    fun embedding(text: String): FloatArray {
        return MNNEmbedding.embedding(nativePtr, text)
    }

    protected fun finalize() {
        release()
    }
}