package com.bkahlert.kommons

import com.bkahlert.kommons.regex.RegularExpressions
import com.bkahlert.kommons.regex.get
import com.bkahlert.kommons.text.Semantics.FieldDelimiters
import com.bkahlert.kommons.text.Semantics.Symbols
import kotlin.reflect.KClass

/**
 * Returns a **simple** string representation of this [KClass].
 *
 * Examples are `ClassName` and `ClassName.InnerClassName`.
 */
public fun KClass<*>.toSimpleString(): String = toString().simpleClassName

/**
 * Returns a **simple** string representation of this object.
 */
public fun Any?.toSimpleString(): String =
    this?.let {
        val string = toString()
        string.simpleClassName
    } ?: Symbols.Null

/**
 * Returns a **simple** string representation of this object's [KClass].
 *
 * Examples are `ClassName` and `ClassName.InnerClassName`.
 */
public fun Any?.toSimpleClassName(): String = this?.let { it::class.toSimpleString() } ?: Symbols.Null

/**
 * Returns a **simple** string representation of this [Function] / lambda.
 *
 * Examples are `() -> Int` and `Receiver.() -> Unit`.
 */
public fun <R> Function<R>.toSimpleString(): String = toString().simpleClassName

private fun String.formatParams(limit: Int = 1): String =
    RegularExpressions.classRegex("format").findAll(this)
        .map { it["formatItype"] ?: "…" }.take(limit + 1)
        .mapIndexed { index, text -> if (index == limit) "⋯" else text }
        .joinToString(", ")

/**
 * Contains a package-free class name like `ClassName` for strings containing a
 * - simple class name
 * - fully qualified class name
 * - any of the above with a `class ` prefix.
 */
private val String.simpleClassName: String
    get() {
        val stringWithoutPossibleClassPrefix = removePrefix("class ")
        return RegularExpressions.lambdaRegex("lambda").matchEntire(stringWithoutPossibleClassPrefix)
            ?.let { lambdaResult ->
                val returnType = lambdaResult["lambdaIreturnItype"]?.trim() ?: Symbols.Unknown
                val params = lambdaResult["lambdaIparams"]?.formatParams() ?: ""
                lambdaResult["lambdaIreceiverItype"]
                    ?.let { receiver -> "$receiver.($params) -> $returnType" }
                    ?: "($params) -> $returnType"
            }
            ?: RegularExpressions.classRegex("class").matchEntire(stringWithoutPossibleClassPrefix)
                ?.let { classNameResult ->
                    classNameResult["classItype"]?.replace("$", FieldDelimiters.UNIT)
                }
            ?: stringWithoutPossibleClassPrefix
    }
