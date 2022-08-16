package com.bkahlert.logging

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
 * will be printed.
 */
@Suppress("GrazieInspection")
@Deprecated("Don't forget to remove after you finished debugging.", replaceWith = ReplaceWith("this"))
public val <T> T.trace: T
    get(): T = apply { println(this.toString().highlight()) }

/**
 * Highlights this character sequence by wrapping it with an [ANSI escape sequence](https://en.wikipedia.org/wiki/ANSI_escape_code)
 * that denotes the color "bright cyan".
 */
private fun CharSequence.highlight(): String = "\u001b[96m$this\u001b[0m"
