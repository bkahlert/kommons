package com.bkahlert.kommons.text

import com.bkahlert.kommons.collections.too
import com.bkahlert.kommons.regex.groupValues
import com.bkahlert.kommons.regex.matchEntire
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.toStringIsEqualTo
import com.bkahlert.kommons.text.CodePoint.Companion.isUsableCodePoint
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue

class CodePointKtTest {

    @TestFactory
    fun `should read code points`() = testEach(
        "a" to 0x61 too ubyteArrayOf(0x61u),
        "¶" to 0xB6 too ubyteArrayOf(0xC2u, 0xB6u),
        "☰" to 0x2630 too ubyteArrayOf(0xE2u, 0x98u, 0xB0u),
        "𝕓" to 0x1D553 too ubyteArrayOf(0xF0u, 0x9Du, 0x95u, 0x93u),
    ) { (char, codePoint, utf8) ->
        expecting { utf8.toByteArray() } that { isEqualTo(char.encodeToByteArray()) }

        expecting { char.asCodePoint() } that { isEqualTo(CodePoint(codePoint)) }

        listOf("a", "¶", "☰", "𝕓").forEach { other ->
            expecting { "$other$char".asCodePoint() } that { isNull() }
            expecting { "$char$other".asCodePoint() } that { isNull() }
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
        val subject = 'A'.codePoint
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
        CodePoint("\u0006") to 0,
        CodePoint("\u2406") to 2,
        CodePoint("${Unicode.ZERO_WIDTH_JOINER}") to 0,
        CodePoint(" ") to 1,
        CodePoint("a") to 1,
        CodePoint("😀") to 2,
        CodePoint("🤓") to 2,
        CodePoint(Unicode.LINE_FEED.toString()) to 0,
        CodePoint("►") to 1,
        CodePoint("㍙") to 2,
    ) { (codePoint, expectedColumns) ->
        expecting { codePoint.columns } that { isEqualTo(expectedColumns) }
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
        Unicode.LINE_FEED to "0A",
        Unicode.ZERO_WIDTH_SPACE to "200B",
        "👽" to "01F47D",
    ) { (codePoint, hex) ->
        expecting { CodePoint(codePoint.toString()).hexCode } that { isEqualTo(hex) }
        expecting { CodePoint(codePoint.toString()).toLiteralRegex() } that { matchEntire(codePoint.toString()) }
    }

    @Nested
    inner class CodePointValidation {

        @Test
        fun `should detekt valid code points`() {
            expectThat('A'.code)
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
            '\u0020'.codePoint,
            '\u00A0'.codePoint,
            '\u1680'.codePoint,
            '\u2000'.codePoint,
            '\u2001'.codePoint,
            '\u2002'.codePoint,
            '\u2003'.codePoint,
            '\u2004'.codePoint,
            '\u2005'.codePoint,
            '\u2006'.codePoint,
            '\u2007'.codePoint,
            '\u2008'.codePoint,
            '\u2009'.codePoint,
            '\u200A'.codePoint,
            '\u202F'.codePoint,
            '\u205F'.codePoint,
            '\u3000'.codePoint,
        ) {
            expecting { isWhitespace } that { isTrue() }
        }

        @TestFactory
        fun `is zero-width whitespace`() = testEach(
            '\u180E'.codePoint,//            MONGOLIAN_VOWEL_SEPARATO
            '\u200B'.codePoint,//            ZERO_WIDTH_SPACE to "ZER
            '\uFEFF'.codePoint,//            ZERO_WIDTH_NO_BREAK_SPAC
        ) {
            expecting { isZeroWidthWhitespace } that { isTrue() }
        }

        @TestFactory
        fun `is not whitespace`() = "Az09Αω𝌀𝍖षि🜃🜂🜁🜄".asCodePointSequence().testEach {
            expecting { isWhitespace } that { isFalse() }
        }


        @TestFactory
        fun `is 0-9`() = "0123456789".asCodePointSequence().testEach {
            expecting { is0to9 } that { isTrue() }
        }

        @TestFactory
        fun `is not 0-9`() = "AzΑωष".asCodePointSequence().testEach {
            expecting { is0to9 } that { isFalse() }
        }


        @TestFactory
        fun `is A-Z_`() = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".asCodePointSequence().testEach {
            expecting { isAtoZ } that { isTrue() }
        }

        @TestFactory
        fun `is not A-Z_`() = "abc123🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
            expecting { isAtoZ } that { isFalse() }
        }


        @TestFactory
        fun `is a-z`() = "abcdefghijklmnopqrstuvwxyz".asCodePointSequence().testEach {
            expecting { isatoz } that { isTrue() }
        }

        @TestFactory
        fun `is not a-z`() = "ABC123🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
            expecting { isatoz } that { isFalse() }
        }

        @Suppress("SpellCheckingInspection")
        @TestFactory
        fun `is A-z `() = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".asCodePointSequence().testEach {
            expecting { isAtoz } that { isTrue() }
        }

        @TestFactory
        fun `is not A-z `() = "123🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
            expecting { isAtoz } that { isFalse() }
        }


        @TestFactory
        fun `is ASCII alphanumeric`() = "Az09".asCodePointSequence().testEach {
            expecting { isAsciiAlphanumeric } that { isTrue() }
        }

        @TestFactory
        fun `is not ASCII alphanumeric`() = "Αωष🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
            expecting { isAsciiAlphanumeric } that { isFalse() }
        }


        @TestFactory
        fun `is alphanumeric`() = "Az09Αωष".asCodePointSequence().testEach {
            expecting { isAlphanumeric } that { isTrue() }
        }

        @TestFactory
        fun `is not alphanumeric`() = "🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
            expecting { isAlphanumeric } that { isFalse() }
        }
//
//        @TestFactory
//        fun `is emoji`() = "😀💂👰🤶".asCodePointSequence().testEach {
//            expecting { isEmoji } that { isTrue() }
//        }
//
//        @TestFactory
//        fun `is no emoji`() = "Az09Αωष🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().testEach {
//            expecting { isEmoji } that { isFalse() }
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
            expectThat("Az09Αω𝌀𝍖षि\n\t\r".asCodePointSequence()).get { map { it.string }.joinLinesToString("") }.isEqualTo("Az09Αω𝌀𝍖षि\n\t\r")
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

    @Nested
    inner class ToHexadecimalRegex {

        @Test
        fun `should convert each code point to its hexadecimal form`() {
            expectThat("Az09Αω𝌀𝍖षि\n\t\r".toLiteralRegex().pattern)
                .isEqualTo("\\x{41}\\x{7A}\\x{30}\\x{39}\\x{0391}\\x{03C9}\\x{01D300}\\x{01D356}\\x{0937}\\x{093F}\\x{0A}\\x{09}\\x{0D}")
        }

        @Test
        fun `should match same input`() {
            val input = "Az09Αω𝌀𝍖षि\n\t\r"
            val regex = input.toLiteralRegex()
            expectThat(regex.matchEntire(input))
                .isNotNull().groupValues.containsExactly(input)
        }
    }
}
