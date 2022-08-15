package com.bkahlert.kommons.debug

import com.bkahlert.kommons.Platform
import com.bkahlert.kommons.Platform.JVM
import com.bkahlert.kommons.debug.Typing.SimplyTyped
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.isMultiline

/** A function that renders any object. */
public typealias Renderer = (Any?) -> String
/** A function that outputs any string. */
public typealias Printer = (String) -> Unit
/** A function that transforms an instance of type `T` for further inspection. */
public typealias Inspector<T> = (T) -> Any?

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging].
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().trace.breaks.print().calls()
 * ```
 * … does the same as …
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 * … with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * is printed.
 */
@Suppress("DEPRECATION")
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val <T> T.trace: T get(): T = trace()

/**
 * Helper property that supports
 * [print debugging][https://en.wikipedia.org/wiki/Debugging#Print_debugging].
 *
 * **Example**
 * ```kotlin
 * chain().of.endless().trace.breaks.print().calls()
 * ```
 * … does the same as …
 *
 * ```kotlin
 * chain().of.endless().calls()
 * ```
 * … with the only difference that the return value of
 *
 * ```kotlin
 * chain().of.endless()
 * ```
 *
 * is printed.
 */
@Suppress("DEPRECATION")
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public fun <T> T.trace(
    caption: CharSequence? = null,
    highlight: Boolean = Platform.Current == JVM,
    includeCallSite: Boolean = true,
    render: Renderer = { it.render() },
    out: Printer? = null,
    inspect: Inspector<T>? = null
): T {
    val call = if (includeCallSite) StackTrace.get().findByLastKnownCallsOrNull(::inspect, ::trace) else null
    buildString {
        if (call != null) {
            append(".ͭ ")
            append("(${call.file}:${call.line}) ")
        }
        caption?.also {
            append(caption.let { if (highlight) it.highlighted else it })
            append(' ')
        }
        appendWrapped(
            render(this@trace).let { if (highlight) it.highlightedStrongly else it },
            if (highlight) "⟨".highlighted to "⟩".highlighted else "⟨" to "⟩",
        )
        inspect?.also {
            append(" ")
            appendWrapped(
                render(inspect(this@trace)).let { if (highlight) it.highlightedStrongly else it },
                if (highlight) "{".highlighted to "}".highlighted else "{" to "}",
            )
        }
    }.also { out?.invoke(it) ?: println(it) }
    return this
}

/**
 * Special version of [trace] that inspects the structure of
 * each object, no matter if a custom [Any.toString] exists or not.
 */
@Suppress("DEPRECATION")
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val <T> T.inspect: T get(): T = inspect()

/**
 * Special version of [trace] that inspects the structure of
 * each object, no matter if a custom [Any.toString] exists or not.
 */
@Suppress("DEPRECATION")
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public fun <T> T.inspect(
    caption: CharSequence? = null,
    highlight: Boolean = Platform.Current == JVM,
    includeCallSite: Boolean = true,
    typing: Typing = SimplyTyped,
    out: Printer? = null,
    inspect: Inspector<T>? = null
): T = trace(caption, highlight, includeCallSite, { it.render { this.typing = typing; customToString = CustomToString.Ignore } }, out, inspect)

internal fun StringBuilder.appendWrapped(value: String, brackets: Pair<String, String>) {
    val separator = if (value.isMultiline()) LF else ' '
    append(brackets.first)
    append(separator)
    append(value)
    append(separator)
    append(brackets.second)
}
