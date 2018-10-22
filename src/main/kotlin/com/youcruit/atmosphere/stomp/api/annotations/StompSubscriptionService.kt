package com.youcruit.atmosphere.stomp.api.annotations

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class StompSubscriptionService(
    /**
     * The destination that the client want to subscribe to.
     * It will be parsed as a UriTemplate in "Jersey"(JSR311)-style.
     */
    val value: String
)
