package com.bkahlert.kommons.test

import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.LineSeparators.isMultiline
import com.bkahlert.kommons.text.matchesCurly
import com.bkahlert.kommons.text.matchesGlob
import com.bkahlert.kommons.toHexadecimalString
import io.kotest.assertions.print.print
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.neverNullMatcher
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

/**
 * Verifies that this character sequence matches the given
 * glob [pattern], using
 * `*` to match within lines, and
 * `**` to match across lines.
 *
 * **Example**
 * ```kotlin
 * val multilineString = """
 *     foo
 *       .bar()
 *       .baz()
 * """.trimIndent()
 *
 * // ✅ matches thanks to the multiline wildcard **
 * multilineString shouldMatchGlob """
 *     foo
 *       .**()
 * """.trimIndent()
 *
 * // ❌ fails to match since the simple wildcard *
 * // does not match across line breaks
 * multilineString shouldMatchGlob """
 *     foo
 *       .*()
 * """.trimIndent()
 * ```
 *
 * @see shouldNotMatchGlob
 * @see matchGlob
 */
public infix fun <A : CharSequence> A?.shouldMatchGlob(pattern: String): A {
    this should matchGlob(pattern)
    return this!!
}

/**
 * Verifies that this character sequence does not match the given
 * glob [pattern], using
 * `*` to match within lines, and
 * `**` to match across lines.
 *
 * @see shouldMatchGlob
 * @see matchGlob
 */
public infix fun <A : CharSequence> A?.shouldNotMatchGlob(pattern: String): A {
    this shouldNot matchGlob(pattern)
    return this!!
}

/**
 * Verifies that this character sequence matches the given
 * glob [pattern] using
 * the specified [wildcard] (default: `*`) to match within lines, and
 * the specified [multilineWildcard] (default: `**`) to match across lines.
 *
 * The specified [lineSeparators] (default: `\r\n`, `\n`, and `\r`) can be used
 * interchangeably.
 *
 * **Example**
 * ```kotlin
 * val multilineString = """
 *     foo
 *       .bar()
 *       .baz()
 * """.trimIndent()
 *
 * // ✅ matches thanks to the multiline wildcard **
 * multilineString should matchGlob(
 * """
 *     foo
 *       .**()
 * """.trimIndent()
 * )
 *
 * // ❌ fails to match since the simple wildcard *
 * // does not match across line breaks
 * multilineString should matchGlob(
 * """
 *     foo
 *       .*()
 * """.trimIndent()
 * )
 * ```
 *
 * @see shouldMatchGlob
 * @see shouldNotMatchGlob
 */
public fun matchGlob(
    pattern: CharSequence,
    wildcard: String = "*",
    multilineWildcard: String = "**",
    vararg lineSeparators: String = LineSeparators.Common,
): Matcher<CharSequence?> = neverNullMatcher { value ->
    val description = buildString {
        append("match the following glob pattern")
        append(" (wildcard: $wildcard")
        append(", multiline wildcard: $multilineWildcard")
        append(", line separators: ${describeLineSeparators(*lineSeparators)}")
        appendLine("):")
        append(describeString(pattern))
    }
    MatcherResult(
        value.matchesGlob(pattern, wildcard, multilineWildcard, *lineSeparators),
        { describeString(value, "should", description) },
        { describeString(value, "should not", description) },
    )
}


/**
 * Verifies that this character sequence matches the given
 * SLF4J / Logback style [pattern], using
 * `{}` to match within lines, and
 * `{{}}` to match across lines.
 *
 * **Example**
 * ```kotlin
 * val multilineString = """
 *     foo
 *       .bar()
 *       .baz()
 * """.trimIndent()
 *
 * // ✅ matches thanks to the multiline wildcard **
 * multilineString shouldMatchGlob """
 *     foo
 *       .{{}}()
 * """.trimIndent()
 *
 * // ❌ fails to match since the simple wildcard *
 * // does not match across line breaks
 * multilineString shouldMatchGlob """
 *     foo
 *       .{}()
 * """.trimIndent()
 * ```
 *
 * @see shouldNotMatchCurly
 * @see matchCurly
 */
public infix fun <A : CharSequence> A?.shouldMatchCurly(pattern: String): A {
    this should matchCurly(pattern)
    return this!!
}

/**
 * Verifies that this character sequence does not match the given
 * SLF4J / Logback style [pattern], using
 * `{}` to match within lines, and
 * `{{}}` to match across lines.
 *
 * @see shouldMatchCurly
 * @see matchCurly
 */
public infix fun <A : CharSequence> A?.shouldNotMatchCurly(pattern: String): A {
    this shouldNot matchCurly(pattern)
    return this!!
}

/**
 * Verifies that this character sequence matches the given
 * SLF4J / Logback style [pattern], using
 * `{}` to match within lines, and
 * `{{}}` to match across lines.
 *
 * The specified [lineSeparators] (default: `\r\n`, `\n`, and `\r`) can be used
 * interchangeably.
 *
 * **Example**
 * ```kotlin
 * val multilineString = """
 *     foo
 *       .bar()
 *       .baz()
 * """.trimIndent()
 *
 * // ✅ matches thanks to the multiline wildcard **
 * multilineString should matchGlob(
 * """
 *     foo
 *       .{{}}()
 * """.trimIndent()
 * )
 *
 * // ❌ fails to match since the simple wildcard *
 * // does not match across line breaks
 * multilineString should matchGlob(
 * """
 *     foo
 *       .{}()
 * """.trimIndent()
 * )
 * ```
 *
 * @see shouldMatchCurly
 * @see shouldNotMatchCurly
 */
public fun matchCurly(
    pattern: CharSequence,
    vararg lineSeparators: String = LineSeparators.Common,
): Matcher<CharSequence?> = neverNullMatcher { value ->
    val description = buildString {
        append("match the following curly pattern")
        append(" (line separators: ${describeLineSeparators(*lineSeparators)}")
        appendLine("):")
        append(describeString(pattern))
    }
    MatcherResult(
        value.matchesCurly(pattern, *lineSeparators),
        { describeString(value, "should", description) },
        { describeString(value, "should not", description) },
    )
}

private fun describeString(
    value: CharSequence,
    vararg suffix: String,
): String = buildString {
    when (value.isMultiline()) {
        true -> {
            appendLine("\"\"\"")
            appendLine(value)
            append("\"\"\"")
        }

        false -> {
            append(value.print().value)
        }
    }
    if (suffix.isNotEmpty()) appendLine()
    suffix.joinTo(this, " ")
}

private fun describeLineSeparators(
    vararg lineSeparators: String,
): String = lineSeparators.joinToString {
    when (it) {
        LineSeparators.CRLF -> "CRLF (\\r\\n)"
        LineSeparators.LF -> "LF (\\n)"
        LineSeparators.CR -> "CR (\\r)"
        LineSeparators.NEL -> "NEL (\\u0085)"
        LineSeparators.PS -> "PS (\\u2029)"
        LineSeparators.LS -> "LS (\\u2028)"
        else -> "Unknown (0x${it.encodeToByteArray().toHexadecimalString()})"
    }
}
