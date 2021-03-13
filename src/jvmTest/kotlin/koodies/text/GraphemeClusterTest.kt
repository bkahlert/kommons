package koodies.text

import koodies.test.toStringIsEqualTo
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(SAME_THREAD)
class GraphemeClusterTest {

    @Test
    fun `should be instantiatable from CodePoints`() {
        val subject = GraphemeCluster(listOf(CodePoint("2"), CodePoint("\u20E3")))
        expectThat(subject).toStringIsEqualTo("2⃣")
    }

    @Test
    fun `should be instantiatable from CharSequence`() {
        expectThat(GraphemeCluster("2⃣")).toStringIsEqualTo("2⃣")
    }

    @Test
    fun `should throw on empty string`() {
        expectCatching { GraphemeCluster("") }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should throw on multi grapheme string`() {
        expectCatching { GraphemeCluster("1⃣2⃣") }.isFailure().isA<IllegalArgumentException>()
    }

    @TestFactory
    fun using() = listOf(
        "\u0041" to 1, // A
        "\uD83E\uDD13" to 1, // 🤓
        "ᾷ" to 1, // 3 code points
        "\u270B\uD83E\uDD1A" to 2, // ✋🤚
    ).flatMap { (string, graphemeClusterCount) ->
        listOf(
            dynamicTest("${string.quoted} should validate successfully") {
                val actual = string.isGraphemeCluster
                expectThat(actual).isEqualTo(graphemeClusterCount == 1)
            },

            dynamicTest("${string.quoted} should count $graphemeClusterCount grapheme clusters") {
                val actual = string.graphemeClusterCount
                expectThat(actual).isEqualTo(graphemeClusterCount)
            },

            if (graphemeClusterCount == 1)
                dynamicTest("${string.quoted} should be re-creatable using chars") {
                    val actual = GraphemeCluster(string)
                    expectThat(actual).get { GraphemeCluster(string) }.isEqualTo(actual)
                } else
                dynamicTest("${string.quoted} should throw on Grapheme construction") {
                    expectCatching { GraphemeCluster(string) }
                },
            if (graphemeClusterCount == 1)
                dynamicTest("${string.quoted} should be re-creatable using chars") {
                    val actual = GraphemeCluster(string)
                    expectThat(actual).get { GraphemeCluster(string) }.isEqualTo(actual)
                }
            else
                dynamicTest("${string.quoted} should throw on Grapheme construction") {
                    expectCatching { GraphemeCluster(string) }
                },
        )
    }

    @Test
    fun `should return nth grapheme`() {
        expectThat("vᾷ⚡⚡⚡⚡").get {
            listOf(getGrapheme(0), getGrapheme(1), getGrapheme(2), getGrapheme(3), getGrapheme(4), getGrapheme(5))
        }.containsExactly("v", "ᾷ", "⚡", "⚡", "⚡", "⚡")
    }

    @Test
    fun `should provide sequence`() {
        expectThat("웃유♋⌚⌛⚡𝌿☯✡☪".asGraphemeClusterSequence().map { it.toString() }.toList()).containsExactly("웃", "유", "♋", "⌚", "⌛", "⚡", "𝌿", "☯", "✡", "☪")
    }

    @Test
    fun `should throw n+1th grapheme`() {
        expectCatching { "웃유♋⌚⌛⚡☯✡☪".let { it.getGrapheme(it.graphemeClusterCount) } }.isFailure().isA<IndexOutOfBoundsException>()
    }

    @Test
    fun `should parse empty`() {
        expectThat("".toGraphemeClusterList()).isEmpty()
    }

    @Test
    fun `should parse latin`() {
        expectThat("yo".toGraphemeClusterList()).containsExactly(GraphemeCluster("y"), GraphemeCluster("o"))
    }

    @Test
    fun `should parse emojis`() {
        expectThat("💩🔥".toGraphemeClusterList()).containsExactly(GraphemeCluster("💩"), GraphemeCluster("🔥"))
    }

    @Disabled
    @Test
    fun `should parse emoji sequences`() {
        expectThat("🏴󠁧󠁢󠁳󠁣󠁴󠁿".toGraphemeClusterList()).containsExactly(GraphemeCluster("🏴󠁧󠁢󠁳󠁣󠁴󠁿"))
    }

    @Disabled
    @Test
    fun `should parse emoji modifiers`() {
        expectThat("👮🏿‍♀️".toGraphemeClusterList()).containsExactly(GraphemeCluster("👮🏿‍♀️"))
    }

    @Disabled
    @Test
    fun `should parse family sequences`() {
        expectThat("👨‍👨‍👧‍👧".toGraphemeClusterList()).containsExactly(GraphemeCluster("👨‍👨‍👧‍👧"))
    }
}
