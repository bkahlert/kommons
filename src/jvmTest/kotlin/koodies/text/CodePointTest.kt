package koodies.text

import koodies.test.toStringIsEqualTo
import koodies.text.CodePoint.Companion.isValidCodePoint
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class CodePointTest {

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
    fun `should be instantiatable from CharArray`() {
        expectThat(CodePoint("A".toCharArray())).toStringIsEqualTo("A")
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
                val actual = CodePoint.count(string)
                expectThat(actual).isEqualTo(codePointCount)
            },

            if (codePointCount == 1L)
                dynamicTest("${string.quoted} should be re-creatable using chars") {
                    val actual = CodePoint(string)
                    expectThat(actual).get { CodePoint(chars) }.isEqualTo(actual)
                } else
                dynamicTest("${string.quoted} should throw on CodePoint construction") {
                    expectCatching { CodePoint(string) }
                },
            if (codePointCount == 1L)
                dynamicTest("${string.quoted} should be re-creatable using chars") {
                    val actual = CodePoint(string)
                    expectThat(actual).get { CodePoint(chars) }.isEqualTo(actual)
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
                .get { isValidCodePoint() }.isTrue()
        }

        @Test
        fun `should detekt invalid code points`() {
            expectThat(Character.MAX_CODE_POINT + 1).get { isValidCodePoint() }.isFalse()
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
