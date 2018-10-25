package com.youcruit.atmosphere.stomp.api.exceptions

/**
 * This will close the connection after sending a message.
 */
open class StompErrorException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause) {
    override val message: String
        get() = super.message!!
}