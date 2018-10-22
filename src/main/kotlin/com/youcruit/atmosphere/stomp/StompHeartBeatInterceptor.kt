package com.youcruit.atmosphere.stomp

import org.atmosphere.cpr.AtmosphereResourceEvent
import org.atmosphere.cpr.AtmosphereResourceHeartbeatEventListener
import org.atmosphere.util.Utils

class StompHeartBeatInterceptor : AtmosphereResourceHeartbeatEventListener {
    private lateinit var heartbeats: List<AtmosphereResourceHeartbeatEventListener>

    override fun onHeartbeat(event: AtmosphereResourceEvent) {
        if (heartbeats.isNotEmpty() && !Utils.pollableTransport(event.resource.transport())) {
            for (heartbeat in heartbeats) {
                heartbeat.onHeartbeat(event)
            }
        }
    }
}