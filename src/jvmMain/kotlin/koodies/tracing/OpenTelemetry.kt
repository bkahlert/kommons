package koodies.tracing

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.AttributeKey
import io.opentelemetry.api.common.AttributeType.BOOLEAN
import io.opentelemetry.api.common.AttributeType.BOOLEAN_ARRAY
import io.opentelemetry.api.common.AttributeType.DOUBLE
import io.opentelemetry.api.common.AttributeType.DOUBLE_ARRAY
import io.opentelemetry.api.common.AttributeType.LONG
import io.opentelemetry.api.common.AttributeType.LONG_ARRAY
import io.opentelemetry.api.common.AttributeType.STRING
import io.opentelemetry.api.common.AttributeType.STRING_ARRAY
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.TracerProvider
import io.opentelemetry.context.propagation.ContextPropagators
import koodies.text.ANSI.ansiRemoved
import koodies.text.withSuffix
import koodies.tracing.Key.KeyValue
import koodies.tracing.OpenTelemetry.NOOP
import koodies.tracing.OpenTelemetry.register
import kotlin.reflect.KProperty
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
public object OpenTelemetry : io.opentelemetry.api.OpenTelemetry {
    private var instance: io.opentelemetry.api.OpenTelemetry? = null
    private val instanceOrDefault: io.opentelemetry.api.OpenTelemetry
        get() = instance ?: GlobalOpenTelemetry.get()

    public fun register(customInstance: io.opentelemetry.api.OpenTelemetry) {
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
public object Tracer : io.opentelemetry.api.trace.Tracer {
    private val instance: TracerAPI get() = OpenTelemetry.tracerProvider.get("com.bkahlert.koodies", "1.5.1")

    override fun spanBuilder(spanName: String): SpanBuilder = instance.spanBuilder(spanName)

    /**
     * [TracerAPI] that does nothing.
     */
    public object NOOP : TracerAPI by OpenTelemetry.NOOP.tracerProvider.get("com.bkahlert.koodies", "0.0.1")
}

/**
 * [AttributeKey] extension
 */
public class Key<T, R>(
    private val attributeKey: AttributeKey<T>,
    private val attributeKeyFactory: (String) -> AttributeKey<T>,
    private val transform: (R) -> T,
) : AttributeKey<T> by attributeKey {
    override fun toString(): String = attributeKey.key
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (attributeKey.javaClass == other?.javaClass) return attributeKey == other
        if (javaClass != other?.javaClass) return false
        other as Key<*, *>
        if (attributeKey != other.attributeKey) return false
        return true
    }

    override fun hashCode(): Int = attributeKey.hashCode()

    /** Whether this key identifies a value used for rendering. */
    public val isRenderingKey: Boolean get() = attributeKey.key.endsWith(RENDERING_KEY_SUFFIX)

    /** Contains the key to access the corresponding value. */
    public val valueKey: Key<T, R>
        get() = if (!this.isRenderingKey) this
        else Key(attributeKeyFactory(attributeKey.key.removeSuffix(RENDERING_KEY_SUFFIX)), attributeKeyFactory, transform)

    /** Contains the key to access the value used for rendering. */
    public val renderingKey: Key<T, R>
        get() = if (this.isRenderingKey) this
        else Key(attributeKeyFactory(attributeKey.key.withSuffix(RENDERING_KEY_SUFFIX)), attributeKeyFactory, transform)

    /**
     * Provides a delegate to read the actual value from the owning [Attributes] instance.
     */
    public operator fun getValue(thisRef: Attributes, property: KProperty<*>): T? = thisRef.get(valueKey)

    /**
     * Wraps this key and the provided [value] for type-safe access.
     *
     * ***Hint:** The [value] is accessible while rendering and will be recorded/traced, too.
     *
     * @see renderingOnly
     */
    public infix fun to(value: R): KeyValue<T, R> = KeyValue(valueKey, value, transform)

    /**
     * Wraps this key and the provided value during rendering for type-safe access.
     *
     * ***Hint:** The [renderingValue] is only accessible while rendering and will **not** be recorded/traced.*
     */
    public infix fun renderingOnly(renderingValue: R): KeyValue<T, R> = KeyValue(renderingKey, renderingValue, transform)

    /**
     * Type-safe wrapper of the given [key] and [renderingKey]. Using the specified [transform]
     * the value to be used for tracing can be computed.
     */
    public data class KeyValue<T, R>(
        public val key: Key<T, R>,
        public val renderingValue: R,
        private val transform: (R) -> T,
    ) {
        public val value: T get() = key.transform(renderingValue)
    }

    public companion object {

        private const val RENDERING_KEY_SUFFIX = ".render"

        /** Returns a new [Key] for [T] valued and [R] rendered attributes, whereas [transform] is used to derive [T] from [R]. */
        private fun <T, R> ((String) -> AttributeKey<T>).asKey(key: String, transform: (R) -> T): Key<T, R> = Key(invoke(key), this, transform)

        /** Returns a new [Key] for String valued attributes. */
        public fun stringKey(key: String): Key<String, String> = stringKey(key) { it }

        /** Returns a new [Key] for String valued and [R] rendered attributes, whereas [transform] is used to derive the string from [R]. */
        public fun <R> stringKey(key: String, transform: (R) -> String): Key<String, R> =
            { it: String -> AttributeKey.stringKey(it) }.asKey(key, transform)

        /** Returns a new [Key] for Boolean valued attributes.  */
        public fun booleanKey(key: String): Key<Boolean, Boolean> = booleanKey(key) { it }

        /** Returns a new [Key] for Boolean valued and [R] rendered attributes, whereas [transform] is used to derive the boolean from [R]. */
        public fun <R> booleanKey(key: String, transform: (R) -> Boolean): Key<Boolean, R> =
            { it: String -> AttributeKey.booleanKey(it) }.asKey(key, transform)

        /** Returns a new [Key] for Long valued attributes.  */
        public fun longKey(key: String): Key<Long, Long> = longKey(key) { it }

        /** Returns a new [Key] for Long valued and [R] rendered attributes, whereas [transform] is used to derive the long from [R]. */
        public fun <R> longKey(key: String, transform: (R) -> Long): Key<Long, R> =
            { it: String -> AttributeKey.longKey(it) }.asKey(key, transform)

        /** Returns a new [Key] for Double valued attributes.  */
        public fun doubleKey(key: String): Key<Double, Double> = doubleKey(key) { it }

        /** Returns a new [Key] for Double valued and [R] rendered attributes, whereas [transform] is used to derive the double from [R]. */
        public fun <R> doubleKey(key: String, transform: (R) -> Double): Key<Double, R> =
            { it: String -> AttributeKey.doubleKey(it) }.asKey(key, transform)

        /** Returns a new [Key] for List&lt;String&gt; valued attributes.  */
        public fun stringArrayKey(key: String): Key<List<String>, List<String>> = stringArrayKey(key) { it }

        /** Returns a new [Key] for List&lt;String&gt; valued and [R] rendered attributes, whereas [transform] is used to derive the string list from [R]. */
        public fun <R> stringArrayKey(key: String, transform: (R) -> List<String>): Key<List<String>, R> =
            { it: String -> AttributeKey.stringArrayKey(it) }.asKey(key, transform)

        /** Returns a new [Key] for List&lt;Boolean&gt; valued attributes.  */
        public fun booleanArrayKey(key: String): Key<List<Boolean>, List<Boolean>> = booleanArrayKey(key) { it }

        /** Returns a new [Key] for List&lt;Boolean&gt; valued and [R] rendered attributes, whereas [transform] is used to derive the boolean list from [R]. */
        public fun <R> booleanArrayKey(key: String, transform: (R) -> List<Boolean>): Key<List<Boolean>, R> =
            { it: String -> AttributeKey.booleanArrayKey(it) }.asKey(key, transform)

        /** Returns a new [Key] for List&lt;Long&gt; valued attributes.  */
        public fun longArrayKey(key: String): Key<List<Long>, List<Long>> = longArrayKey(key) { it }

        /** Returns a new [Key] for List&lt;Long&gt; valued and [R] rendered attributes, whereas [transform] is used to derive the long list from [R]. */
        public fun <R> longArrayKey(key: String, transform: (R) -> List<Long>): Key<List<Long>, R> =
            { it: String -> AttributeKey.longArrayKey(it) }.asKey(key, transform)

        /** Returns a new [Key] for List&lt;Double&gt; valued attributes.  */
        public fun doubleArrayKey(key: String): Key<List<Double>, List<Double>> = doubleArrayKey(key) { it }

        /** Returns a new [Key] for List&lt;Double&gt; valued and [R] rendered attributes, whereas [transform] is used to derive the string double from [R]. */
        public fun <R> doubleArrayKey(key: String, transform: (R) -> List<Double>): Key<List<Double>, R> =
            { it: String -> AttributeKey.doubleArrayKey(it) }.asKey(key, transform)
    }
}

/** Converts this collection of [KeyValue] to [Attributes]. */
public fun Iterable<KeyValue<*, *>>.toAttributes(): Attributes =
    Attributes.builder().apply {
        forEach {
            if (!it.key.isRenderingKey) {
                when (it.key.type) {
                    STRING -> (it.value as? String)?.let { value ->
                        put(AttributeKey.stringKey(it.key.key), value.ansiRemoved)
                    }
                    BOOLEAN -> (it.value as? Boolean)?.let { value ->
                        put(AttributeKey.booleanKey(it.key.key), value)
                    }
                    LONG -> (it.value as? Long)?.let { value ->
                        put(AttributeKey.longKey(it.key.key), value)
                    }
                    DOUBLE -> (it.value as? Double)?.let { value ->
                        put(AttributeKey.doubleKey(it.key.key), value)
                    }
                    STRING_ARRAY -> (it.value as? List<*>)?.let { value ->
                        put(AttributeKey.stringArrayKey(it.key.key), value.filterIsInstance<String>().map { it.ansiRemoved })
                    }
                    BOOLEAN_ARRAY -> (it.value as? List<*>)?.let { value ->
                        put(AttributeKey.booleanArrayKey(it.key.key), value.filterIsInstance<Boolean>())
                    }
                    LONG_ARRAY -> (it.value as? List<*>)?.let { value ->
                        put(AttributeKey.longArrayKey(it.key.key), value.filterIsInstance<Long>())
                    }
                    DOUBLE_ARRAY -> (it.value as? List<*>)?.let { value ->
                        put(AttributeKey.doubleArrayKey(it.key.key), value.filterIsInstance<Double>())
                    }
                    else -> error("${it.key} has no type")
                }
            }
        }
    }.build()

/** Converts this [Attributes] instance to a list of [Key]-value pairs */
public fun Attributes.toList(): List<Pair<Key<out Any, out Any>, Any>> =
    asMap().map { (key: AttributeKey<*>, value) ->
        when (key.type) {
            STRING -> Key.stringKey(key.key)
            BOOLEAN -> Key.booleanKey(key.key)
            LONG -> Key.longKey(key.key)
            DOUBLE -> Key.doubleKey(key.key)
            STRING_ARRAY -> Key.stringArrayKey(key.key)
            BOOLEAN_ARRAY -> Key.booleanArrayKey(key.key)
            LONG_ARRAY -> Key.longArrayKey(key.key)
            DOUBLE_ARRAY -> Key.doubleArrayKey(key.key)
            else -> error("$key has no type")
        } to value
    }
