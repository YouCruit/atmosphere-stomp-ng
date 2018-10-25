package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.api.DefaultTranscoder
import com.youcruit.atmosphere.stomp.api.MessageDecoder
import com.youcruit.atmosphere.stomp.api.annotations.StompEndpoint
import com.youcruit.atmosphere.stomp.api.annotations.StompHeartbeat
import com.youcruit.atmosphere.stomp.api.annotations.StompService
import com.youcruit.atmosphere.stomp.api.annotations.StompSubscriptionService
import com.youcruit.atmosphere.stomp.invoker.HeartbeatMethodInvocation
import com.youcruit.atmosphere.stomp.invoker.InjectingMethodInvocation
import com.youcruit.atmosphere.stomp.invoker.stompHeartbeatInvoker
import com.youcruit.atmosphere.stomp.invoker.stompReceiveInvoker
import com.youcruit.atmosphere.stomp.invoker.stompSubscribeInvoker
import com.youcruit.atmosphere.stomp.util.FixedUriTemplate
import org.atmosphere.annotation.Processor
import org.atmosphere.config.AtmosphereAnnotation
import org.atmosphere.cpr.AtmosphereFramework
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

@AtmosphereAnnotation(StompEndpoint::class)
internal class StompEndpointProcessor : Processor<Any> {
    private val messageDecoderCache = LinkedHashMap<Class<*>, MessageDecoder<*>?>()

    override fun handle(framework: AtmosphereFramework, annotatedClass: Class<Any>) {
        val obj = framework.newClassInstance(Any::class.java, annotatedClass)
        try {
            for (method in annotatedClass.methods) {
                val stompServiceAnnotation = method.getAnnotation(StompService::class.java)
                if (stompServiceAnnotation != null) {
                    stompService(stompServiceAnnotation, framework, method, obj)
                }
                val stompSubscriptionService = method.getAnnotation(StompSubscriptionService::class.java)
                if (stompSubscriptionService != null) {
                    stompSubscribe(stompSubscriptionService, framework, method, obj)
                }
                if (method.isAnnotationPresent(StompHeartbeat::class.java)) {
                    stompHeartbeat(framework, method, obj)
                }
            }
        } catch (t: Throwable) {
            logger.error("Caught an error while processing endpoint: ${annotatedClass.name}", t)
            throw t
        }
    }

    private fun stompService(stompServiceAnnotation: StompService, framework: AtmosphereFramework, method: Method, obj: Any) {
        if (stompServiceAnnotation.value.isEmpty()) {
            throw IllegalArgumentException("StompService.value cannot be empty")
        }

        val decoder = if (stompServiceAnnotation.decoder == DefaultTranscoder::class) {
            cachedDecoder(framework, MessageDecoder::class.java)
                ?: DefaultTranscoder
        } else {
            framework.newClassInstance(MessageDecoder::class.java, stompServiceAnnotation.decoder.java)
        }

        val methodInvocation = InjectingMethodInvocation(
            method = method,
            bodyConverter = decoder,
            obj = obj
        )

        val invokers = framework.atmosphereConfig.stompReceiveInvoker()

        val uriTemplate = FixedUriTemplate(stompServiceAnnotation.value)

        val oldEndpoint = invokers.endpoints[uriTemplate]
        if (oldEndpoint != null) {
            throw IllegalArgumentException("path ${stompServiceAnnotation.value} cannot be registered to $method, since it's already ${oldEndpoint.method}")
        }

        invokers.endpoints[uriTemplate] = methodInvocation
    }

    private fun stompSubscribe(stompSubscriptionService: StompSubscriptionService, framework: AtmosphereFramework, method: Method, obj: Any) {
        if (stompSubscriptionService.value.isEmpty()) {
            throw IllegalArgumentException("StompSubscriptionService.value cannot be empty")
        }

        val methodInvocation = InjectingMethodInvocation(
            method = method,
            bodyConverter = DefaultTranscoder,
            obj = obj
        )

        if (method.returnType != Boolean::class.java) {
            throw IllegalArgumentException("method $method has to return nonnull boolean value")
        }

        val invokers = framework.atmosphereConfig.stompSubscribeInvoker()

        val template = FixedUriTemplate(stompSubscriptionService.value)
        val oldEndpoint = invokers.endpoints[template]
        if (oldEndpoint != null) {
            throw IllegalArgumentException("path ${stompSubscriptionService.value} cannot be registered to $method, since it's already ${oldEndpoint.method}")
        }

        invokers.endpoints[template] = methodInvocation
    }

    private fun stompHeartbeat(framework: AtmosphereFramework, method: Method, obj: Any) {
        val methodInvocation = HeartbeatMethodInvocation(
            method = method,
            obj = obj
        )
        if (method.returnType != Void.TYPE) {
            throw IllegalArgumentException("method $method has to return nonnull boolean value")
        }

        val invokers = framework.atmosphereConfig.stompHeartbeatInvoker()

        invokers.endpoints.add(methodInvocation)
    }

    private fun cachedDecoder(framework: AtmosphereFramework, decoder: Class<MessageDecoder<*>>): MessageDecoder<*>? {
        return if (messageDecoderCache.containsKey(decoder)) {
            messageDecoderCache[decoder]
        } else {
            val stomp = try {
                framework.newClassInstance(MessageDecoder::class.java, decoder)
            } catch (e: Exception) {
                null
            }
            messageDecoderCache[decoder] = stomp
            stomp
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(StompEndpointProcessor::class.java)
    }
}