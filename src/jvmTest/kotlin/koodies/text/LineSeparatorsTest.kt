package koodies.text

import koodies.debug.replaceNonPrintableCharacters
import koodies.regex.group
import koodies.regex.groupValues
import koodies.regex.matchEntire
import koodies.regex.value
import koodies.test.testEach
import koodies.text.LineSeparators.CR
import koodies.text.LineSeparators.CRLF
import koodies.text.LineSeparators.LAST_LINE_REGEX
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.LS
import koodies.text.LineSeparators.NEL
import koodies.text.LineSeparators.PS
import koodies.text.LineSeparators.REGEX
import koodies.text.LineSeparators.firstLineSeparator
import koodies.text.LineSeparators.firstLineSeparatorLength
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.isMultiline
import koodies.text.LineSeparators.lineSequence
import koodies.text.LineSeparators.lines
import koodies.text.LineSeparators.trailingLineSeparator
import koodies.text.LineSeparators.unify
import koodies.text.LineSeparators.withTrailingLineSeparator
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNullOrEmpty
import strikt.assertions.isTrue

class LineSeparatorsTest {

    @Nested
    inner class Multiline {

        @TestFactory
        fun `should detect multi-lines`() = testEach(
            LF,
            "a${CRLF}b",
            "b${CR}a${LF}c",
            "sss$CR",
        ) {
            asserting { isMultiLine() }
            asserting { not { isSingleLine() } }
        }

        @TestFactory
        fun `should detect single-line`() = testEach(
            "",
            "b",
            "bds sd sd sds dac",
        ) {
            asserting { isSingleLine() }
            asserting { not { isMultiLine() } }
        }
    }

    @Test
    fun `should provide MAX_LENGTH`() {
        expectThat(LineSeparators.MAX_LENGTH).isEqualTo(2)
    }

    @Test
    fun `should iterate all line breaks in order`() {
        expectThat(LineSeparators.joinToString(" ") { "($it)" }).isEqualTo("($CRLF) ($LF) ($CR) ($LS) ($PS) ($NEL)")
    }

    @Nested
    inner class Dict {

        @Test
        fun `should iterate all line break names in order`() {
            expectThat(LineSeparators.Names.values.joinToString(" ") { "($it)" }).isEqualTo(
                "(CARRIAGE RETURN + LINE FEED) " +
                    "(LINE FEED) " +
                    "(CARRIAGE RETURN) " +
                    "(LINE SEPARATOR) " +
                    "(PARAGRAPH SEPARATOR) " +
                    "(NEXT LINE)")
        }

        @Test
        fun `should iterate all line breaks in order`() {
            expectThat(LineSeparators.Names.keys.joinToString(" ") { "($it)" }).isEqualTo("($CRLF) ($LF) ($CR) ($LS) ($PS) ($NEL)")
        }
    }

    @TestFactory
    fun lineOperations() = mapOf(
        "lineSequence" to { input: CharSequence, ignoreTrailSep: Boolean, keepDelimiters: Boolean ->
            input.lineSequence(ignoreTrailingSeparator = ignoreTrailSep, keepDelimiters = keepDelimiters).toList()
        },
        "lines" to { input: CharSequence, ignoreTrailSep: Boolean, keepDelimiters: Boolean ->
            input.lines(ignoreTrailingSeparator = ignoreTrailSep, keepDelimiters = keepDelimiters).toList()
        }
    ).testEach("{}(…)") { (_: String, operation: (CharSequence, Boolean, Boolean) -> Iterable<String>) ->
        val multiLineTrail = "1${CR}2${CRLF}3$PS"
        val multiLine = "1${CR}2${CRLF}3"
        val singleLine = "1"
        val empty = ""
        fun lineTest(input: String, ignoreTrailSep: Boolean, keepDelimiters: Boolean, vararg lines: String) =
            expecting("input: " + input.replaceNonPrintableCharacters()) { operation(input, ignoreTrailSep, keepDelimiters) } that { containsExactly(*lines) }

        group("keep trailing line separator + ignore delimiters") {
            lineTest(multiLineTrail, false, false, "1", "2", "3", "")
            lineTest(multiLine, false, false, "1", "2", "3")
            lineTest(singleLine, false, false, "1")
            lineTest(empty, false, false, "")
        }
        group("keep trailing line separator + keep delimiters") {
            lineTest(multiLineTrail, false, true, "1$CR", "2$CRLF", "3$PS", "")
            lineTest(multiLine, false, true, "1$CR", "2$CRLF", "3")
            lineTest(singleLine, false, true, "1")
            lineTest(empty, false, true, "")
        }
        group("ignore trailing line separator + ignore delimiters") {
            lineTest(multiLineTrail, true, false, "1", "2", "3")
            lineTest(multiLine, true, false, "1", "2", "3")
            lineTest(singleLine, true, false, "1")
            lineTest(empty, true, false, lines = emptyArray())
        }
        group("ignore trailing line separator + keep delimiters") {
            lineTest(multiLineTrail, true, true, "1$CR", "2$CRLF", "3$PS")
            lineTest(multiLine, true, true, "1$CR", "2$CRLF", "3")
            lineTest(singleLine, true, true, "1")
            lineTest(empty, true, true, lines = emptyArray())
        }
    }

    @TestFactory
    fun `each line separator`() = LineSeparators.testEach { lineSeparator ->
        asserting { isEqualTo(lineSeparator) }

        expecting { "line$lineSeparator".trailingLineSeparator } that { isEqualTo(lineSeparator) }
        expecting { "line${lineSeparator}X".trailingLineSeparator } that { isNullOrEmpty() }

        expecting { "line$lineSeparator".hasTrailingLineSeparator } that { isTrue() }
        expecting { "line${lineSeparator}X".hasTrailingLineSeparator } that { isFalse() }

        expecting { "line$lineSeparator".withoutTrailingLineSeparator } that { isEqualTo("line") }
        expecting { "line${lineSeparator}X".withoutTrailingLineSeparator } that { isEqualTo("line${lineSeparator}X") }

        group("firstLineSeparatorAndLength") {
            group("should return first line separator if present") {
                listOf(
                    "at the beginning as single line separator" to "${lineSeparator}line",
                    "in the middle as single line separator" to "li${lineSeparator}ne",
                    "at the end as single line separator" to "line$lineSeparator",
                    "at the beginning" to "${lineSeparator}line${LineSeparators.joinToString()}",
                    "in the middle" to "li${lineSeparator}ne${LineSeparators.joinToString()}",
                    "at the end" to "line$lineSeparator${LineSeparators.joinToString()}",
                ).forEach { (case, text) ->
                    expecting(case) { text.firstLineSeparator } that { isEqualTo(lineSeparator) }
                    expecting(case) { text.firstLineSeparatorLength } that { isEqualTo(lineSeparator.length) }
                }
                expecting { "line".firstLineSeparatorLength } that { isEqualTo(0) }
            }
        }

        group(::REGEX.name) {
            expecting("should not match empty string") { REGEX } that { not { matchEntire("") } }
            expecting("should match itself") { REGEX } that { matchEntire(lineSeparator).groupValues.containsExactly(lineSeparator) }
            expecting("should not match line$lineSeparator".replaceNonPrintableCharacters()) { REGEX } that { not { matchEntire("line$lineSeparator") } }
        }

        group(LineSeparators::LAST_LINE_REGEX.name) {
            expecting("should not match empty string") { LAST_LINE_REGEX } that { not { matchEntire("") } }
            expecting("should match line") { LAST_LINE_REGEX } that { matchEntire("line").groupValues.containsExactly("line") }
            expecting("should not match line$lineSeparator".replaceNonPrintableCharacters()) { LAST_LINE_REGEX } that { not { matchEntire("line$lineSeparator") } }
            expecting("should not match line$lineSeparator...".replaceNonPrintableCharacters()) { LAST_LINE_REGEX } that { not { matchEntire("line$lineSeparator...") } }
        }

        group(LineSeparators::INTERMEDIARY_LINE_PATTERN.name) {
            expecting("should not match empty string") { LineSeparators.INTERMEDIARY_LINE_PATTERN } that { not { matchEntire("") } }
            expecting("should match itself") { LineSeparators.INTERMEDIARY_LINE_PATTERN } that {
                matchEntire(lineSeparator).groupValues.all { isEqualTo(lineSeparator) }
            }
            expecting("should not match line") { LineSeparators.INTERMEDIARY_LINE_PATTERN } that { not { matchEntire("line") } }
            expecting("should match line$lineSeparator".replaceNonPrintableCharacters()) { LineSeparators.INTERMEDIARY_LINE_PATTERN } that {
                matchEntire("line$lineSeparator").group("separator").value.isEqualTo(lineSeparator)
            }
            expecting("should not match line$lineSeparator...".replaceNonPrintableCharacters()) { LineSeparators.INTERMEDIARY_LINE_PATTERN } that {
                not { matchEntire("line$lineSeparator...") }
            }
        }

        group(LineSeparators::LINE_PATTERN.name) {
            expecting("should not match empty string") { LineSeparators.LINE_PATTERN } that { not { matchEntire("") } }
            expecting("should match itself") { LineSeparators.LINE_PATTERN } that {
                matchEntire(lineSeparator).groupValues.containsExactly(lineSeparator, lineSeparator)
            }
            expecting("should match line") { LineSeparators.LINE_PATTERN } that { matchEntire("line").groupValues.first().isEqualTo("line") }
            expecting("should match line$lineSeparator".replaceNonPrintableCharacters()) { LineSeparators.LINE_PATTERN } that {
                matchEntire("line$lineSeparator").group("separator").value.isEqualTo(lineSeparator)
            }
            expecting("should not match line$lineSeparator...".replaceNonPrintableCharacters()) { LineSeparators.LINE_PATTERN } that { not { matchEntire("line$lineSeparator...") } }
        }
    }

    @Nested
    inner class WithTrailingLineSeparator {
        @Test
        fun `should append if missing`() {
            expectThat("line".withTrailingLineSeparator()).isEqualTo("line$LF")
        }

        @Test
        fun `should not append if missing but toggle set`() {
            expectThat("line".withTrailingLineSeparator(append = false)).isEqualTo("line")
        }

        @Test
        fun `should not append if already present`() {
            expectThat("line$CR".withTrailingLineSeparator()).isEqualTo("line$CR")
        }
    }

    @TestFactory
    fun `each unify each line separator`() = LineSeparators.testEach { lineSeparator ->
        expecting { unify("abc${lineSeparator}def") } that { isEqualTo("abc${LF}def") }
    }
}

fun <T : CharSequence> Assertion.Builder<T>.isMultiLine() =
    assert("is multi line") {
        if (it.isMultiline) pass()
        else fail()
    }

fun <T : CharSequence> Assertion.Builder<T>.isSingleLine() =
    assert("is single line") {
        if (!it.isMultiline) pass()
        else fail("has ${it.lines().size} lines")
    }

fun <T : CharSequence> Assertion.Builder<T>.lines(
    ignoreTrailingSeparator: Boolean = false,
    keepDelimiters: Boolean = false,
): DescribeableBuilder<List<String>> = get("lines %s") { lines(ignoreTrailingSeparator = ignoreTrailingSeparator, keepDelimiters = keepDelimiters) }
