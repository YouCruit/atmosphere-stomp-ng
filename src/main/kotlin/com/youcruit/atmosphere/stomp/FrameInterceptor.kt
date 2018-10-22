package com.youcruit.atmosphere.stomp

import com.youcruit.atmosphere.stomp.invoker.StompHeartBeatInterceptor
import com.youcruit.atmosphere.stomp.invoker.StompReceiveFromClientInvoker
import com.youcruit.atmosphere.stomp.invoker.StompSubscribeHandler
import com.youcruit.atmosphere.stomp.invoker.stompHeartbeatInvoker
import com.youcruit.atmosphere.stomp.protocol.ClientStompCommand
import com.youcruit.atmosphere.stomp.protocol.ServerStompCommand
import com.youcruit.atmosphere.stomp.protocol.Stomp10Protocol
import com.youcruit.atmosphere.stomp.protocol.StompErrorException
import com.youcruit.atmosphere.stomp.protocol.StompException
import com.youcruit.atmosphere.stomp.protocol.StompFrame
import com.youcruit.atmosphere.stomp.protocol.StompProtocol
import com.youcruit.atmosphere.stomp.protocol.selectBestProtocol
import com.youcruit.atmosphere.stomp.util.protocol
import com.youcruit.atmosphere.stomp.util.subscriptions
import org.atmosphere.cpr.Action
import org.atmosphere.cpr.AtmosphereConfig
import org.atmosphere.cpr.AtmosphereFramework
import org.atmosphere.cpr.AtmosphereInterceptorAdapter
import org.atmosphere.cpr.AtmosphereResource
import org.atmosphere.cpr.AtmosphereResourceImpl
import org.atmosphere.cpr.AtmosphereResourceSessionFactory
import org.atmosphere.cpr.HeartbeatAtmosphereResourceEvent
import org.atmosphere.interceptor.InvokationOrder
import org.atmosphere.util.IOUtils
import org.slf4j.LoggerFactory
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.PrintWriter
import java.io.StringWriter
import java.util.SortedSet

class FrameInterceptor : AtmosphereInterceptorAdapter() {
    private lateinit var framework: AtmosphereFramework
    private lateinit var heartBeatInterceptor: StompHeartBeatInterceptor
    private lateinit var sessionFactory: AtmosphereResourceSessionFactory
    private lateinit var stompReceiveFromClientInvoker: StompReceiveFromClientInvoker
    private lateinit var stompSubscribeHandler: StompSubscribeHandler
    private var stackInErrors = false

    override fun inspect(r: AtmosphereResource): Action {
        super.inspect(r)

        val body = try {
            IOUtils.readEntirelyAsByte(r)
        } catch (e: IOException) {
            r.write(
                Stomp10Protocol.encodeFrame(
                    StompFrame(
                        command = ServerStompCommand.ERROR,
                        headers = mapOf(),
                        body = "Failed to read body"
                    )
                )
            )
            return Action.CANCELLED
        }
        try {

            // Let the global handler suspend the connection if no action is submitted
            if (body.isEmpty()) {
                return Action.CONTINUE
            } else {
                val resourceSession = sessionFactory.getSession(r)!!
                if (resourceSession.protocol.eol(body)) {
                    val event = HeartbeatAtmosphereResourceEvent(r as AtmosphereResourceImpl)
                    // Fire event
                    r.notifyListeners(event)
                    return Action.SKIP_ATMOSPHEREHANDLER
                } else {
                    val frame = resourceSession.protocol.parse(ByteArrayInputStream(body))
                    try {
                        val action = when (frame.command as ClientStompCommand) {
                            ClientStompCommand.CONNECT,
                            ClientStompCommand.STOMP -> {
                                val clientVersions = parseVersion(frame.headers["accept-version"])
                                resourceSession.protocol = selectBestProtocol(clientVersions)
                                resourceSession.subscriptions = Subscriptions()
                                Action.CANCELLED
                            }
                            ClientStompCommand.DISCONNECT,
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
                                    StompFrame.receiptOf(frame)
                                )
                            )
                        }
                        return action
                    } catch (e: StompException) {
                        logger.info("STOMP exception: {} ", e.message)
                    } catch (e: StompErrorException) {
                        return errorAndClose(resourceSession.protocol, e, frame.receipt, r)
                    }
                }
            }
        } catch (e: StompErrorException) {
            return errorAndClose(Stomp10Protocol, e, null, r)
        }
        return Action.CANCELLED
    }

    private fun errorAndClose(stompProtocol: StompProtocol, e: StompErrorException, receipt: String?, r: AtmosphereResource): Action {
        logger.error("STOMP ERROR: {} ", e.message, e)

        val headers = LinkedHashMap<String, String>()
        if (receipt != null) {
            headers["receipt-id"] = receipt
        }
        headers["content-type"] = "text/plain"
        headers["message"] = e.message!!

        r.write(
            stompProtocol.encodeFrame(
                StompFrame(
                    command = ServerStompCommand.ERROR,
                    headers = headers,
                    body = if (stackInErrors) {
                        StringWriter().use {
                            e.printStackTrace(PrintWriter(it))
                            it
                        }.toString()
                    } else {
                        e.message!!
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

    private fun parseVersion(versionHeader: String?): SortedSet<Float> {
        return (versionHeader ?: "")
            .trim()
            .splitToSequence(",")
            .map { it.toFloatOrNull() }
            .filterNotNull()
            .toSortedSet()
    }

    override fun configure(config: AtmosphereConfig) {
        framework = config.framework()
        heartBeatInterceptor = framework.atmosphereConfig.stompHeartbeatInvoker()
        sessionFactory = framework.atmosphereConfig.sessionFactory()
        if (config.getInitParameter(STOMP_ERROR_FRAME_CONTAINS_STACKTRACE)?.toBoolean() == true) {
            stackInErrors = true
        }
    }

    override fun priority() = InvokationOrder.PRIORITY.AFTER_DEFAULT

    companion object {
        const val STOMP_ERROR_FRAME_CONTAINS_STACKTRACE = "com.youcruit.atmosphere.stomp.error.contains.stack"

        private val logger = LoggerFactory.getLogger(FrameInterceptor::class.java)!!
    }
}