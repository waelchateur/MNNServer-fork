// Created by ruoyi.sjd on 2024/12/25.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnnserver.engine

import android.util.Log
import com.alibaba.mls.api.ApplicationProvider
import com.google.gson.Gson
import io.kindbrave.mnnserver.repository.SettingsRepository
import io.kindbrave.mnnserver.engine.MNN.AudioDataListener
import io.kindbrave.mnnserver.engine.MNN.GenerateProgressListener
import io.kindbrave.mnnserver.utils.FileUtils
import io.kindbrave.mnnserver.utils.ModelConfig
import io.kindbrave.mnnserver.utils.ModelPreferences
import io.kindbrave.mnnserver.utils.ModelUtils
import java.io.File

class ChatSession(
    internal val modelId: String,
    var sessionId: String,
    private val configPath: String,
    private val isDiffusion: Boolean = false
) {
    private val tag = ChatSession::class.java.simpleName

    private val settingsRepository = SettingsRepository(ApplicationProvider.get())

    private var extraAssistantPrompt: String? = null
    var supportOmni: Boolean = false

    private var nativePtr: Long = 0

    @Volatile
    private var modelLoading = false

    @Volatile
    private var generating = false

    @Volatile
    private var releaseRequeted = false

    suspend fun load() {
        Log.d(tag, "Loading model: $modelId")
        modelLoading = true

        var rootCacheDir: String? = ""
        if (ModelPreferences.useMmap(ApplicationProvider.get(), modelId)) {
            rootCacheDir = FileUtils.getMmapDir(modelId, configPath.contains("modelscope"))
            File(rootCacheDir).mkdirs()
        }
        val useOpencl = ModelPreferences.getBoolean(
            ApplicationProvider.get(),
            modelId, ModelPreferences.KEY_BACKEND, false
        )
        val backend = if (useOpencl) "opencl" else "cpu"
        val configMap = HashMap<String, Any>().apply {
            put("is_diffusion", isDiffusion)
            put("is_r1", ModelUtils.isR1Model(modelId))
            put("mmap_dir", rootCacheDir ?: "")
            put("diffusion_memory_mode", settingsRepository.getDiffusionMemoryMode())
        }
        val extraConfig = ModelConfig.loadConfig(configPath, getModelSettingsFile())?.apply {
            extraAssistantPrompt = if (this.thinkingMode == true) {
                "<|im_start|>assistant\n%s<|im_end|>\n"
            } else {
                "<|im_start|>assistant\n<think>\n</think>%s<|im_end|>\n"
            }
            this.assistantPromptTemplate = extraAssistantPrompt
            this.backendType = backend
        }
        Log.d(tag, "MNN_DEBUG load initNative")
        nativePtr = MNN.initNative(
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

    val debugInfo: String
        get() = MNN.getDebugInfoNative(nativePtr) + "\n"

    fun generateNewSession(): String {
        this.sessionId = System.currentTimeMillis().toString()
        return this.sessionId
    }

    fun generate(
        history: List<Pair<String, String>>,
        progressListener: GenerateProgressListener
    ): HashMap<String, Any> {
        synchronized(this) {
            Log.d(tag, "MNN_DEBUG submit$history")
            generating = true
            val result = MNN.submitNative(nativePtr, history, progressListener)
            generating = false
            if (releaseRequeted) {
                release()
            }
            return result
        }
    }

    fun generateDiffusion(
        input: String,
        output: String,
        iterNum: Int,
        randomSeed: Int,
        progressListener: GenerateProgressListener
    ): HashMap<String, Any> {
        synchronized(this) {
            Log.d(tag, "MNN_DEBUG submit$input")
            generating = true
            val result = MNN.submitDiffusionNative(
                nativePtr,
                input,
                output,
                iterNum,
                randomSeed,
                progressListener
            )
            generating = false
            if (releaseRequeted) {
                releaseInner()
            }
            return result
        }
    }

    fun reset() {
        synchronized(this) {
            MNN.resetNative(nativePtr, isDiffusion)
        }
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
        if (isDiffusion) {
            return null
        }
        return ModelConfig.loadConfig(configPath, getModelSettingsFile())
    }

    fun getModelSettingsFile():String {
        return FileUtils.getModelConfigDir(modelId) + "/custom_config.json"
    }

    private fun releaseInner() {
        if (nativePtr != 0L) {
            MNN.releaseNative(nativePtr, isDiffusion)
            nativePtr = 0
            (this as Object).notifyAll()
        }
    }

    fun clearMmapCache() {
        FileUtils.clearMmapCache(modelId)
    }

    fun setAudioDataListener(listener: AudioDataListener?) {
        synchronized(this) {
            if (nativePtr != 0L) {
                MNN.setWavformCallbackNative(nativePtr, listener)
            } else {
                Log.e(tag, "nativePtr null")
            }
        }
    }

    fun updateMaxNewTokens(maxNewTokens: Int) {
        MNN.updateMaxNewTokensNative(nativePtr, maxNewTokens)
    }

    fun updateSystemPrompt(systemPrompt: String) {
        MNN.updateSystemPromptNative(nativePtr, systemPrompt)
    }

    fun updateAssistantPrompt(assistantPrompt: String) {
        extraAssistantPrompt = assistantPrompt
        MNN.updateAssistantPromptNative(nativePtr, assistantPrompt)
    }

    protected fun finalize() {
        release()
    }
}