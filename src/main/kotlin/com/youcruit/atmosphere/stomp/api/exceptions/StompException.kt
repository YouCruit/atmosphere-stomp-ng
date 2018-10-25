package com.youcruit.atmosphere.stomp.api.exceptions

open class StompException(
    message: String,
    cause: Throwable? = null
) : StompErrorException(message, cause)