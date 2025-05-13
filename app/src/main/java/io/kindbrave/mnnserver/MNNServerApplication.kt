package io.kindbrave.mnnserver

import android.app.Application
import com.alibaba.mls.api.ApplicationProvider
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.flattener.DefaultFlattener
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.backup.FileSizeBackupStrategy2
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import dagger.hilt.android.HiltAndroidApp
import io.kindbrave.mnnserver.utils.LogUtils

@HiltAndroidApp
class MNNServerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        LogUtils.init(this)
        initXLog()
        ApplicationProvider.set(this)
    }

    private fun initXLog() {
        val filePrinter = FilePrinter.Builder("${filesDir.path}/logs")
            .fileNameGenerator(DateFileNameGenerator())
            .backupStrategy(FileSizeBackupStrategy2(3 * 1024 * 1024, 5))    //3M
            .cleanStrategy(FileLastModifiedCleanStrategy(30L * 24L * 60L * 60L * 1000L))    // 30days
            .flattener(DefaultFlattener())
            .build()
        val config = LogConfiguration.Builder()
            .build()
        XLog.init(config)
    }
}