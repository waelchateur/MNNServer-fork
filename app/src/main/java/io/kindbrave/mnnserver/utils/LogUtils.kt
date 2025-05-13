package io.kindbrave.mnnserver.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

object LogUtils {
    private const val DEFAULT_TAG = "AistLog"
    private const val LOG_FOLDER = "logs"
    private var logFile: File? = null
    private var fileWriter: FileWriter? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        try {
            val logDir = File(context.filesDir, LOG_FOLDER)
            if (!logDir.exists()) {
                logDir.mkdirs()
            }

            logFile = File(logDir, "app.log")
            fileWriter = FileWriter(logFile, true)
        } catch (e: Exception) {
            Log.e(DEFAULT_TAG, "Failed to initialize log file", e)
        }
    }

    fun d(tag: String, message: String) {
        writeLog(tag, "D", message)
    }

    fun i(tag: String, message: String) {
        writeLog(tag, "I", message)
    }

    fun w(tag: String, message: String) {
        writeLog(tag, "W", message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        writeLog(tag, "E", message)
        throwable?.let {
            writeLog(tag, "E", "Exception: ${it.message}")
            writeLog(tag, "E", it.stackTraceToString())
        }
    }

    private fun writeLog(tag: String, level: String, message: String) {
        try {
            val timestamp = dateFormat.format(Date())
            val logMessage = "$timestamp $level/$tag: $message\n"

            when (level) {
                "D" -> Log.d(tag, message)
                "I" -> Log.i(tag, message)
                "W" -> Log.w(tag, message)
                "E" -> Log.e(tag, message)
            }

            fileWriter?.write(logMessage)
            fileWriter?.flush()
        } catch (e: Exception) {
            Log.e(DEFAULT_TAG, "Failed to write log", e)
        }
    }

    fun close() {
        try {
            fileWriter?.close()
            fileWriter = null
        } catch (e: Exception) {
            Log.e(DEFAULT_TAG, "Failed to close log file", e)
        }
    }
} 