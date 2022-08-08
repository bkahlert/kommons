package com.bkahlert.kommons.debug

import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.isMultiline

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
public inline val <T> T.traceJs: T get(): T = traceJs()

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
public inline fun <T> T.traceJs(
    caption: CharSequence? = null,
    includeCallSite: Boolean = true,
    render: Renderer = { it.render() },
    noinline out: Printer? = null,
    noinline transform: Inspector<T>? = null,
): T {

    val appendWrapped: (StringBuilder, String, Pair<String, String>) -> Unit = { sb, value, (left, right) ->
        val separator = if (value.isMultiline()) LF else ' '
        sb.append(left)
        sb.append(separator)
        sb.append(value)
        sb.append(separator)
        sb.append(right)
    }

    buildString {
        if (caption != null) {
            append(caption)
            append(" ")
        }
        appendWrapped(
            this,
            render(this@traceJs),
            "⟨" to "⟩",
        )
        transform?.also {
            append(" ")
            appendWrapped(
                this,
                render(transform(this@traceJs)),
                "{" to "}",
            )
        }
    }.also {
        out?.invoke(it) ?: run {
            if (includeCallSite) console.trace(it)
            else console.log(it)
        }
    }
    return this
}
