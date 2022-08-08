package com.bkahlert.kommons.debug

import com.bkahlert.kommons.text.EMPTY
import com.bkahlert.kommons.Parser
import com.bkahlert.kommons.Parser.Companion.parser
import com.bkahlert.kommons.Platform
import com.bkahlert.kommons.text.takeUnlessEmpty
import kotlin.reflect.KFunction

/** Representation of a single element of a [StackTrace] on [Platform.JS]. */
public data class JsStackTraceElement(
    override val receiver: String?,
    override val function: String?,
    override val file: String,
    override val line: Int,
    override val column: Int,
) : StackTraceElement {

    public override val demangledFunction: String? = function?.let { StackTrace.demangleFunction(function) }

    override fun toString(): String = buildString {
        var brackets = false
        if (receiver != null || function != null) {
            brackets = true
            if (receiver != null) {
                append(receiver)
                append('.')
            }
            if (function != null) {
                append(function)
            }
            append(" ")
            if (brackets) append("(")
        }
        append(file)
        append(':')
        append(line)
        append(':')
        append(column)
        if (brackets) append(')')
    }

    public companion object : Parser<JsStackTraceElement> by (parser {
        RenderedStackTraceElementRegex.matchEntire(it)
            ?.destructured
            ?.let { (receiver, function, file, line, column) ->
                JsStackTraceElement(
                    receiver.takeUnlessEmpty(),
                    function.takeUnlessEmpty(),
                    file,
                    line.toInt(),
                    column.toInt()
                )
            }
    })
}

private val RenderedStackTraceElementRegex =
    "^(?:(?:(?<receiver>[^. ]+)\\.)?(?<function>[^ ]*)?\\s+)?\\(?(?<file>[^()]+):(?<line>\\d+):(?<column>\\d+)\\)?\$".toRegex()

private val functionMangleRegex: Regex = "_[a-z\\d]+_k\\$$".toRegex()
private val renderedStackTraceElementUnificationRegex = "^(?<receiverAndFunction>[^@]*)@(?<location>[^@]*)$".toRegex()
private val generatedFunctionRegex = "\\\$.*".toRegex()

/** Returns the specified [renderedStackTraceElement]. */
public fun StackTrace.Companion.unify(renderedStackTraceElement: String): String =
    if (renderedStackTraceElement.contains("@")) {
        renderedStackTraceElementUnificationRegex.replace(renderedStackTraceElement) {
            it.destructured.let { (receiverAndFunction, location) ->
                if (receiverAndFunction.isNotEmpty()) "$receiverAndFunction ($location)" else location
            }
        }
    } else renderedStackTraceElement.removePrefix("    at ")

/** Returns the specified [function] with mangling information removed. */
public actual fun StackTrace.Companion.demangleFunction(function: String): String =
    function.replace(functionMangleRegex, String.EMPTY)

/** Gets the current [StackTrace]. */
public inline fun StackTrace.Companion.get(stackTrace: () -> Sequence<String>): StackTrace = stackTrace()
    .map { unify(it) }
    .dropWhile { it.startsWith("RuntimeException") || it.startsWith("captureStack ") }
    .mapNotNull { JsStackTraceElement.parseOrNull(it) }
    .toList()
    .let { StackTrace(it) }

/** Gets the current [StackTrace]. */
@Suppress("NOTHING_TO_INLINE") // inline to avoid impact on stack trace
public actual inline fun StackTrace.Companion.get(): StackTrace = get {
    try {
        throw RuntimeException()
    } catch (ex: Throwable) {
        ex.stackTraceToString().removeSuffix("\n")
    }.lineSequence()
}

private fun String.jsBeanMethodToKotlinProperty() = takeUnless {
    length >= 6 && substring(0, 5).let { it == "_get_" || it == "_set_" }
} ?: substring(5).replace("__\\d+$".toRegex(), String.EMPTY)

private fun String.simplifyFunction() =
    replace(generatedFunctionRegex, String.EMPTY).jsBeanMethodToKotlinProperty()

/**
 * Finds the [StackTraceElement] that represents the caller
 * invoking the [StackTraceElement] matching a call to the specified [functions].
 */
public actual fun StackTrace.findByLastKnownCallsOrNull(vararg functions: String): StackTraceElement? {
    var skipNull = false
    val simplifiedFunctions = functions.map { StackTrace.demangleFunction(it).simplifyFunction() }

    return findOrNull {
        if (simplifiedFunctions.contains(it.demangledFunction?.simplifyFunction())) {
            skipNull = true
            true
        } else {
            if (skipNull) {
                it.receiver == null && it.function == null
            } else false
        }
    }
}

/**
 * Finds the [StackTraceElement] that represents the caller
 * invoking the [StackTraceElement] matching a call to the specified [functions].
 */
public actual fun StackTrace.findByLastKnownCallsOrNull(vararg functions: KFunction<*>): StackTraceElement? =
    findByLastKnownCallsOrNull(*functions.map { it.name }.toTypedArray())
