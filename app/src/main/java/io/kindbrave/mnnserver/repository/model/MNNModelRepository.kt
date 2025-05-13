package io.kindbrave.mnnserver.repository.model

import android.content.Context
import com.alibaba.mls.api.ModelItem
import com.alibaba.mls.api.download.ModelDownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnnserver.annotation.LogAfter
import io.kindbrave.mnnserver.annotation.LogBefore
import io.kindbrave.mnnserver.service.LLMService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.mutableSetOf

@Singleton
class MNNModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmService: LLMService
){
    private val tag = MNNModelRepository::class.java.simpleName
    private val modelDownloadManager = ModelDownloadManager.getInstance(context)

    @LogAfter("")
    suspend fun loadModel(model: ModelItem) {
        if (llmService.isModelLoaded(model.modelId.toString())) return
        if (model.modelId.isNullOrBlank()) throw NullPointerException("modelId is null")
        val modelPath = modelDownloadManager.getDownloadPath(model.modelId!!)
        llmService.createChatSession(
            modelId = model.modelId!!,
            modelDir = modelPath.path,
            sessionId = model.modelId!!
        )
    }

    @LogAfter("")
    suspend fun unloadModel(model: ModelItem) {
        if (llmService.isModelLoaded(model.modelId.toString()).not()) return
        if (model.modelId.isNullOrBlank()) throw NullPointerException("modelId is null")
        llmService.removeChatSession(model.modelId!!)
    }

    fun isModelLoaded(model: ModelItem): Boolean {
        return llmService.isModelLoaded(model.modelId.toString())
    }
}