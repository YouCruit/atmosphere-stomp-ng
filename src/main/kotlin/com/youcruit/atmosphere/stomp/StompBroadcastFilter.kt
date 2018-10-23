package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.protocol.EfficientByteArrayOutputStream
import com.youcruit.atmosphere.stomp.protocol.ServerStompCommand
import com.youcruit.atmosphere.stomp.protocol.StompFrame
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
import java.util.UUID

class StompBroadcastFilter : PerRequestBroadcastFilter, BroadcastFilterLifecycle {

    private lateinit var framework: AtmosphereFramework
    private lateinit var sessionFactory: AtmosphereResourceSessionFactory

    override fun filter(broadcasterId: String, r: AtmosphereResource, originalMessage: Any, message: Any): BroadcastFilter.BroadcastAction {
        val session = sessionFactory.getSession(r)
        val subscriptions = session.subscriptions
        val ids = synchronized(subscriptions) {
            subscriptions.findAllByDestination(broadcasterId)
        }
        val protocol = session.protocol

        val baos = EfficientByteArrayOutputStream()

        val body = if (message is ByteArray) {
            message
        } else {
            message.toString().toByteArray(StandardCharsets.UTF_8)
        }

        // Generate a frame for each subscription
        for (id in ids) {
            protocol.writeFrame(
                baos,
                StompFrame(
                    command = ServerStompCommand.MESSAGE,
                    headers = mapOf(
                        "subscription" to id,
                        "message-id" to UUID.randomUUID().toString(),
                        "destination" to broadcasterId
                    ),
                    body = body
                )
            )
        }

        if (baos.size() == 0) {
            throw IllegalStateException("No subscribers, but still got message for $broadcasterId")
        }

        return BroadcastFilter.BroadcastAction(baos.asString(StandardCharsets.UTF_8))
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