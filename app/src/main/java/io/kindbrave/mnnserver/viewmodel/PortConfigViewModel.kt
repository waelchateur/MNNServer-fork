package io.kindbrave.mnnserver.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.kindbrave.mnnserver.data.DataStoreManager
import io.kindbrave.mnnserver.model.PortConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PortConfigViewModel : ViewModel() {
    private var dataStoreManager: DataStoreManager? = null
    
    private val _portConfig = MutableStateFlow(PortConfig())
    val portConfig: StateFlow<PortConfig> = _portConfig.asStateFlow()
    
    fun initialize(context: Context) {
        dataStoreManager = DataStoreManager(context)
        viewModelScope.launch {
            dataStoreManager?.port?.collect { port ->
                _portConfig.value = _portConfig.value.copy(port = port)
            }
        }
    }
    
    fun updatePort(port: Int) {
        viewModelScope.launch {
            dataStoreManager?.updatePort(port)
        }
    }
} 