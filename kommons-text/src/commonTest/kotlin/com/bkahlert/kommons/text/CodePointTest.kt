package com.bkahlert.kommons.text

import com.bkahlert.kommons.EMPTY
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.text.CodePoint.Companion.codePoints
import com.bkahlert.kommons.text.Text.ChunkedText
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowWithMessage
import io.kotest.inspectors.forAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.sequences.shouldBeEmpty
import io.kotest.matchers.sequences.shouldContainExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.Char.Companion.MIN_HIGH_SURROGATE
import kotlin.Char.Companion.MIN_LOW_SURROGATE
import kotlin.test.Test

class CodePointTest {

    @Test fun codepoint_position_iterator() = testAll {
        CodePointBreakIterator("").asSequence().shouldBeEmpty()
        CodePointBreakIterator("a").asSequence().shouldContainExactly(1)
        CodePointBreakIterator("¶").asSequence().shouldContainExactly(1)
        CodePointBreakIterator("☰").asSequence().shouldContainExactly(1)
        CodePointBreakIterator("𝕓").asSequence().shouldContainExactly(2)
        CodePointBreakIterator("a̳o").asSequence().shouldContainExactly(1, 2, 3)

        CodePointBreakIterator("$MIN_HIGH_SURROGATE", throwOnInvalidSequence = false).asSequence().shouldContainExactly(1)
        CodePointBreakIterator("${MIN_HIGH_SURROGATE}a", throwOnInvalidSequence = false).asSequence().shouldContainExactly(1, 2)
        CodePointBreakIterator("${MIN_LOW_SURROGATE}a", throwOnInvalidSequence = false).asSequence().shouldContainExactly(1, 2)

        shouldThrow<CharacterCodingException> { CodePointBreakIterator("$MIN_HIGH_SURROGATE", throwOnInvalidSequence = true).asSequence().toList() }
            .message shouldBe "Input length = 0"
        shouldThrow<CharacterCodingException> { CodePointBreakIterator("${MIN_HIGH_SURROGATE}a", throwOnInvalidSequence = true).asSequence().toList() }
            .message shouldBe "Input length = 0"
        shouldThrow<CharacterCodingException> { CodePointBreakIterator("${MIN_LOW_SURROGATE}a", throwOnInvalidSequence = true).asSequence().toList() }
            .message shouldBe "Input length = 0"
    }

    @Test fun codepoint_iterator() = testAll {
        CodePointIterator("").asSequence().shouldBeEmpty()
        CodePointIterator("a").asSequence().shouldContainExactly(CodePoint(0x61))
        CodePointIterator("¶").asSequence().shouldContainExactly(CodePoint(0xB6))
        CodePointIterator("☰").asSequence().shouldContainExactly(CodePoint(0x2630))
        CodePointIterator("𝕓").asSequence().shouldContainExactly(CodePoint(0x1D553))
        CodePointIterator("a̳o").asSequence().shouldContainExactly(CodePoint('a'.code), CodePoint('̳'.code), CodePoint('o'.code))

        CodePointIterator("$MIN_HIGH_SURROGATE", throwOnInvalidSequence = false).asSequence()
            .shouldContainExactly(MIN_HIGH_SURROGATE.codePoint)
        CodePointIterator("${MIN_HIGH_SURROGATE}a", throwOnInvalidSequence = false).asSequence()
            .shouldContainExactly(MIN_HIGH_SURROGATE.codePoint, CodePoint(0x61))
        CodePointIterator("${MIN_LOW_SURROGATE}a", throwOnInvalidSequence = false).asSequence()
            .shouldContainExactly(MIN_LOW_SURROGATE.codePoint, CodePoint(0x61))

        shouldThrow<CharacterCodingException> { CodePointIterator("$MIN_HIGH_SURROGATE", throwOnInvalidSequence = true).asSequence().toList() }
            .message shouldBe "Input length = 0"
        shouldThrow<CharacterCodingException> { CodePointIterator("${MIN_HIGH_SURROGATE}a", throwOnInvalidSequence = true).asSequence().toList() }
            .message shouldBe "Input length = 0"
        shouldThrow<CharacterCodingException> { CodePointIterator("${MIN_LOW_SURROGATE}a", throwOnInvalidSequence = true).asSequence().toList() }
            .message shouldBe "Input length = 0"
    }

    @Test fun as_code_point_sequences() = testAll {
        "".asCodePointSequence().shouldBeEmpty()
        "a".asCodePointSequence().shouldContainExactly(CodePoint(0x61))
        "¶".asCodePointSequence().shouldContainExactly(CodePoint(0xB6))
        "☰".asCodePointSequence().shouldContainExactly(CodePoint(0x2630))
        "𝕓".asCodePointSequence().shouldContainExactly(CodePoint(0x1D553))
        "a̳o".asCodePointSequence().shouldContainExactly(CodePoint('a'.code), CodePoint('̳'.code), CodePoint('o'.code))
        "a̳o".asCodePointSequence(startIndex = 1).shouldContainExactly(CodePoint('̳'.code), CodePoint('o'.code))
        "a̳o".asCodePointSequence(startIndex = 2).shouldContainExactly(CodePoint('o'.code))
        "a̳o".asCodePointSequence(startIndex = 3).shouldBeEmpty()
        "a̳o".asCodePointSequence(endIndex = 1).shouldContainExactly(CodePoint('a'.code))
        "a̳o".asCodePointSequence(endIndex = 2).shouldContainExactly(CodePoint('a'.code), CodePoint('̳'.code))
        "a̳o".asCodePointSequence(endIndex = 3).shouldContainExactly(CodePoint('a'.code), CodePoint('̳'.code), CodePoint('o'.code))

        shouldThrowWithMessage<IndexOutOfBoundsException>("begin -1, end 0, length 0") { "".asCodePointSequence(startIndex = -1).toList() }
        shouldThrowWithMessage<IndexOutOfBoundsException>("begin 0, end -1, length 0") { "".asCodePointSequence(endIndex = -1).toList() }
        "$MIN_HIGH_SURROGATE".asCodePointSequence(throwOnInvalidSequence = false).shouldContainExactly(MIN_HIGH_SURROGATE.codePoint)
        "${MIN_HIGH_SURROGATE}a".asCodePointSequence(throwOnInvalidSequence = false).shouldContainExactly(MIN_HIGH_SURROGATE.codePoint, CodePoint(0x61))
        "${MIN_LOW_SURROGATE}a".asCodePointSequence(throwOnInvalidSequence = false).shouldContainExactly(MIN_LOW_SURROGATE.codePoint, CodePoint(0x61))
        shouldThrow<CharacterCodingException> { "$MIN_HIGH_SURROGATE".asCodePointSequence(throwOnInvalidSequence = true).toList() }
            .message shouldBe "Input length = 0"
        shouldThrow<CharacterCodingException> { "${MIN_HIGH_SURROGATE}a".asCodePointSequence(throwOnInvalidSequence = true).toList() }
            .message shouldBe "Input length = 0"
        shouldThrow<CharacterCodingException> { "${MIN_LOW_SURROGATE}a".asCodePointSequence(throwOnInvalidSequence = true).toList() }
            .message shouldBe "Input length = 0"
    }

    @Test fun to_code_point_list() = testAll {
        "".toCodePointList().shouldBeEmpty()
        "a".toCodePointList().shouldContainExactly(CodePoint(0x61))
        "¶".toCodePointList().shouldContainExactly(CodePoint(0xB6))
        "☰".toCodePointList().shouldContainExactly(CodePoint(0x2630))
        "𝕓".toCodePointList().shouldContainExactly(CodePoint(0x1D553))
        "a̳o".toCodePointList().shouldContainExactly(CodePoint('a'.code), CodePoint('̳'.code), CodePoint('o'.code))
        "a̳o".toCodePointList(startIndex = 1).shouldContainExactly(CodePoint('̳'.code), CodePoint('o'.code))
        "a̳o".toCodePointList(startIndex = 2).shouldContainExactly(CodePoint('o'.code))
        "a̳o".toCodePointList(startIndex = 3).shouldBeEmpty()
        "a̳o".toCodePointList(endIndex = 1).shouldContainExactly(CodePoint('a'.code))
        "a̳o".toCodePointList(endIndex = 2).shouldContainExactly(CodePoint('a'.code), CodePoint('̳'.code))
        "a̳o".toCodePointList(endIndex = 3).shouldContainExactly(CodePoint('a'.code), CodePoint('̳'.code), CodePoint('o'.code))

        shouldThrowWithMessage<IndexOutOfBoundsException>("begin -1, end 0, length 0") { "".toCodePointList(startIndex = -1) }
        shouldThrowWithMessage<IndexOutOfBoundsException>("begin 0, end -1, length 0") { "".toCodePointList(endIndex = -1) }
        "$MIN_HIGH_SURROGATE".toCodePointList(throwOnInvalidSequence = false).shouldContainExactly(MIN_HIGH_SURROGATE.codePoint)
        "${MIN_HIGH_SURROGATE}a".toCodePointList(throwOnInvalidSequence = false).shouldContainExactly(MIN_HIGH_SURROGATE.codePoint, CodePoint(0x61))
        "${MIN_LOW_SURROGATE}a".toCodePointList(throwOnInvalidSequence = false).shouldContainExactly(MIN_LOW_SURROGATE.codePoint, CodePoint(0x61))
        shouldThrow<CharacterCodingException> { "$MIN_HIGH_SURROGATE".toCodePointList(throwOnInvalidSequence = true) }
            .message shouldBe "Input length = 0"
        shouldThrow<CharacterCodingException> { "${MIN_HIGH_SURROGATE}a".toCodePointList(throwOnInvalidSequence = true) }
            .message shouldBe "Input length = 0"
        shouldThrow<CharacterCodingException> { "${MIN_LOW_SURROGATE}a".toCodePointList(throwOnInvalidSequence = true) }
            .message shouldBe "Input length = 0"
    }

    @Test fun code_point_count() = testAll {
        "".codePointCount() shouldBe 0
        "a".codePointCount() shouldBe 1
        "¶".codePointCount() shouldBe 1
        "☰".codePointCount() shouldBe 1
        "𝕓".codePointCount() shouldBe 1
        "a̳o".codePointCount() shouldBe 3
        "a̳o".codePointCount(startIndex = 1) shouldBe 2
        "a̳o".codePointCount(startIndex = 2) shouldBe 1
        "a̳o".codePointCount(startIndex = 3) shouldBe 0
        "a̳o".codePointCount(endIndex = 1) shouldBe 1
        "a̳o".codePointCount(endIndex = 2) shouldBe 2
        "a̳o".codePointCount(endIndex = 3) shouldBe 3

        shouldThrowWithMessage<IndexOutOfBoundsException>("begin -1, end 0, length 0") { "".codePointCount(startIndex = -1) }
        shouldThrowWithMessage<IndexOutOfBoundsException>("begin 0, end -1, length 0") { "".codePointCount(endIndex = -1) }
        "$MIN_HIGH_SURROGATE".codePointCount(throwOnInvalidSequence = false) shouldBe 1
        "${MIN_HIGH_SURROGATE}a".codePointCount(throwOnInvalidSequence = false) shouldBe 2
        "${MIN_LOW_SURROGATE}a".codePointCount(throwOnInvalidSequence = false) shouldBe 2
        shouldThrow<CharacterCodingException> { "$MIN_HIGH_SURROGATE".codePointCount(throwOnInvalidSequence = true) }
            .message shouldBe "Input length = 0"
        shouldThrow<CharacterCodingException> { "${MIN_HIGH_SURROGATE}a".codePointCount(throwOnInvalidSequence = true) }
            .message shouldBe "Input length = 0"
        shouldThrow<CharacterCodingException> { "${MIN_LOW_SURROGATE}a".codePointCount(throwOnInvalidSequence = true) }
            .message shouldBe "Input length = 0"

        "🫠🇩🇪👨🏾‍🦱👩‍👩‍👦‍👦".codePointCount() shouldBe 14
    }

    @Test fun instantiate() = testAll {
        shouldThrow<IndexOutOfBoundsException> { CodePoint(CodePoint.MIN_INDEX - 1) }
        shouldNotThrowAny { CodePoint(CodePoint.MIN_INDEX) }
        shouldNotThrowAny { CodePoint(0x61) }
        shouldNotThrowAny { CodePoint(CodePoint.MAX_INDEX) }
        shouldThrow<IndexOutOfBoundsException> { CodePoint(CodePoint.MAX_INDEX + 1) }
    }

    @Test fun plus() = testAll {
        CodePoint(0x61) + 0 shouldBe CodePoint(0x61)
        CodePoint(0x61) + 1 shouldBe CodePoint(0x62)
        CodePoint(0x61) + 2 shouldBe CodePoint(0x63)
        shouldThrow<IndexOutOfBoundsException> { CodePoint(0x61) + CodePoint.MAX_INDEX + 1 }
    }

    @Test fun minus() = testAll {
        shouldThrow<IndexOutOfBoundsException> { CodePoint(0x61) - 0x62 }
        CodePoint(0x61) - 0 shouldBe CodePoint(0x61)
        CodePoint(0x61) - 1 shouldBe CodePoint(0x60)
        CodePoint(0x61) - 2 shouldBe CodePoint(0x5F)
    }

    @Test fun inc() = testAll {
        var codePoint = CodePoint(0x61)
        ++codePoint shouldBe CodePoint(0x61) + 1
    }

    @Test fun dec() = testAll {
        var codePoint = CodePoint(0x61)
        --codePoint shouldBe CodePoint(0x61) - 1
    }

    @Test fun range_to() = testAll {
        CodePoint(0x61)..CodePoint(0xB6) shouldBe CodePointRange(CodePoint(0x61), CodePoint(0xB6))
    }

    @Test fun equality() = testAll {
        CodePoint(0x61) shouldNotBe CodePoint(0xB6)
        CodePoint(0xB6) shouldBe CodePoint(0xB6)
    }

    @Test fun compare() = testAll {
        CodePoint(0x61) shouldBeLessThan CodePoint(0xB6)
        CodePoint(0x2630) shouldBeGreaterThan CodePoint(0xB6)
        CodePoint(0xB6) shouldBeEqualComparingTo CodePoint(0xB6)
    }

    @Test fun string() = testAll {
        CodePoint(0x61).string should {
            it shouldBe "a"
            it.encodeToByteArray() shouldBe ubyteArrayOf(0x61u).toByteArray()
        }
        CodePoint(0xB6).string should {
            it shouldBe "¶"
            it.encodeToByteArray() shouldBe ubyteArrayOf(0xC2u, 0xB6u).toByteArray()
        }
        CodePoint(0x2630).string should {
            it shouldBe "☰"
            it.encodeToByteArray() shouldBe ubyteArrayOf(0xE2u, 0x98u, 0xB0u).toByteArray()
        }
        CodePoint(0x1D553).string should {
            it shouldBe "𝕓"
            it.encodeToByteArray() shouldBe ubyteArrayOf(0xF0u, 0x9Du, 0x95u, 0x93u).toByteArray()
        }
    }

    @Test fun to_string() = testAll {
        CodePoint(0x61).toString() shouldBe "a"
        CodePoint(0xB6).toString() shouldBe "¶"
        CodePoint(0x2630).toString() shouldBe "☰"
        CodePoint(0x1D553).toString() shouldBe "𝕓"
    }

    @Test fun char() = testAll {
        CodePoint(0x61).char shouldBe 'a'
        CodePoint(0xB6).char shouldBe '¶'
        CodePoint(0x2630).char shouldBe '☰'
        CodePoint(0x1D553).char shouldBe null
    }

    @Test fun chars() = testAll {
        CodePoint(0x61).chars shouldBe "a".toCharArray()
        CodePoint(0xB6).chars shouldBe "¶".toCharArray()
        CodePoint(0x2630).chars shouldBe "☰".toCharArray()
        CodePoint(0x1D553).chars shouldBe "𝕓".toCharArray()
    }

    @Test fun char_count() = testAll {
        CodePoint(0x61).charCount shouldBe 1
        CodePoint(0xB6).charCount shouldBe 1
        CodePoint(0x2630).charCount shouldBe 1
        CodePoint(0x1D553).charCount shouldBe 2
    }

    @Test fun code_point() = testAll {
        'a'.codePoint shouldBe CodePoint(0x61)
        '¶'.codePoint shouldBe CodePoint(0xB6)
        '☰'.codePoint shouldBe CodePoint(0x2630)
    }

    @Test fun as_code_point() = testAll {
        0x61.toByte().asCodePoint() shouldBe CodePoint(0x61)
        0xB6.toByte().asCodePoint() shouldBe CodePoint(0xB6)

        shouldThrow<IllegalArgumentException> { "".asCodePoint() }
        "a".asCodePoint() shouldBe CodePoint(0x61)
        "¶".asCodePoint() shouldBe CodePoint(0xB6)
        "☰".asCodePoint() shouldBe CodePoint(0x2630)
        "𝕓".asCodePoint() shouldBe CodePoint(0x1D553)
        shouldThrow<IllegalArgumentException> { "a̳o".asCodePoint() }

        "".asCodePointOrNull() shouldBe null
        "a".asCodePointOrNull() shouldBe CodePoint(0x61)
        "¶".asCodePointOrNull() shouldBe CodePoint(0xB6)
        "☰".asCodePointOrNull() shouldBe CodePoint(0x2630)
        "𝕓".asCodePointOrNull() shouldBe CodePoint(0x1D553)
        "a̳o".asCodePointOrNull() shouldBe null
    }

    @Test fun is_0to9() = testAll {
        "0123456789".asCodePointSequence().forEach { it.is0to9 shouldBe true }
        "AzΑωष".asCodePointSequence().forEach { it.is0to9 shouldBe false }
    }

    @Test fun is_AtoZ() = testAll {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ".asCodePointSequence().forEach { it.isAtoZ shouldBe true }
        "abc123🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().forEach { it.isAtoZ shouldBe false }
    }

    @Suppress("SpellCheckingInspection")
    @Test fun is_atoz() = testAll {
        "abcdefghijklmnopqrstuvwxyz".asCodePointSequence().forEach { it.isatoz shouldBe true }
        "ABC123🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().forEach { it.isatoz shouldBe false }
    }

    @Suppress("SpellCheckingInspection")
    @Test fun is_Atoz() = testAll {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".asCodePointSequence().forEach { it.isAtoz shouldBe true }
        "123🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().forEach { it.isAtoz shouldBe false }
    }

    @Test fun is_ascii_alphanumeric() = testAll {
        "Az09".asCodePointSequence().forEach { it.isAsciiAlphanumeric shouldBe true }
        "Αωष🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().forEach { it.isAsciiAlphanumeric shouldBe false }
    }

    @Test fun is_alphanumeric() = testAll {
        "Az09Αωष".asCodePointSequence().forEach { it.isAlphanumeric shouldBe true }
        "🜃🜂🜁🜄𝌀𝍖ि".asCodePointSequence().forEach { it.isAlphanumeric shouldBe false }
    }

    @Test fun is_letter() = testAll {
        "AzΑωष".asCodePointSequence().forEach { it.isLetter shouldBe true }
        "🜃🜂🜁🜄𝌀𝍖ि09".asCodePointSequence().forEach { it.isLetter shouldBe false }
    }

    @Test fun is_digit() = testAll {
        "0123456789".asCodePointSequence().forEach { it.isDigit shouldBe true }
        "AzΑωष".asCodePointSequence().forEach { it.isDigit shouldBe false }
    }

    @Test fun is_whitespace() = testAll {
        listOf(' ', '\u2000').forAll { it.codePoint.isWhitespace shouldBe true }
        "Az09Αω𝌀𝍖षि🜃🜂🜁🜄".asCodePointSequence().forEach { it.isWhitespace shouldBe false }
    }

    @Test fun text_unit() = testAll(emojiCharSequence, emojiString) {
        CodePoint.name shouldBe "code point"
        CodePoint.textOf(String.EMPTY) shouldBe Text.emptyText()
        CodePoint.textOf(it) should beText(
            ChunkedText(
                it,
                0..0,
                1..2,
                3..4,
                5..6,
                7..8,
                9..10,
                11..12,
                13..13,
                14..15,
                16..17,
                18..18,
                19..20,
                21..21,
                22..23,
                24..24,
                25..26,
                transform = ::CodePoint
            ),
            *emojiCodePoints
        )
    }

    @Test fun text_length() = testAll {
        CodePoint.lengthOf(42) should {
            it.value shouldBe 42
            it.unit shouldBe CodePoint
            it shouldBe TextLength(42, CodePoint)
            it shouldNotBe TextLength(42, Word)
        }

        42.codePoints shouldBe CodePoint.lengthOf(42)
    }

    @Test fun code_point_range() = testAll {
        CodePointRange(CodePoint(0x61), CodePoint(0x6A)) should {
            it.start shouldBe CodePoint(0x61)
            it.endInclusive shouldBe CodePoint(0x6A)
            it.iterator().asSequence().joinToString { it.value.toString() } shouldBe (0x61..0x6A).map { it.toString() }.joinToString()
        }
    }
}
