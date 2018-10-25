package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.util.protocol
import com.youcruit.atmosphere.stomp.util.subscriptions
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.AtmosphereResourceSessionFactory
import org.atmosphere.cpr.BroadcastFilter
import org.atmosphere.cpr.BroadcastFilterLifecycle
import org.atmosphere.cpr.PerRequestBroadcastFilter
import java.nio.charset.StandardCharsets

class StompBroadcastFilter : PerRequestBroadcastFilter, BroadcastFilterLifecycle {

    private lateinit var framework: AtmosphereFramework
    private lateinit var sessionFactory: AtmosphereResourceSessionFactory

    override fun filter(broadcasterId: String, r: AtmosphereResource, originalMessage: Any, message: Any): BroadcastFilter.BroadcastAction {
        val session = sessionFactory.getSession(r)
        val subscriptions = session.subscriptions
        val frames = subscriptions.createFrames(broadcasterId, session.protocol, message)

        if (frames.size() == 0) {
            throw IllegalStateException("No subscribers, but still got message for $broadcasterId")
        }

        return BroadcastFilter.BroadcastAction(frames.asString(StandardCharsets.UTF_8))
    }

    override fun filter(broadcasterId: String?, originalMessage: Any?, message: Any?): BroadcastFilter.BroadcastAction {
        return BroadcastFilter.BroadcastAction(message)
    }

    override fun destroy() {
    }

    override fun init(config: AtmosphereConfig) {
        framework = config.framework()
        sessionFactory = config.sessionFactory()
    }
}