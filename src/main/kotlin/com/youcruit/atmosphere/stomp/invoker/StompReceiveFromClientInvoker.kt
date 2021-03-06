package com.youcruit.atmosphere.stomp.invoker

import com.youcruit.atmosphere.stomp.api.exceptions.StompErrorException
import com.youcruit.atmosphere.stomp.api.exceptions.StompException
import com.youcruit.atmosphere.stomp.protocol.StompFrameFromClient
import com.youcruit.atmosphere.stomp.util.FixedUriTemplate
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereResource

internal class StompReceiveFromClientInvoker {
    val endpoints = LinkedHashMap<FixedUriTemplate, InjectingMethodInvocation>()

    fun invoke(atmosphereResource: AtmosphereResource, stompFrame: StompFrameFromClient) {
        val destination = stompFrame.destination
            ?: throw StompErrorException("SEND requires a destination")
        var sent = false
        endpoints
            .asSequence()
            .filter { (key) -> key.pattern.match(destination) != null }
            .forEach { (template, invoker) ->
                invoker.invoke(atmosphereResource, stompFrame, template)
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