package com.bkahlert.kommons.uri

import io.ktor.util.StringValues
import io.ktor.util.StringValuesBuilder
import io.ktor.http.formUrlEncode as ktorFormUrlEncode

/**
 * Builds a [StringValues] instance with the given [builder] function, and
 * the [StringValuesBuilder] initialized with the given [parameters].
 */
public fun StringValues.Companion.build(parameters: StringValues = Empty, builder: StringValuesBuilder.() -> Unit): StringValues =
    build { appendAll(parameters); builder() }

/**
 * Appends the specified [name] without any values.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun StringValuesBuilder.append(name: String): Unit = appendAll(name, emptyList())

/**
 * Returns the parameters form-URL-encoded, that is, in the form `key=value`.
 *
 * If [keepEmptyValues] is enabled—in contrast to [ktorFormUrlEncode]—a key with
 * no value is encoded as `key` (e.g. https://example.com/?key).
 */
public fun StringValues.formUrlEncode(keepEmptyValues: Boolean = true): String = entries()
    .flatMap { (key, values) ->
        if (!keepEmptyValues || values.isNotEmpty()) {
            values.map { key to it }
        } else {
            listOf(key to null)
        }
    }.ktorFormUrlEncode()
