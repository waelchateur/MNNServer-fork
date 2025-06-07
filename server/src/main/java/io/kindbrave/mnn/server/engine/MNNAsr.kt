package io.kindbrave.mnn.server.engine

object MNNAsr {
    external fun initNative(configPath: String): Long

    external fun recognizeFromFileStreamNative(
        asrPtr: Long,
        wavFilePath: String,
        callback: AsrCallback
    )
    external fun releaseNative(asrPtr: Long)

    interface AsrCallback {
        fun onPartialResult(text: String?)
        fun onFinalResult(text: String?)
    }

    init {
        System.loadLibrary("mnnllmapp")
    }
}
