package com.youcruit.atmosphere.stomp.protocol

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.charset.UnsupportedCharsetException

class StompFrame(
    val command: StompCommand,
    val headers: Map<String, String>,
    val body: ByteArray
) {

    val id: String?
        get() = headers["id"]

    val destination: String?
        get() = headers["destination"]

    val receipt: String?
        get() = headers["receipt"]

    @Suppress("DeprecatedCallableAddReplaceWith")
    @Deprecated("Transaction is NOT supported")
    val transaction: String?
        get() = headers["transaction"]

    fun bodyAsString(): String {
        return body.toString(charset = charset)
    }

    val charset: Charset
        get() = getCharset(headers)

    constructor(
        command: StompCommand,
        headers: Map<String, String>,
        body: String
    ) : this(
        command = command,
        headers = headers,
        body = body.toByteArray(getCharset(headers))
    )

    companion object {
        private fun getCharset(headers: Map<String, String>): Charset {
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

        fun receiptOf(frame: StompFrame) =
            StompFrame(
                command = ServerStompCommand.RECEIPT,
                headers = mapOf("receipt-id" to frame.receipt!!),
                body = byteArrayOf()
            )
    }
}

interface StompCommand {
    val name: String
}

enum class ClientStompCommand : StompCommand {
    STOMP,
    CONNECT,
    DISCONNECT,

    SEND,
    SUBSCRIBE,
    UNSUBSCRIBE,
    BEGIN,
    COMMIT,
    ABORT
}

enum class ServerStompCommand : StompCommand {
    CONNECTED,
    MESSAGE,
    RECEIPT,
    ACK,
    NACK,

    ERROR
}
