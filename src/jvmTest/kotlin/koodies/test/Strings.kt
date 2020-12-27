package koodies.test

import koodies.debug.asEmoji
import koodies.debug.debug
import koodies.functional.compositionOf
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiColors.gray
import koodies.terminal.AnsiColors.magenta
import koodies.text.CodePoint
import koodies.text.Grapheme
import koodies.text.LineSeparators.isMultiline
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.matchesCurlyPattern
import koodies.unit.BinaryPrefix
import koodies.unit.Size
import strikt.api.Assertion
import strikt.api.DescribeableBuilder

infix fun <T> Assertion.Builder<T>.toStringIsEqualTo(expected: String): Assertion.Builder<T> =
    assert("is equal to %s", expected) {
        when (val actual = it.toString()) {
            expected -> pass()
            else -> fail(actual = actual)
        }
    }

infix fun <T> Assertion.Builder<T>.toStringContains(expected: String): Assertion.Builder<T> =
    assert("contains %s", expected) {
        when (val actual = it.toString().contains(expected)) {
            true -> pass()
            else -> fail(actual = actual)
        }
    }

fun <T> Assertion.Builder<T>.toStringContainsAll(vararg expected: String): Assertion.Builder<T> =
    if (expected.size == 1) toStringContains(expected.single())
    else compose("contains %s", expected.joinToString(", ")) {
        expected.forEach { toStringContains(it) }
    }.then { if (allPassed && passedCount > 0) pass() else fail() }


@Deprecated("use toStringIsEqualTo")
fun Assertion.Builder<*>.asString(trim: Boolean = true): DescribeableBuilder<String> {
    return this.get("asString") {
        val string = when (this) {
            is CodePoint -> this.string
            is Grapheme -> this.asString
            is Size -> this.toString<BinaryPrefix>()
            else -> this.toString()
        }
        string.takeUnless { trim } ?: string.trim()
    }
}


fun <T : CharSequence> Assertion.Builder<T>.matchesCurlyPattern(
    curlyPattern: String,
    removeTrailingBreak: Boolean = true,
    removeEscapeSequences: Boolean = true,
    trimmed: Boolean = removeTrailingBreak,
    ignoreTrailingLines: Boolean = false,
): Assertion.Builder<T> = assert(if (curlyPattern.isMultiline) "matches\n$curlyPattern" else "matches $curlyPattern") { actual ->
    val preprocessor = compositionOf(
        removeTrailingBreak to { s: String -> s.withoutTrailingLineSeparator },
        removeEscapeSequences to { s: String -> s.removeEscapeSequences() },
        trimmed to { s: String -> s.trim() },
    )
    var processedActual = preprocessor("$actual")
    var processedPattern = preprocessor(curlyPattern)
    if (ignoreTrailingLines) {
        val lines = processedActual.lines().size.coerceAtMost(processedPattern.lines().size)
        processedActual = processedActual.lines().take(lines).joinToString("\n")
        processedPattern = processedPattern.lines().take(lines).joinToString("\n")
    }
    if (processedActual.matchesCurlyPattern(preprocessor.invoke(curlyPattern))) pass()
    else {
        if (processedActual.lines().size == processedPattern.lines().size) {
            val analysis = processedActual.lines().zip(processedPattern.lines()).joinToString("\n\n") { (actualLine, patternLine) ->
                val lineMatches = actualLine.matchesCurlyPattern(patternLine)
                lineMatches.asEmoji + "   <-\t${actualLine.debug}\nmatch?\t${patternLine.debug}"
            }
            fail(description = "\nbut was: ${if (curlyPattern.isMultiline) "\n$processedActual" else processedActual}\nAnalysis:\n$analysis")
        } else {
            if (processedActual.lines().size > processedPattern.lines().size) {
                fail(description = "\nactual has too many lines:\n${processedActual.highlightTooManyLinesTo(processedPattern)}")
            } else {
                fail(description = "\npattern has too many lines:\n${processedPattern.highlightTooManyLinesTo(processedActual)}")
            }
        }
    }
}

private fun String.highlightTooManyLinesTo(other: String): String {
    val lines = lines()
    val tooManyStart = other.lines().size
    val sb = StringBuilder()
    lines.take(tooManyStart).forEach { sb.append(it.gray() + "\n") }
    lines.drop(tooManyStart).forEach { sb.append(it.magenta() + "\n") }
    @Suppress("ReplaceToStringWithStringTemplate")
    return sb.toString()
}
