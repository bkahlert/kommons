package koodies

import koodies.regex.RegularExpressions
import koodies.regex.get
import koodies.text.Semantics.Symbols
import kotlin.reflect.KClass


/**
 * Returns a string representation of this lambda with eventually
 * existing classes reduced to their [KClass.simpleName].
 */
public fun <T : Any> KClass<T>.toSimpleString(): String = simpleName ?: error("$this has no simple name")

public val String.simpleClassName: String
    get() = RegularExpressions.fullyClassifiedClassNameRegex.matchEntire(this)?.let {
        it["class"] ?: "❓"
    } ?: this

private fun String.formatParams(limit: Int = 1): String =
    RegularExpressions.fullyClassifiedClassNameRegex.findAll(this)
        .map { it["class"] ?: "…" }
        .take(limit + 1)
        .mapIndexed { index, text -> if (index == limit) "⋯" else text }
        .joinToString(", ")

public fun Any?.toSimpleString(): String = if (this == null) Symbols.Null else toString()

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

