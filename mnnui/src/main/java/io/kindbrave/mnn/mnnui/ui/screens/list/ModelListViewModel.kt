package io.kindbrave.mnn.mnnui.ui.screens.list

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.mls.api.ModelItem
import com.alibaba.mls.api.download.DownloadInfo
import com.alibaba.mls.api.download.DownloadInfo.DownloadSate
import com.alibaba.mls.api.download.DownloadListener
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnn.mnnui.repository.ConfigRepository
import io.kindbrave.mnn.mnnui.utils.CustomModelUtils
import io.kindbrave.mnn.mnnui.utils.ModelNameUtils
import io.kindbrave.mnn.webserver.annotation.LogAfter
import io.kindbrave.mnnserver.R
import io.kindbrave.mnn.webserver.repository.KindBraveMNNModelDownloadRepository
import io.kindbrave.mnn.webserver.repository.UserUploadModelRepository
import io.kindbrave.mnn.webserver.repository.MNNModelDownloadRepository
import io.kindbrave.mnn.webserver.repository.MNNModelRepository
import io.kindbrave.mnn.server.service.LLMService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModelListViewModel @Inject constructor(
    private val mnnModelRepository: MNNModelRepository,
    private val mnnModelDownloadRepository: MNNModelDownloadRepository,
    private val kindBraveMNNModelDownloadRepository: KindBraveMNNModelDownloadRepository,
    private val userUploadModelRepository: UserUploadModelRepository,
    private val configRepository: ConfigRepository,
    private val llmService: LLMService,
    @ApplicationContext private val context: Context
) : ViewModel(), DownloadListener {
    private val tag = ModelListViewModel::class.simpleName
    private val _downloadModels: MutableStateFlow<List<ModelItem>> = MutableStateFlow(emptyList())
    val downloadModels: StateFlow<List<ModelItem>> = _downloadModels
    private val _customDownloadModels: MutableStateFlow<List<ModelItem>> = MutableStateFlow(emptyList())
    val customDownloadModels: StateFlow<List<ModelItem>> = _customDownloadModels
    private val _userUploadModels: MutableStateFlow<List<UserUploadModelRepository.ModelInfo>> = MutableStateFlow(emptyList())
    val userUploadModels: StateFlow<List<UserUploadModelRepository.ModelInfo>> = _userUploadModels

    private val _getDownloadModelState: MutableStateFlow<GetDownloadModelState> = MutableStateFlow(GetDownloadModelState.Idle)
    val getDownloadModelState: StateFlow<GetDownloadModelState> = _getDownloadModelState
    private val _getCustomDownloadModelState: MutableStateFlow<GetDownloadModelState> = MutableStateFlow(GetDownloadModelState.Idle)
    val getCustomDownloadModelState: StateFlow<GetDownloadModelState> = _getCustomDownloadModelState

    val downloadStateMap: MutableMap<String, MutableStateFlow<ModelDownloadState>> = mutableMapOf()

    private val _loadingState: MutableStateFlow<LoadingState> = MutableStateFlow(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState

    private var lastDownloadTime = 0L

    val loadedModels: MutableStateFlow<Map<String, ModelItem>> = MutableStateFlow(emptyMap())

    init {
        getDownloadModels()
        getCustomDownloadModels()
        getUserUploadModels()
        mnnModelDownloadRepository.setListener(this)
        kindBraveMNNModelDownloadRepository.setListener(this)
        collectLoadedChatSessions()
    }

    private fun collectLoadedChatSessions() {
        viewModelScope.launch {
            llmService.loadedModelsState.collect { loadedModelMap ->
                loadedModels.emit(loadedModelMap)
                // 加载模型更新后存储到配置文件
                configRepository.setLastRunningModels(loadedModelMap)
            }
        }
    }

    private fun getDownloadModels() {
        viewModelScope.launch(Dispatchers.IO) {
            _getDownloadModelState.emit(GetDownloadModelState.Loading)
            mnnModelDownloadRepository.loadFromCache()?.let { items ->
                _downloadModels.emit(CustomModelUtils.processList(items))
            }
            mnnModelDownloadRepository.requestRepoList(
                onSuccess = {
                    _downloadModels.value = CustomModelUtils.processList(it)
                    _getDownloadModelState.value = GetDownloadModelState.Success
                },
                onFailure = {
                    _getDownloadModelState.value = GetDownloadModelState.Error(it ?: "Unknown error")
                }
            )
        }
    }

    private fun getCustomDownloadModels() {
        viewModelScope.launch(Dispatchers.IO) {
            kindBraveMNNModelDownloadRepository.loadFromCache()?.let { items ->
                _customDownloadModels.emit(CustomModelUtils.processList(items))
            }
            kindBraveMNNModelDownloadRepository.requestRepoList(
                onSuccess = {
                    _customDownloadModels.value = CustomModelUtils.processList(it)
                    _getCustomDownloadModelState.value = GetDownloadModelState.Success
                },
                onFailure = {
                    _getCustomDownloadModelState.value = GetDownloadModelState.Error(it ?: "Unknown error")
                }
            )
        }
    }

    fun startDownload(model: ModelItem) {
        val now = System.currentTimeMillis()
        if (now - lastDownloadTime < 500) {
            lastDownloadTime = now
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            mnnModelDownloadRepository.startDownload(model)
        }
    }

    fun pauseDownload(model: ModelItem) {
        val now = System.currentTimeMillis()
        if (now - lastDownloadTime < 500) {
            lastDownloadTime = now
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            mnnModelDownloadRepository.pauseDownload(model)
        }
    }

    fun deleteDownloadModel(model: ModelItem) {
        viewModelScope.launch(Dispatchers.IO) {
            mnnModelDownloadRepository.deleteModel(model)
        }
    }

    /**
     * 用户进入界面后刷新模型下载状态
     */
    fun updateDownloadState(model: ModelItem) {
        val modelId = model.modelId ?: ""
        downloadStateMap[modelId] = MutableStateFlow(ModelDownloadState.Idle)
        viewModelScope.launch {
            mnnModelDownloadRepository.getModelDownloadInfo(model)?.let { downloadInfo ->
                when (downloadInfo.downlodaState) {
                    DownloadSate.NOT_START -> {
                        downloadStateMap[modelId]!!.emit(ModelDownloadState.Idle)
                    }
                    DownloadSate.DOWNLOADING -> {
                        downloadStateMap[modelId]!!.emit(ModelDownloadState.Progress(downloadInfo.progress))
                    }
                    DownloadSate.COMPLETED -> {
                        downloadStateMap[modelId]!!.emit(ModelDownloadState.Finished(downloadInfo.currentFile.toString()))
                    }
                    DownloadSate.PAUSED -> {
                        // 如果是暂停状态，但是进度为0，显示为NOT_START
                        if (downloadInfo.progress == 0.0) {
                            downloadStateMap[modelId]!!.emit(ModelDownloadState.Idle)
                        } else{
                            downloadStateMap[modelId]!!.emit(ModelDownloadState.Paused(downloadInfo.progress))
                        }
                    }
                    DownloadSate.FAILED -> {
                        downloadStateMap[modelId]!!.emit(ModelDownloadState.Failed(downloadInfo.errorMessage.toString()))
                    }
                }
            }
        }
    }

    fun startCustomDownload(model: ModelItem) {
        val now = System.currentTimeMillis()
        if (now - lastDownloadTime < 500) {
            lastDownloadTime = now
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            kindBraveMNNModelDownloadRepository.startDownload(model)
        }
    }

    fun pauseCustomDownload(model: ModelItem) {
        val now = System.currentTimeMillis()
        if (now - lastDownloadTime < 500) {
            lastDownloadTime = now
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            kindBraveMNNModelDownloadRepository.pauseDownload(model)
        }
    }

    fun deleteCustomDownloadModel(model: ModelItem) {
        viewModelScope.launch(Dispatchers.IO) {
            kindBraveMNNModelDownloadRepository.deleteModel(model)
        }
    }

    fun updateCustomDownloadState(model: ModelItem) {
        val modelId = model.modelId ?: ""
        downloadStateMap[modelId] = MutableStateFlow(ModelDownloadState.Idle)
        viewModelScope.launch {
            kindBraveMNNModelDownloadRepository.getModelDownloadInfo(model)?.let { downloadInfo ->
                when (downloadInfo.downlodaState) {
                    DownloadSate.NOT_START -> {
                        downloadStateMap[modelId]!!.emit(ModelDownloadState.Idle)
                    }
                    DownloadSate.DOWNLOADING -> {
                        downloadStateMap[modelId]!!.emit(ModelDownloadState.Progress(downloadInfo.progress))
                    }
                    DownloadSate.COMPLETED -> {
                        downloadStateMap[modelId]!!.emit(ModelDownloadState.Finished(downloadInfo.currentFile.toString()))
                    }
                    DownloadSate.PAUSED -> {
                        // 如果是暂停状态，但是进度为0，显示为NOT_START
                        if (downloadInfo.progress == 0.0) {
                            downloadStateMap[modelId]!!.emit(ModelDownloadState.Idle)
                        } else{
                            downloadStateMap[modelId]!!.emit(ModelDownloadState.Paused(downloadInfo.progress))
                        }
                    }
                    DownloadSate.FAILED -> {
                        downloadStateMap[modelId]!!.emit(ModelDownloadState.Failed(downloadInfo.errorMessage.toString()))
                    }
                }
            }
        }
    }

    fun loadDownloadModel(model: ModelItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.emit(LoadingState.Loading(context.getString(R.string.loading_model, model.modelName)))
            runCatching {
                mnnModelRepository.loadModel(model)
                _loadingState.emit(LoadingState.Idle)
            }.onFailure { e ->
                _loadingState.emit(LoadingState.Error(e.message.toString()))
            }
        }
    }

    fun unloadDownloadModel(model: ModelItem) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.emit(LoadingState.Loading(context.getString(R.string.unloading_model, model.modelName)))
            runCatching {
                mnnModelRepository.unloadModel(model)
                _loadingState.emit(LoadingState.Idle)
            }.onFailure { e ->
                _loadingState.emit(LoadingState.Error(e.message.toString()))
            }
        }
    }

    fun isModelLoaded(model: ModelItem): Boolean {
        return mnnModelRepository.isModelLoaded(model)
    }

    private fun getUserUploadModels() {
        viewModelScope.launch {
            userUploadModelRepository.refreshModels()
            userUploadModelRepository.modelList.collect { models ->
                _userUploadModels.emit(models)
            }
        }
    }

    fun loadUserUploadModel(model: UserUploadModelRepository.ModelInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.emit(LoadingState.Loading(context.getString(R.string.loading_model, model.name)))
            runCatching {
                userUploadModelRepository.loadModel(model)
                _loadingState.emit(LoadingState.Idle)
                configRepository.setLastRunningModels(loadedModels.value)
            }.onFailure { e ->
                _loadingState.emit(LoadingState.Error(e.message.toString()))
            }
        }
    }

    fun unloadUserUploadModel(model: UserUploadModelRepository.ModelInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.emit(LoadingState.Loading(context.getString(R.string.unloading_model, model.name)))
            runCatching {
                userUploadModelRepository.unloadModel(model)
                _loadingState.emit(LoadingState.Idle)
            }.onFailure { e ->
                _loadingState.emit(LoadingState.Error(e.message.toString()))
            }
        }
    }

    fun deleteUserUploadModel(model: UserUploadModelRepository.ModelInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            _loadingState.emit(LoadingState.Loading(context.getString(R.string.delete_model)))
            runCatching {
                userUploadModelRepository.deleteModel(model.id)
                _loadingState.emit(LoadingState.Idle)
            }.onFailure { e ->
                _loadingState.emit(LoadingState.Error(e.message.toString()))
            }
        }
    }

    fun isModelLoaded(model: UserUploadModelRepository.ModelInfo): Boolean {
        return userUploadModelRepository.isModelLoaded(model)
    }

    fun onModelNameEntered(modelName: String, folderUri: Uri?) {
        if (ModelNameUtils.isValidModelName(modelName).not()) {
            _loadingState.value = LoadingState.Error(context.getString(R.string.invalid_model_name))
            return
        }

        viewModelScope.launch {
            _loadingState.emit(LoadingState.Loading(context.getString(R.string.import_model)))
            runCatching {
                userUploadModelRepository.uploadUserModelFromUri(folderUri, modelName)
                _loadingState.emit(LoadingState.Idle)
            }.onFailure {
                _loadingState.emit(LoadingState.Error(it.message.toString()))
            }
        }
    }

    fun getModelPath(modelId: String): String {
        return mnnModelRepository.getDownloadPath(modelId)
    }

    override fun onDownloadTotalSize(modelId: String, totalSize: Long) {

    }

    override fun onDownloadStart(modelId: String) {
        viewModelScope.launch {
            downloadStateMap[modelId]?.emit(ModelDownloadState.Start)
        }
    }

    override fun onDownloadFailed(modelId: String, hfApiException: Exception) {
        viewModelScope.launch {
            downloadStateMap[modelId]?.emit(ModelDownloadState.Failed(hfApiException.message.toString()))
        }
    }

    override fun onDownloadProgress(
        modelId: String,
        progress: DownloadInfo
    ) {
        viewModelScope.launch {
            downloadStateMap[modelId]?.emit(ModelDownloadState.Progress(progress.progress))
        }
    }

    override fun onDownloadFinished(modelId: String, path: String) {
        viewModelScope.launch {
            downloadStateMap[modelId]?.emit(ModelDownloadState.Finished(path))
        }
    }

    override fun onDownloadPaused(modelId: String) {
        viewModelScope.launch {
            downloadStateMap[modelId]?.emit(ModelDownloadState.Paused((-1.0)))
        }
    }

    override fun onDownloadFileRemoved(modelId: String) {
        viewModelScope.launch {
            downloadStateMap[modelId]?.emit(ModelDownloadState.FileRemoved)
        }
    }
}

sealed class ModelDownloadState {
    data object Idle: ModelDownloadState()
    data object Start: ModelDownloadState()
    data class Failed(val message: String): ModelDownloadState()
    data class Progress(val progress: Double): ModelDownloadState()
    data class Finished(val path: String): ModelDownloadState()
    data class Paused(val progress: Double): ModelDownloadState()
    data object FileRemoved: ModelDownloadState()
}

sealed class GetDownloadModelState {
    data object Idle: GetDownloadModelState()
    data object Loading : GetDownloadModelState()
    data object Success : GetDownloadModelState()
    data class Error(val message: String) : GetDownloadModelState()
}

sealed class LoadingState {
    data object Idle: LoadingState()
    data class Loading(val message: String) : LoadingState()
    data class Error(val message: String) : LoadingState()
    data object Success : LoadingState()
}

sealed interface DownloadModelType {
    data object OFFICIAL : DownloadModelType
    data object CUSTOM : DownloadModelType
}