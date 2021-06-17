package koodies.tracing

import koodies.tracing.rendering.Printer
import koodies.tracing.rendering.Renderer
import koodies.tracing.rendering.Settings
import java.time.Instant
import io.opentelemetry.api.trace.Tracer as TracerAPI

@TracingDsl
public fun <R> spanning(
    name: CharSequence,
    customize: Settings.() -> Settings = { this },
    timestamp: Instant = Instant.now(),
    tracer: TracerAPI = Tracer,
    block: SpanContext.() -> R,
): R {
    return OpenTelemetrySpan.spanning(name, customize, timestamp, tracer, block)
}

@TracingDsl
public fun <R> spanning(
    name: CharSequence,
    renderer: (Settings, Printer) -> Renderer,
    timestamp: Instant = Instant.now(),
    tracer: TracerAPI = Tracer,
    block: SpanContext.() -> R,
): R {
    return OpenTelemetrySpan.spanning(name, renderer, timestamp, tracer, block)
}
