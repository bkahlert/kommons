package com.bkahlert.kommons.debug


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
    val actualRender = render ?: { it.render() }
    buildString {
        caption?.also {
            append(caption)
            append(' ')
        }
        appendWrapped(
            actualRender(this@trace),
            "⟨" to "⟩",
        )
        inspect?.also {
            append(" ")
            appendWrapped(
                actualRender(inspect(this@trace)),
                "{" to "}",
            )
        }
    }.also { out?.invoke(it) ?: println(it) }
    return this
}
