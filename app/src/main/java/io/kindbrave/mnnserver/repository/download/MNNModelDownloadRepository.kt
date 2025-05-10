package io.kindbrave.mnnserver.repository.download

import android.content.Context
import android.widget.Toast
import com.alibaba.mls.api.ModelItem
import com.alibaba.mls.api.download.DownloadInfo
import com.alibaba.mls.api.download.DownloadListener
import com.alibaba.mls.api.download.ModelDownloadManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MNNModelDownloadRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val modelDownloadManager = ModelDownloadManager.getInstance(context)

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

    fun startDownload(model: ModelItem) {
        modelDownloadManager.startDownload(model.modelId!!)
    }

    fun pauseDownload(model: ModelItem) {
        modelDownloadManager.pauseDownload(model.modelId!!)
    }
}