package koodies.text

import koodies.collections.to
import koodies.regex.matchEntire
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.CodePoint.Companion.isUsableCodePoint
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNull
import strikt.assertions.isTrue

@Execution(SAME_THREAD)
class CodePointKtTest {

    @TestFactory
    fun `should read code points`() = testEach(
        "a" to 0x61 to ubyteArrayOf(0x61u),
        "¶" to 0xB6 to ubyteArrayOf(0xC2u, 0xB6u),
        "☰" to 0x2630 to ubyteArrayOf(0xE2u, 0x98u, 0xB0u),
        "𝕓" to 0x1D553 to ubyteArrayOf(0xF0u, 0x9Du, 0x95u, 0x93u),
    ) { (char, codePoint, utf8) ->
        expect { utf8.toByteArray() }.that { isEqualTo(char.encodeToByteArray()) }

        expect { char.asCodePoint() }.that { isEqualTo(CodePoint(codePoint)) }

        listOf("a", "¶", "☰", "𝕓").forEach { other ->
            expect { "$other$char".asCodePoint() }.that { isNull() }
            expect { "$char$other".asCodePoint() }.that { isNull() }
        }
    }

    @Test
    fun `should be instantiatable from Byte`() {
        val subject = 0x41.toByte().asCodePoint()
        expectThat(subject).toStringIsEqualTo("A")
    }

    @Test
    fun `should be instantiatable from Int`() {
        val subject = CodePoint(0x41)
        expectThat(subject).toStringIsEqualTo("A")
    }

    @Test
    fun `should be instantiatable from char`() {
        val subject = 'A'.asCodePoint()
        expectThat(subject).toStringIsEqualTo("A")
    }

    @Test
    fun `should be instantiatable from CharSequence`() {
        expectThat(CodePoint("A".subSequence(0, 1))).toStringIsEqualTo("A")
    }

    @Test
    fun `should throw on empty string`() {
        expectCatching { CodePoint("") }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should throw on multi codepoint string`() {
        expectCatching { CodePoint("ab") }.isFailure().isA<IllegalArgumentException>()
    }

    @TestFactory
    fun columns() = testEach(
        CodePoint("\u0006") to -1,
        CodePoint("\u2406") to 1,
        CodePoint("${Unicode.zeroWidthJoiner}") to 0,
        CodePoint(" ") to 1,
        CodePoint("a") to 1,
//        CodePoint("😀") to 2,
//        CodePoint("🤓") to 2,
        CodePoint(Unicode.lineFeed.toString()) to -1,
        CodePoint("►") to 1,
        CodePoint("㍙") to 2,
    ) { (codePoint, expectedColumns) ->
        expect { codePoint.columns }.that { isEqualTo(expectedColumns) }
    }

    @TestFactory
    fun using() = listOf(
        "\u0041" to 1L, // A
        "\uD83E\uDD13" to 1L, // 🤓
        "\u2192\uD808\uDC31\u2190" to 3L, // →𒀱←
        "\uD83D\uDF03\uD83D\uDF02\uD83D\uDF01\uD83D\uDF04" to 4L, // 🜃🜂🜁🜄
    ).flatMap { (string, codePointCount) ->
        listOf(
            dynamicTest("${string.quoted} should validate successfully") {
                val actual = string.isValidCodePoint()
                expectThat(actual).isEqualTo(codePointCount == 1L)
            },

            dynamicTest("${string.quoted} should count $codePointCount code points") {
                val actual = string.asCodePointSequence().count().toLong()
                expectThat(actual).isEqualTo(codePointCount)
            },

            if (codePointCount == 1L)
                dynamicTest("${string.quoted} should be re-creatable using chars") {
                    val actual = CodePoint(string)
                    expectThat(actual).get { CodePoint(String(chars)) }.isEqualTo(actual)
                } else
                dynamicTest("${string.quoted} should throw on CodePoint construction") {
                    expectCatching { CodePoint(string) }
                },
            if (codePointCount == 1L)
                dynamicTest("${string.quoted} should be re-creatable using chars") {
                    val actual = CodePoint(string)
                    expectThat(actual).get { CodePoint(String(chars)) }.isEqualTo(actual)
                }
            else
                dynamicTest("${string.quoted} should throw on CodePoint construction") {
                    expectCatching { CodePoint(string) }
                },
        )
    }

    @Test
    fun `should have name`() {
        expectThat(Unicode[66].unicodeName).isEqualTo("LATIN CAPITAL LETTER B")
    }

    @Test
    fun `should have formatted name`() {
        expectThat(Unicode[66].formattedName).isEqualTo("❲LATIN CAPITAL LETTER B❳")
    }

    @TestFactory
    fun `should format as hex string`() = testEach(
        Unicode.lineFeed to "0A",
        Unicode.zeroWidthSpace to "200B",
        "👽" to "01F47D",
    ) { (codePoint, hex) ->
        expect { CodePoint(codePoint.toString()).toHexadecimalString() }.isEqualTo(hex)
        expect { CodePoint(codePoint.toString()).toRegex() }.matchEntire(codePoint.toString())
    }

    @Nested
    inner class CodePointValidation {

        @Test
        fun `should detekt valid code points`() {
            expectThat('A'.toInt())
                .isEqualTo(65)
                .get { isUsableCodePoint() }.isTrue()
        }

        @Test
        fun `should detekt invalid code points`() {
            expectThat(Character.MAX_CODE_POINT + 1).get { isUsableCodePoint() }.isFalse()
        }
    }

    @Nested
    inner class PropertyValidation {

        @TestFactory
        fun `is whitespace`() = testEach(
            '\u0020'.asCodePoint(),
            '\u00A0'.asCodePoint(),
            '\u1680'.asCodePoint(),
            '\u2000'.asCodePoint(),
            '\u2001'.asCodePoint(),
            '\u2002'.asCodePoint(),
            '\u2003'.asCodePoint(),
            '\u2004'.asCodePoint(),
            '\u2005'.asCodePoint(),
            '\u2006'.asCodePoint(),
            '\u2007'.asCodePoint(),
            '\u2008'.asCodePoint(),
            '\u2009'.asCodePoint(),
            '\u200A'.asCodePoint(),
            '\u202F'.asCodePoint(),
            '\u205F'.asCodePoint(),
            '\u3000'.asCodePoint(),
        ) {
            expect { isWhitespace }.that { isTrue() }
        }

        @TestFactory
        fun `is zero-width whitespace`() = testEach(
            '\u180E'.asCodePoint(),//            MONGOLIAN_VOWEL_SEPARATO
            '\u200B'.asCodePoint(),//            ZERO_WIDTH_SPACE to "ZER
            '\uFEFF'.asCodePoint(),//            ZERO_WIDTH_NO_BREAK_SPAC
        ) {
            expect { isZeroWidthWhitespace }.that { isTrue() }
        }

        @TestFactory
        fun `is not whitespace`() = "Az09Αω𝌀𝍖षि🜃🜂🜁🜄".asCodePointSequence().testEach {
            expect { isWhitespace }.that { isFalse() }
        }


        @TestFactory
        fun `is 0-9`() = "0123456789".asCodePointSequence().testEach {
            expect { is0to9 }.that { isTrue() }
        }

        @TestFactory
        fun `is not 0-9`() = "AzΑωष".asCodePointSequence().testEach {
            expect { is0to9 }.that { isFalse() }
        }


        @TestFactory
        fun `is A-Z_`() = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".asCodePointSequence().testEach {
            expect { isAtoZ }.that { isTrue() }
        }

        @TestFactory
        fun `is not A-Z_`() = "abc123🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
            expect { isAtoZ }.that { isFalse() }
        }


        @TestFactory
        fun `is a-z`() = "abcdefghijklmnopqrstuvwxyz".asCodePointSequence().testEach {
            expect { isatoz }.that { isTrue() }
        }

        @TestFactory
        fun `is not a-z`() = "ABC123🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
            expect { isatoz }.that { isFalse() }
        }

        @Suppress("SpellCheckingInspection")
        @TestFactory
        fun `is A-z `() = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".asCodePointSequence().testEach {
            expect { isAtoz }.that { isTrue() }
        }

        @TestFactory
        fun `is not A-z `() = "123🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
            expect { isAtoz }.that { isFalse() }
        }


        @TestFactory
        fun `is ASCII alphanumeric`() = "Az09".asCodePointSequence().testEach {
            expect { isAsciiAlphanumeric }.that { isTrue() }
        }

        @TestFactory
        fun `is not ASCII alphanumeric`() = "Αωष🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
            expect { isAsciiAlphanumeric }.that { isFalse() }
        }


        @TestFactory
        fun `is alphanumeric`() = "Az09Αωष".asCodePointSequence().testEach {
            expect { isAlphanumeric }.that { isTrue() }
        }

        @TestFactory
        fun `is not alphanumeric`() = "🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
            expect { isAlphanumeric }.that { isFalse() }
        }
//
//        @TestFactory
//        fun `is emoji`() = "😀💂👰🤶".asCodePointSequence().testEach {
//            expect { isEmoji }.that { isTrue() }
//        }
//
//        @TestFactory
//        fun `is no emoji`() = "Az09Αωष🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
//            expect { isEmoji }.that { isFalse() }
//        }
    }

    @Nested
    inner class CodePointSequence {

        @Test
        fun `should count all unicode points`() {
            expectThat("Az09Αω𝌀𝍖षि\n\t\r".codePointCount).isEqualTo(13)
        }

        @Test
        fun `should contain all unicode points`() {
            expectThat("Az09Αω𝌀𝍖षि\n\t\r".asCodePointSequence()).get { map { it.string }.joinToString("") }.isEqualTo("Az09Αω𝌀𝍖षि\n\t\r")
        }
    }

    @Nested
    inner class Plus {

        @Test
        fun `should add element to list`() {
            expectThat("Az09Αω𝌀𝍖" - -1).isEqualTo("B{1:Βϊ𝌁\uD834\uDF57")
        }
    }

    @Nested
    inner class Minus {

        @Test
        fun `should remove element from list`() {
            expectThat("B{1:Βϊ𝌁\uD834\uDF57" - 1).isEqualTo("Az09Αω𝌀𝍖")
        }
    }

    @Nested
    inner class PlusMinus {

        @Test
        fun `should be inverse`() {
            expectThat(("Az09Αω𝌀𝍖" - -1) - 1).isEqualTo("Az09Αω𝌀𝍖")
        }
    }
}
