package com.bkahlert.kommons.logging.logback

import net.logstash.logback.argument.DeferredStructuredArgument
import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN
import net.logstash.logback.argument.StructuredArguments.VALUE_ONLY_MESSAGE_FORMAT_PATTERN
import net.logstash.logback.marker.MapEntriesAppendingMarker
import net.logstash.logback.marker.ObjectAppendingMarker
import net.logstash.logback.marker.ObjectFieldsAppendingMarker
import net.logstash.logback.marker.RawJsonAppendingMarker
import java.util.Arrays
import net.logstash.logback.argument.StructuredArguments as LogbackStructuredArguments

/** Kotlin-specific factory for creating [StructuredArgument] instances. */
public object StructuredArguments {

    /**
     * Adds `"key":"value"` to the JSON event AND
     * `name/value` to the formatted message using the given [messageFormatPattern].
     *
     * @param key the key (field name)
     * @param value the value
     * @param messageFormatPattern the pattern used to concatenate the key and the value
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see ObjectAppendingMarker
     */
    public fun keyValue(
        key: String,
        value: Any?,
        messageFormatPattern: String = DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN
    ): StructuredArgument =
        LogbackStructuredArguments.keyValue(key, value, messageFormatPattern)

    /**
     * Abbreviated convenience method for calling [keyValue].
     *
     * @param key the key (field name)
     * @param value the value
     * @param messageFormatPattern the pattern used to concatenate the key and the value
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see keyValue
     */
    public fun kv(
        key: String,
        value: Any?,
        messageFormatPattern: String = DEFAULT_KEY_VALUE_MESSAGE_FORMAT_PATTERN
    ): StructuredArgument =
        LogbackStructuredArguments.kv(key, value, messageFormatPattern)

    /**
     * Adds `"key":"value"` to the JSON event AND
     * `value` to the formatted message (without the [key]).
     *
     * @param key the key (field name)
     * @param value the value
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see keyValue
     * @see VALUE_ONLY_MESSAGE_FORMAT_PATTERN
     */
    public fun value(
        key: String,
        value: Any?
    ): StructuredArgument =
        LogbackStructuredArguments.value(key, value)

    /**
     * Abbreviated convenience method for calling [value].
     *
     * @param key the key (field name)
     * @param value the value
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see value
     */
    public fun v(
        key: String,
        value: Any?
    ): StructuredArgument =
        LogbackStructuredArguments.v(key, value)

    /**
     * Adds a `"key":"value"` entry for each Map entry to the JSON event AND
     * `map.toString()` to the formatted message.
     *
     * @param map [Map] holding the key/value pairs
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see MapEntriesAppendingMarker
     */
    public fun entries(map: Map<*, *>): StructuredArgument =
        LogbackStructuredArguments.entries(map)

    /**
     * Adds a `"key":"value"` entry for each Map entry to the JSON event AND
     * `map.toString()` to the formatted message.
     *
     * @param entries [Pair] instances holding the key/value pairs
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see MapEntriesAppendingMarker
     */
    public fun entries(vararg entries: Pair<*, *>): StructuredArgument =
        LogbackStructuredArguments.entries(entries.toMap())

    /**
     * Adds a `"key":"value"` entry for each Map entry to the JSON event AND
     * `map.toString()` to the formatted message.
     *
     * @param entries [Map.Entry] instances holding the key/value pairs
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see MapEntriesAppendingMarker
     */
    public fun entries(vararg entries: Map.Entry<*, *>): StructuredArgument =
        LogbackStructuredArguments.entries(entries.associate { (key, value) -> key to value })

    /**
     * Abbreviated convenience method for calling [entries].
     *
     * @param map [Map] holding the key/value pairs
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see entries
     */
    public fun e(map: Map<*, *>): StructuredArgument =
        LogbackStructuredArguments.e(map)

    /**
     * Abbreviated convenience method for calling [entries].
     *
     * @param entries [Pair] instances holding the key/value pairs
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see entries
     */
    public fun e(vararg entries: Pair<*, *>): StructuredArgument =
        LogbackStructuredArguments.e(entries.toMap())

    /**
     * Abbreviated convenience method for calling [entries].
     *
     * @param entries [Map.Entry] holding the key/value pairs
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see entries
     */
    public fun e(vararg entries: Map.Entry<*, *>): StructuredArgument =
        LogbackStructuredArguments.e(entries.associate { (key, value) -> key to value })

    /**
     * Adds a `"key":"value"` entry for each field in the given object to the JSON event AND
     * `object.toString()` to the formatted message.
     *
     * @param obj the object to write fields from
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see ObjectFieldsAppendingMarker
     */
    public fun fields(obj: Any?): StructuredArgument =
        LogbackStructuredArguments.fields(obj)

    /**
     * Abbreviated convenience method for calling [fields].
     *
     * @param object the object to write fields from
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see fields
     */
    public fun f(obj: Any?): StructuredArgument =
        LogbackStructuredArguments.f(obj)

    /**
     * Adds a field to the JSON event whose key is `fieldName` and whose value
     * is a JSON array of objects AND a string version of the array to the formatted message.
     *
     * @param fieldName field name
     * @param objects elements of the array to write under the `fieldName` key
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see ObjectAppendingMarker
     */
    public fun array(
        fieldName: String,
        vararg objects: Any?
    ): StructuredArgument =
        LogbackStructuredArguments.array(fieldName, *objects)

    /**
     * Abbreviated convenience method for calling [.array].
     *
     * @param fieldName field name
     * @param objects elements of the array to write under the `fieldName` key
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see .array
     */
    public fun a(
        fieldName: String,
        vararg objects: Any?
    ): StructuredArgument =
        LogbackStructuredArguments.a(fieldName, *objects)

    /**
     * Adds a field to the JSON event whose key is `fieldName` and whose value
     * is a JSON array of objects AND a string version of the array to the formatted message.
     *
     * @param fieldName field name
     * @param objects elements of the array to write under the `fieldName` key
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see ObjectAppendingMarker
     */
    public fun objects(
        fieldName: String,
        objects: Collection<Any?>
    ): StructuredArgument =
        LogbackStructuredArguments.array(fieldName, *objects.toTypedArray())

    /**
     * Abbreviated convenience method for calling [objects].
     *
     * @param fieldName field name
     * @param objects elements of the array to write under the `fieldName` key
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see objects
     */
    public fun o(
        fieldName: String,
        objects: Collection<Any?>
    ): StructuredArgument =
        LogbackStructuredArguments.a(fieldName, *objects.toTypedArray())

    /**
     * Adds the [rawJsonValue] to the JSON event AND
     * the [rawJsonValue] to the formatted message.
     *
     * @param fieldName field name
     * @param rawJsonValue the raw JSON value
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see RawJsonAppendingMarker
     */
    public fun raw(
        fieldName: String,
        rawJsonValue: String?
    ): StructuredArgument =
        LogbackStructuredArguments.raw(fieldName, rawJsonValue)

    /**
     * Abbreviated convenience method for calling [raw].
     *
     * @param fieldName field name
     * @param rawJsonValue the raw JSON value
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see raw
     */
    public fun r(
        fieldName: String,
        rawJsonValue: String?
    ): StructuredArgument =
        LogbackStructuredArguments.r(fieldName, rawJsonValue)

    /**
     * Defer the evaluation of the argument until actually required.
     *
     * @param structuredArgumentSupplier a supplier for the argument value
     * @return a pre-populated [StructuredArgument] instance
     *
     * @see DeferredStructuredArgument
     */
    public fun defer(structuredArgumentSupplier: () -> StructuredArgument): StructuredArgument =
        LogbackStructuredArguments.defer(structuredArgumentSupplier)

    /**
     * Format the [argument] into a string.
     *
     * This method mimics the SLF4j behavior:
     * array objects are formatted as array using [Arrays.toString],
     * non array object using [String.valueOf].
     *
     * @param arg the argument to format
     * @return formatted string version of the argument
     *
     * @see org.slf4j.helpers.MessageFormatter#deeplyAppendParameter(StringBuilder, Object, Map)}
     */
    public fun toString(argument: Any?): String =
        LogbackStructuredArguments.toString(argument)
}
