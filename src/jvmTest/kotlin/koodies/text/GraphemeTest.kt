package koodies.text

import koodies.test.toStringIsEqualTo
import koodies.text.Grapheme.Companion.asGraphemeSequence
import koodies.text.Grapheme.Companion.getGrapheme
import koodies.text.Grapheme.Companion.getGraphemeCount
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class GraphemeTest {

    @Test
    fun `should be instantiatable from CodePoints`() {
        val subject = Grapheme(listOf(CodePoint("2"), CodePoint("\u20E3")))
        expectThat(subject).toStringIsEqualTo("2⃣")
    }

    @Test
    fun `should be instantiatable from CharSequence`() {
        expectThat(Grapheme("2⃣")).toStringIsEqualTo("2⃣")
    }

    @Test
    fun `should throw on empty string`() {
        expectCatching { Grapheme("") }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should throw on multi grapheme string`() {
        expectCatching { Grapheme("1⃣2⃣") }.isFailure().isA<IllegalArgumentException>()
    }

    @TestFactory
    fun using() = listOf(
        "\u0041" to 1, // A
        "\uD83E\uDD13" to 1, // 🤓
        "ᾷ" to 1, // 3 code points
        "\u270B\uD83E\uDD1A" to 2, // ✋🤚
    ).flatMap { (string, graphemeCount) ->
        listOf(
            dynamicTest("${string.quoted} should validate successfully") {
                val actual = Grapheme.isGrapheme(string)
                expectThat(actual).isEqualTo(graphemeCount == 1)
            },

            dynamicTest("${string.quoted} should count $graphemeCount code points") {
                val actual = Grapheme.count(string)
                expectThat(actual).isEqualTo(graphemeCount)
            },

            if (graphemeCount == 1)
                dynamicTest("${string.quoted} should be re-creatable using chars") {
                    val actual = Grapheme(string)
                    expectThat(actual).get { Grapheme(string) }.isEqualTo(actual)
                } else
                dynamicTest("${string.quoted} should throw on Grapheme construction") {
                    expectCatching { Grapheme(string) }
                },
            if (graphemeCount == 1)
                dynamicTest("${string.quoted} should be re-creatable using chars") {
                    val actual = Grapheme(string)
                    expectThat(actual).get { Grapheme(string) }.isEqualTo(actual)
                }
            else
                dynamicTest("${string.quoted} should throw on Grapheme construction") {
                    expectCatching { Grapheme(string) }
                },
        )
    }

    @Test
    fun `should return nth grapheme`() {
        val string = "vᾷ⚡⚡⚡⚡"
        expectThat(string).get {
            listOf(
                getGrapheme(0),
                getGrapheme(1),
                getGrapheme(2),
                getGrapheme(3),
                getGrapheme(4),
                getGrapheme(5),
            )
        }.containsExactly("v", "ᾷ", "⚡", "⚡", "⚡", "⚡")
    }

    @Test
    fun `should provide sequence`() {
        expectThat("웃유♋⌚⌛⚡𝌿☯✡☪".asGraphemeSequence().map { it.toString() }.toList()).containsExactly("웃", "유", "♋", "⌚", "⌛", "⚡", "𝌿", "☯", "✡", "☪")
    }

    @Test
    fun `should throw n+1th grapheme`() {
        expectCatching { "웃유♋⌚⌛⚡☯✡☪".let { it.getGrapheme(it.getGraphemeCount()) } }.isFailure().isA<StringIndexOutOfBoundsException>()
    }

    @Test
    fun `should parse empty`() {
        expectThat(Grapheme.toGraphemeList("")).isEmpty()
    }

    @Test
    fun `should parse latin`() {
        val string = "yo"
        expectThat(Grapheme.toGraphemeList(string))
            .containsExactly(
                nonEmojiResultAt(string, 0, 1),
                nonEmojiResultAt(string, 1, 1)
            )
    }

    @Test
    fun `should parse emojis`() {
        val emoji = StringBuilder()
            .appendCodePoint(0x1F4A9)
            .appendCodePoint(0x1F525)
            .toString()
        expectThat(Grapheme.toGraphemeList(emoji))
            .containsExactly(
                emojiResultAt(emoji, 0, 2),
                emojiResultAt(emoji, 2, 2)
            )
    }

    @Test
    fun `should parse emoji sequences`() {
        /**
         * [Flag of Scotland](http://emojipedia.org/flag-for-scotland/)
         */
        val emoji = StringBuilder()
            .appendCodePoint(0x1F3F4)
            .appendCodePoint(0xE0067)
            .appendCodePoint(0xE0062)
            .appendCodePoint(0xE0073)
            .appendCodePoint(0xE0063)
            .appendCodePoint(0xE0074)
            .appendCodePoint(0xE007F)
            .toString()
        expectThat(Grapheme.toGraphemeList(emoji))
            .containsExactly(
                emojiResultAt(emoji, 0, 14)
            )
    }

    @Test
    fun `should parse emoji modifiers`() {
        val emoji = StringBuilder()
            .appendCodePoint(0x1F46E)
            .appendCodePoint(0x1F3FF)
            .appendCodePoint(0x200D)
            .appendCodePoint(0x2640)
            .appendCodePoint(0xFE0F)
            .toString()
        expectThat(Grapheme.toGraphemeList(emoji))
            .containsExactly(
                emojiResultAt(emoji, 0, 7)
            )
    }

    @Test
    fun `should parse family sequences`() {
        /**
         * [Family of two men with two girls](http://www.iemoji.com/view/emoji/1712/smileys-people/family-of-two-men-with-two-girls)
         */
        val emoji = StringBuilder()
            .appendCodePoint(0x1F468)
            .appendCodePoint(0x200D)
            .appendCodePoint(0x1F468)
            .appendCodePoint(0x200D)
            .appendCodePoint(0x1F467)
            .appendCodePoint(0x200D)
            .appendCodePoint(0x1F467)
            .toString()
        expectThat(Grapheme.toGraphemeList(emoji))
            .containsExactly(
                emojiResultAt(emoji, 0, 11)
            )
    }

    companion object {
        private fun nonEmojiResultAt(string: String, offset: Int, length: Int): Grapheme =
            Grapheme(string.subSequence(offset, offset + length))

        private fun emojiResultAt(string: String, offset: Int, length: Int): Grapheme =
            Grapheme(string.subSequence(offset, offset + length))
    }
}
