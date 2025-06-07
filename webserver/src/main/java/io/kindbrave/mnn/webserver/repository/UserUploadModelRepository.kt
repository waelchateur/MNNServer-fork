package io.kindbrave.mnn.webserver.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import com.elvishew.xlog.XLog
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnn.webserver.annotation.LogAfter
import io.kindbrave.mnn.webserver.annotation.LogBefore
import io.kindbrave.mnn.webserver.service.LLMService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.collections.forEach

@Singleton
class UserUploadModelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val llmService: LLMService
) {
    private val tag = UserUploadModelRepository::class.simpleName

    private val _modelList = MutableStateFlow<List<ModelInfo>>(emptyList())
    val modelList: StateFlow<List<ModelInfo>> = _modelList

    fun refreshModels() {
        val modelsDir = getModelsDirectory()
        if (!modelsDir.exists()) {
            modelsDir.mkdirs()
            return
        }

        val models = modelsDir.listFiles()?.filter { it.isDirectory }?.map { dir ->
            ModelInfo(
                id = dir.name,
                name = dir.name,
                path = dir.absolutePath
            )
        } ?: emptyList()

        _modelList.value = models
    }

    @LogBefore("")
    @LogAfter("")
    suspend fun loadModel(model: ModelInfo) {
        llmService.createChatSession(
            modelId = model.id,
            modelDir = model.path,
            sessionId = model.id
        )
    }

    @LogBefore("")
    @LogAfter("")
    suspend fun unloadModel(model: ModelInfo) {
        llmService.removeChatSession(model.id)
    }

    @LogAfter("")
    suspend fun uploadUserModelFromUri(uri: Uri?, modelName: String) {
        return withContext(Dispatchers.IO) {
            try {
                val modelDir = File(getModelsDirectory(), modelName)
                if (!modelDir.exists()) {
                    modelDir.mkdirs()
                }

                val docTree = context.contentResolver.getTreeDocumentUri(uri!!)
                if (docTree != null) {
                    copyDirectory(docTree, modelDir)

                    val newModel = ModelInfo(
                        id = modelName,
                        name = modelName,
                        path = modelDir.absolutePath
                    )

                    _modelList.value += newModel
                    return@withContext
                } else {
                    XLog.tag(tag).e("uploadUserModelFromUri:Invalid URI")
                    throw IllegalArgumentException("Invalid URI")
                }
            } catch (e: Exception) {
                XLog.tag(tag).e("uploadUserModelFromUri: error", e)
                throw e
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

    @LogBefore("")
    @LogAfter("")
    fun deleteModel(modelId: String): Boolean {
        val model = _modelList.value.find { it.id == modelId } ?: throw NullPointerException("Delete Model $modelId Failed")

        val modelDir = File(model.path)
        if (modelDir.exists() && modelDir.deleteRecursively()) {
            _modelList.value = _modelList.value.filter { it.id != modelId }
            return true
        }
        throw IllegalStateException("Delete Model $modelId Failed")
    }

    fun getModelsDirectory(): File {
        return File(context.filesDir, "models")
    }

    fun isModelLoaded(modelInfo: ModelInfo): Boolean {
        return llmService.isModelLoaded(modelInfo.id)
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

    fun getModelById(modelId: String): ModelInfo? {
        return _modelList.value.find { it.id == modelId }
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
        val path: String
    )
}