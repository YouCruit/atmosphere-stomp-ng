package com.youcruit.atmosphere.stomp.invoker

import com.youcruit.atmosphere.stomp.protocol.StompErrorException
import com.youcruit.atmosphere.stomp.protocol.StompException
import com.youcruit.atmosphere.stomp.protocol.StompFrame
import com.youcruit.atmosphere.stomp.util.subscriptions
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.Broadcaster
import org.atmosphere.util.uri.UriPattern

internal class StompSubscribeHandler(
    private val framework: AtmosphereFramework
) {
    val endpoints = LinkedHashMap<UriPattern, InjectingMethodInvocation>()

    private val sessionFactory
        get() = framework.sessionFactory()

    fun subscribe(atmosphereResource: AtmosphereResource, stompFrame: StompFrame) {
        val id = stompFrame.id
        if (id?.isBlank() != false) {
            throw StompErrorException("SUBSCRIBE must have an id")
        }

        val substitutions = sessionFactory.getSession(atmosphereResource).subscriptions

        if (substitutions.getById(id) != null) {
            throw StompErrorException("Already subscribed with $id")
        }

        val destination = stompFrame.destination
            ?: throw StompErrorException("SUBSCRIBE requires a destination")
        val accepted = endpoints
            .asSequence()
            .filter { (key) -> key.match(destination) != null }
            .any { (_, invoker) ->
                invoker.invoke(atmosphereResource, stompFrame) as Boolean
            }

        if (!accepted) {
            throw StompException("No subscription endpoint allowed subscription")
        }

        val broadcaster: Broadcaster = framework.broadcasterFactory.lookup(destination, true)
        val ar = atmosphereResource.atmosphereConfig.resourcesFactory().find(atmosphereResource.uuid())
            ?: atmosphereResource
        broadcaster.addAtmosphereResource(ar)

        substitutions.addSubscription(id = id, destination = destination)
    }

    fun unsubscribe(atmosphereResource: AtmosphereResource, stompFrame: StompFrame) {
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

        subscriptions.removeById(id)
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