package com.youcruit.atmosphere.stomp

import org.atmosphere.cpr.AtmosphereResourceEvent
import org.atmosphere.cpr.AtmosphereResourceEventListenerAdapter
import org.atmosphere.websocket.WebSocket
import java.time.Duration
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal class Heartbeater(
    heartbeatInterval: Duration,
    private val webSocket: WebSocket
) : AtmosphereResourceEventListenerAdapter(), Runnable {

    override fun run() {
        webSocket.write("\n")
    }

    private val heartbeater = heartbeatExecutor.scheduleAtFixedRate(
        this,
        heartbeatInterval.toMillis(),
        heartbeatInterval.toMillis(),
        TimeUnit.MILLISECONDS
    )

    override fun onClose(event: AtmosphereResourceEvent) {
        onDisconnect(event)
    }

    override fun onDisconnect(event: AtmosphereResourceEvent) {
        heartbeater.cancel(true)
    }

    companion object {
        @Volatile
        private var heartbeatThreadsStarted = 0
        private val heartbeatExecutor = Executors.newScheduledThreadPool(1) {
            Thread(it, "Heartbeat ${heartbeatThreadsStarted++}")
        }!!
    }
}