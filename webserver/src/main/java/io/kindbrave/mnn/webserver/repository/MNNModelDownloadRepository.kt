package io.kindbrave.mnn.webserver.repository

import android.content.Context
import com.alibaba.mls.api.hf.HfApiClient
import com.alibaba.mls.api.ModelItem
import com.alibaba.mls.api.download.DownloadInfo
import com.alibaba.mls.api.download.DownloadListener
import com.alibaba.mls.api.download.ModelDownloadManager
import com.elvishew.xlog.XLog
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import io.kindbrave.mnn.webserver.annotation.LogAfter
import io.kindbrave.mnn.webserver.annotation.LogBefore
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MNNModelDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val tag = MNNModelDownloadRepository::class.simpleName
    private val modelDownloadManager = ModelDownloadManager.getInstance(context)

    private var bestApiClient: HfApiClient? = null
    private var networkErrorCount = 0

    fun requestRepoList(
        onSuccess: (hfModelItems: List<ModelItem>) -> Unit,
        onFailure: (error: String?) -> Unit
    ) {
        networkErrorCount = 0
        if (bestApiClient != null) {
            requestRepoListWithClient(bestApiClient!!, bestApiClient!!.host, 1, onSuccess, onFailure)
        } else {
            val defaultApiClient = HfApiClient(HfApiClient.Companion.HOST_DEFAULT)
            val mirrorApiClient = HfApiClient(HfApiClient.Companion.HOST_MIRROR)
            requestRepoListWithClient(defaultApiClient, HfApiClient.Companion.HOST_DEFAULT, 2, onSuccess, onFailure)
            requestRepoListWithClient(mirrorApiClient, HfApiClient.Companion.HOST_MIRROR, 2, onSuccess, onFailure)
        }
    }

    private fun requestRepoListWithClient(
        hfApiClient: HfApiClient,
        tag: String,
        loadCount: Int,
        onSuccess: (hfModelItems: List<ModelItem>) -> Unit,
        onFailure: (error: String?) -> Unit
    ) {
        hfApiClient.searchRepos("", object : HfApiClient.RepoSearchCallback {
            override fun onSuccess(hfModelItems: List<ModelItem>) {
                if (bestApiClient == null) {
                    bestApiClient = hfApiClient
                    saveToCache(hfModelItems)
                    onSuccess(hfModelItems)
                    HfApiClient.Companion.bestClient = bestApiClient
                }
            }

            override fun onFailure(error: String?) {
                networkErrorCount++
                XLog.tag(tag).d(
                    "requestRepoListWithClient:on requestRepoListWithClient Failure $error"
                )
                if (networkErrorCount == loadCount) {
                    XLog.tag(tag).e(
                        "requestRepoListWithClient:on requestRepoListWithClient Failure With Retry $error"
                    )
                    onFailure(error)
                }
            }
        })
    }

    fun loadFromCache(): List<ModelItem>? {
        val cacheFileName = "model_list.json"
        val cacheFile = File(context.filesDir, "model_list.json")
        if (!cacheFile.exists()) {
            return loadFromAssets(this.context, cacheFileName)
        }
        try {
            FileReader(cacheFile.absolutePath).use { reader ->
                val gson = Gson()
                val listType = object : TypeToken<List<ModelItem?>?>() {}.type
                return gson.fromJson(reader, listType)
            }
        } catch (e: FileNotFoundException) {
            XLog.tag(tag).d("loadFromCache:Cache file not found.")
            return null
        } catch (e: IOException) {
            XLog.tag(tag).e("loadFromCache:loadFromCacheError", e) // Log the full exception for debugging
            return null
        } catch (e: JsonSyntaxException) {
            XLog.tag(tag).e("loadFromCache:loadFromCacheError: Invalid JSON", e)
            return null
        }
    }

    private fun loadFromAssets(context: Context, fileName: String): List<ModelItem>? {
        val assetManager = context.assets
        try {
            val inputStream = assetManager.open(fileName)
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val gson = Gson()
            val listType = object : TypeToken<List<ModelItem?>?>() {}.type
            return gson.fromJson(bufferedReader, listType)
        } catch (e: IOException) {
            XLog.tag(tag).e("loadFromAssets:Error reading from assets", e)
            return null
        } catch (e: JsonSyntaxException) {
            XLog.tag(tag).e("loadFromAssets:Invalid JSON in assets", e)
            return null
        }
    }

    private fun saveToCache(hfModelItems: List<ModelItem>) {
        val gson = GsonBuilder().setPrettyPrinting().create()
        val json = gson.toJson(hfModelItems)

        try {
            FileWriter(context.filesDir.toString() + "/model_list.json").use { writer ->
                writer.write(json)
            }
        } catch (e: IOException) {
            XLog.tag(tag).e("saveToCache:saveToCacheError $e")
        }
    }

    fun setListener(downloadListener: DownloadListener) {
        modelDownloadManager.setListener(downloadListener)
    }

    fun getModelDownloadInfo(model: ModelItem): DownloadInfo? {
        if (model.isLocal.not()) {
            val downloadInfo = modelDownloadManager.getDownloadInfo(model.modelId!!)
            return downloadInfo
        }
        return null
    }

    @LogBefore("")
    fun startDownload(model: ModelItem) {
        modelDownloadManager.startDownload(model.modelId!!)
    }

    fun pauseDownload(model: ModelItem) {
        modelDownloadManager.pauseDownload(model.modelId!!)
    }

    @LogAfter("")
    suspend fun deleteModel(model: ModelItem) {
        modelDownloadManager.deleteModel(model.modelId!!)
    }
}