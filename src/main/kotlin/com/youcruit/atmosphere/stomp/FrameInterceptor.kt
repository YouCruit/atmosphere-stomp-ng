package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.api.exceptions.StompErrorException
import com.youcruit.atmosphere.stomp.api.exceptions.StompException
import com.youcruit.atmosphere.stomp.api.exceptions.StompWithReplyException
import com.youcruit.atmosphere.stomp.invoker.StompHeartBeatInterceptor
import com.youcruit.atmosphere.stomp.invoker.StompOnDisconnectInterceptor
import com.youcruit.atmosphere.stomp.invoker.StompReceiveFromClientInvoker
import com.youcruit.atmosphere.stomp.invoker.StompSubscribeHandler
import com.youcruit.atmosphere.stomp.invoker.stompHeartbeatInvoker
import com.youcruit.atmosphere.stomp.invoker.stompOnDisconnectInvoker
import com.youcruit.atmosphere.stomp.invoker.stompReceiveInvoker
import com.youcruit.atmosphere.stomp.invoker.stompSubscribeInvoker
import com.youcruit.atmosphere.stomp.protocol.AVAILABLE_STOMP_PROTOCOLS
import com.youcruit.atmosphere.stomp.protocol.ClientStompCommand
import com.youcruit.atmosphere.stomp.protocol.ServerStompCommand
import com.youcruit.atmosphere.stomp.protocol.Stomp10Protocol
import com.youcruit.atmosphere.stomp.protocol.StompFrameFromClient
import com.youcruit.atmosphere.stomp.protocol.StompFrameFromServer
import com.youcruit.atmosphere.stomp.protocol.StompProtocol
import com.youcruit.atmosphere.stomp.protocol.selectBestProtocol
import com.youcruit.atmosphere.stomp.util.PROTOCOL_VERSION
import com.youcruit.atmosphere.stomp.util.protocol
import com.youcruit.atmosphere.stomp.util.subscriptions
import org.atmosphere.cpr.Action
import org.atmosphere.cpr.ApplicationConfig.CLIENT_HEARTBEAT_INTERVAL_IN_SECONDS
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.AtmosphereHandler
import org.atmosphere.cpr.AtmosphereInterceptorAdapter
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.AtmosphereResourceImpl
import org.atmosphere.cpr.AtmosphereResourceSession
import org.atmosphere.cpr.AtmosphereResourceSessionFactory
import org.atmosphere.cpr.BroadcastFilterLifecycle
import org.atmosphere.cpr.HeartbeatAtmosphereResourceEvent
import org.atmosphere.handler.AbstractReflectorAtmosphereHandler
import org.atmosphere.interceptor.InvokationOrder
import org.atmosphere.util.IOUtils
import org.atmosphere.websocket.WebSocket
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.time.Duration
import java.util.SortedSet

class FrameInterceptor : AtmosphereInterceptorAdapter() {
    private lateinit var framework: AtmosphereFramework
    private lateinit var heartBeatInterceptor: StompHeartBeatInterceptor
    private lateinit var onDisconnectInterceptor: StompOnDisconnectInterceptor
    private lateinit var sessionFactory: AtmosphereResourceSessionFactory
    private lateinit var stompReceiveFromClientInvoker: StompReceiveFromClientInvoker
    private lateinit var stompSubscribeHandler: StompSubscribeHandler
    private var stackInErrors = false

    override fun inspect(r: AtmosphereResource): Action {
        super.inspect(r)
        r as AtmosphereResourceImpl

        val body = try {
            IOUtils.readEntirelyAsByte(r)
        } catch (e: IOException) {
            return errorAndClose(Stomp10Protocol, StompErrorException("Failed to read body", e), null, r)
        }
        try {
            if (body.isEmpty()) {
                return Action.CONTINUE
            } else {
                val resourceSession = sessionFactory.getSession(r)!!
                if (resourceSession.protocol.eol(body)) {
                    val event = HeartbeatAtmosphereResourceEvent(r)
                    // Fire event
                    r.notifyListeners(event)
                    return Action.SKIP_ATMOSPHEREHANDLER
                } else {
                    val frame = resourceSession.protocol.parse(ByteArrayInputStream(body))
                    try {
                        val action = when (frame.command) {
                            ClientStompCommand.CONNECT,
                            ClientStompCommand.STOMP -> {
                                if (connect(resourceSession, frame, r)) {
                                    // all good!
                                    Action.CANCELLED
                                } else {
                                    // Error
                                    return Action.DESTROYED
                                }
                            }
                            ClientStompCommand.DISCONNECT,
                            ClientStompCommand.ACK,
                            ClientStompCommand.NACK,
                            ClientStompCommand.BEGIN,
                            ClientStompCommand.COMMIT,
                            ClientStompCommand.ABORT -> Action.CONTINUE // Noop
                            ClientStompCommand.SEND -> {
                                stompReceiveFromClientInvoker.invoke(r, frame)
                                Action.CONTINUE
                            }
                            ClientStompCommand.SUBSCRIBE -> {
                                stompSubscribeHandler.subscribe(r, frame)
                                Action.CONTINUE
                            }
                            ClientStompCommand.UNSUBSCRIBE -> {
                                stompSubscribeHandler.unsubscribe(r, frame)
                                Action.CONTINUE
                            }
                        }
                        if (frame.receipt != null) {
                            r.write(
                                resourceSession.protocol.encodeFrame(
                                    StompFrameFromServer.receiptOf(frame)
                                )
                            )
                        }
                        return action
                    } catch (e: StompWithReplyException) {
                        val frames = sessionFactory
                            .getSession(r)
                            .subscriptions
                            .createFrames(
                                destination = e.destination,
                                protocol = resourceSession.protocol,
                                message = e.message,
                                extraHeaders = e.headers
                            )
                        r.write(frames.toByteArray())
                    } catch (e: StompException) {
                        logger.info("STOMP exception: {} ", e.message)
                    } catch (e: StompErrorException) {
                        return errorAndClose(resourceSession.protocol, e, frame.receipt, r)
                    } catch (e: Exception) {
                        return errorAndClose(resourceSession.protocol, e, frame.receipt, r, "")
                    }
                }
            }
        } catch (e: StompErrorException) {
            return errorAndClose(Stomp10Protocol, e, null, r)
        }
        return Action.CANCELLED
    }

    private fun connect(resourceSession: AtmosphereResourceSession, frame: StompFrameFromClient, r: AtmosphereResourceImpl): Boolean {
        if (resourceSession.getAttribute(PROTOCOL_VERSION) != null) {
            throw StompErrorException("Already connected")
        }

        val clientVersions = parseVersion(frame.headers["accept-version"])
            ?: sortedSetOf(1.0f)
        val selectBestProtocol = selectBestProtocol(clientVersions)
        if (selectBestProtocol == null) {
            r.write(
                Stomp10Protocol.encodeFrame(
                    StompFrameFromServer(
                        command = ServerStompCommand.ERROR,
                        headers = mapOf(
                            "version" to (AVAILABLE_STOMP_PROTOCOLS.joinToString(separator = ",") { it.version.toString() }),
                            "content-type" to "text/plain"
                        ),
                        body = "Supported protocol versions are ${AVAILABLE_STOMP_PROTOCOLS.joinToString(separator = " ") { it.version.toString() }}"
                    )
                )
            )
            try {
                r.response.flushBuffer()
            } catch (ignored: Exception) {
            }
            r.close()
            return false
        }
        resourceSession.protocol = selectBestProtocol

        if (resourceSession.protocol.version >= 1.2f) {
            // Ignoring host for now
            frame.headers["host"]
                ?: throw StompErrorException("Host not set. It's ignored, but required according to the spec.")
        }
        val webSocket = r.response.asyncIOWriter as WebSocket
        webSocket.resource().addEventListener(onDisconnectInterceptor)

        resourceSession.subscriptions = Subscriptions()
        val headers = LinkedHashMap(mapOf(
            "version" to resourceSession.protocol.version.toString(),
            "session" to r.uuid(),
            "server" to "atmosphere-stomp-ng"
        ))
        if (resourceSession.protocol.version == 1.2f) {
            val serverHeartbeat = framework.atmosphereConfig.getInitParameter(CLIENT_HEARTBEAT_INTERVAL_IN_SECONDS, 0)

            val heartbeat = selectHeartbeat(frame.headers["heart-beat"], IntRange(0, serverHeartbeat))
            // Do something with heartbeat
            if (!heartbeat.isZero) {
                r.addEventListener(Heartbeater(heartbeat, webSocket))
            }

            headers["heart-beat"] = "0,$serverHeartbeat"
        }
        r.write(
            resourceSession.protocol.encodeFrame(
                StompFrameFromServer(
                    command = ServerStompCommand.CONNECTED,
                    headers = headers,
                    body = byteArrayOf()
                )
            )
        )
        return true
    }

    private fun selectHeartbeat(
        heartBeatHeader: String?,
        @Suppress("UNUSED_PARAMETER")
        serverHeartbeat: IntRange
    ): Duration {
        val clientInterval = heartBeatHeader
            ?.let {
                HEART_BEAT_REGEX.matchEntire(it)
            }?.groupValues
            ?.let { (_, from, to) ->
                IntRange(from.toInt(), to.toInt())
            } ?: IntRange(0, Int.MAX_VALUE)
        if (clientInterval.first < 0 || clientInterval.last < 0) {
            throw StompErrorException("heart-beat must be positive")
        }
        return Duration.ofMillis(clientInterval.last.toLong())
    }

    private fun errorAndClose(
        stompProtocol: StompProtocol,
        e: Exception,
        receipt: String?,
        r: AtmosphereResource,
        message: String = e.message ?: ""
    ): Action {
        logger.error("STOMP ERROR: ${e.message}", e)

        val headers = LinkedHashMap<String, String>()
        if (receipt != null) {
            headers["receipt-id"] = receipt
        }
        headers["content-type"] = "text/plain"
        headers["message"] = message

        r.write(
            stompProtocol.encodeFrame(
                StompFrameFromServer(
                    command = ServerStompCommand.ERROR,
                    headers = headers,
                    body = if (stackInErrors) {
                        StringWriter().use {
                            e.printStackTrace(PrintWriter(it))
                            it
                        }.toString()
                    } else {
                        message
                    }
                )
            )
        )
        try {
            r.response.flushBuffer()
        } catch (ignored: Exception) {
        }
        r.close()
        return Action.DESTROYED
    }

    private fun parseVersion(versionHeader: String?): SortedSet<Float>? {
        return versionHeader
            ?.trim()
            ?.splitToSequence(",")
            ?.map { it.toFloatOrNull() }
            ?.filterNotNull()
            ?.toSortedSet()
    }

    override fun configure(config: AtmosphereConfig) {
        framework = config.framework()
        heartBeatInterceptor = framework.atmosphereConfig.stompHeartbeatInvoker()
        onDisconnectInterceptor = framework.atmosphereConfig.stompOnDisconnectInvoker()
        stompSubscribeHandler = framework.atmosphereConfig.stompSubscribeInvoker()
        stompReceiveFromClientInvoker = framework.atmosphereConfig.stompReceiveInvoker()
        sessionFactory = framework.atmosphereConfig.sessionFactory()
        if (config.getInitParameter(STOMP_ERROR_FRAME_CONTAINS_STACKTRACE)?.toBoolean() == true) {
            stackInErrors = true
        }

        val stompPath = config.getInitParameter(STOMP_PATH, "/ws/stomp")

        framework.addAtmosphereHandler(stompPath, framework.newClassInstance<AtmosphereHandler, AbstractReflectorAtmosphereHandler.Default>(AtmosphereHandler::class.java, AbstractReflectorAtmosphereHandler.Default::class.java))

        val filter = framework.newClassInstance<BroadcastFilterLifecycle, StompBroadcastFilter>(BroadcastFilterLifecycle::class.java, StompBroadcastFilter::class.java)
        framework.broadcasterFilters(filter)
    }

    override fun priority() = InvokationOrder.PRIORITY.AFTER_DEFAULT

    companion object {
        const val STOMP_ERROR_FRAME_CONTAINS_STACKTRACE = "com.youcruit.atmosphere.stomp.error.contains.stack"
        const val STOMP_PATH = "com.youcruit.atmosphere.stomp.path"
        const val STOMP_REQUEST_FRAME = "com.youcruit.atmosphere.stomp.request.frame"
        private val HEART_BEAT_REGEX = Regex("([0-9]+),([0-9]+)")

        private val logger = LoggerFactory.getLogger(FrameInterceptor::class.java)!!
    }
}