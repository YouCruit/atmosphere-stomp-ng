package com.youcruit.atmosphere.stomp.invoker

import com.youcruit.atmosphere.stomp.protocol.StompErrorException
import com.youcruit.atmosphere.stomp.protocol.StompException
import com.youcruit.atmosphere.stomp.protocol.StompFrame
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.util.uri.UriPattern

internal class StompReceiveFromClientInvoker {
    val endpoints = LinkedHashMap<UriPattern, InjectingMethodInvocation>()

    fun invoke(atmosphereResource: AtmosphereResource, stompFrame: StompFrame) {
        val destination = stompFrame.destination
            ?: throw StompErrorException("SEND requires a destination")
        var sent = false
        endpoints
            .asSequence()
            .filter { (key) -> key.match(destination) != null }
            .forEach { (_, invoker) ->
                invoker.invoke(atmosphereResource, stompFrame)
                sent = true
            }
        if (!sent) {
            throw StompException("No matching destination for '$destination'")
        }
    }
}

internal fun AtmosphereConfig.stompReceiveInvoker() =
    servletContext.getAttribute(RECEIVE_FROM_CLIENT_INVOKER) as? StompReceiveFromClientInvoker
        ?: let {
            val invokers = StompReceiveFromClientInvoker()
            servletContext.setAttribute(RECEIVE_FROM_CLIENT_INVOKER, invokers)
            invokers
        }

private const val RECEIVE_FROM_CLIENT_INVOKER = "com.youcruit.atmosphere.stomp.invoker.StompReceiveFromClientInvoker"
