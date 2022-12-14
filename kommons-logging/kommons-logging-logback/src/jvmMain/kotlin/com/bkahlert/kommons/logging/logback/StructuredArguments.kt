package com.bkahlert.kommons.logging.logback

import com.bkahlert.kommons.logging.SLF4J
import com.bkahlert.kommons.logging.logback.StructuredArguments.entries
import com.bkahlert.kommons.logging.logback.StructuredArguments.toString
import com.bkahlert.kommons.text.pluralize
import com.bkahlert.kommons.text.simpleKebabCasedName
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN
import net.logstash.logback.marker.MapEntriesAppendingMarker
import net.logstash.logback.marker.ObjectAppendingMarker
import net.logstash.logback.marker.ObjectFieldsAppendingMarker
import net.logstash.logback.marker.RawJsonAppendingMarker
import java.util.Arrays
import net.logstash.logback.argument.StructuredArguments as LogbackStructuredArguments

/** Kotlin-specific factory for creating [StructuredArgument] instances. */
public object StructuredArguments {

    /**
     * Adds
     * - a `"key":"value"` for the given [key] and [value] to the JSON event, and
     * - `key=value` to the formatted message.
     *
     * Example:
     * ```kotlin
     * logger.info("msg: {}", keyValue("key", FooBar()))
     *
     * data class FooBar(val foo: String? = null, val bar: String = "baz")
     * ```
     * ```json
     * {"@timestamp":"...","level":"INFO","message":"msg: key=FooBar(foo=null, bar=baz)","key":{"foo":null,"bar":"baz"}}
     * ```
     *
     * @param key the key/field name
     * @param value the value
     * @param messageFormatPattern if specified, it will be used instead of the `{0}={1}` pattern for the formatted message
     * @param transform if specified, the values are based on its return values
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see ObjectAppendingMarker
     */
    public inline fun <T> keyValue(
        key: String,
        value: T,
        messageFormatPattern: String = DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN,
        transform: (T) -> Any? = { it },
    ): StructuredArgument =
        LogbackStructuredArguments.keyValue(key, transform(value), messageFormatPattern)

    /**
     * With key being the derived kebab-case name of [T], adds
     * - a `"key":"value"` for the given [value] to the JSON event, and
     * - `key=value` to the formatted message.
     *
     * Example:
     * ```kotlin
     * logger.info("msg: {}", keyValue(FooBar()))
     *
     * data class FooBar(val foo: String? = null, val bar: String = "baz")
     * ```
     * ```json
     * {"@timestamp":"...","level":"INFO","message":"msg: foo-bar=FooBar(foo=null, bar=baz)","foo-bar":{"foo":null,"bar":"baz"}}
     * ```
     *
     * @param value the value
     * @param messageFormatPattern if specified, it will be used instead of the `{0}={1}` pattern for the formatted message
     * @param transform if specified, the values are based on its return values
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see ObjectAppendingMarker
     */
    public inline fun <reified T> keyValue(
        value: T,
        messageFormatPattern: String = DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN,
        noinline transform: (T) -> Any? = { it },
    ): StructuredArgument =
        keyValue(key = deriveFieldNameFrom<T>(), value = value, messageFormatPattern = messageFormatPattern, transform = transform)

    /**
     * Abbreviated convenience method for calling [keyValue].
     *
     * @see keyValue
     */
    public inline fun <T> kv(
        key: String,
        value: T,
        messageFormatPattern: String = DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN,
        transform: (T) -> Any? = { it },
    ): StructuredArgument =
        keyValue(key = key, value = value, messageFormatPattern = messageFormatPattern, transform = transform)

    /**
     * Abbreviated convenience method for calling [keyValue].
     *
     * @see keyValue
     */
    public inline fun <reified T> kv(
        value: T,
        messageFormatPattern: String = DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN,
        noinline transform: (T) -> Any? = { it },
    ): StructuredArgument =
        keyValue(key = deriveFieldNameFrom<T>(), value = value, messageFormatPattern = messageFormatPattern, transform = transform)


    /**
     * Adds
     * - a `"key":"value"` for the given [key] and [value] to the JSON event, and
     * - `value` to the formatted message.
     *
     * Example:
     * ```kotlin
     * logger.info("msg: {}", value("key", FooBar()))
     *
     * data class FooBar(val foo: String? = null, val bar: String = "baz")
     * ```
     * ```json
     * {"@timestamp":"...","level":"INFO","message":"msg: FooBar(foo=null, bar=baz)","key":{"foo":null,"bar":"baz"}}
     * ```
     *
     * @param key the key/field name
     * @param value the value
     * @param transform if specified, the values are based on its return values
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see ObjectAppendingMarker
     */
    public inline fun <T> value(
        key: String,
        value: T,
        transform: (T) -> Any? = { it },
    ): StructuredArgument =
        LogbackStructuredArguments.value(key, transform(value))

    /**
     * With key being the derived kebab-case name of [T], adds
     * - a `"key":"value"` for the given [value] to the JSON event, and
     * - `value` to the formatted message.
     *
     * Example:
     * ```kotlin
     * logger.info("msg: {}", value(FooBar()))
     *
     * data class FooBar(val foo: String? = null, val bar: String = "baz")
     * ```
     * ```json
     * {"@timestamp":"...","level":"INFO","message":"msg: FooBar(foo=null, bar=baz)","foo-bar":{"foo":null,"bar":"baz"}}
     * ```
     *
     * @param value the value
     * @param transform if specified, the values are based on its return values
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see ObjectAppendingMarker
     */
    public inline fun <reified T> value(
        value: T,
        noinline transform: (T) -> Any? = { it },
    ): StructuredArgument =
        value(key = deriveFieldNameFrom<T>(), value = value, transform = transform)

    /**
     * Abbreviated convenience method for calling [value].
     *
     * @see value
     */
    public inline fun <T> v(
        key: String,
        value: T,
        transform: (T) -> Any? = { it },
    ): StructuredArgument =
        value(key = key, value = value, transform = transform)

    /**
     * Abbreviated convenience method for calling [value].
     *
     * @see value
     */
    public inline fun <reified T> v(
        value: T,
        noinline transform: (T) -> Any? = { it },
    ): StructuredArgument =
        value(key = deriveFieldNameFrom<T>(), value = value, transform = transform)


    /**
     * Adds
     * - a `"key":"value"` entry for each [Map.Entry] in the given [map] to the JSON event, and
     * - [toString] to the formatted message.
     *
     * Example:
     * ```kotlin
     * logger.info("msg: {}", entries(mapOf("foo" to null, "bar" to "baz")))
     * ```
     * ```json
     * {"@timestamp":"...","level":"INFO","message":"msg: {foo=null, bar=baz}","foo":null,"bar":"baz"}
     * ```
     *
     * @param map [Map] holding the key/value pairs
     * @param transform if specified, the values are based on its return values
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see MapEntriesAppendingMarker
     */
    public inline fun <K, V> entries(
        map: Map<K, V>,
        transform: (Map.Entry<K, V>) -> Any? = { it.value },
    ): StructuredArgument =
        LogbackStructuredArguments.entries(map.mapValues(transform))

    /**
     * Adds
     * - a `"key":"value"` entry for each [Pair] in the given [entries] to the JSON event, and
     * - [toString] to the formatted message.
     *
     * Example:
     * ```kotlin
     * logger.info("msg: {}", entries("foo" to null, "bar" to "baz"))
     * ```
     * ```json
     * {"@timestamp":"...","level":"INFO","message":"msg: {foo=null, bar=baz}","foo":null,"bar":"baz"}
     * ```
     *
     * @param entries [Pair] instances holding the key/value pairs
     * @param transform if specified, the values are based on its return values
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see MapEntriesAppendingMarker
     */
    public inline fun <K, V> entries(
        vararg entries: Pair<K, V>,
        transform: (Map.Entry<K, V>) -> Any? = { it.value },
    ): StructuredArgument =
        entries(map = entries.toMap(), transform = transform)

    /**
     * Adds
     * - a `"key":"value"` entry for each [Map.Entry] in the given [entries] to the JSON event, and
     * - [toString] to the formatted message.
     *
     * Example:
     * ```kotlin
     * logger.info("msg: {}", entries(entry("foo" to null), entry("bar" to "baz")))
     * ```
     * ```json
     * {"@timestamp":"...","level":"INFO","message":"msg: {foo=null, bar=baz}","foo":null,"bar":"baz"}
     * ```
     *
     * @param entries [Map.Entry] instances holding the key/value pairs
     * @param transform if specified, the values are based on its return values
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see MapEntriesAppendingMarker
     */
    public inline fun <K, V> entries(
        vararg entries: Map.Entry<K, V>,
        transform: (Map.Entry<K, V>) -> Any? = { it.value },
    ): StructuredArgument =
        entries(map = entries.associate { (key, value) -> key to value }, transform = transform)


    /**
     * Abbreviated convenience method for calling [entries].
     *
     * @see entries
     */
    public inline fun <K, V> e(
        map: Map<K, V>,
        transform: (Map.Entry<K, V>) -> Any? = { it.value },
    ): StructuredArgument =
        entries(map = map, transform = transform)

    /**
     * Abbreviated convenience method for calling [entries].
     *
     * @see entries
     */
    public inline fun <K, V> e(
        vararg entries: Pair<K, V>,
        transform: (Map.Entry<K, V>) -> Any? = { it.value },
    ): StructuredArgument =
        entries(map = entries.toMap(), transform = transform)

    /**
     * Abbreviated convenience method for calling [entries].
     *
     * @see entries
     */
    public inline fun <K, V> e(
        vararg entries: Map.Entry<K, V>,
        transform: (Map.Entry<K, V>) -> Any? = { it.value },
    ): StructuredArgument =
        entries(map = entries.associate { (key, value) -> key to value }, transform = transform)


    /**
     * Adds
     * - a `"key":"value"` entry for each field in the given [obj] to the JSON event, and
     * - [toString] to the formatted message.
     *
     * Example:
     * ```kotlin
     * logger.info("msg: {}", fields(FooBar()))
     *
     * data class FooBar(val foo: String? = null, val bar: String = "baz")
     * ```
     * ```json
     * {"@timestamp":"...","level":"INFO","message":"msg: FooBar(foo=null, bar=baz)","foo":null,"bar":"baz"}
     * ```
     *
     * @param obj the object to write fields from
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see ObjectFieldsAppendingMarker
     */
    @Suppress("NOTHING_TO_INLINE")
    public inline fun <T> fields(obj: T): StructuredArgument =
        LogbackStructuredArguments.fields(obj)

    /**
     * Abbreviated convenience method for calling [fields].
     *
     * @see fields
     */
    @Suppress("NOTHING_TO_INLINE")
    public inline fun <T> f(obj: T): StructuredArgument =
        fields(obj)


    /**
     * Adds
     * - a field [key] whose value is a JSON array of the given [objects], and
     * - `key=` a string version of the array to the formatted message.
     *
     * Example:
     * ```kotlin
     * logger.info("msg: {}", entries("foo" to null, "bar" to "baz"))
     * ```
     * ```json
     * {"@timestamp":"...","level":"INFO","message":"msg: key=[FooBar(foo=null, bar=baz), FooBar(foo=alt, bar=baz)]","key":[{"foo":null,"bar":"baz"},{"foo":"alt","bar":"baz"}]}
     * ```
     *
     * @param objects elements of the array to write under the [key] key
     * @param key if not specified, the key is the derived kebab-case plural name of [T]
     * @param transform if specified, the values are based on its return values
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see ObjectAppendingMarker
     */
    public inline fun <reified T> array(
        vararg objects: T,
        key: String = derivePluralFieldNameFrom<T>(),
        transform: (T) -> Any? = { it },
    ): StructuredArgument =
        LogbackStructuredArguments.array(key, *objects.map(transform).toTypedArray())

    /**
     * Abbreviated convenience method for calling [array].
     *
     * @see array
     */
    public inline fun <reified T> a(
        vararg objects: T,
        key: String = derivePluralFieldNameFrom<T>(),
        transform: (T) -> Any? = { it },
    ): StructuredArgument =
        array(key = key, objects = objects, transform = transform)


    /**
     * Variant of [array] that accepts a [Collection] instead of an [Array].
     *
     * @see array
     */
    public inline fun <reified T> objects(
        key: String,
        objects: Collection<T>,
        noinline transform: (T) -> Any? = { it },
    ): StructuredArgument =
        array(key = key, objects = objects.toTypedArray(), transform = transform)

    /**
     * Variant of [array] that accepts a [Collection] instead of an [Array].
     *
     * @see array
     */
    public inline fun <reified T> objects(
        objects: Collection<T>,
        noinline transform: (T) -> Any? = { it },
    ): StructuredArgument =
        array(objects = objects.toTypedArray(), transform = transform)

    /**
     * Abbreviated convenience method for calling [objects].
     *
     * @see objects
     */
    public inline fun <reified T> o(
        fieldName: String,
        objects: Collection<T>,
        noinline transform: (T) -> Any? = { it },
    ): StructuredArgument =
        a(key = fieldName, objects = objects.toTypedArray(), transform = transform)

    /**
     * Abbreviated convenience method for calling [objects].
     *
     * @see objects
     */
    public inline fun <reified T> o(
        objects: Collection<T>,
        noinline transform: (T) -> Any? = { it },
    ): StructuredArgument =
        a(objects = objects.toTypedArray(), transform = transform)


    /**
     * Adds
     * - a `"key":rawJsonValue` for the given [key] and [rawJsonValue] to the JSON event, and
     * - `key=rawJsonValue` to the formatted message.
     *
     * Example:
     * ```kotlin
     * logger.info("msg: {}", keyValue("key", FooBar()))
     *
     * data class FooBar(val foo: String? = null, val bar: String = "baz")
     * ```
     * ```json
     * {"@timestamp":"...","level":"INFO","message":"msg: key=FooBar(foo=null, bar=baz)","key":{"foo":null,"bar":"baz"}}
     * ```
     *
     * @param key the key/field name
     * @param rawJsonValue the raw JSON value
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see RawJsonAppendingMarker
     */
    @Suppress("NOTHING_TO_INLINE")
    public inline fun raw(
        key: String,
        rawJsonValue: String,
    ): StructuredArgument =
        LogbackStructuredArguments.raw(key, rawJsonValue)

    /**
     * Abbreviated convenience method for calling [raw].
     *
     * @see raw
     */
    @Suppress("NOTHING_TO_INLINE")
    public inline fun r(
        key: String,
        rawJsonValue: String,
    ): StructuredArgument =
        LogbackStructuredArguments.r(key, rawJsonValue)


    /**
     * Defers the evaluation of the [StructuredArgument] until actually required.
     *
     * @param structuredArgumentSupplier a supplier for the argument value
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see DeferredStructuredArgument
     */
    public fun defer(structuredArgumentSupplier: () -> StructuredArgument): StructuredArgument =
        DeferredStructuredArgument(structuredArgumentSupplier)


    /**
     * Formats the given [arg] into a string.
     *
     * This method mimics the [SLF4J] behavior:
     * - [Array] objects are formatted as array using [Arrays.toString], and
     * - non [Array] objects using [java.lang.String.valueOf].
     *
     * @param arg the argument to format
     * @return formatted string version of the argument
     *
     * @see org.slf4j.helpers.MessageFormatter#deeplyAppendParameter(StringBuilder, Object, Map)}
     */
    @Suppress("NOTHING_TO_INLINE")
    public inline fun toString(arg: Any?): String =
        LogbackStructuredArguments.toString(arg)
}

/** Derives a field name from the specified [T]. */
public inline fun <reified T> deriveFieldNameFrom(): String =
    T::class.simpleKebabCasedName ?: "object"

/** Derives a field name in plural form from the specified [T]. */
public inline fun <reified T> derivePluralFieldNameFrom(): String =
    deriveFieldNameFrom<T>().pluralize()

/**
 * A [DeferredStructuredArgument] that properly delegates [toString] correctly.
 */
private class DeferredStructuredArgument(
    private val structureArgumentSupplier: Lazy<StructuredArgument>,
) : net.logstash.logback.argument.DeferredStructuredArgument({ structureArgumentSupplier.value }) {
    constructor(structureArgumentSupplier: () -> StructuredArgument) : this(lazy(structureArgumentSupplier))

    override fun toString(): String =
        structureArgumentSupplier.value.toString()
}
