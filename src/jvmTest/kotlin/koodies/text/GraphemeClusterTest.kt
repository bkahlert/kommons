package koodies.text

import koodies.test.testEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.last
import koodies.text.Unicode.escape as e

class GraphemeClusterTest {

    @Nested
    inner class AsGraphemeClusterSequence {

        @TestFactory
        fun `should handle empty string`() = testEach<CharSequence.() -> List<GraphemeCluster>>(
            { asGraphemeClusterSequence().toList() },
            { toGraphemeClusterList() },
        ) { fn ->
            expecting { "".fn() } that { isEmpty() }
        }

        @TestFactory
        fun `should handle control characters`() = testEach<CharSequence.() -> List<GraphemeCluster>>(
            { asGraphemeClusterSequence().toList() },
            { toGraphemeClusterList() },
        ) { fn ->
            expecting { "$e".fn() } that {
                hasSize(1)
                first().get { graphemes } and {
                    hasSize(1)
                    first().isEqualTo(Unicode.escape.codePoint)
                }
            }
            expecting { "${e}M".fn() } that {
                hasSize(2)
                first().get { graphemes } and {
                    hasSize(1)
                    first().isEqualTo(Unicode.escape.codePoint)
                }
                last().get { graphemes } and {
                    hasSize(1)
                    first().isEqualTo("M".asCodePoint())
                }
            }
        }

        @TestFactory
        fun `should handle multi-codepoint clusters`() = testEach<CharSequence.() -> List<GraphemeCluster>>(
            { asGraphemeClusterSequence().toList() },
            { toGraphemeClusterList() },
        ) { fn ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn() } that {
                containsExactly(
                    "‾͟͟͞".toGraphemeClusterList().first(),
                    "(".toGraphemeClusterList().first(),
                    "(".toGraphemeClusterList().first(),
                    "(".toGraphemeClusterList().first(),
                    "ꎤ".toGraphemeClusterList().first(),
                    " ".toGraphemeClusterList().first(),
                    "✧".toGraphemeClusterList().first(),
                    "曲".toGraphemeClusterList().first(),
                    "✧".toGraphemeClusterList().first(),
                    ")̂".toGraphemeClusterList().first(),
                    "—̳͟͞͞".toGraphemeClusterList().first(),
                    "O".toGraphemeClusterList().first(),
                    " ".toGraphemeClusterList().first(),
                    "H".toGraphemeClusterList().first(),
                    "I".toGraphemeClusterList().first(),
                    "T".toGraphemeClusterList().first(),
                    "!".toGraphemeClusterList().first(),
                )
            }
        }

        @TestFactory
        fun `should handle multi-chars codepoints`() = testEach<CharSequence.() -> List<GraphemeCluster>>(
            { asGraphemeClusterSequence().toList() },
            { toGraphemeClusterList() },
        ) { fn ->
            expecting { "🟥🟧🟨🟩🟦🟪".fn() } that {
                containsExactly(
                    "🟥".toGraphemeClusterList().first(),
                    "🟧".toGraphemeClusterList().first(),
                    "🟨".toGraphemeClusterList().first(),
                    "🟩".toGraphemeClusterList().first(),
                    "🟦".toGraphemeClusterList().first(),
                    "🟪".toGraphemeClusterList().first(),
                )
            }
        }
    }

    @TestFactory
    fun `should return input string`() = testEach(
        "A",
        "曲",
        "🟥",
        "‾͟͟͞",
        "👨🏾‍",
    ) { input ->
        val graphemeCluster = input.asGraphemeClusterSequence().single()
        expecting { graphemeCluster.toString() } that { isEqualTo(input) }
    }

    @TestFactory
    fun `should return grapheme cluster count`() = testEach(
        "" to 0,
        "$e" to 1,
        "${e}M" to 2,
        "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!" to 17,
        "🟥🟧🟨🟩🟦🟪" to 6,
        "👨🏾‍" to 3,
    ) { (string, expectedCount) ->
        expecting { string.graphemeClusterCount } that { isEqualTo(expectedCount) }
    }

    @TestFactory
    fun `should map grapheme clusters`() = testEach(
        "" to emptyList(),
        "$e" to listOf(1),
        "${e}M" to listOf(1, 1),
        "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!" to listOf(4, 1, 1, 1, 1, 1, 1, 1, 1, 2, 5, 1, 1, 1, 1, 1, 1),
        "🟥🟧🟨🟩🟦🟪" to listOf(1, 1, 1, 1, 1, 1),
        "👨🏾‍" to listOf(3),
    ) { (string, expectedCount) ->
        expecting { string.mapGraphemeClusters { it.graphemes.size } } that {
            isEqualTo(expectedCount)
            get { sumOf { it } }.isEqualTo(string.codePointCount)
        }
    }
}
