package io.kindbrave.mnnserver.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.kindbrave.mnnserver.model.ModelManager
import io.kindbrave.mnnserver.service.LLMService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelListViewModel(application: Application) : AndroidViewModel(application) {
    private val modelManager = ModelManager(application)
    private val llmService = LLMService.getInstance()
    
    private val _modelList = MutableStateFlow<List<ModelManager.ModelInfo>>(emptyList())
    val modelList: StateFlow<List<ModelManager.ModelInfo>> = _modelList.asStateFlow()
    
    // 导入状态管理
    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()
    
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
        viewModelScope.launch {
            try {
                // 创建聊天会话
                llmService.createChatSession(
                    modelId = model.id,
                    modelDir = model.path,
                    useTmpPath = false,
                    sessionId = model.id
                )
                modelManager.updateModelLoadState(model.id, true)
            } catch (e: Exception) {
                // TODO: 处理加载失败的情况
                modelManager.updateModelLoadState(model.id, false)
            }
        }
    }
    
    fun unloadModel(model: ModelManager.ModelInfo) {
        viewModelScope.launch {
            try {
                // 移除并释放会话
                llmService.removeChatSession(model.id)
                modelManager.updateModelLoadState(model.id, false)
            } catch (e: Exception) {
                // TODO: 处理卸载失败的情况
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
} 