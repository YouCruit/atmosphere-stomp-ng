package com.youcruit.atmosphere.stomp.invoker

import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereResourceEvent
import org.atmosphere.cpr.AtmosphereResourceHeartbeatEventListener
import org.atmosphere.interceptor.HeartbeatInterceptor
import org.atmosphere.util.Utils
import java.util.LinkedList

internal class StompHeartBeatInterceptor : AtmosphereResourceHeartbeatEventListener, HeartbeatInterceptor() {
    val heartbeats = LinkedList<HeartbeatMethodInvocation>()

    override fun onHeartbeat(event: AtmosphereResourceEvent) {
        if (heartbeats.isNotEmpty() && !Utils.pollableTransport(event.resource.transport())) {
            for (heartbeat in heartbeats) {
                heartbeat.invoke(event)
            }
        }
    }
}

internal fun AtmosphereConfig.stompHeartbeatInvoker() =
    servletContext.getAttribute(RECEIVE_FROM_HEARTBEAT_INVOKER) as? StompHeartBeatInterceptor
        ?: let {
            val invokers = StompHeartBeatInterceptor()
            servletContext.setAttribute(RECEIVE_FROM_HEARTBEAT_INVOKER, invokers)
            invokers
        }

private const val RECEIVE_FROM_HEARTBEAT_INVOKER = "com.youcruit.atmosphere.stomp.invoker.StompHeartBeatInterceptor"
