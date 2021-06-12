package koodies.tracing

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.context.propagation.ContextPropagators
import koodies.tracing.OpenTelemetry.register
import io.opentelemetry.api.OpenTelemetry as OpenTelemetryAPI
import io.opentelemetry.api.trace.Tracer as TracerAPI

/**
 * [OpenTelemetry](https://opentelemetry.io) instance used by this library.
 *
 * The actual implementation to be used can be specified using [register].
 * If none is explicitly registered, [GlobalOpenTelemetry.get] is used.
 */
public object OpenTelemetry : OpenTelemetryAPI {
    private var instance: OpenTelemetryAPI? = null
    private val instanceOrDefault: OpenTelemetryAPI
        get() = instance ?: GlobalOpenTelemetry.get()

    public fun register(customInstance: OpenTelemetryAPI) {
        instance = customInstance
    }

    override fun getTracerProvider(): TracerProvider = instanceOrDefault.tracerProvider
    override fun getPropagators(): ContextPropagators = instanceOrDefault.propagators
}

/**
 * [OpenTelemetry](https://opentelemetry.io) [TracerAPI] used for tracing.
 *
 * The actual implementation to be used can be specified using [register].
 * If none is explicitly registered, the [TracerAPI] returned by the [TracerProvider] of [GlobalOpenTelemetry.get] is used.
 */
public object Tracer : TracerAPI {
    private val instance: TracerAPI get() = OpenTelemetry.tracerProvider.get("com.bkahlert.koodies", "1.5.1")

    override fun spanBuilder(spanName: String): SpanBuilder = instance.spanBuilder(spanName)
}
