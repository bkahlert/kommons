package com.bkahlert.kommons.text.styling

import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.math.ceilDiv
import com.bkahlert.kommons.math.floorDiv
import com.bkahlert.kommons.text.Unicode
import com.bkahlert.kommons.text.joinLinesToString
import com.bkahlert.kommons.text.maxLength
import com.bkahlert.kommons.text.repeat

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
public fun <T : CharSequence> Iterable<T>.center(whitespace: Char = Unicode.NBSP, minLength: Int = 0): List<String> {
    val trimmed = map { it.trim() }
    val maxLength = trimmed.maxLength()
    val finalLength = maxLength.coerceAtLeast(minLength)
    return trimmed.map { line ->
        val missing: Int = finalLength - line.ansiRemoved.length
        whitespace.repeat(missing floorDiv 2) + line + whitespace.repeat(missing ceilDiv 2)
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
public fun <T : CharSequence> T.center(whitespace: Char = Unicode.NBSP, minLength: Int = 0): String =
    lines().center(whitespace, minLength).joinLinesToString()
