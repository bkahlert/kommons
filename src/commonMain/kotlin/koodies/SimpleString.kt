package koodies

import koodies.regex.RegularExpressions
import koodies.regex.get
import koodies.text.Semantics.FieldDelimiters
import koodies.text.Semantics.Symbols
import koodies.text.withoutPrefix
import kotlin.reflect.KClass

/**
 * Returns a **simple** string representation of `this` [KClass].
 *
 * Examples are `ClassName` and `ClassName.InnerClassName`.
 */
public fun KClass<*>.toSimpleString(): String = toString().simpleClassName ?: "object"

/**
 * Returns a **simple** string representation of `this` object.
 */
public fun Any?.toSimpleString(): String =
    this?.let {
        val string = toString()
        string.simpleClassName ?: string
    } ?: Symbols.Null

/**
 * Returns a **simple** string representation of `this` object's [KClass].
 *
 * Examples are `ClassName` and `ClassName.InnerClassName`.
 */
public fun Any?.toSimpleClassName(): String = this?.let { it::class.toSimpleString() } ?: Symbols.Null

/**
 * Returns a **simple** string representation of `this` [Function] / lambda.
 *
 * Examples are `() -> Int` and `Receiver.() -> Unit`.
 */
public fun <R> Function<R>.toSimpleString(): String =
    RegularExpressions.ignoreArgsLambdaRegex.matchEntire(toString())?.let { result ->
        val returnType = result["returnValueClass"] ?: "❓"
        val params = result["params"]?.formatParams() ?: ""
        result["receiverClass"]
            ?.let { receiver -> "$receiver.($params) -> $returnType" }
            ?: "($params) -> $returnType"
    } ?: toString()

private fun String.formatParams(limit: Int = 1): String =
    RegularExpressions.fullyClassifiedClassNameRegex.findAll(this)
        .map { it["class"] ?: "…" }
        .take(limit + 1)
        .mapIndexed { index, text -> if (index == limit) "⋯" else text }
        .joinToString(", ")

/**
 * Contains an package-free class name like `ClassName`.
 * for strings containing a
 * - simple class name
 * - fully qualified class name
 * - any of the above with a `class ` prefix.
 */
private val String.simpleClassName: String?
    get() {
        val classPrefixStripped = toString().withoutPrefix("class ")
        val classNameMatch = RegularExpressions.fullyClassifiedClassNameRegex.matchEntire(classPrefixStripped)
        if (classNameMatch != null) {
            val fqcn = classNameMatch["class"] ?: error("expected named group \"class\" missing.")
            return fqcn.replace("$", FieldDelimiters.UNIT)
        }
        return null
    }
