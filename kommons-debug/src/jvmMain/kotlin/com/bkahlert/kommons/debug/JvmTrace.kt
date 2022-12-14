package com.bkahlert.kommons.debug

import com.bkahlert.kommons.text.LineSeparators

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
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public actual fun <T> T.trace(
    caption: CharSequence?,
    render: Renderer?,
    out: Printer?,
    inspect: Inspector<T>?,
): T {
    val includeCallSite = true
    val highlight = true

    @Suppress("DEPRECATION")
    val call = if (includeCallSite) StackTrace.get().findByLastKnownCallsOrNull(::inspect, ::trace) else null
    val actualRender = render ?: { it.render() }
    buildString {
        if (call != null) {
            append(".ͭ ")
            append("(${call.fileName}:${call.lineNumber}) ")
        }
        caption?.also {
            append(caption.let { if (highlight) it.highlighted else it })
            append(' ')
        }
        appendWrapped(
            actualRender(this@trace).let { if (highlight) it.highlightedStrongly else it },
            if (highlight) "⟨".highlighted to "⟩".highlighted else "⟨" to "⟩",
        )
        inspect?.also {
            append(" ")
            appendWrapped(
                actualRender(inspect(this@trace)).let { if (highlight) it.highlightedStrongly else it },
                if (highlight) "{".highlighted to "}".highlighted else "{" to "}",
            )
        }
    }.also { out?.invoke(it) ?: println(it) }
    return this
}


/** This string cyan colored and emphasized. */
public val CharSequence.highlighted: String
    get() = lineSequence().joinToString(LineSeparators.LF) { "\u001b[1;36m$it\u001B[0m" }

/** This string bright cyan colored. */
public val CharSequence.highlightedStrongly: String
    get() = lineSequence().joinToString(LineSeparators.LF) { "\u001b[96m$it\u001B[0m" }
