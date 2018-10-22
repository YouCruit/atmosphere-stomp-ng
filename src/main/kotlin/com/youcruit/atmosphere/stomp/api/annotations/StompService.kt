package com.youcruit.atmosphere.stomp.api.annotations

import com.youcruit.atmosphere.stomp.api.DefaultTranscoder
import com.youcruit.atmosphere.stomp.api.MessageDecoder
import kotlin.reflect.KClass

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class StompService(
    /**
     * The destination of the service that receives the messages from the client.
     * It will be parsed as a UriTemplate in "Jersey"(JSR311)-style.
     */
    val value: String,

    /*
     * The decoder for the body. This will turn the body from bytes to something else.
     * If this is left as-is, a MessageDecoder implementation can be set by making
     * the @{org.atmosphere.cpr.AtmosphereFramework#newClassInstance} return that
     * implementation when it tries to instantiate MessageDecoder::class
     */
    val decoder: KClass<out MessageDecoder<*>> = DefaultTranscoder::class
)