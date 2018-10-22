package com.youcruit.atmosphere.stomp.protocol

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException

class StompFrame(
    val command: StompCommand,
    val headers: Map<String, String>,
    val body: ByteArray
) {

    fun bodyAsString(): String {
        return body.toString(charset = charset)
    }

    val charset: Charset
        get() {
            val contentType = headers["content-type"]
                ?: return StandardCharsets.UTF_8
            val charset = contentType
                .substringAfter(";charset=", "")
                .substringBefore(';')
            return try {
                Charset.forName(charset)
            } catch (e: UnsupportedCharsetException) {
                StandardCharsets.UTF_8
            }
        }
}

interface StompCommand

enum class ClientStompCommand : StompCommand {
    SEND,
    SUBSCRIBE,
    UNSUBSCRIBE,
    ACK,
    NACK,
    BEGIN,
    COMMIT,
    ABORT,
    DISCONNECT
}

enum class ServerStompCommand : StompCommand {
    MESSAGE,
    RECEIPT,
    ERROR
}
