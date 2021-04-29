package koodies.text

import koodies.test.testEach
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.AnsiString.Companion.ansiString
import koodies.text.AnsiString.Companion.asAnsiString
import koodies.text.AnsiString.Companion.getChar
import koodies.text.AnsiString.Companion.render
import koodies.text.AnsiString.Companion.subSequence
import koodies.text.AnsiString.Companion.tokenize
import koodies.text.LineSeparators.CRLF
import koodies.text.LineSeparators.LF
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
import koodies.text.Unicode.escape as ESC

class AnsiStringTest {

    companion object {
        val italicCyan = ANSI.Colors.cyan + ANSI.Style.italic
        val nonAnsiString = "Important: This line has no ANSI escapes.\nThis one's bold!${CRLF}Last one is clean."
        val ansiString =
            AnsiString(italicCyan("${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.\nThis one's ${"bold!".ansi.bold}${CRLF}Last one is clean."))
        val blankAnsiString = AnsiString("$ESC[3;36m$ESC[4m$ESC[24m$ESC[9m$ESC[29m$ESC[23;39m")
    }

    @Suppress("SpellCheckingInspection")
    val expectedAnsiFormattedLines = listOf(
        "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.$ESC[23;39m",
        "$ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m",
        "$ESC[3;36mLast one is clean.$ESC[23;39m",
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
            val ansiString = text.ansiString
            expectThat(ansiString).isSameInstanceAs(text.ansiString)
        }

        @Test
        fun `should match equals`() {
            val text: CharSequence = "abc"
            val ansiString = text.ansiString
            expectThat(ansiString).isSameInstanceAs("abc".ansiString)
        }
    }

    @Nested
    inner class Tokenization {
        val string: String =
            italicCyan("${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.\nThis one's ${"bold!".ansi.bold}${CRLF}Last one is clean.").toString()

        @Test
        fun `should tokenize string`() {
            val tokens = string.tokenize()
            expectThat(tokens.toList()).containsExactly(
                "$ESC[3;36m" to 0,
                "$ESC[4m" to 0,
                "Important:" to 10,
                "$ESC[24m" to 0,
                " This line has " to 15,
                "$ESC[9m" to 0,
                "no" to 2,
                "$ESC[29m" to 0,
                " ANSI escapes.\nThis one's " to 26,
                "$ESC[1m" to 0,
                "bold!" to 5,
                "$ESC[22m" to 0,
                "${CRLF}Last one is clean." to 20,
                "$ESC[23;39m" to 0)
            expectThat(tokens.sumBy { it.second }).isEqualTo(78)
            expectThat(string.length).isEqualTo(120)
        }

        @Test
        fun `should create ansi string from first n tokens`() {
            val tokens = string.tokenize()
            expectThat(tokens.subSequence(0, 26)).isEqualTo(
                "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m")
        }

        @Test
        internal fun `should create ansi string from subSequence`() {
            val tokens = string.tokenize()
            expectThat(tokens.subSequence(11, 25)).isEqualTo("$ESC[3;36mThis line has $ESC[23;39m")
        }

        @Test
        internal fun `should get char at specified position`() {
            val tokens = string.tokenize()
            expectThat(tokens.getChar(26)).isEqualTo('o')
        }

        @Test
        internal fun `should render string`() {
            val tokens = string.tokenize()
            expectThat(tokens.render()).isEqualTo(string)
        }

        @Test
        internal fun `should render string without ansi`() {
            val tokens = string.tokenize()
            val subject: String = tokens.render(ansi = false)
            val expected: String =
                "Important: This line has no ANSI escapes.$LF" +
                    "This one's bold!$CRLF" +
                    "Last one is clean."
            expectThat(subject).isEqualTo(expected)
        }
    }


    @Nested
    inner class Length {
        @TestFactory
        fun `should return length`(): List<DynamicTest> {
            return listOf(
                41 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.$ESC[23;39m"),
                40 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes$ESC[23;39m"),
                26 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;29;39m"),
                11 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[24m $ESC[23;39m"),
                10 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[23;24;39m"),
                9 to AnsiString("$ESC[3;36m$ESC[4mImportant$ESC[23;24;39m"),
                0 to AnsiString(""),
            ).map { (expected, ansiString) ->
                dynamicTest("${ansiString.quoted}.length should be $expected") {
                    expectThat(ansiString.length).isEqualTo(expected)
                }
            }
        }
    }

    @Nested
    inner class Get {
        val ansiString = AnsiString("${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.")

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
        internal fun `should throw if beyond length`() {
            expectCatching { ansiString[41] }
                .isFailure().isA<IndexOutOfBoundsException>()
        }
    }

    @Nested
    inner class SubSequence {
        val ansiString = AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.$ESC[0m")

        @TestFactory
        fun `should product right substring`() = testEach(
            41 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.$ESC[23;39m"),
            40 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes$ESC[23;39m"),
            25 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[23;39m"),
            11 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[24m $ESC[23;39m"),
            10 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[23;39;24m"),
            9 to AnsiString("$ESC[3;36m$ESC[4mImportant$ESC[23;39;24m"),
            0 to AnsiString(""),
        ) { (length, expected) ->
            group("$expected ...") {

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
        fun `should product right non zero start substring`() = testEach(
            0 to AnsiString("$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[23;39m"),
            1 to AnsiString("$ESC[3;36;4mmportant:$ESC[24m This line has $ESC[23;39m"),
            9 to AnsiString("$ESC[3;36;4m:$ESC[24m This line has $ESC[23;39m"),
            10 to AnsiString("$ESC[3;36;4m$ESC[24m This line has $ESC[23;39m"),
            11 to AnsiString("$ESC[3;36mThis line has $ESC[23;39m"),
            25 to AnsiString("$ESC[3;36m$ESC[23;39m"),
        ) { (startIndex, expected) ->
            group("$expected ...") {

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
        internal fun `should throw if beyond length`() {
            expectCatching { ansiString.subSequence(0, ansiString.length + 1) }.isFailure().isA<IndexOutOfBoundsException>()
        }
    }

    @Nested
    inner class Unformatted {

        @Suppress("SpellCheckingInspection", "LongLine")
        @TestFactory
        fun `should strip ANSI escape sequences off`() = listOf(
            ansiString to nonAnsiString,
            AnsiString("[$ESC[0;32m  OK  $ESC[0m] Listening on $ESC[0;1;39mudev Control Socket$ESC[0m.") to
                "[  OK  ] Listening on udev Control Socket.",
            AnsiString("Text") to "Text",
            AnsiString("__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___") to "__̴ı̴̴̡̡̡ ̡͌l̡̡̡ ̡͌l̡*̡̡ ̴̡ı̴̴̡ ̡̡͡|̲̲̲͡͡͡ ̲▫̲͡ ̲̲̲͡͡π̲̲͡͡ ̲̲͡▫̲̲͡͡ ̲|̡̡̡ ̡ ̴̡ı̴̡̡ ̡͌l̡̡̡̡.___"
        ).flatMap { (ansiString, expected) ->
            listOf(
                dynamicTest("\"$ansiString\" should produce \"$expected\"") {
                    expectThat(ansiString.unformatted).isEqualTo(expected)
                }
            )
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
            expectThat(ansiString.toString(removeEscapeSequences = false)).isEqualTo(italicCyan("${"Important:".ansi.underline} This line has ${"no".ansi.strikethrough} ANSI escapes.\nThis one's ${"bold!".ansi.bold}${CRLF}Last one is clean.").toString())
        }

        @Test
        fun `should create string without escape sequences`() {
            expectThat(ansiString.toString(removeEscapeSequences = true)).isEqualTo("Important: This line has no ANSI escapes.\nThis one's bold!${CRLF}Last one is clean.")
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
        @Test
        fun `should split non-ANSI string`() {
            expectThat(ansiString.unformatted.mapLines { it.replace("escapes".toRegex(), "control sequences") }).isEqualTo("""
                Important: This line has no ANSI control sequences.
                This one's bold!
                Last one is clean.
            """.trimIndent())
        }

        @Test
        fun `should split ANSI string`() {
            @Suppress("SpellCheckingInspection")
            expectThat(ansiString.mapLines { it.replace("escapes".toRegex(), "control sequences") }).isEqualTo("""
                $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI control sequences.$ESC[23;39m
                $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m
                $ESC[3;36mLast one is clean.$ESC[23;39m
            """.trimIndent())
        }

        @Test
        fun `should split char sequence casted ANSI string`() {
            @Suppress("SpellCheckingInspection")
            expectThat((ansiString as CharSequence).mapLines { it.replace("escapes".toRegex(), "control sequences") }).isEqualTo("""
                $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI control sequences.$ESC[23;39m
                $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m
                $ESC[3;36mLast one is clean.$ESC[23;39m
            """.trimIndent())
        }

        @Test
        fun `should not throw on errors`() {
            val subject = AnsiString("$ESC[4;m ← missing second code $ESC[24m").mapLines {
                it.ansi.black
            }.mapLines {
                "$it".replace("second", "second".ansi.magenta.toString())
            }
            val expected = "$ESC[30m$ESC[4m ← missing $ESC[35msecond$ESC[39m code $ESC[24m$ESC[39m"
            expectThat(subject).isEqualTo(expected)
        }
    }

    @Nested
    inner class LineSequence {
        @Test
        fun `should split non-ANSI string`() {
            expectThat(ansiString.unformatted.lineSequence().toList()).containsExactly(expectedLines)
        }

        @Test
        fun `should split ANSI string`() {
            expectThat(ansiString.lineSequence().toList()).containsExactly(expectedAnsiFormattedLines)
        }

        @Test
        fun `should split char sequences casted ANSI string`() {
            expectThat((ansiString as CharSequence).lineSequence().toList()).containsExactly(expectedAnsiFormattedLines)
        }

        @Test
        fun `should skip errors`() {
            expectThat("$ESC[4;m ← missing second code $ESC[24m".lineSequence()
                .toList()).containsExactly("$ESC[4;m ← missing second code $ESC[24m")
        }
    }

    @Nested
    inner class Lines {
        @Test
        fun `should split non-ANSI string`() {
            expectThat(ansiString.unformatted.lines()).containsExactly(expectedLines)
        }

        @Test
        fun `should join split ANSI string`() {
            expectThat(expectedAnsiFormattedLines.joinLinesToString { it }).isEqualTo("""
                $ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.$ESC[23;39m
                $ESC[3;36mThis one's $ESC[1mbold!$ESC[23;39;22m
                $ESC[3;36mLast one is clean.$ESC[23;39m
            """.trimIndent())
        }

        @Test
        fun `should split ANSI string`() {
            expectThat(ansiString.lines()).containsExactly(expectedAnsiFormattedLines)
        }

        @Test
        fun `should split char sequence casted ANSI string`() {
            expectThat((ansiString as CharSequence).lines()).containsExactly(expectedAnsiFormattedLines)
        }

        @Test
        fun `should skip errors`() {
            expectThat("$ESC[4;m ← missing second code $ESC[24m".lines()).containsExactly("$ESC[4;m ← missing second code $ESC[24m")
        }
    }

    @Nested
    inner class ChunkedSequence {
        @Test
        fun `should chunk non-ANSI string`() {
            expectThat(nonAnsiString.chunkedSequence(26).toList()).containsExactly(
                "Important: This line has n",
                "o ANSI escapes.\nThis one's",
                " bold!${CRLF}Last one is clean.",
            )
        }

        @Test
        fun `should chunk ANSI string`() {
            expectThat(ansiString.chunkedSequence(26).toList()).containsExactly(
                "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mn$ESC[23;39;29m".asAnsiString(),
                "$ESC[3;36;9mo$ESC[29m ANSI escapes.\nThis one's$ESC[23;39m".asAnsiString(),
                "$ESC[3;36m $ESC[1mbold!$ESC[22m${CRLF}Last one is clean.$ESC[23;39m".asAnsiString(),
            )
        }
    }

    @Nested
    inner class Plus {
        @Test
        fun `should create string from existing plus added string`() {
            expectThat(nonAnsiString + "plus").isEqualTo(
                "Important: This line has no ANSI escapes.\nThis one's bold!${CRLF}Last one is clean.plus",
            )
        }

        @Test
        fun `should create ANSI string from existing plus added string`() {
            expectThat(ansiString + "plus").isEqualTo(
                "$ESC[3;36m$ESC[4mImportant:$ESC[24m This line has $ESC[9mno$ESC[29m ANSI escapes.\nThis one's $ESC[1mbold!$ESC[22m${CRLF}Last one is clean.$ESC[23;39mplus".asAnsiString(),
            )
        }
    }


    @Nested
    inner class ContainsTest {

        @TestFactory
        fun `NOT ignoring case AND NOT ignoring ANSI`() = listOf(
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  ok") to false,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  OK") to false,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                DynamicTest.dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = false, ignoreAnsiFormatting = false
                    )
                    expectThat(actual).isEqualTo(expected)
                },
                DynamicTest.dynamicTest("should be default") {
                    val actual = input.first.contains(
                        input.second
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }

        @TestFactory
        fun `NOT ignoring case AND ignoring ANSI`() = listOf(
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  ok") to false,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                DynamicTest.dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = false, ignoreAnsiFormatting = true
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }

        @TestFactory
        fun `ignoring case AND NOT ignoring ANSI`() = listOf(
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  ok") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  OK") to false,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  ok") to false,
        ).flatMap { (input, expected) ->
            listOf(
                DynamicTest.dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = true, ignoreAnsiFormatting = false
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }

        @TestFactory
        fun `ignoring case AND ignoring ANSI`() = listOf(
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[$ESC[0;32m  ok") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  OK") to true,
            ("[$ESC[0;32m  OK  $ESC[0m]" to "[  ok") to true,
        ).flatMap { (input, expected) ->
            listOf(
                DynamicTest.dynamicTest("$input > $expected") {
                    val actual = input.first.contains(
                        input.second, ignoreCase = true, ignoreAnsiFormatting = true
                    )
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }
    }
}


fun <T : AnsiString> Assertion.Builder<out T>.toStringIsEqualTo(other: Any?): Assertion.Builder<out T> =
    assert("have same toString value") { value ->
        val actualString = value.unformatted
        val expectedString = other.let { if (it is AnsiString) it.unformatted else it.toString().ansiRemoved }
        when (actualString == expectedString) {
            true -> pass()
            else -> fail("was $actualString instead of $expectedString.")
        }
    }
