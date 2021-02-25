package koodies.text

import koodies.collections.to
import koodies.test.testEach
import koodies.test.toStringIsEqualTo
import koodies.text.CodePoint.Companion.asCodePoint
import koodies.text.CodePoint.Companion.isUsableCodePoint
import koodies.text.CodePoint.Companion.isValidCodePoint
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
class CodePointTest {

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
    inner class CodePointSequence {
        @Test
        fun `should contain all unicode points`() {
            expectThat("Az09Αω𝌀𝍖".asCodePointSequence())
                .get { map { it.string }.joinToString("") }
                .isEqualTo("Az09Αω𝌀𝍖")
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
