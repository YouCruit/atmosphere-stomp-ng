package org.atmosphere.cpr

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.mock
import com.youcruit.atmosphere.stomp.FrameInterceptor
import com.youcruit.atmosphere.stomp.StompEndpointProcessor
import com.youcruit.atmosphere.stomp.api.Destination
import com.youcruit.atmosphere.stomp.protocol.ClientStompCommand
import com.youcruit.atmosphere.stomp.protocol.ServerStompCommand
import com.youcruit.atmosphere.stomp.protocol.Stomp10Protocol
import com.youcruit.atmosphere.stomp.protocol.StompFrame
import com.youcruit.atmosphere.stomp.protocol.StompFrameFromClient
import com.youcruit.atmosphere.stomp.protocol.StompFrameFromServer
import com.youcruit.atmosphere.stomp.util.subscriptions
import org.atmosphere.container.BlockingIOCometSupport
import org.atmosphere.interceptor.AtmosphereResourceLifecycleInterceptor
import org.atmosphere.interceptor.HeartbeatInterceptor
import org.junit.After
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.util.Collections
import java.util.Enumeration
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import javax.servlet.ServletConfig
import javax.servlet.ServletContext
import javax.servlet.ServletException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class StompTest {
    lateinit var processor: AsynchronousProcessor
    lateinit var config: AtmosphereConfig
    lateinit var atmosphereHandler: AtmosphereHandler
    private lateinit var framework: AtmosphereFramework
    @Volatile
    private var disconnect: Boolean = false

    @After
    fun destroyAtmosphere() {
        framework.destroy()
        config.destroy()
    }

    fun initAtmosphere(vararg classes: Class<*>) {
        atmosphereHandler = mock()
        framework = AtmosphereFramework()

        framework.addCustomAnnotationPackage(StompEndpointProcessor::class.java)

        // Detect service
        for (clazz in classes) {
            framework.addAnnotationPackage(clazz)
        }

        framework.customAnnotationPackages().add("com.youcruit.atmosphere.stomp")
        configureFrameWork(framework)
        config = framework.atmosphereConfig
        framework.asyncSupport = BlockingIOCometSupport(config)
        framework.init(object : ServletConfig {
            val servletContext = StompTestServletContext()

            override fun getServletName(): String {
                return "void"
            }

            override fun getServletContext(): ServletContext {
                return servletContext
            }

            override fun getInitParameter(name: String): String? {
                return when (name) {
                    ApplicationConfig.READ_GET_BODY -> "true"
                    else -> null
                }
            }

            override fun getInitParameterNames(): Enumeration<String>? {
                return null
            }
        })

        framework.addAtmosphereHandler("/ws/stomp", atmosphereHandler)
        processor = object : AsynchronousProcessor(config) {
            @Throws(IOException::class, ServletException::class)
            override fun service(req: AtmosphereRequest, res: AtmosphereResponse): Action {
                return action(req, res)
            }
        }
        framework.setAsyncSupport(processor)
    }

    @Suppress("UNUSED_PARAMETER")
    open fun configureFrameWork(framework: AtmosphereFramework) {}

    protected fun newAtmosphereResource(
        req: AtmosphereRequest,
        res: AtmosphereResponse
    ): AtmosphereResource {

        var ar = framework.arFactory.find(MAGIC_UUID) as? AtmosphereResourceImpl

        if (ar == null) {
            ar = AtmosphereResourceImpl()
            val b: Broadcaster = framework.broadcasterFactory.lookup("/ws/stomp", true)
            ar.initialize(config, b, req, res, framework.asyncSupport, atmosphereHandler)
            ar.transport(AtmosphereResource.TRANSPORT.WEBSOCKET)
        } else {
            ar.request.body(req.body().asString())
            ar.request.body(req.inputStream)
            ar.request.headers(req.headersMap())
            ar.atmosphereHandler(atmosphereHandler)
            ar.transport(AtmosphereResource.TRANSPORT.WEBSOCKET)
        }

        ar.addEventListener(object : AtmosphereResourceEventListenerAdapter.OnDisconnect() {

            override fun onDisconnect(event: AtmosphereResourceEvent) {
                disconnect = true
            }
        })

        return ar
    }

    fun simpleSend(
        destination: String,
        command: ClientStompCommand = ClientStompCommand.SEND,
        requestReceipt: Boolean = false
    ): StompFrame {
        val headers = mutableMapOf(
            "content-type" to "text/plain;charset=utf-8",
            "id" to UUID.randomUUID().toString(),
            "destination" to destination
        )
        if (requestReceipt) {
            headers += "receipt-id" to MAGIC_UUID
        }

        return StompFrameFromClient(
            command = command,
            body = """{
              "timestamp": "${Clock.systemUTC().instant()}",
              "message": "hello"
            }""",
            headers = headers
        )
    }

    fun newRequest(stompFrame: StompFrame): AtmosphereRequest {
        val req = AtmosphereRequestImpl.Builder()
            .pathInfo("/ws/stomp")
            .method("GET")
            .body(Stomp10Protocol.asString(stompFrame))
            .headers(mutableMapOf())
            .build()

        req.setAttribute(ApplicationConfig.SUSPENDED_ATMOSPHERE_RESOURCE_UUID, MAGIC_UUID)

        return req
    }

    fun addToBroadcaster(destination: Destination, ar: AtmosphereResource) {
        val b = framework.broadcasterFactory.lookup<Broadcaster>(destination)
        ar.atmosphereConfig
            .sessionFactory()
            .getSession(ar)
            .subscriptions
            .addSubscription(SUBSCRIPTION_ID, destination)
        b.addAtmosphereResource(ar)
    }

    fun runMessage(
        frame: StompFrameFromClient,
        bindToRequest: Boolean = true,
        assertNoMessage: Boolean = false,
        verifier: (StompFrameFromServer) -> Boolean
    ) {
        val req = newRequest(frame)
        val res = AtmosphereResponseImpl.newInstance()

        val latch = LinkedBlockingQueue<Any>()

        val ar = newAtmosphereResource(req, res)

        doAnswer {
            try {
                val data = (it.arguments[0] as AtmosphereResourceEvent).message as String
                @Suppress("DEPRECATION")
                val serverFrame = Stomp10Protocol.parseFromServer(ByteArrayInputStream(data.toByteArray(StandardCharsets.UTF_8)))
                if (verifier(serverFrame)) {
                    latch.add(serverFrame)
                }
            } catch (t: Throwable) {
                latch.add(t)
                throw t
            }
        }
            .`when`(ar.atmosphereHandler)
            .onStateChange(any())

        ar.response.asyncIOWriter(object : AsyncIOWriterAdapter() {
            override fun write(r: AtmosphereResponse, data: ByteArray): AsyncIOWriter {
                try {
                    @Suppress("DEPRECATION")
                    val serverFrame = Stomp10Protocol.parseFromServer(ByteArrayInputStream(data))
                    if (verifier(serverFrame)) {
                        latch.add(serverFrame)
                    }
                    return this
                } catch (t: Throwable) {
                    latch.add(t)
                    throw t
                }
            }
        })

        processor.service(ar.request, ar.response)

        val result = latch.poll(3, TimeUnit.SECONDS)
        if (assertNoMessage) {
            assertNull(result)
        } else {
            if (result is Throwable) {
                throw result
            }
            assertNotNull("Did not receive any data before timeout")
        }
    }

    internal fun runMessage(
        frame: StompFrameFromClient,
        bindToRequest: Boolean = false,
        regex: String
    ) {
        runMessage(frame, bindToRequest) {
            val rex = Regex(regex, RegexOption.DOT_MATCHES_ALL)
            assertTrue(rex.matches(it.bodyAsString()))
            true
        }
    }

    protected fun subscribeStatus() {
        val receiptId = UUID.randomUUID().toString()
        runMessage(
            StompFrameFromClient(
                command = ClientStompCommand.SUBSCRIBE,
                headers = mapOf(
                    "destination" to "/status",
                    "id" to "1",
                    "receipt" to receiptId
                ),
                body = "No care"
            ),
            true
        ) {
            assertEquals(it.receipt, receiptId)
            assertEquals(it.command, ServerStompCommand.RECEIPT)
            true
        }
    }

    companion object {
        private val MAGIC_UUID = UUID.randomUUID().toString()
        private val SUBSCRIPTION_ID = UUID.randomUUID().toString()
    }
}

class StompTestServletContext : ServletContext by mock() {
    val attributes = LinkedHashMap<String, Any?>(
        mapOf(
            AtmosphereFramework.MetaServiceAction::class.java.name to mapOf(
                FrameInterceptor::class.java.name to AtmosphereFramework.MetaServiceAction.INSTALL,
                AtmosphereResourceLifecycleInterceptor::class.java.name to AtmosphereFramework.MetaServiceAction.INSTALL,
                HeartbeatInterceptor::class.java.name to AtmosphereFramework.MetaServiceAction.EXCLUDE
            )
        )
    )

    override fun getAttribute(name: String): Any? =
        attributes[name]

    override fun getAttributeNames(): Enumeration<String> =
        Collections.enumeration(attributes.keys)

    override fun setAttribute(name: String, `object`: Any?) {
        attributes[name] = `object`
    }

    override fun removeAttribute(name: String?) {
        attributes.remove(name)
    }
}