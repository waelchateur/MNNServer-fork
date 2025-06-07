package io.kindbrave.mnn.server.engine

import android.util.Log
import io.kindbrave.mnn.server.utils.FileUtils

class AsrSession(
    override val modelId: String,
    override var sessionId: String,
    override val configPath: String,
) : Session(modelId, sessionId, configPath) {
    private val tag = AsrSession::class.java.simpleName

    private var nativePtr: Long = 0

    @Volatile
    private var modelLoading = false
    @Volatile
    private var generating = false
    @Volatile
    private var releaseRequeted = false

    fun load() {
        modelLoading = true

        nativePtr = MNNAsr.initNative(configPath)
        modelLoading = false
        if (releaseRequeted) {
            release()
        }
    }

    fun generate(wavFileTag: String, progressListener: MNNAsr.AsrCallback) {
        synchronized(this) {
            val wavFilePath = FileUtils.extractAudioPath(wavFileTag)
            if (wavFilePath == null) {
                return
            }
            generating = true
            MNNAsr.recognizeFromFileStreamNative(nativePtr, wavFilePath, progressListener)
            generating = false
            if (releaseRequeted) {
                release()
            }
        }
    }

    fun release() {
        synchronized(this) {
            if (!generating && !modelLoading) {
                releaseInner()
            } else {
                releaseRequeted = true
                while (generating || modelLoading) {
                    try {
                        (this as Object).wait()
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        Log.e(tag, "Thread interrupted while waiting for release", e)
                    }
                }
                releaseInner()
            }
        }
    }

    private fun releaseInner() {
        if (nativePtr != 0L) {
            MNNAsr.releaseNative(nativePtr)
            nativePtr = 0
            (this as Object).notifyAll()
        }
    }
}