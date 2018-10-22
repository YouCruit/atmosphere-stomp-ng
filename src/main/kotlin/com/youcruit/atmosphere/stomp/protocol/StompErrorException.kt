package com.youcruit.atmosphere.stomp.protocol

/**
 * This will close the connection after sending a message.
 */
open class StompErrorException : RuntimeException {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}