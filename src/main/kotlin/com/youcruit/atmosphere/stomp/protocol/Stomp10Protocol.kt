package com.youcruit.atmosphere.stomp.protocol

import java.io.InputStream

object Stomp10Protocol : StompProtocol(1.0f) {
    override fun InputStream.readCommand(): ClientStompCommand {
        val command = generateSequence { readUtf8Line(14) }
            .dropWhile { it.isBlank() }
            .first()
        return ClientStompCommand.valueOf(command)
    }
}