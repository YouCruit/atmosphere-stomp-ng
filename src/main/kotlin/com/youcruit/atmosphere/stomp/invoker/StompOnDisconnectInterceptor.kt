package com.youcruit.atmosphere.stomp.invoker

import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereResourceEvent
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter
import org.atmosphere.util.Utils
import java.util.LinkedList

internal class StompOnDisconnectInterceptor : AtmosphereResourceEventListenerAdapter() {
    val endpoints = LinkedList<OnDisconnectMethodInvocation>()

    override fun onDisconnect(event: AtmosphereResourceEvent) {
        if (endpoints.isNotEmpty() && !Utils.pollableTransport(event.resource.transport())) {
            for (onDisconnectInvocation in endpoints) {
                onDisconnectInvocation.invoke(event)
            }
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