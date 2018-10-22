package com.youcruit.atmosphere.stomp.protocol

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException

class StompFrame(
    val command: StompCommand,
    val headers: Map<String, String>,
    val body: ByteArray
) {

    val destination: String
        get() = headers["destination"]!!

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
    STOMP,
    CONNECT,
    DISCONNECT,

    SEND,
    SUBSCRIBE,
    UNSUBSCRIBE,
    BEGIN,
    COMMIT,
    ABORT,
    RECEIPT
}

enum class ServerStompCommand : StompCommand {
    CONNECTED,
    MESSAGE,
    RECEIPT,
    ACK,
    NACK,

    ERROR
}
