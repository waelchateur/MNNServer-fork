package io.kindbrave.mnn.server.engine

open class Session(
    open val modelId: String,
    open var sessionId: String,
    open val configPath: String,
)