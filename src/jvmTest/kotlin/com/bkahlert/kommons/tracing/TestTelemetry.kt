package com.bkahlert.kommons.tracing

import com.bkahlert.kommons.Kommons
import com.bkahlert.kommons.collections.synchronizedMapOf
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.tracing.TestSpanParameterResolver.Companion.testTrace
import com.bkahlert.kommons.tracing.TestTelemetry.Companion.InMemoryStoringSpanProcessor
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.exporter.jaeger.JaegerGrpcSpanExporter
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.ReadWriteSpan
import io.opentelemetry.sdk.trace.ReadableSpan
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.SpanLimits
import io.opentelemetry.sdk.trace.SpanProcessor
import io.opentelemetry.sdk.trace.data.SpanData
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan
import strikt.api.Assertion.Builder
import strikt.api.DescribeableBuilder
import strikt.api.expectThat

/**
 * [KommonsTelemetry] integration in JUnit that runs OpenTelemetry
 * along a [TestPlan] execution and provides [InMemoryStoringSpanProcessor] and [expectTraced] to assess recorded data.
 */
class TestTelemetry : TestExecutionListener {

    private lateinit var batchExporter: BatchSpanProcessor

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        if (ENABLED) {
            val jaegerExporter = JaegerGrpcSpanExporter.builder()
                .setEndpoint(Jaeger("localhost").startLocally())
                .build()

            batchExporter = BatchSpanProcessor.builder(jaegerExporter).build()

            val tracerProvider = SdkTracerProvider.builder()
                .let { if (TEST_SPAN_CHECK_ENABLED) it.addSpanProcessor(TestSpanCheckSpanProcessor) else it }
                .addSpanProcessor(InMemoryStoringSpanProcessor)
                .addSpanProcessor(batchExporter)
                .setResource(Resource.create(Attributes.of(
                    AttributeKey.stringKey("service.name"), "${Kommons.name}-test",
                    AttributeKey.stringKey("service.version"), Kommons.version.toString(),
                )))
                .setSpanLimits { SpanLimits.builder().setMaxNumberOfEvents(2500).build() }
                .build()

            OpenTelemetrySdk.builder()
                .setTracerProvider(tracerProvider)
                .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
                .buildAndRegisterGlobal()
                .let { KommonsTelemetry.register(it) }
        } else {
            KommonsTelemetry.register(io.opentelemetry.api.OpenTelemetry.noop())
        }
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        if (ENABLED) {
            batchExporter.shutdown()
        }
    }

    companion object {

        /** Whether [TestTelemetry] is enabled. */
        const val ENABLED: Boolean = false

        /** Whether span should be checked if they were only created by [TestSpanScope] / [TestSpanParameterResolver]. */
        const val TEST_SPAN_CHECK_ENABLED: Boolean = true

        /**
         * A span processor that throws if a span is encountered that was not created by [TestSpanScope] / [TestSpanParameterResolver].
         *
         * ***Note:** Only spans created with [KommonsTracer] are checked.*
         */
        private object TestSpanCheckSpanProcessor : SpanProcessor {
            override fun isStartRequired(): Boolean = false
            override fun onStart(parentContext: Context, span: ReadWriteSpan): Unit = Unit
            override fun isEndRequired(): Boolean = true
            override fun onEnd(span: ReadableSpan) {
                if (span.name == "check docker") return
                if (span.name == "docker info") return
                require(span.traceId.testTrace) { "Span ${span.name.formattedAs.input} (trace ID: ${span.traceId.formattedAs.input}) is no test span." }
            }
        }

        /** Stores created traces to allow assertions. */
        private val traces = synchronizedMapOf<TraceId, MutableList<SpanData>>()

        /**
         * A span processor that stores created traces to allow assertions.
         *
         * ***Note:** Only spans created with [KommonsTracer] are stored.*
         */
        private object InMemoryStoringSpanProcessor : SpanProcessor {
            override fun isStartRequired(): Boolean = false
            override fun onStart(parentContext: Context, span: ReadWriteSpan): Unit = Unit
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
    }
}

/** Contains the [TraceId] of this span. */
val ReadableSpan.traceId: TraceId get() = TraceId(toSpanData().traceId)

/** Contains the attributes of this span. */
val ReadableSpan.attributes: Attributes get() = toSpanData().attributes

/** Contains the invalid / non-operation trace ID consists only of zeros. */
val TraceId.Companion.NOOP: TraceId get() = TraceId(io.opentelemetry.api.trace.TraceId.getInvalid())

/** Contains the invalid / non-operation span ID consists only of zeros. */
val SpanId.Companion.NOOP: SpanId get() = SpanId(io.opentelemetry.api.trace.SpanId.getInvalid())

/** Returns a [Builder] to run assertions on the recorded [SpanData]. */
fun TraceId.expectTraced(): DescribeableBuilder<List<SpanData>> = expectThat(TestTelemetry[this])

/** Ends this spans and runs the specified [assertions] on the recorded [SpanData]. */
fun TraceId.expectTraced(assertions: Builder<List<SpanData>>.() -> Unit): Unit = expectThat(TestTelemetry[this], assertions)

/** Ends this spans and returns a [Builder] to run assertions on the recorded [SpanData]. */
fun endAndExpect(): DescribeableBuilder<List<SpanData>> = Span.current().run { end(); expectThat(TestTelemetry[traceId]) }

/** Ends this spans and runs the specified [assertions] on the recorded [SpanData]. */
fun endAndExpect(assertions: Builder<List<SpanData>>.() -> Unit): Unit = Span.current().run { end(); expectThat(TestTelemetry[TraceId.current], assertions) }
