package com.youcruit.atmosphere.stomp.protocol

import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

abstract class StompFrame(
    val headers: Map<String, String>,
    val body: ByteArray
) {
    abstract val command: StompCommand

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

    companion object {
        internal fun getCharset(headers: Map<String, String>): Charset {
            val contentType = headers["content-type"]
                ?: return StandardCharsets.UTF_8
            val charset = contentType
                .substringAfter(";charset=", "")
                .substringBefore(';')
            return try {
                Charset.forName(charset)
            } catch (e: Exception) {
                StandardCharsets.UTF_8
            }
        }
    }
}

interface StompCommand {
    val name: String
}