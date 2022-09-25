package com.bkahlert.kommons_deprecated.text

import com.bkahlert.kommons.ansiRemoved
import com.bkahlert.kommons.quoted
import com.bkahlert.kommons.text.LineSeparators
import com.bkahlert.kommons.text.LineSeparators.CRLF
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.mapLines
import com.bkahlert.kommons.text.Unicode
import com.bkahlert.kommons_deprecated.test.AnsiRequiring
import com.bkahlert.kommons_deprecated.test.testEachOld
import com.bkahlert.kommons_deprecated.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons_deprecated.text.AnsiString.Companion.toAnsiString
import com.bkahlert.kommons_deprecated.text.AnsiString.Companion.tokenize
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isBlank
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotBlank
import strikt.assertions.isSameInstanceAs

@AnsiRequiring
class AnsiStringTest {

    companion object {
        val italicCyan = ANSI.Colors.cyan + ANSI.Style.italic
        val ansiString
            get() =
                italicCyan("${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.\nThis one's ${"bold!".ansi.bold}${CRLF}Last one is clean.").tokenize()
        val blankAnsiString get() = "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4m${Unicode.ESCAPE}[24m${Unicode.ESCAPE}[9m${Unicode.ESCAPE}[29m${Unicode.ESCAPE}[23;39m".tokenize()
    }

    @Suppress("SpellCheckingInspection")
    val expectedAnsiFormattedLines = listOf(
        "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m",
        "${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m",
        "${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m",
    )
    val expectedLines = listOf(
        "Important: This line has no ANSI escapes.",
        "This one's bold!",
        "Last one is clean.",
    )

    @Nested
    inner class AnsiStringCache {

        @Test
        fun `should match same`() {
            val text: CharSequence = "abc"
            val ansiString = text.toAnsiString()
            expectThat(ansiString).isSameInstanceAs(text.toAnsiString())
        }

        @Test
        fun `should match equals`() {
            val text: CharSequence = "abc"
            val ansiString = text.toAnsiString()
            expectThat(ansiString).isSameInstanceAs("abc".toAnsiString())
        }

        @Test
        fun `should not match same hash`() {
            val a: CharSequence = object : CharSequence by "2s" {
                override fun hashCode(): Int = 1
            }
            val b: CharSequence = object : CharSequence by "45" {
                override fun hashCode(): Int = 1
            }
            expectThat(a.toAnsiString()).not {
                toStringIsEqualTo(b.toAnsiString())
            }
        }
    }

    @Nested
    inner class Tokenization {
        val string: String
            get() =
                italicCyan("${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.\nThis one's ${"bold!".ansi.bold}${CRLF}Last one is clean.").toString()

        @Test
        fun `should tokenize string`() {
            val tokens = string.tokenize()
            expectThat(tokens.tokens.toList()).containsExactly(
                Token.escapeSequence("${Unicode.ESCAPE}[3;36m"),
                Token.escapeSequence("${Unicode.ESCAPE}[4m"),
                Token.text("Important:"),
                Token.escapeSequence("${Unicode.ESCAPE}[24m"),
                Token.text(" This line has "),
                Token.escapeSequence("${Unicode.ESCAPE}[9m"),
                Token.text("no"),
                Token.escapeSequence("${Unicode.ESCAPE}[29m"),
                Token.text(" ANSI escapes.\nThis one's "),
                Token.escapeSequence("${Unicode.ESCAPE}[1m"),
                Token.text("bold!"),
                Token.escapeSequence("${Unicode.ESCAPE}[22m"),
                Token.text("${CRLF}Last one is clean."),
                Token.escapeSequence("${Unicode.ESCAPE}[23;39m"),
            )
            expectThat(tokens.tokens.sumOf { it.logicalLength }).isEqualTo(78)
            expectThat(string.length).isEqualTo(120)
        }

        @Test
        fun `should create ansi string from first n tokens`() {
            val tokens = string.tokenize()
            val subSequence = tokens.subSequence(0, 26)
            expectThat(subSequence.toString()).isEqualTo(
                "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m"
            )
        }

        @Test
        fun `should create ansi string from subSequence`() {
            val tokens = string.tokenize()
            expectThat(tokens.subSequence(11, 25).toString()).isEqualTo("${Unicode.ESCAPE}[3;36mThis line has ${Unicode.ESCAPE}[23;39m")
        }

        @Test
        fun `should get char at specified position`() {
            val tokens = AnsiString(string.tokenize().tokens)
            expectThat(tokens[26]).isEqualTo('o')
        }

        @Test
        fun `should render string`() {
            val tokens = string.tokenize()
            expectThat(tokens.toString()).isEqualTo(string)
        }

        @Test
        fun `should render string without ansi`() {
            val tokens = string.tokenize()
            val subject: String = tokens.toString(removeAnsi = true)
            val expected: String =
                "Important: This line has no ANSI escapes.$LF" +
                    "This one's bold!$CRLF" +
                    "Last one is clean."
            expectThat(subject).isEqualTo(expected)
        }

        @Test
        fun `should tokenize true color foreground`() {
            val string = "${Unicode.ESCAPE}[38;2;200;10;10m-dark red-${Unicode.ESCAPE}[39m"
            val ansiString = string.tokenize()
            expectThat(ansiString.tokens.toList()).containsExactly(
                Token.escapeSequence("${Unicode.ESCAPE}[38;2;200;10;10m"),
                Token.text("-dark red-"),
                Token.escapeSequence("${Unicode.ESCAPE}[39m"),
            )
            expectThat(ansiString.toString()).isEqualTo(string)
        }

        @Test
        fun `should tokenize true color background`() {
            val string = "${Unicode.ESCAPE}[48;2;200;10;10m-dark red-${Unicode.ESCAPE}[49m"
            val ansiString = string.tokenize()
            expectThat(ansiString.tokens.toList()).containsExactly(
                Token.escapeSequence("${Unicode.ESCAPE}[48;2;200;10;10m"),
                Token.text("-dark red-"),
                Token.escapeSequence("${Unicode.ESCAPE}[49m"),
            )
            expectThat(ansiString.toString()).isEqualTo(string)
        }
    }


    @Nested
    inner class Length {
        @TestFactory
        fun `should return length`(): List<DynamicTest> {
            return listOf(
                41 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m".tokenize(),
                40 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI escapes${Unicode.ESCAPE}[23;39m".tokenize(),
                26 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;29;39m".tokenize(),
                11 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m ${Unicode.ESCAPE}[23;39m".tokenize(),
                10 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[23;24;39m".tokenize(),
                9 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant${Unicode.ESCAPE}[23;24;39m".tokenize(),
                0 to "".tokenize(),
            ).map { (expected, ansiString) ->
                dynamicTest("${ansiString.quoted}.length should be $expected") {
                    expectThat(ansiString.length).isEqualTo(expected)
                }
            }
        }
    }

    @Nested
    inner class Get {
        val ansiString get() = "${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.".tokenize()

        @TestFactory
        fun `should char at position`(): List<DynamicTest> {
            return listOf(
                40 to '.',
                39 to 's',
                26 to 'o',
                10 to ' ',
                9 to ':',
                8 to 't',
                0 to 'I',
            ).map { (i, c) ->
                dynamicTest("pos($i) should be $c") {
                    expectThat(ansiString[i]).isEqualTo(c)
                }
            }
        }

        @Test
        fun `should throw if beyond length`() {
            expectCatching { ansiString[41] }
                .isFailure().isA<IndexOutOfBoundsException>()
        }
    }

    @Nested
    inner class SubSequence {
        val ansiString get() = "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[0m".tokenize()

        @TestFactory
        fun `should product right substring`() = testEachOld(
            41 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m".tokenize(),
            40 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI escapes${Unicode.ESCAPE}[23;39m".tokenize(),
            25 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[23;39m".tokenize(),
            11 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m ${Unicode.ESCAPE}[23;39m".tokenize(),
            10 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[23;39;24m".tokenize(),
            9 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant${Unicode.ESCAPE}[23;39;24m".tokenize(),
            0 to "".tokenize(),
        ) { (length, expected) ->
            group("$expected …") {

                expecting("should have ansiAwareSubSequence(0, $length): \"$expected\"") {
                    ansiString.subSequence(0, length)
                } that { isEqualTo(expected) }

                expecting("should have subString(0, $length): \"$expected\"") {
                    @Suppress("ReplaceSubstringWithTake")
                    ansiString.substring(0, length)
                } that { isEqualTo(expected.toString()) }

                expecting("should take first $length characters: \"$expected\"") {
                    ansiString.take(length)
                } that {
                    isEqualTo(expected)
                }

                expecting("should have length $length") {
                    ansiString.subSequence(0, length).length
                } that { isEqualTo(length) }
            }
        }

        @TestFactory
        fun `should product right non zero start substring`() = testEachOld(
            0 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[23;39m".tokenize(),
            1 to "${Unicode.ESCAPE}[3;36;4mmportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[23;39m".tokenize(),
            9 to "${Unicode.ESCAPE}[3;36;4m:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[23;39m".tokenize(),
            10 to "${Unicode.ESCAPE}[3;36;4m${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[23;39m".tokenize(),
            11 to "${Unicode.ESCAPE}[3;36mThis line has ${Unicode.ESCAPE}[23;39m".tokenize(),
            25 to "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[23;39m".tokenize(),
        ) { (startIndex, expected) ->
            group("$expected …") {

                expecting("should have ansiAwareSubSequence($startIndex, 25): \"$expected\"") {
                    ansiString.subSequence(startIndex, 25)
                } that { isEqualTo(expected) }

                expecting("should have substring($startIndex, 25): \"$expected\"") {
                    ansiString.substring(startIndex, 25)
                } that { isEqualTo(expected.toString()) }

                expecting("should drop first $startIndex characters: \"$expected\"") {
                    ansiString.take(25).drop(startIndex)
                } that { isEqualTo(expected) }

                expecting("should have length ${25 - startIndex}") {
                    ansiString.subSequence(startIndex, 25).length
                } that { isEqualTo(25 - startIndex) }
            }
        }

        @Test
        fun `should subsequence true color foreground`() {
            val string = "${Unicode.ESCAPE}[38;2;200;10;10m-ark red-${Unicode.ESCAPE}[39m"
            val ansiString = string.tokenize()
            val subSequence = ansiString.subSequence(1, 8)
            expectThat(subSequence.toString()).isEqualTo("${Unicode.ESCAPE}[38;2;200;10;10mark red${Unicode.ESCAPE}[39m")
        }

        @Test
        fun `should subsequence true color background`() {
            val string = "${Unicode.ESCAPE}[48;2;200;10;10m-ark red-${Unicode.ESCAPE}[49m"
            val ansiString = string.tokenize()
            val subSequence = ansiString.subSequence(1, 8)
            expectThat(subSequence.toString()).isEqualTo("${Unicode.ESCAPE}[48;2;200;10;10mark red${Unicode.ESCAPE}[49m")
        }

        @Test
        fun `should throw if beyond length`() {
            expectCatching { ansiString.subSequence(0, ansiString.length + 1) }.isFailure().isA<IndexOutOfBoundsException>()
        }
    }

    @Nested
    inner class AnsiRemoved {

        @Suppress("SpellCheckingInspection", "LongLine")
        @TestFactory
        fun `should strip ANSI escape sequences off`() = testEachOld(
            ansiString to ansiString.ansiRemoved,
            "[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m] Listening on ${Unicode.ESCAPE}[0;1;39mudev Control Socket${Unicode.ESCAPE}[0m.".tokenize() to
                "[  OK  ] Listening on udev Control Socket.",
            "Text".tokenize() to "Text",
            "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___".tokenize() to "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___"
        ) { (ansiString, expected) ->
            expecting { ansiString.ansiRemoved } that { isEqualTo(expected) }
        }
    }

    @Nested
    inner class IsBlank {

        @Test
        fun `should return true if blank`() {
            expectThat(blankAnsiString).isBlank()
        }
    }

    @Nested
    inner class IsNotBlank {

        @Test
        fun `should return true if not blank`() {
            expectThat(blankAnsiString).not { isNotBlank() }
        }
    }

    @Nested
    inner class ToString {
        @Test
        fun `should be string-wise comparable`() {
            expectThat(ansiString).toStringIsEqualTo(italicCyan("${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.\nThis one's ${"bold!".ansi.bold}${CRLF}Last one is clean."))
        }

        @Test
        fun `should create string with escape sequences`() {
            expectThat(ansiString.toString(removeAnsi = false)).isEqualTo(italicCyan("${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.\nThis one's ${"bold!".ansi.bold}${CRLF}Last one is clean.").toString())
        }

        @Test
        fun `should create string without escape sequences`() {
            expectThat(ansiString.toString(removeAnsi = true)).isEqualTo("Important: This line has no ANSI escapes.\nThis one's bold!${CRLF}Last one is clean.")
        }

        @Test
        fun `should create string with escape sequences by default`() {
            expectThat(ansiString.toString()).isEqualTo(italicCyan("${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.\nThis one's ${"bold!".ansi.bold}${CRLF}Last one is clean.").toString())
        }
    }

    @Nested
    inner class Equals {
        @Test
        fun `should be equal to itself`() {
            expectThat(ansiString).isEqualTo(ansiString)
        }
    }

    @Nested
    inner class MapLines {
        @Disabled
        @Test
        fun `should split non-ANSI string`() {
            expectThat(ansiString.ansiRemoved.mapLines { it.replace("escapes".toRegex(), "control sequences") }).isEqualTo(
                """
                Important: This line has no ANSI control sequences.
                This one's bold!
                Last one is clean.
            """.trimIndent()
            )
        }

        @Disabled
        @Test
        fun `should split ANSI string`() {
            @Suppress("SpellCheckingInspection")
            expectThat(ansiString.mapLines { it.replace("escapes".toRegex(), "control sequences") }).isEqualTo(
                """
                ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI control sequences.${Unicode.ESCAPE}[23;39m
                ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m
                ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m
            """.trimIndent()
            )
        }

        @Disabled
        @Test
        fun `should split character sequence casted ANSI string`() {
            @Suppress("SpellCheckingInspection")
            expectThat((ansiString as CharSequence).mapLines { it.replace("escapes".toRegex(), "control sequences") }).isEqualTo(
                """
                ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI control sequences.${Unicode.ESCAPE}[23;39m
                ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m
                ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m
            """.trimIndent()
            )
        }

        @Test
        fun `should not throw on errors`() {
            val subject = "${Unicode.ESCAPE}[4;m ← missing second code ${Unicode.ESCAPE}[24m".tokenize().mapLines {
                it.ansi.black
            }.mapLines {
                "$it".replace("second", "second".ansi.magenta.toString())
            }
            val expected =
                "${Unicode.ESCAPE}[30m${Unicode.ESCAPE}[4m ← missing ${Unicode.ESCAPE}[35msecond${Unicode.ESCAPE}[39m code ${Unicode.ESCAPE}[24m${Unicode.ESCAPE}[39m"
            expectThat(subject).isEqualTo(expected)
        }
    }

    @Nested
    inner class LineSequence {

        @Test
        fun `should split non-ANSI string`() {
            expectThat(ansiString.ansiRemoved.lineSequence().toList()).containsExactly(expectedLines)
        }

        @Test
        fun `should split ANSI string`() {
            expectThat(ansiString.lineSequence().toList()).containsExactly(expectedAnsiFormattedLines)
        }

        @Test
        fun `should split character sequences casted ANSI string`() {
            expectThat((ansiString as CharSequence).lineSequence().toList()).containsExactly(expectedAnsiFormattedLines)
        }

        @Test
        fun `should skip errors`() {
            expectThat(
                "${Unicode.ESCAPE}[4;m ← missing second code ${Unicode.ESCAPE}[24m".lineSequence()
                    .toList()
            ).containsExactly("${Unicode.ESCAPE}[4;m ← missing second code ${Unicode.ESCAPE}[24m")
        }
    }

    @Nested
    inner class Lines {
        @Test
        fun `should split non-ANSI string`() {
            expectThat(ansiString.ansiRemoved.lines()).containsExactly(expectedLines)
        }

        @Test
        fun `should join split ANSI string`() {
            expectThat(expectedAnsiFormattedLines.joinToString(LineSeparators.Default) { it }).isEqualTo(
                """
                ${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI escapes.${Unicode.ESCAPE}[23;39m
                ${Unicode.ESCAPE}[3;36mThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[23;39;22m
                ${Unicode.ESCAPE}[3;36mLast one is clean.${Unicode.ESCAPE}[23;39m
            """.trimIndent()
            )
        }

        @Test
        fun `should split ANSI string`() {
            expectThat(ansiString.lines()).containsExactly(expectedAnsiFormattedLines)
        }

        @Test
        fun `should split character sequence casted ANSI string`() {
            expectThat((ansiString as CharSequence).lines()).containsExactly(expectedAnsiFormattedLines)
        }

        @Test
        fun `should skip errors`() {
            expectThat("${Unicode.ESCAPE}[4;m ← missing second code ${Unicode.ESCAPE}[24m".lines()).containsExactly("${Unicode.ESCAPE}[4;m ← missing second code ${Unicode.ESCAPE}[24m")
        }
    }

    @Nested
    inner class ChunkedSequence {

        @Test
        fun `should chunk ANSI string`() {
            expectThat(ansiString.chunkedSequence(26).toList()).containsExactly(
                "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mn${Unicode.ESCAPE}[23;39;29m".toAnsiString(),
                "${Unicode.ESCAPE}[3;36;9mo${Unicode.ESCAPE}[29m ANSI escapes.\nThis one's${Unicode.ESCAPE}[23;39m".toAnsiString(),
                "${Unicode.ESCAPE}[3;36m ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[22m${CRLF}Last one is clean.${Unicode.ESCAPE}[23;39m".toAnsiString(),
            )
        }
    }

    @Nested
    inner class Plus {
        @Test
        fun `should create string from existing plus added string`() {
            expectThat(ansiString.ansiRemoved + "plus").isEqualTo(
                "Important: This line has no ANSI escapes.\nThis one's bold!${CRLF}Last one is clean.plus",
            )
        }

        @Test
        fun `should create ANSI string from existing plus added string`() {
            expectThat(ansiString + "plus").isEqualTo(
                "${Unicode.ESCAPE}[3;36m${Unicode.ESCAPE}[4mImportant:${Unicode.ESCAPE}[24m This line has ${Unicode.ESCAPE}[9mno${Unicode.ESCAPE}[29m ANSI escapes.\nThis one's ${Unicode.ESCAPE}[1mbold!${Unicode.ESCAPE}[22m${CRLF}Last one is clean.${Unicode.ESCAPE}[23;39mplus".toAnsiString(),
            )
        }
    }


    @Nested
    inner class ContainsTest {

        @TestFactory
        fun `NOT ignoring case AND NOT ignoring ANSI`() = listOf(
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[${Unicode.ESCAPE}[0;32m  OK") to true,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[${Unicode.ESCAPE}[0;32m  ok") to false,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[  OK") to false,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = false, ignoreAnsi = false
                    )
                    expectThat(actual).isEqualTo(expected)
                },
                dynamicTest("should be default") {
                    val actual = input.first.contains(
                        input.second
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }

        @TestFactory
        fun `NOT ignoring case AND ignoring ANSI`() = listOf(
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[${Unicode.ESCAPE}[0;32m  OK") to true,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[${Unicode.ESCAPE}[0;32m  ok") to false,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[  OK") to true,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = false, ignoreAnsi = true
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }

        @TestFactory
        fun `ignoring case AND NOT ignoring ANSI`() = listOf(
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[${Unicode.ESCAPE}[0;32m  OK") to true,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[${Unicode.ESCAPE}[0;32m  ok") to true,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[  OK") to false,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = true, ignoreAnsi = false
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }

        @TestFactory
        fun `ignoring case AND ignoring ANSI`() = listOf(
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[${Unicode.ESCAPE}[0;32m  OK") to true,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[${Unicode.ESCAPE}[0;32m  ok") to true,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[  OK") to true,
            ("[${Unicode.ESCAPE}[0;32m  OK  ${Unicode.ESCAPE}[0m]" to "[  ok") to true,
        ).flatMap { (input, expected) ->
            listOf(
                dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = true, ignoreAnsi = true
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }
    }
}


fun <T : AnsiString> Assertion.Builder<out T>.toStringIsEqualTo(other: Any?): Assertion.Builder<out T> =
    assert("have same toString value") { value ->
        val actualString = value.ansiRemoved
        val expectedString = other.let { if (it is AnsiString) it.ansiRemoved else it.toString().ansiRemoved }
        when (actualString == expectedString) {
            true -> pass()
            else -> fail("was $actualString instead of $expectedString.")
        }
    }
