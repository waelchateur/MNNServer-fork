package io.kindbrave.mnnserver.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.kindbrave.mnnserver.model.ModelManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ModelListViewModel(application: Application) : AndroidViewModel(application) {
    private val modelManager = ModelManager(application)
    
    private val _modelList = MutableStateFlow<List<ModelManager.ModelInfo>>(emptyList())
    val modelList: StateFlow<List<ModelManager.ModelInfo>> = _modelList.asStateFlow()
    
    init {
        viewModelScope.launch {
            modelManager.modelList.collect { models ->
                _modelList.value = models
            }
        }
    }
    
    fun importModel() {
        //modelManager.importModel()
    }
    
    fun loadModel(model: ModelManager.ModelInfo) {
        //modelManager.loadModel(model)
    }
    
    fun unloadModel(model: ModelManager.ModelInfo) {
        //modelManager.unloadModel(model)
    }
    
    fun deleteModel(model: ModelManager.ModelInfo) {
        //modelManager.deleteModel(model)
    }
} 