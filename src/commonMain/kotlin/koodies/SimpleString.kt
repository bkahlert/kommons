package koodies

import koodies.regex.RegularExpressions
import koodies.regex.get
import koodies.text.Semantics.FieldDelimiters
import koodies.text.Semantics.Symbols
import kotlin.reflect.KClass


/**
 * Contains an package-free class name like `ClassName`.
 * for strings containing a
 * - simple class name
 * - fully qualified class name
 * - any of the above with a `class ` prefix.
 */
private val String.simpleClassName: String
    get() {
        val classPrefixStripped = toString().substringAfter(" ")
        return RegularExpressions.fullyClassifiedClassNameRegex.matchEntire(classPrefixStripped)?.let {
            it["class"] ?: "❓"
        }?.replace("$", FieldDelimiters.UNIT).takeIf { it != "null" } ?: "object"
    }

/**
 * Contains the (inner) class name like `ClassName` or `ClassName.InnerClassName` of `this` [KClass].
 */
public val KClass<*>.simpleClassName: String get() = toString().simpleClassName

/**
 * Contains the (inner) class name like `ClassName` or `ClassName.InnerClassName` of `this` [KClass].
 */
public fun KClass<*>.simpleName(): String = toString().simpleClassName

private fun String.formatParams(limit: Int = 1): String =
    RegularExpressions.fullyClassifiedClassNameRegex.findAll(this)
        .map { it["class"] ?: "…" }
        .take(limit + 1)
        .mapIndexed { index, text -> if (index == limit) "⋯" else text }
        .joinToString(", ")

public fun Any?.toSimpleString(): String = this?.toString() ?: Symbols.Null

/**
 * Returns a string representation of this lambda with eventually
 * existing classes reduced to their [KClass.simpleName].
 */
public fun <R> Function<R>.toSimpleString(): String =
    RegularExpressions.ignoreArgsLambdaRegex.matchEntire(toString())?.let { result ->
        val returnType = result["returnValueClass"] ?: "❓"
        val params = result["params"]?.formatParams() ?: ""
        result["receiverClass"]
            ?.let { receiver -> "$receiver.($params) -> $returnType" }
            ?: "($params) -> $returnType"
    } ?: toString()

