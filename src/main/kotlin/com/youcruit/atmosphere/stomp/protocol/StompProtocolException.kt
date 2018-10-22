package com.youcruit.atmosphere.stomp.protocol

import java.io.IOException

class StompProtocolException : IOException {
    constructor() : super()
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)
}