package io.kindbrave.mnnserver.ui.screens.download

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alibaba.mls.api.ModelItem
import com.alibaba.mls.api.download.DownloadInfo
import com.alibaba.mls.api.download.DownloadInfo.DownloadSate
import com.alibaba.mls.api.download.DownloadListener
import dagger.hilt.android.lifecycle.HiltViewModel
import io.kindbrave.mnnserver.repository.download.MNNModelDownloadRepository
import io.kindbrave.mnnserver.repository.download.MNNModelListRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val mnnModelListRepository: MNNModelListRepository,
    private val mnnModelDownloadRepository: MNNModelDownloadRepository
) : ViewModel(), DownloadListener {
    private val tag = DownloadViewModel::class.simpleName
    private val _models: MutableStateFlow<List<ModelItem>> = MutableStateFlow(emptyList())
    val models: StateFlow<List<ModelItem>> = _models

    private val _loadModelState: MutableStateFlow<LoadingModelState> = MutableStateFlow(LoadingModelState.Idle)
    val loadModelState: StateFlow<LoadingModelState> = _loadModelState
    val downloadStateMap: MutableMap<String, MutableStateFlow<ModelDownloadState>> = mutableMapOf()

    private var lastDownloadTime = 0L

    init {
        loadModels()
        mnnModelDownloadRepository.setListener(this)
    }

    fun loadModels() {
        viewModelScope.launch(Dispatchers.IO) {
            _loadModelState.emit(LoadingModelState.Loading)
            mnnModelListRepository.loadFromCache()?.let { items ->
                _models.emit(items)
            }
            mnnModelListRepository.requestRepoList(
                onSuccess = {
                    _models.value = it
                    _loadModelState.value = LoadingModelState.Success
                },
                onFailure = {
                    _loadModelState.value = LoadingModelState.Error(it ?: "Unknown error")
                }
            )
        }
    }

    fun onModelItemClick(model: ModelItem) {
        val now = System.currentTimeMillis()
        if (now - lastDownloadTime < 500) {
            lastDownloadTime = now
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            mnnModelDownloadRepository.getModelDownloadInfo(model)?.let { downloadInfo ->
                when (downloadInfo.downlodaState) {
                    DownloadSate.NOT_START, DownloadSate.FAILED, DownloadSate.PAUSED -> {
                        mnnModelDownloadRepository.startDownload(model)
                    }
                    DownloadSate.DOWNLOADING -> {
                        mnnModelDownloadRepository.pauseDownload(model)
                    }
                }
            }
        }
    }

    override fun onDownloadStart(modelId: String) {
        viewModelScope.launch {
            downloadStateMap[modelId] = MutableStateFlow(ModelDownloadState.Start)
        }
    }

    override fun onDownloadFailed(modelId: String, hfApiException: Exception) {
        viewModelScope.launch {
            downloadStateMap[modelId]?.emit(ModelDownloadState.Failed(hfApiException))
        }
    }

    override fun onDownloadProgress(
        modelId: String,
        progress: DownloadInfo
    ) {
        viewModelScope.launch {
            downloadStateMap[modelId]?.emit(ModelDownloadState.Progress(progress))
        }
    }

    override fun onDownloadFinished(modelId: String, path: String) {
        viewModelScope.launch {
            downloadStateMap[modelId]?.emit(ModelDownloadState.Finished(path))
        }
    }

    override fun onDownloadPaused(modelId: String) {
        viewModelScope.launch {
            downloadStateMap[modelId]?.emit(ModelDownloadState.Paused)
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
    data class Failed(val hfApiException: Exception): ModelDownloadState()
    data class Progress(val progress: DownloadInfo): ModelDownloadState()
    data class Finished(val path: String): ModelDownloadState()
    data object Paused: ModelDownloadState()
    data object FileRemoved: ModelDownloadState()
}

sealed class LoadingModelState {
    data object Idle: LoadingModelState()
    data object Loading : LoadingModelState()
    data object Success : LoadingModelState()
    data class Error(val message: String) : LoadingModelState()
}