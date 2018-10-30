package com.youcruit.atmosphere.stomp.invoker

import com.youcruit.atmosphere.stomp.api.exceptions.StompErrorException
import com.youcruit.atmosphere.stomp.api.exceptions.StompException
import com.youcruit.atmosphere.stomp.protocol.StompFrameFromClient
import com.youcruit.atmosphere.stomp.util.FixedUriTemplate
import com.youcruit.atmosphere.stomp.util.subscriptions
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.Broadcaster

internal class StompSubscribeHandler(
    private val framework: AtmosphereFramework
) {
    val endpoints = LinkedHashMap<FixedUriTemplate, InjectingMethodInvocation>()

    private val sessionFactory
        get() = framework.sessionFactory()

    fun subscribe(atmosphereResource: AtmosphereResource, stompFrame: StompFrameFromClient) {
        val id = stompFrame.id
        if (id?.isBlank() != false) {
            throw StompErrorException("SUBSCRIBE must have an id")
        }

        val subscriptions = sessionFactory.getSession(atmosphereResource).subscriptions

        synchronized(subscriptions) {

            if (subscriptions.getById(id) != null) {
                throw StompErrorException("Already subscribed with $id")
            }

            val destination = stompFrame.destination
                ?: throw StompErrorException("SUBSCRIBE requires a destination")
            val accepted = endpoints
                .asSequence()
                .filter { (key) -> key.pattern.match(destination) != null }
                .any { (template, invoker) ->
                    invoker.invoke(atmosphereResource, stompFrame, template) as Boolean
                }

            if (!accepted) {
                throw StompException("No subscription endpoint allowed subscription")
            }

            val broadcaster: Broadcaster = framework.broadcasterFactory.lookup(destination, true)
            val ar = atmosphereResource.atmosphereConfig.resourcesFactory().find(atmosphereResource.uuid())
                ?: atmosphereResource
            broadcaster.addAtmosphereResource(ar)

            subscriptions.addSubscription(id = id, destination = destination)
        }
    }

    fun unsubscribe(atmosphereResource: AtmosphereResource, stompFrame: StompFrameFromClient) {
        val id = stompFrame.headers["id"]
        if (id?.isBlank() != false) {
            throw StompErrorException("UNSUBSCRIBE must have an id")
        }

        val resourceSession = sessionFactory.getSession(atmosphereResource)
        val subscriptions = resourceSession.subscriptions
        val dest = subscriptions.getById(id)
            ?: throw StompException("Id '$id' not found among subscriptions")

        val broadcaster: Broadcaster = framework.broadcasterFactory.lookup(dest, true)
        val ar = atmosphereResource.atmosphereConfig.resourcesFactory().find(atmosphereResource.uuid())
            ?: atmosphereResource
        broadcaster.removeAtmosphereResource(ar)

        synchronized(subscriptions) {
            subscriptions.removeById(id)
        }
    }
}

private const val SUBSCRIBE_CLIENT_INVOKER = "com.youcruit.atmosphere.stomp.invoker.StompSubscribeInvoker"

internal fun AtmosphereConfig.stompSubscribeInvoker() =
    servletContext.getAttribute(SUBSCRIBE_CLIENT_INVOKER) as? StompSubscribeHandler
        ?: let {
            val invokers = StompSubscribeHandler(framework())
            servletContext.setAttribute(SUBSCRIBE_CLIENT_INVOKER, invokers)
            invokers
        }