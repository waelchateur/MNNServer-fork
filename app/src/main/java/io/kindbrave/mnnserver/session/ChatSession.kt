// Created by ruoyi.sjd on 2024/12/26.
// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
// Created by KindBrave on 2025/03/26.
package io.kindbrave.mnnserver.session

import android.util.Log
import io.kindbrave.mnnserver.api.ApplicationProvider
import io.kindbrave.mnnserver.model.ChatDataItem
import io.kindbrave.mnnserver.service.LLMService
import io.kindbrave.mnnserver.utils.FileUtils
import io.kindbrave.mnnserver.utils.ModelPreferences
import io.kindbrave.mnnserver.utils.ModelUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.Serializable

class ChatSession(
    internal val modelId: String,
    var sessionId: String,
    private val configPath: String,
    private val useTmpPath: Boolean,
    private val useTemplate: Boolean = true
) : Serializable {

    private val tag = ChatSession::class.java.simpleName
    
    private var nativePtr: Long = 0
    
    @Volatile
    private var isModelLoading = false
    
    @Volatile
    private var isGenerating = false
    
    @Volatile
    private var isReleaseRequested = false

    private val mnn = MNN()
    
    init {
        Log.d(tag, "Initializing ChatSession with modelId: $modelId")
    }
    
    fun load() {
        Log.d(tag, "Loading model: $modelId")
        isModelLoading = true
        
        try {
            // 获取模型配置
            val useOpenCL = ModelPreferences.getBoolean(ApplicationProvider.get(), modelId, ModelPreferences.KEY_BACKEND, false);
            val sampler = ModelPreferences.getString(ApplicationProvider.get(), modelId, ModelPreferences.KEY_SAMPLER, "greedy");
            val isR1Model = ModelUtils.isR1Model(modelId)
            val rootCacheDir = if (ModelPreferences.useMmap(ApplicationProvider.get(), modelId)) {
                val dir = FileUtils.getMmapDir(modelId, configPath.contains("modelscope"));
                File(dir).mkdirs()
                dir
            } else ""

            val configJson = JSONObject()
            try {
                configJson.put("backend", "cpu")
                configJson.put("sampler", sampler)
                configJson.put("is_diffusion", false)
                configJson.put("is_r1", ModelUtils.isR1Model(modelId))
                configJson.put(
                    "diffusion_memory_mode",
                    "0"
                )
            } catch (e: JSONException) {
                throw RuntimeException(e)
            }
            
            // 调用JNI初始化模型
            nativePtr = mnn.initNative(
                rootCacheDir = rootCacheDir,
                modelId = modelId,
                configPath = configPath,
                useTmpPath = useTmpPath,
                configJsonStr = configJson.toString()
            )
            
            Log.d(tag, "Model loaded successfully, nativePtr: $nativePtr")
        } catch (e: Exception) {
            Log.e(tag, "Failed to load model", e)
            throw e
        } finally {
            isModelLoading = false
            if (isReleaseRequested) {
                release()
            }
        }
    }
    
    fun generate(
        history: MutableList<ChatDataItem>,
        progressListener: GenerateProgressListener?
    ): HashMap<String?, Any?>? {
        Log.e(tag, "$history")
        synchronized(this) {
            if (nativePtr == 0L) {
                Log.e(tag, "Cannot generate: model not loaded")
                return null
            }

            isGenerating = true
            
            try {
                return mnn.submitNative(nativePtr, history, progressListener)
            } finally {
                isGenerating = false
                if (isReleaseRequested) {
                    release()
                }
            }
        }
    }
    
    fun reset() {
        synchronized(this) {
            if (nativePtr != 0L) {
                Log.d(tag, "Resetting session")
                mnn.resetNative(nativePtr)
            }
        }
    }
    
    fun release() {
        synchronized(this) {
            Log.d(tag, "Releasing session, nativePtr: $nativePtr, isGenerating: $isGenerating")
            
            if (!isGenerating && !isModelLoading) {
                releaseInternal()
            } else {
                isReleaseRequested = true
                // 等待生成或加载完成
                while (isGenerating || isModelLoading) {
                    try {
                        (this as Object).wait(100)
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        Log.e(tag, "Thread interrupted while waiting for release", e)
                    }
                }
                releaseInternal()
            }
        }
    }
    
    private fun releaseInternal() {
        if (nativePtr != 0L) {
            mnn.releaseNative(nativePtr, false)
            nativePtr = 0
            LLMService.getInstance().removeChatSession(sessionId)
            (this as Object).notifyAll()
        }
    }
    
    fun getDebugInfo(): String {
        return if (nativePtr != 0L) {
            mnn.getDebugInfoNative(nativePtr) ?: "No debug info available"
        } else {
            "Model not loaded"
        }
    }

    protected fun finalize() {
        release()
    }
    
    interface GenerateProgressListener {
        /**
         * 生成进度回调
         * @param progress 生成的文本片段
         * @return 是否停止生成
         */
        fun onProgress(progress: String?): Boolean
    }
}