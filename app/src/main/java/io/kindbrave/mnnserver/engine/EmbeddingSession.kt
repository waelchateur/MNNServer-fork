//// Created by ruoyi.sjd on 2024/12/26.
//// Copyright (c) 2024 Alibaba Group Holding Limited All rights reserved.
//// Created by KindBrave on 2025/03/26.
//package io.kindbrave.mnnserver.engine
//
//import android.util.Log
//import io.kindbrave.mnnserver.api.ApplicationProvider
//import io.kindbrave.mnnserver.service.LLMService
//import io.kindbrave.mnnserver.utils.FileUtils
//import io.kindbrave.mnnserver.utils.ModelPreferences
//import java.io.File
//import java.io.Serializable
//
//class EmbeddingSession(
//    private val modelId: String,
//    var sessionId: String,
//    private val configPath: String,
//    private val useTmpPath: Boolean
//) : Serializable {
//
//    private var nativePtr: Long = 0
//    private val TAG = "EmbeddingSession"
//
//    @Volatile
//    private var isModelLoading = false
//
//    @Volatile
//    private var isProcessing = false
//
//    @Volatile
//    private var isReleaseRequested = false
//
//    init {
//        Log.d(TAG, "Initializing EmbeddingSession with modelId: $modelId")
//    }
//
//    fun load() {
//        Log.d(TAG, "Loading embedding model: $modelId")
//        isModelLoading = true
//
//        try {
//            val useOpenCL = ModelPreferences.getBoolean(ApplicationProvider.get(), modelId, ModelPreferences.KEY_BACKEND, false);
//            val rootCacheDir = if (ModelPreferences.useMmap(ApplicationProvider.get(), modelId)) {
//                val dir = FileUtils.getMmapDir(modelId, configPath.contains("modelscope"));
//                File(dir).mkdirs()
//                dir
//            } else ""
//
//            nativePtr = mnn.initEmbeddingNative(
//                rootCacheDir = rootCacheDir,
//                configPath = configPath,
//                backend = useOpenCL
//            )
//
//            Log.d(TAG, "Embedding model loaded successfully, nativePtr: $nativePtr")
//        } catch (e: Exception) {
//            Log.e(TAG, "Failed to load embedding model", e)
//        } finally {
//            isModelLoading = false
//            if (isReleaseRequested) {
//                release()
//            }
//        }
//    }
//
//    fun getEmbedding(text: String): FloatArray? {
//        synchronized(this) {
//            if (nativePtr == 0L) {
//                Log.e(TAG, "Cannot generate embedding: model not loaded")
//                return null
//            }
//
//            Log.d(TAG, "Generating embedding for text: $text")
//            isProcessing = true
//
//            try {
//                return mnn.getTextEmbeddingNative(nativePtr, text)
//            } finally {
//                isProcessing = false
//                if (isReleaseRequested) {
//                    release()
//                }
//            }
//        }
//    }
//
//    fun release() {
//        synchronized(this) {
//            Log.d(TAG, "Releasing embedding session, nativePtr: $nativePtr")
//
//            if (!isProcessing && !isModelLoading) {
//                releaseInternal()
//            } else {
//                isReleaseRequested = true
//                // 等待处理或加载完成
//                while (isProcessing || isModelLoading) {
//                    try {
//                        (this as Object).wait(100)
//                    } catch (e: InterruptedException) {
//                        Thread.currentThread().interrupt()
//                        Log.e(TAG, "Thread interrupted while waiting for release", e)
//                    }
//                }
//                releaseInternal()
//            }
//        }
//    }
//
//    private fun releaseInternal() {
//        if (nativePtr != 0L) {
//            mnn.releaseEmbeddingNative(nativePtr)
//            nativePtr = 0
//            LLMService.Companion.getInstance().removeEmbeddingSession(sessionId)
//            (this as Object).notifyAll()
//        }
//    }
//
//    protected fun finalize() {
//        release()
//    }
//}