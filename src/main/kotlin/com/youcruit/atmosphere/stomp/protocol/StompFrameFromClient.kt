package com.youcruit.atmosphere.stomp.protocol

internal class StompFrameFromClient(
    override val command: ClientStompCommand,
    headers: Map<String, String>,
    body: ByteArray
) : StompFrame(
    headers,
    body
) {

    override val receipt: String?
        get() = headers["receipt"]

    constructor(
        command: ClientStompCommand,
        headers: Map<String, String>,
        body: String
    ) : this(
        command = command,
        headers = headers,
        body = body.toByteArray(getCharset(headers))
    )
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
    ABORT,

    ACK,
    NACK
}