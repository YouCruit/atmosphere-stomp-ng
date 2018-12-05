package com.youcruit.atmosphere.stomp.invoker

import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.AtmosphereResourceEvent
import org.atmosphere.util.Utils
import org.atmosphere.websocket.WebSocketEventListener
import org.atmosphere.websocket.WebSocketEventListenerAdapter
import java.util.LinkedList

internal class StompOnDisconnectInterceptor : WebSocketEventListenerAdapter() {
    val endpoints = LinkedList<OnDisconnectMethodInvocation>()

    override fun onDisconnect(event: WebSocketEventListener.WebSocketEvent<*>) {
        onDisconnect(event.webSocket().resource())
    }

    override fun onDisconnect(event: AtmosphereResourceEvent) {
        if (!Utils.pollableTransport(event.resource.transport())) {
            onDisconnect(event.resource)
        }
    }

    private fun onDisconnect(atmosphereResource: AtmosphereResource) {
        for (onDisconnectInvocation in endpoints) {
            onDisconnectInvocation.invoke(atmosphereResource)
        }
    }
}

internal fun AtmosphereConfig.stompOnDisconnectInvoker() =
    servletContext.getAttribute(ON_DISCONNECT_INTERCEPTOR) as? StompOnDisconnectInterceptor
        ?: let {
            val invoker = StompOnDisconnectInterceptor()
            servletContext.setAttribute(ON_DISCONNECT_INTERCEPTOR, invoker)
            invoker
        }

private const val ON_DISCONNECT_INTERCEPTOR = "com.youcruit.atmosphere.stomp.invoker.OnDisconnectInterceptor"