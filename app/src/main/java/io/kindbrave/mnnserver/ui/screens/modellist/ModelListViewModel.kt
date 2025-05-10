package io.kindbrave.mnnserver.ui.screens.modellist

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.kindbrave.mnnserver.R
import io.kindbrave.mnnserver.model.ModelManager
import io.kindbrave.mnnserver.service.LLMService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelListViewModel(private val application: Application) : AndroidViewModel(application) {
    private val modelManager = ModelManager.Companion.getInstance(application)
    private val llmService = LLMService.Companion.getInstance()

    private val _modelList = MutableStateFlow<List<ModelManager.ModelInfo>>(emptyList())
    val modelList: StateFlow<List<ModelManager.ModelInfo>> = _modelList.asStateFlow()

    // 导入状态管理
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    // 加载状态管理
    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    // 选中的URI
    private var selectedUri: Uri? = null

    init {
        viewModelScope.launch {
            modelManager.modelList.collect { models ->
                _modelList.value = models
            }
        }
    }

    fun importModel() {
        _importState.value = ImportState.SelectFolder
    }

    fun onFolderSelected(uri: Uri) {
        selectedUri = uri
        _importState.value = ImportState.EnterName
    }

    fun onNameEntered(modelName: String) {
        if (!isValidModelName(modelName)) {
            _importState.value = ImportState.Error(application.getString(R.string.invalid_model_name))
            return
        }

        viewModelScope.launch {
            _importState.value = ImportState.Importing
            selectedUri?.let { uri ->
                try {
                    val success = modelManager.importModelFromUri(uri, modelName)
                    if (success) {
                        _importState.value = ImportState.Success
                    } else {
                        _importState.value = ImportState.Error("导入失败")
                    }
                } catch (e: Exception) {
                    _importState.value = ImportState.Error(e.message ?: "导入失败")
                }
            } ?: run {
                _importState.value = ImportState.Error("未选择文件夹")
            }
        }
    }

    fun resetImportState() {
        _importState.value = ImportState.Idle
        selectedUri = null
    }

    fun loadModel(model: ModelManager.ModelInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _loadingState.value = LoadingState.Loading(application.getString(R.string.loading_model, model.name))
                // 创建聊天会话
                llmService.createChatSession(
                    modelId = model.id,
                    modelDir = model.path,
                    sessionId = model.id
                )
                modelManager.updateModelLoadState(model.id, true)
            } catch (e: Exception) {
                modelManager.updateModelLoadState(model.id, false)
            } finally {
                _loadingState.value = LoadingState.Idle
            }
        }
    }

    fun unloadModel(model: ModelManager.ModelInfo) {
        viewModelScope.launch {
            try {
                _loadingState.value = LoadingState.Loading(application.getString(R.string.unloading_model, model.name))
                // 移除并释放会话
                llmService.removeChatSession(model.id)
                modelManager.updateModelLoadState(model.id, false)
            } catch (e: Exception) {
                // TODO: 处理卸载失败的情况
            } finally {
                _loadingState.value = LoadingState.Idle
            }
        }
    }

    fun deleteModel(model: ModelManager.ModelInfo) {
        viewModelScope.launch {
            // 先卸载模型
            unloadModel(model)
            // 然后删除模型文件
            modelManager.deleteModel(model.id)
        }
    }

    fun refreshModels() {
        modelManager.refreshModels()
    }

    sealed class ImportState {
        object Idle : ImportState()
        object SelectFolder : ImportState()
        object EnterName : ImportState()
        object Importing : ImportState()
        object Success : ImportState()
        data class Error(val message: String) : ImportState()
    }

    sealed class LoadingState {
        object Idle : LoadingState()
        data class Loading(val message: String) : LoadingState()
    }

    companion object {
        /**
         * 验证模型名称是否合法
         * @param modelName 要验证的模型名称
         * @return 如果名称合法返回true，否则返回false
         */
        private fun isValidModelName(modelName: String): Boolean {
            // 检查是否为空
            if (modelName.isBlank()) {
                return false
            }

            // 检查长度限制（避免过长的文件名）
            if (modelName.length > 255) {
                return false
            }

            // 检查是否包含非法字符
            val invalidChars = Regex("[\\\\/:*?\"<>|]")
            if (invalidChars.containsMatchIn(modelName)) {
                return false
            }

            // 检查是否以点或空格开头或结尾
            if (modelName.startsWith(".") || modelName.endsWith(".") ||
                modelName.startsWith(" ") || modelName.endsWith(" ")) {
                return false
            }

            return true
        }
    }
}