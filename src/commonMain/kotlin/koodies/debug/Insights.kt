@file:Suppress("unused")

package koodies.debug

import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.isMultiline
import koodies.text.Semantics.formattedAs

private fun highlight(subject: Any?) = subject.toString().formattedAs.debug
private val selfBrackets = highlight("⟨") to highlight("⟩")
private val transformedBrackets = highlight("{") to highlight("}")

private fun <T> asString(subject: T): String = when (subject) {
    is Array<*> -> asString(subject.toList())
    else -> subject.toString()
}

public fun <T> T.selfString(): String = if (toString().isMultiline) {
    "${selfBrackets.first}$LF${asString(this)}$LF${selfBrackets.second}"
} else {
    "${selfBrackets.first} ${asString(this)} ${selfBrackets.second}"
}

private fun <T> T.transformedString(): String = if (asString(this).isMultiline) {
    "${transformedBrackets.first}$LF${asString(this)}$LF${transformedBrackets.second}"
} else {
    "${transformedBrackets.first} ${asString(this)} ${transformedBrackets.second}"
}

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing `this` to [println] while still returning `this`.
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().trace.calls()
 * ```
 *
 * ... does the same as ...
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 *
 * ... with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * will be printed.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val <T> T.trace: T
    get() : T = apply { println(selfString()) }

/**
 * Helper function that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging]
 * passing `this` and `this` applied to the given [transform] to [println]
 * while still returning `this`.
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().trace { prop }.calls()
 * ```
 *
 * ... does the same as ...
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 *
 * ... with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * at the property `prop` of that value are printed.
 */
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public fun <T> T.trace(transform: (T.() -> Any?)): T =
    apply { println("${selfString()} ${transform().transformedString()}") }
