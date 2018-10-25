package com.youcruit.atmosphere.stomp.api.exceptions

import com.youcruit.atmosphere.stomp.api.Destination

/**
 * Throwing this exception from a method annotated with @StompService or
 * @StompSubscriptionService sends the message to the destination in the exception,
 * but ONLY over the AtmosphereResource (connection) that invoked the method. I.e.
 * it is a one server to one client (but possibly multiple subscriptions) message
 * and will not be broadcasted to all other connections that subscribe to this
 * destination.
 *
 * This is useful for e.g. creating your own error handling because stomp lacks it for
 * SUBSCRIBE and SEND when guessing what and if the frame caused a problem.
 */
@Suppress("unused")
class StompWithReplyException(
    message: String,
    val destination: Destination,
    val headers: Map<String, String> = mapOf(),
    cause: Throwable? = null
) : StompException(message, cause)