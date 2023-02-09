package com.bkahlert.kommons.uri

import io.ktor.http.Parameters
import io.ktor.http.ParametersBuilder
import io.ktor.http.formUrlEncode as ktorFormUrlEncode

/**
 * Builds a [Parameters] instance with the given [builder] function, and
 * the [ParametersBuilder] initialized with the given [parameters].
 */
public fun Parameters.Companion.build(parameters: Parameters = Empty, builder: ParametersBuilder.() -> Unit): Parameters =
    build { appendAll(parameters); builder() }

/**
 * Appends the specified [name] without any values.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun ParametersBuilder.append(name: String): Unit = appendAll(name, emptyList())

/**
 * Returns the parameters form-URL-encoded, that is, in the form `key=value`.
 *
 * If [keepEmptyValues] is enabled—in contrast to [ktorFormUrlEncode]—a key with
 * no value is encoded as `key` (e.g. https://example.com/?key).
 */
public fun Parameters.formUrlEncode(keepEmptyValues: Boolean = true): String = entries()
    .flatMap { (key, values) ->
        if (!keepEmptyValues || values.isNotEmpty()) {
            values.map { key to it }
        } else {
            listOf(key to null)
        }
    }.ktorFormUrlEncode()
