package koodies.text

import koodies.debug.replaceNonPrintableCharacters
import koodies.regex.group
import koodies.regex.groupValues
import koodies.regex.matchEntire
import koodies.regex.value
import koodies.test.Slow
import koodies.test.expecting
import koodies.test.testEach
import koodies.test.tests
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.LineSeparators.CR
import koodies.text.LineSeparators.CRLF
import koodies.text.LineSeparators.LAST_LINE_REGEX
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.LS
import koodies.text.LineSeparators.NEL
import koodies.text.LineSeparators.PS
import koodies.text.LineSeparators.REGEX
import koodies.text.LineSeparators.autoDetect
import koodies.text.LineSeparators.breakLines
import koodies.text.LineSeparators.firstLineSeparator
import koodies.text.LineSeparators.firstLineSeparatorLength
import koodies.text.LineSeparators.flatMapLines
import koodies.text.LineSeparators.hasLeadingLineSeparator
import koodies.text.LineSeparators.hasTrailingLineSeparator
import koodies.text.LineSeparators.isMultiline
import koodies.text.LineSeparators.leadingLineSeparator
import koodies.text.LineSeparators.lineSequence
import koodies.text.LineSeparators.lines
import koodies.text.LineSeparators.linesOfLength
import koodies.text.LineSeparators.linesOfLengthSequence
import koodies.text.LineSeparators.mapLines
import koodies.text.LineSeparators.prefixLinesWith
import koodies.text.LineSeparators.trailingLineSeparator
import koodies.text.LineSeparators.unify
import koodies.text.LineSeparators.withTrailingLineSeparator
import koodies.text.LineSeparators.withoutLeadingLineSeparator
import koodies.text.LineSeparators.withoutTrailingLineSeparator
import koodies.text.LineSeparators.wrapLines
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.isContainedIn
import strikt.assertions.isEqualTo
import strikt.assertions.isFalse
import strikt.assertions.isNullOrEmpty
import strikt.assertions.isTrue
import koodies.text.Unicode.escape as e

class LineSeparatorsTest {

    @Test
    fun `should have valid default`() {
        expectThat(LineSeparators.DEFAULT).isContainedIn(LineSeparators)
    }

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
        expectThat(LineSeparators.joinToString(" ") { "($it)" }).isEqualTo("($CRLF) ($LF) ($CR) ($NEL) ($PS) ($LS)")
    }

    @Nested
    inner class Dict {

        @Test
        fun `should iterate all line break names in order`() {
            expectThat(LineSeparators.Names.values.joinToString(" ") { "($it)" }).isEqualTo(
                "(CARRIAGE RETURN + LINE FEED) " +
                    "(LINE FEED) " +
                    "(CARRIAGE RETURN) " +
                    "(NEXT LINE) " +
                    "(PARAGRAPH SEPARATOR) " +
                    "(LINE SEPARATOR)")
        }

        @Test
        fun `should iterate all line breaks in order`() {
            expectThat(LineSeparators.Names.keys.joinToString(" ") { "($it)" }).isEqualTo("($CRLF) ($LF) ($CR) ($NEL) ($PS) ($LS)")
        }
    }

    @TestFactory
    fun lineOperations() = testEach(
        "lineSequence" to { input: CharSequence, ignoreTrailSep: Boolean, keepDelimiters: Boolean ->
            input.lineSequence(ignoreTrailingSeparator = ignoreTrailSep, keepDelimiters = keepDelimiters).toList()
        },
        "lines" to { input: CharSequence, ignoreTrailSep: Boolean, keepDelimiters: Boolean ->
            input.lines(ignoreTrailingSeparator = ignoreTrailSep, keepDelimiters = keepDelimiters).toList()
        },
        containerNamePattern = "{}(…)") { (_: String, operation: (CharSequence, Boolean, Boolean) -> Iterable<String>) ->
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

    @Slow @TestFactory
    fun `each line separator`() = LineSeparators.testEach { lineSeparator ->
        asserting { isEqualTo(lineSeparator) }


        expecting { "${lineSeparator}line".leadingLineSeparator } that { isEqualTo(lineSeparator) }
        expecting { "X${lineSeparator}line".leadingLineSeparator } that { isNullOrEmpty() }

        expecting { "${lineSeparator}line".hasLeadingLineSeparator } that { isTrue() }
        expecting { "X${lineSeparator}line".hasLeadingLineSeparator } that { isFalse() }

        expecting { "${lineSeparator}line".withoutLeadingLineSeparator } that { isEqualTo("line") }
        expecting { "X${lineSeparator}line".withoutLeadingLineSeparator } that { isEqualTo("X${lineSeparator}line") }


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
            expecting("should not match line$lineSeparator".replaceNonPrintableCharacters()) { LAST_LINE_REGEX } that {
                not {
                    matchEntire("line$lineSeparator")
                }
            }
            expecting("should not match line$lineSeparator…".replaceNonPrintableCharacters()) { LAST_LINE_REGEX } that {
                not {
                    matchEntire("line$lineSeparator…")
                }
            }
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
            expecting("should not match line$lineSeparator…".replaceNonPrintableCharacters()) { LineSeparators.INTERMEDIARY_LINE_PATTERN } that {
                not { matchEntire("line$lineSeparator…") }
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
            expecting("should not match line$lineSeparator…".replaceNonPrintableCharacters()) { LineSeparators.LINE_PATTERN } that {
                not { matchEntire("line$lineSeparator…") }
            }
        }
    }

    @Nested
    inner class WithTrailingLineSeparator {

        @Test
        fun `should append if missing`() {
            expectThat("line".withTrailingLineSeparator()).isEqualTo("line$LF")
        }

        @Test
        fun `should use auto-detected line separator`() {
            expectThat("line${CR}line".withTrailingLineSeparator()).isEqualTo("line${CR}line$CR")
        }

        @Test
        fun `should use specified line separator`() {
            expectThat("line".withTrailingLineSeparator(lineSeparator = NEL)).isEqualTo("line$NEL")
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

    @Nested
    inner class AutoDetection {

        private fun documentWithLineSeparator(vararg lineSeparators: String): String = lineSeparators
            .mapIndexed { index: Int, lineSeparator: String -> "line $index$lineSeparator" }
            .joinToString("")

        @Test
        fun `should return default if single line`() {
            expecting { autoDetect("single-line") } that { isEqualTo(LineSeparators.DEFAULT) }
        }

        @TestFactory
        fun `should return line separator if only type`() = LineSeparators.testEach { sep ->
            expecting { autoDetect(documentWithLineSeparator(sep, sep, sep)) } that { isEqualTo(sep) }
        }

        @Test
        fun `should return most frequent line separator if multiple types`() {
            expecting { autoDetect(documentWithLineSeparator(CR, NEL, NEL, PS)) } that { isEqualTo(NEL) }
        }

        @TestFactory
        fun `should return line separator with higher precedence on tie`() = tests {
            expecting("leading " + LineSeparators.Names[CRLF]) { autoDetect(documentWithLineSeparator(CRLF, LF, CR, NEL, PS, LS)) } that { isEqualTo(CRLF) }
            expecting("trailing " + LineSeparators.Names[CRLF]) { autoDetect(documentWithLineSeparator(LS, PS, NEL, CR, LF, CRLF)) } that { isEqualTo(CRLF) }

            expecting("leading " + LineSeparators.Names[LF]) { autoDetect(documentWithLineSeparator(LF, CR, NEL, PS, LS)) } that { isEqualTo(LF) }
            expecting("trailing " + LineSeparators.Names[LF]) { autoDetect(documentWithLineSeparator(LS, PS, NEL, CR, LF)) } that { isEqualTo(LF) }

            expecting("leading " + LineSeparators.Names[CR]) { autoDetect(documentWithLineSeparator(CR, NEL, PS, LS)) } that { isEqualTo(CR) }
            expecting("trailing " + LineSeparators.Names[CR]) { autoDetect(documentWithLineSeparator(LS, PS, NEL, CR)) } that { isEqualTo(CR) }

            expecting("leading " + LineSeparators.Names[NEL]) { autoDetect(documentWithLineSeparator(NEL, PS, LS)) } that { isEqualTo(NEL) }
            expecting("trailing " + LineSeparators.Names[NEL]) { autoDetect(documentWithLineSeparator(LS, PS, NEL)) } that { isEqualTo(NEL) }

            expecting("leading " + LineSeparators.Names[PS]) { autoDetect(documentWithLineSeparator(PS, LS)) } that { isEqualTo(PS) }
            expecting("trailing " + LineSeparators.Names[PS]) { autoDetect(documentWithLineSeparator(LS, PS)) } that { isEqualTo(PS) }

            expecting(LineSeparators.Names[LF]) { autoDetect(documentWithLineSeparator(LS)) } that { isEqualTo(LS) }
        }
    }

    @TestFactory
    fun `each unify each line separator`() = LineSeparators.testEach { lineSeparator ->
        expecting { unify("abc${lineSeparator}def") } that { isEqualTo("abc${LF}def") }
    }

    @Nested
    inner class MapLines {

        val transform = { s: CharSequence -> "$s" + s.reversed() }

        @Test
        fun `should transform single line`() {
            expectThat("AB".mapLines(ignoreTrailingSeparator = true, transform)).isEqualTo("ABBA")
        }

        @Test
        fun `should transform multi line`() {
            @Suppress("SpellCheckingInspection")
            expectThat("AB\nBA".mapLines(ignoreTrailingSeparator = true, transform)).isEqualTo("ABBA\nBAAB")
        }

        @Test
        fun `should keep trailing line`() {
            expectThat("AB\nBA$LF".mapLines { "X" }).isEqualTo("X\nX$LF")
        }

        @Test
        fun `should map empty string`() {
            expectThat("".mapLines { "X" }).isEqualTo("X")
        }

        @Test
        fun `should map empty string and keep trailing line`() {
            expectThat(LF.mapLines { "X" }).isEqualTo("X$LF")
        }

        @Test
        fun `should map trailing empty line if not ignored`() {
            expectThat(LF.mapLines(ignoreTrailingSeparator = false) { "X" }).isEqualTo("X\nX")
        }
    }

    @Nested
    inner class FlatMapLinesKtTest {

        val transform = { s: CharSequence -> listOf("$s" + s.reversed(), "${s.reversed()}" + "$s") }

        @Test
        fun `should transform single line`() {
            @Suppress("SpellCheckingInspection")
            expectThat("AB".flatMapLines(ignoreTrailingSeparator = true, transform)).isEqualTo("ABBA\nBAAB")
        }

        @Test
        fun `should transform multi line`() {
            @Suppress("SpellCheckingInspection")
            expectThat("AB\nBA".flatMapLines(ignoreTrailingSeparator = true, transform)).isEqualTo("ABBA\nBAAB\nBAAB\nABBA")
        }

        @Test
        fun `should keep trailing line`() {
            expectThat("AB\nBA$LF".flatMapLines { listOf("X", "Y") }).isEqualTo("X\nY\nX\nY$LF")
        }

        @Test
        fun `should map empty string`() {
            expectThat("".flatMapLines { listOf("X", "Y") }).isEqualTo("X\nY")
        }

        @Test
        fun `should map empty string and keep trailing line`() {
            expectThat(LF.flatMapLines { listOf("X", "Y") }).isEqualTo("X\nY$LF")
        }

        @Test
        fun `should map trailing empty line if not ignored`() {
            expectThat(LF.flatMapLines(ignoreTrailingSeparator = false) { listOf("X", "Y") }).isEqualTo("X\nY\nX\nY")
        }
    }

    @Nested
    inner class PrefixLinesKtTest {

        @Test
        fun `should add prefix to each line`() {
            val prefixedLines = "12345     12345\nsnake    snake".prefixLinesWith("ab ", ignoreTrailingSeparator = true)
            expectThat(prefixedLines).isEqualTo("ab 12345     12345\nab snake    snake")
        }

        @Test
        fun `should do nothing on empty prefix`() {
            val prefixedLines = "12345     12345\nsnake    snake".prefixLinesWith("", ignoreTrailingSeparator = true)
            expectThat(prefixedLines).isEqualTo("12345     12345\nsnake    snake")
        }

        @Test
        fun `should keep trailing new line`() {
            val prefixedLines = "12345     12345\nsnake    snake$LF".prefixLinesWith("ab ", ignoreTrailingSeparator = true)
            expectThat(prefixedLines).isEqualTo("ab 12345     12345\nab snake    snake$LF")
        }

        @Test
        fun `should prefix trailing new line if not ignored`() {
            val prefixedLines = "12345     12345\nsnake    snake$LF".prefixLinesWith("ab ", ignoreTrailingSeparator = false)
            expectThat(prefixedLines).isEqualTo("ab 12345     12345\nab snake    snake\nab ")
        }
    }

    @Nested
    inner class BreakLinesKtTest {

        @Test
        fun `should break do nothing on single short line`() {
            expectThat("short line".breakLines(15)).isEqualTo("short line")
        }

        @Test
        fun `should break do nothing on multiple short lines`() {
            expectThat("short line\nshort line".breakLines(15)).isEqualTo("short line\nshort line")
        }

        @Test
        fun `should break long line`() {
            expectThat("very very long line".breakLines(15)).isEqualTo("very very long \nline")
        }

        @Test
        fun `should break multiple long line`() {
            expectThat("very very long line\nvery very long line".breakLines(15)).isEqualTo("very very long \nline\nvery very long \nline")
        }

        @Test
        fun `should only break long line if mixed with short line`() {
            expectThat("short line\nvery very long line\nshort line".breakLines(15)).isEqualTo("short line\nvery very long \nline\nshort line")
        }

        @Test
        fun `should break long line in as many lines as needed`() {
            expectThat("very very long line".breakLines(5)).isEqualTo("very \nvery \nlong \nline")
        }
    }


    @Nested
    inner class LinesOfLengthKtTest {

        @Nested
        inner class NonAnsiString {
            @TestFactory
            fun `should be split with maximum line length`(): List<DynamicNode> = listOf(
                "sequence" to "${AnsiStringTest.nonAnsiString}$LF".linesOfLengthSequence(26).toList(),
                "list" to "${AnsiStringTest.nonAnsiString}$LF".linesOfLength(26),
            ).map { (method, lines) ->
                DynamicTest.dynamicTest("using $method") {
                    expectThat(lines).containsExactly(
                        "Important: This line has n",
                        "o ANSI escapes.",
                        "This one's bold!",
                        "Last one is clean.",
                        "",
                    )
                }
            }

            @TestFactory
            fun `should be split with maximum line length with trailing line removed`(): List<DynamicNode> = listOf(
                "sequence" to "${AnsiStringTest.nonAnsiString}$LF".linesOfLengthSequence(26, ignoreTrailingSeparator = true).toList(),
                "list" to "${AnsiStringTest.nonAnsiString}$LF".linesOfLength(26, ignoreTrailingSeparator = true),
            ).map { (method, lines) ->
                DynamicTest.dynamicTest("using $method") {
                    expectThat(lines).containsExactly(
                        "Important: This line has n",
                        "o ANSI escapes.",
                        "This one's bold!",
                        "Last one is clean.",
                    )
                }
            }
        }


        @Nested
        inner class AnsiString {
            @TestFactory
            fun `should be split with maximum line length`(): List<DynamicNode> = testEach(
                "sequence" to (AnsiStringTest.ansiString + LF).linesOfLengthSequence(26).toList(),
                "list" to (AnsiStringTest.ansiString + LF).linesOfLength(26),
            ) { (method, lines) ->
                expecting("using $method") { lines } that {
                    @Suppress("SpellCheckingInspection")
                    containsExactly(
                        "$e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m".asAnsiString(),
                        "$e[3;36;9mo$e[29m ANSI escapes.$e[23;39m".asAnsiString(),
                        "$e[3;36mThis one's $e[1mbold!$e[23;39;22m".asAnsiString(),
                        "$e[3;36mLast one is clean.$e[23;39m".asAnsiString(),
                        "".asAnsiString(),
                    )
                }
            }

            @TestFactory
            fun `should be split with maximum line length with trailing line removed`(): List<DynamicNode> = testEach(
                "sequence" to (AnsiStringTest.ansiString + LF).linesOfLengthSequence(26, ignoreTrailingSeparator = true).toList(),
                "list" to (AnsiStringTest.ansiString + LF).linesOfLength(26, ignoreTrailingSeparator = true),
            ) { (method, lines) ->
                expecting("using $method") { lines } that {
                    @Suppress("SpellCheckingInspection")
                    containsExactly(
                        "$e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m".asAnsiString(),
                        "$e[3;36;9mo$e[29m ANSI escapes.$e[23;39m".asAnsiString(),
                        "$e[3;36mThis one's $e[1mbold!$e[23;39;22m".asAnsiString(),
                        "$e[3;36mLast one is clean.$e[23;39m".asAnsiString(),
                    )
                }
            }
        }
    }

    @Nested
    inner class WrapLinesKtTest {

        @Test
        fun `should wrap non-ANSI lines`() {
            expectThat(AnsiStringTest.nonAnsiString.wrapLines(26)).isEqualTo("""
                Important: This line has n
                o ANSI escapes.
                This one's bold!
                Last one is clean.
            """.trimIndent())
        }

        @Test
        fun `should wrap ANSI lines`() {
            @Suppress("SpellCheckingInspection")
            expectThat(AnsiStringTest.ansiString.wrapLines(26)).isEqualTo("""
                $e[3;36m$e[4mImportant:$e[24m This line has $e[9mn$e[23;39;29m
                $e[3;36;9mo$e[29m ANSI escapes.$e[23;39m
                $e[3;36mThis one's $e[1mbold!$e[23;39;22m
                $e[3;36mLast one is clean.$e[23;39m
            """.trimIndent())
        }
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
