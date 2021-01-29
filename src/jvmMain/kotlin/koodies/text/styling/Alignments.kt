package koodies.text.styling

import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.Unicode.NBSP
import koodies.text.maxLength
import koodies.text.repeat
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Centers this collection of strings by adding the needed amount of whitespaces from the left (and right)
 * of each line.
 *
 * For example:
 * ```
 * foo
 *   bar baz
 * ```
 * becomes
 * ```
 *   foo
 * bar baz
 * ```
 */
fun <T : CharSequence> Iterable<T>.center(whitespace: Char = NBSP, minLength: Int = 0): List<String> {
    val trimmed = map { it.trim() }
    val maxLength = trimmed.maxLength()
    val finalLength = maxLength.coerceAtLeast(minLength)
    return trimmed.map { line ->
        val missing: Double = (finalLength - line.removeEscapeSequences().length) / 2.0
        whitespace.repeat(floor(missing).toInt()) + line + whitespace.repeat(ceil(missing).toInt())
    }.toList()
}


/**
 * Centers this collection of strings by adding the needed amount of whitespaces from the left (and right)
 * of each line.
 *
 * For example:
 * ```
 * foo
 *   bar baz
 * ```
 * becomes
 * ```
 *   foo
 * bar baz
 * ```
 */
fun <T : CharSequence> T.center(whitespace: Char = NBSP, minLength: Int = 0): String =
    lines().center(whitespace, minLength).joinToString("\n")
