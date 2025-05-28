package io.kindbrave.mnnserver.repository.model

import android.content.Context
import com.alibaba.mls.api.ModelItem
import com.alibaba.mls.api.download.ModelDownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnnserver.annotation.LogAfter
import io.kindbrave.mnnserver.service.LLMService
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MNNModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmService: LLMService
){
    private val modelDownloadManager = ModelDownloadManager.getInstance(context)

    @LogAfter("")
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
        } else {
            llmService.removeChatSession(model.modelId!!)
        }
    }

    fun isModelLoaded(model: ModelItem): Boolean {
        return llmService.isModelLoaded(model.modelId.toString())
    }

    fun getDownloadPath(modelId: String): String {
        return modelDownloadManager.getDownloadPath(modelId).absolutePath
    }
}