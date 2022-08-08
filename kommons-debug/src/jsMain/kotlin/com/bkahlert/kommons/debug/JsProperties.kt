package com.bkahlert.kommons.debug

import kotlin.js.Json
import kotlin.js.json
import kotlin.reflect.KProperty0
import kotlin.reflect.KProperty1

private val windowOrNull by lazy { runCatching { kotlinx.browser.window }.getOrNull() }
private val documentOrNull by lazy { runCatching { kotlinx.browser.document }.getOrNull() }

private val ignoredValues = listOfNotNull(windowOrNull, documentOrNull).toTypedArray()

private fun sanitizeKey(key: String): String =
    key.replace("(?<key>.+)_\\d+".toRegex()) { it.groupValues.drop(1).first() }

/**
 * A [Map] of this object's properties with each [Map.Entry.key] representing
 * a property name and [Map.Entry.value] the corresponding value.
 *
 * ***Important:** The property names are heuristically sanitized and can not reliably be used to
 * create copies of the original object.*
 */
public actual val Any.properties: Map<String, Any?>
    get() = entries.associate {
        it[0].unsafeCast<String>() to it[1]
    }.filterNot { (key, _) ->
        key.startsWith("\$")
    }.mapKeys { (key, _) ->
        sanitizeKey(key)
    }

/** Gets the value of this property or the result of [onFailure]. */
public actual fun <V> KProperty0<V>.getOrElse(onFailure: (Throwable) -> V): V =
    kotlin.runCatching { get() }.getOrElse(onFailure)

/** Gets the value of this property or the result of [onFailure]. */
public actual fun <T, V> KProperty1<T, V>.getOrElse(receiver: T, onFailure: (Throwable) -> V): V =
    kotlin.runCatching { get(receiver) }.getOrElse(onFailure)


/** Returns a simple JavaScript object, as [Json], using this key-value pairs as names and values of its properties. */
@Suppress("NOTHING_TO_INLINE")
public inline fun Iterable<Pair<String, Any?>>.toJson(): Json =
    json(*map { (key, value) -> key to value }.toTypedArray())

/** Returns a simple JavaScript object, as [Json], using entries of this map as names and values of its properties. */
@Suppress("NOTHING_TO_INLINE")
public inline fun Map<String, Any?>.toJson(): Json = toList().toJson()

/** Returns an array of [Json] instances. */
public fun Iterable<Any?>.toJsonArray(): Array<Json> = map { it.toJson() }.toTypedArray()

/** Returns an array of [Json] instances. */
public fun Array<out Any?>.toJsonArray(): Array<Json> = map { it.toJson() }.toTypedArray()

/**
 * Returns this object as a string by applying [JSON.stringify] to its [Any.properties] and
 * by filtering a couple of internal properties.
 */
public fun Any?.stringify(
    space: Int = 2,
    predicate: (Any?, String, Any?) -> Boolean = run {
        val ignoredKeyPrefixes = arrayOf("\$", "_", "coroutine", "jQuery")
        val ignoredKeyInfix = arrayOf("\$")
        ({ self, key, value ->
            ignoredKeyPrefixes.none { key.startsWith(it) } &&
                ignoredKeyInfix.none { key.contains(it) } &&
                ignoredValues.none { value === it } &&
                value !== self
        })
    }
): String {
    val o = when (this) {
        null -> null
        is CharSequence -> toString()

        is Boolean, is Char, is Float, is Double,
        is UByte, is UShort, is UInt, is ULong,
        is Byte, is Short, is Int, is Long -> this

        is BooleanArray, is CharArray, is FloatArray, is DoubleArray,
        is UByteArray, is UShortArray, is UIntArray, is ULongArray,
        is ByteArray, is ShortArray, is IntArray, is LongArray -> this

        is Array<*> -> this

        else -> {
            if (this is Collection<*> && this.isPlain) toTypedArray()
            else if (this is Map<*, *> && this.isPlain) map { (key, value) -> sanitizeKey(key.toString()) to value }.toJson()
            else entries.map { sanitizeKey(it[0].unsafeCast<String>()) to it[1] }.toJson()
        }
    }
    return JSON.stringify(
        o = o,
        replacer = { key, value ->
            val self: Any? = js("this").unsafeCast<Any?>()
            if (key.isEmpty()) {
                value
            } else if (predicate(self, key, value)) {
                value.stringify(space, predicate).parseJson()
            } else {
                undefined
            }
        },
        space = space,
    )
}

/** Returns a simple JavaScript object, as [Json], by applying [JSON.parse] to this string. */
public fun String.parseJson(): Json = when (this) {
    "null" -> json()
    else -> JSON.parse(this)
}

/** Returns a simple JavaScript object, as [Json], by applying [JSON.stringify] to this object and [JSON.parse] to its output. */
public fun Any?.toJson(): Json = stringify().parseJson()
