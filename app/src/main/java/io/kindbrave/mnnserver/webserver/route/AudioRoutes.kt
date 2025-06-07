package io.kindbrave.mnnserver.webserver.route

import io.kindbrave.mnnserver.webserver.MNNHandler
import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import java.io.ByteArrayOutputStream

fun Route.audioRoutes(mnnHandler: MNNHandler) {
    webSocket("/audio/asr") {
        val pcmBuffer = ByteArrayOutputStream()

        while (true) {
            when (val frame = incoming.receive()) {
                is Frame.Binary -> {
                    val pcmData = frame.readBytes()
                    pcmBuffer.write(pcmData)

                    if (pcmBuffer.size() >= 3200) {  // ~200ms @16kHz
//                        val result = NativeASR.recognize(pcmBuffer.toByteArray())
//                        send(Frame.Text(result))
                        send(Frame.Text("TODO: Implement ASR"))

                        pcmBuffer.reset()
                    }
                }
                else -> Unit
            }
        }
    }
}