package io.kindbrave.mnnserver.model

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ModelManager private constructor(private val context: Context) {
    
    private val tag = "ModelManager"

    private val _modelList = MutableStateFlow<List<ModelInfo>>(emptyList())
    val modelList: StateFlow<List<ModelInfo>> = _modelList
    
    private val _importProgress = MutableStateFlow<ImportProgress>(ImportProgress.Idle)
    val importProgress: StateFlow<ImportProgress> = _importProgress
    
    init {
        loadModels()
    }
    
    private fun loadModels() {
        val modelsDir = getModelsDirectory()
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
            return
        }
        
        val models = modelsDir.listFiles()?.filter { it.isDirectory }?.map { dir ->
            ModelInfo(
                id = dir.name,
                name = dir.name,
                path = dir.absolutePath,
                isLoaded = false
            )
        } ?: emptyList()

        _modelList.value = models
    }
    
    suspend fun importModelFromUri(uri: Uri, modelName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                _importProgress.value = ImportProgress.InProgress(0f)

                val modelDir = File(getModelsDirectory(), modelName)
                if (!modelDir.exists()) {
                    modelDir.mkdirs()
                }

                val docTree = context.contentResolver.getTreeDocumentUri(uri)
                if (docTree != null) {
                    copyDirectory(docTree, modelDir)
                    
                    val newModel = ModelInfo(
                        id = modelName,
                        name = modelName,
                        path = modelDir.absolutePath,
                        isLoaded = false
                    )

                    _modelList.value += newModel
                    _importProgress.value = ImportProgress.Success
                    return@withContext true
                } else {
                    _importProgress.value = ImportProgress.Error("无法访问选择的文件夹")
                    return@withContext false
                }
            } catch (e: Exception) {
                Log.e(tag, "导入模型失败", e)
                _importProgress.value = ImportProgress.Error("导入失败: ${e.message}")
                return@withContext false
            }
        }
    }
    
    private fun copyDirectory(sourceUri: Uri, destDir: File) {
        val childDocs = context.getChildDocuments(sourceUri)
        
        childDocs.forEach { childUri ->
            val docInfo = context.getDocumentInfo(childUri)
            val destFile = File(destDir, docInfo.name)
            
            if (docInfo.isDirectory) {
                destFile.mkdir()
                copyDirectory(childUri, destFile)
            } else {
                context.contentResolver.openInputStream(childUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
        }
    }
    
    fun deleteModel(modelId: String): Boolean {
        val model = _modelList.value.find { it.id == modelId } ?: return false
        
        val modelDir = File(model.path)
        if (modelDir.exists() && modelDir.deleteRecursively()) {
            _modelList.value = _modelList.value.filter { it.id != modelId }
            return true
        }
        return false
    }
    
    fun getModelsDirectory(): File {
        return File(context.filesDir, "models")
    }
    
    private fun Context.getChildDocuments(parentUri: Uri): List<Uri> {
        val childDocs = mutableListOf<Uri>()
        
        try {
            // 使用 DocumentsContract 来获取子文档
            val documentId = DocumentsContract.getTreeDocumentId(parentUri)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(parentUri, documentId)
            
            contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idColumn)
                    val childUri = DocumentsContract.buildDocumentUriUsingTree(parentUri, docId)
                    childDocs.add(childUri)
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "获取子文档失败", e)
        }
        
        return childDocs
    }
    
    private fun Context.getDocumentInfo(uri: Uri): DocumentInfo {
        var name = ""
        var isDirectory = false
        
        contentResolver.query(
            uri,
            arrayOf("_display_name", "mime_type"),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow("_display_name"))
                val mimeType = cursor.getString(cursor.getColumnIndexOrThrow("mime_type"))
                isDirectory = mimeType == "vnd.android.document/directory"
            }
        }
        
        return DocumentInfo(name, isDirectory)
    }

    private fun ContentResolver.getTreeDocumentUri(uri: Uri): Uri? {
        return if (uri.toString().startsWith("content://com.android.externalstorage.documents/tree/")) {
            uri
        } else {
            null
        }
    }
    
    // 添加一个方法来更新模型的加载状态
    fun updateModelLoadState(modelId: String, isLoaded: Boolean) {
        _modelList.value = _modelList.value.map { model ->
            if (model.id == modelId) {
                model.copy(isLoaded = isLoaded)
            } else {
                model
            }
        }
    }
    
    // 获取特定模型信息
    fun getModelById(modelId: String): ModelInfo? {
        return _modelList.value.find { it.id == modelId }
    }
    
    // 刷新模型列表
    fun refreshModels() {
        loadModels()
    }

    data class DocumentInfo(val name: String, val isDirectory: Boolean)

    sealed class ImportProgress {
        object Idle : ImportProgress()
        data class InProgress(val progress: Float) : ImportProgress()
        object Success : ImportProgress()
        data class Error(val message: String) : ImportProgress()
    }

    data class ModelInfo(
        val id: String,
        val name: String,
        val path: String,
        var isLoaded: Boolean = false
    )

    companion object {
        @Volatile
        private var INSTANCE: ModelManager? = null

        fun getInstance(context: Context): ModelManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ModelManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}