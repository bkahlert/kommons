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
import koodies.collections.synchronizedMapOf
import koodies.io.ByteArrayOutputStream
import koodies.io.TeeOutputStream
import koodies.logging.FixedWidthRenderingLogger.Border
import koodies.logging.InMemoryLogger
import koodies.logging.lineEndsTrimmed
import koodies.test.executionResult
import koodies.test.get
import koodies.test.isVerbose
import koodies.test.output.TestLogger
import koodies.test.output.endSpanAndLogTestResult
import koodies.test.put
import koodies.test.testName
import koodies.text.LineSeparators.mapLines
import koodies.tracing.Span.State.Started
import koodies.tracing.rendering.Renderer
import koodies.tracing.rendering.toRenderer
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace
import org.junit.jupiter.api.extension.ExtensionContext.Store
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.support.TypeBasedParameterResolver
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import io.opentelemetry.api.OpenTelemetry as OpenTelemetryAPI

class TestTelemetry : TestExecutionListener, TypeBasedParameterResolver<Span>(), AfterEachCallback {

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

    private val ExtensionContext.store: Store get() = getStore(Namespace.create(TestTelemetry::class.java, requiredTestMethod))
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Span {
        val name = extensionContext.testName
        val border = Border.DEFAULT
        val isVerbose = extensionContext.isVerbose || parameterContext.isVerbose
        val stored = ByteArrayOutputStream()
        val outputStream = if (isVerbose) TeeOutputStream(stored, System.out) else stored
        val logger = TestLogger(extensionContext, parameterContext, name, border, outputStream)
        val renderer = logger.toRenderer()

        @Suppress("JoinDeclarationAndAssignment")
        lateinit var span: OpenTelemetrySpan
        span = OpenTelemetrySpan(name, null, object : Renderer by renderer {
            override fun start(name: CharSequence, started: Started) {
                renderer.start(name, started)
                logs[checkNotNull(span.traceId)] = logger
            }
        })
        extensionContext.store.put(span)
        return span
    }

    override fun afterEach(extensionContext: ExtensionContext) {
        val span: OpenTelemetrySpan? = extensionContext.store.get()
        span?.end(extensionContext.executionResult)
        extensionContext.endSpanAndLogTestResult()
    }


    companion object {
        const val ENABLED: Boolean = true

        private val traces = synchronizedMapOf<TraceId, MutableList<SpanData>>()

        private object InMemoryStoringSpanProcessor : SpanProcessor {
            override fun isStartRequired(): Boolean = false
            override fun onStart(parentContext: Context?, span: ReadWriteSpan?): Unit = Unit
            override fun isEndRequired(): Boolean = true
            override fun onEnd(span: ReadableSpan) {
                val spanData = span.toSpanData()
                traces.getOrPut(TraceId(spanData.traceId)) { mutableListOf() }.add(spanData)
            }
        }

        /**
         * Returns the trace recorded for the given [spanId].
         */
        operator fun get(traceId: TraceId): List<SpanData> =
            traces.getOrDefault(traceId, emptyList())

        private val logs = synchronizedMapOf<TraceId, InMemoryLogger>()
        fun logs(traceId: TraceId?) = logs[traceId]?.toString(null, false, 1)?.mapLines { it.removePrefix("│   ") } ?: ""
    }
}

/**
 * Ends this spans and returns a [Builder] to run assertions on the recorded [SpanData].
 */
fun Span.endAndExpect() =
    expectThat(TestTelemetry[end()])

/**
 * Ends this spans and runs the specified [assertions] on the recorded [SpanData].
 */
fun Span.endAndExpect(assertions: Builder<List<SpanData>>.() -> Unit) {
    expectThat(TestTelemetry[end()], assertions)
}

/**
 * Returns a [Builder] to run assertions on what was rendered.
 */
fun Span.expectThatRendered() =
    expectThat(TestTelemetry.logs(traceId).lineEndsTrimmed)

/**
 * Runs the specified [assertions] on what was rendered.
 */
fun Span.expectThatRendered(assertions: Builder<String>.() -> Unit) =
    expectThat(TestTelemetry.logs(traceId).lineEndsTrimmed, assertions)
