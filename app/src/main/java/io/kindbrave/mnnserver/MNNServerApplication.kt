package io.kindbrave.mnnserver

import android.app.Application
import com.alibaba.mls.api.ApplicationProvider
import com.elvishew.xlog.LogConfiguration
import com.elvishew.xlog.XLog
import com.elvishew.xlog.flattener.PatternFlattener
import com.elvishew.xlog.printer.AndroidPrinter
import com.elvishew.xlog.printer.file.FilePrinter
import com.elvishew.xlog.printer.file.backup.FileSizeBackupStrategy2
import com.elvishew.xlog.printer.file.clean.FileLastModifiedCleanStrategy
import com.elvishew.xlog.printer.file.naming.ChangelessFileNameGenerator
import com.elvishew.xlog.printer.file.naming.DateFileNameGenerator
import dagger.hilt.android.HiltAndroidApp
import kotlin.system.exitProcess

@HiltAndroidApp
class MNNServerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        initXLog()
        ApplicationProvider.set(this)
        setDefaultCacheHandler()
    }

    private fun initXLog() {
        val filePrinter = FilePrinter.Builder("${filesDir.path}/logs")
            .fileNameGenerator(ChangelessFileNameGenerator("log.txt"))
            .backupStrategy(FileSizeBackupStrategy2(3 * 1024 * 1024, 5))    //3M
            .cleanStrategy(FileLastModifiedCleanStrategy(30L * 24L * 60L * 60L * 1000L))    // 30days
            .flattener(PatternFlattener("{d yy/MM/dd HH:mm:ss} {l}|{t}: {m}"))
            .build()
        val config = LogConfiguration.Builder()
            .build()
        XLog.init(config, filePrinter, AndroidPrinter())
    }

    private fun setDefaultCacheHandler() {
        Thread.setDefaultUncaughtExceptionHandler(object : Thread.UncaughtExceptionHandler {
            override fun uncaughtException(t: Thread, e: Throwable) {
                XLog.e("Uncaught exception in thread " + t.name, e)

                exitProcess(1)
            }
        })
    }
}