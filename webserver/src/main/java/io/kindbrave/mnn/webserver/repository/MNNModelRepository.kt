package io.kindbrave.mnn.webserver.repository

import android.content.Context
import com.alibaba.mls.api.ModelItem
import com.alibaba.mls.api.download.ModelDownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnn.webserver.annotation.LogAfter
import io.kindbrave.mnn.webserver.service.LLMService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MNNModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmService: LLMService
){
    private val modelDownloadManager = ModelDownloadManager.getInstance(context)

    suspend fun loadModel(model: ModelItem) {
        if (llmService.isModelLoaded(model.modelId.toString())) return
        if (model.modelId.isNullOrBlank()) throw NullPointerException("modelId is null")
        val modelPath = getDownloadPath(model.modelId!!)
        if (model.getTags().contains("embedding")) {
            llmService.createEmbeddingSession(
                modelId = model.modelId!!,
                modelDir = modelPath,
                sessionId = model.modelId!!
            )
        } else if (model.getTags().contains("asr")) {
            llmService.createAsrSession(
                modelId = model.modelId!!,
                modelDir = modelPath,
                sessionId = model.modelId!!
            )
        } else {
            llmService.createChatSession(
                modelId = model.modelId!!,
                modelDir = modelPath,
                sessionId = model.modelId!!
            )
        }
    }

    @LogAfter("")
    suspend fun unloadModel(model: ModelItem) {
        if (llmService.isModelLoaded(model.modelId.toString()).not()) return
        if (model.modelId.isNullOrBlank()) throw NullPointerException("modelId is null")
        if (model.getTags().contains("embedding")) {
            llmService.removeEmbeddingSession(model.modelId!!)
        } else if (model.getTags().contains("asr")) {
            llmService.removeAsrSession(model.modelId!!)
        } else {
            llmService.removeChatSession(model.modelId!!)
        }
    }

    fun isModelLoaded(model: ModelItem): Boolean {
        return llmService.isModelLoaded(model.modelId.toString())
    }

    fun getDownloadPath(modelId: String): String {
        return modelDownloadManager.getDownloadedFile(modelId)?.absolutePath ?: ""
    }
}