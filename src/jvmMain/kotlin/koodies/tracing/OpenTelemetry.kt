package koodies.tracing

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.context.propagation.ContextPropagators
import koodies.exec.Executable
import koodies.tracing.OpenTelemetry.NOOP
import koodies.tracing.OpenTelemetry.register
import koodies.tracing.rendering.Renderable
import io.opentelemetry.api.OpenTelemetry as OpenTelemetryAPI
import io.opentelemetry.api.trace.Tracer as TracerAPI

/**
 * [OpenTelemetry](https://opentelemetry.io) instance used by this library.
 *
 * The actual implementation to be used can be specified using [register].
 * If none is explicitly registered, [GlobalOpenTelemetry.get] is used.
 *
 * For an implementation that never traces, use [NOOP].
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

    /**
     * [OpenTelemetryAPI] that does nothing.
     */
    public object NOOP : OpenTelemetryAPI by OpenTelemetryAPI.noop()
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

    /**
     * [TracerAPI] that does nothing.
     */
    public object NOOP : TracerAPI by OpenTelemetry.NOOP.tracerProvider.get("com.bkahlert.koodies", "0.0.1")
}

public object RenderingAttributes {

    public fun description(description: Any): Pair<String, Any> = Keys.DESCRIPTION to description
    public fun name(name: Renderable): Pair<String, Any> = Keys.NAME to name

    public object Keys {
        public const val DESCRIPTION: String = "description.renderable"
        public const val NAME: String = "name.renderable"
    }
}

public object KoodiesSpans {
    public const val EXEC: String = "koodies.exec"
    public const val IO: String = "koodies.io"
}

/**
 * Convenience [Attributes] wrapper that provides
 * access to library-related attributes.
 */
public class KoodiesAttributes(private val attributes: Attributes) : Attributes by attributes {

    public val description: String? get() = attributes.get(DESCRIPTION)
    public val execName: String? get() = attributes.get(EXEC_NAME)
    public val execExecutable: String? get() = attributes.get(EXEC_EXECUTABLE)
    public val ioType: String? get() = attributes.get(IO_TYPE)
    public val ioText: String? get() = attributes.get(IO_TEXT)

    public companion object Keys {
        public val DESCRIPTION: AttributeKey<String> = AttributeKey.stringKey("koodies.description")
        public fun description(description: String): Pair<String, Any> = DESCRIPTION.key to description

        public val EXEC_NAME: AttributeKey<String> = AttributeKey.stringKey("koodies.exec.name")
        public fun execName(execName: String?): Pair<String, Any>? = execName?.let { EXEC_NAME.key to it }

        public val EXEC_EXECUTABLE: AttributeKey<String> = AttributeKey.stringKey("koodies.exec.executable")
        public fun execExecutable(executable: Executable<*>): Pair<String, Any> = EXEC_EXECUTABLE.key to executable

        public val IO_TYPE: AttributeKey<String> = AttributeKey.stringKey("koodies.io.type")
        public fun ioType(ioType: String): Pair<String, Any> = IO_TYPE.key to ioType

        public val IO_TEXT: AttributeKey<String> = AttributeKey.stringKey("koodies.io.text")
        public fun ioText(ioText: String): Pair<String, Any> = IO_TEXT.key to ioText
    }
}

/**
 * Convenience access of library-related attributes.
 */
public val Attributes.koodies: KoodiesAttributes get() = KoodiesAttributes(this)

/**
 * Convenience [AttributesBuilder] wrapper that provides
 * access to library-related attributes.
 */
public class KoodiesAttributesBuilder(private val attributesBuilder: AttributesBuilder) : AttributesBuilder by attributesBuilder {

    public fun description(description: String): KoodiesAttributesBuilder {
        attributesBuilder.put(KoodiesAttributes.DESCRIPTION, description)
        return this
    }
}

/**
 * Convenience access of library-related attributes.
 */
public val AttributesBuilder.koodies: KoodiesAttributesBuilder get() = KoodiesAttributesBuilder(this)
