package koodies.tracing

import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import koodies.docker.DockerImage
import koodies.docker.DockerRunCommandLine
import koodies.net.headers
import koodies.test.output.TestLogger
import koodies.text.Semantics.formattedAs
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import strikt.api.Assertion
import strikt.api.expectThat
import java.net.URI
import io.opentelemetry.api.OpenTelemetry as OpenTelemetryAPI

class TestTelemetry : TestExecutionListener {

    private lateinit var batchExporter: BatchSpanProcessor

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        if (ENABLED) {
            val jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint(Jaeger.startLocally())
                .build()

            batchExporter = BatchSpanProcessor.builder(jaegerExporter).build()

            val tracerProvider = SdkTracerProvider.builder()
                .addSpanProcessor(InMemoryStoringSpanProcessor)
                .addSpanProcessor(batchExporter)
                .setResource(Resource.create(attributesOf("service.name" to "koodies-test")))
                .build()

            val openTelemetry: OpenTelemetryAPI = OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal()

            OpenTelemetry.register(openTelemetry)
        } else {
            OpenTelemetry.register(OpenTelemetryAPI.noop())
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan?) {
        if (ENABLED) {
            batchExporter.shutdown()
        }
    }

    companion object {
        const val ENABLED: Boolean = false

        private val traces = mutableMapOf<String, MutableList<SpanData>>()

        private object InMemoryStoringSpanProcessor : SpanProcessor {
            override fun isStartRequired(): Boolean = false
            override fun onStart(parentContext: Context?, span: ReadWriteSpan?): Unit = Unit
            override fun isEndRequired(): Boolean = true
            override fun onEnd(span: ReadableSpan) {
                val spanData = span.toSpanData()
                traces.getOrPut(spanData.traceId) { mutableListOf() }.add(spanData)
            }
        }

        operator fun get(traceId: TraceId): List<SpanData> =
            traces.getOrDefault(traceId.value, emptyList())
    }
}

private object Jaeger : DockerImage("jaegertracing", listOf("all-in-one")) {

    val restEndpoint = URI.create("http://localhost:16686")
    val protobufEndpoint = URI.create("http://localhost:14250")
    val isRunning: Boolean
        get() = kotlin.runCatching {
            restEndpoint.headers()["status"]?.any { it.contains("200 OK") } ?: false
        }.onFailure { it.printStackTrace() }.getOrDefault(false)

    fun startLocally(): String {
        if (isRunning) return protobufEndpoint.toString()
        check(protobufEndpoint.host == "localhost") { "Can only locally but ${protobufEndpoint.formattedAs.input} was specified." }

        DockerRunCommandLine {
            image by this@Jaeger
            options {
                name { "jaeger" }
                detached { on }
                publish {
                    +"5775:5775/udp"
                    +"6831:6831/udp"
                    +"6832:6832/udp"
                    +"5778:5778"
                    +"16686:${restEndpoint.port}"
                    +"14268:14268"
                    +"14250:${protobufEndpoint.port}"
                    +"9411:9411"
                }
            }
        }.exec.logging()

        return protobufEndpoint.toString()
    }
}

fun TestLogger.expectThatTraced(): Assertion.Builder<List<SpanData>> =
    expectThat(end())

fun TestLogger.expectThatTraced(assertions: Assertion.Builder<List<SpanData>>.() -> Unit) =
    expectThat(end(), assertions)
