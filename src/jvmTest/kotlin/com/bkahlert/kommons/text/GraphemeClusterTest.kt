package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.text.LineSeparators.CR
import com.bkahlert.kommons.text.LineSeparators.CRLF
import com.bkahlert.kommons.text.LineSeparators.LF
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.get
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.last
import com.bkahlert.kommons.text.Unicode.ESCAPE as e

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
                first().get { codePoints } and {
                    hasSize(1)
                    first().isEqualTo(Unicode.ESCAPE.codePoint)
                }
            }
            expecting { "${e}M".fn() } that {
                hasSize(2)
                first().get { codePoints } and {
                    hasSize(1)
                    first().isEqualTo(Unicode.ESCAPE.codePoint)
                }
                last().get { codePoints } and {
                    hasSize(1)
                    first().isEqualTo("M".asCodePoint())
                }
            }
            expecting { CRLF.fn() } that {
                hasSize(1)
                first().get { codePoints } and {
                    hasSize(2)
                    first().isEqualTo(CR.asCodePoint())
                    last().isEqualTo(LF.asCodePoint())
                }
            }
        }

        @Test
        fun `should handle multi-codepoint cluster`() {
            expecting { "‾͟͟͞".toGraphemeClusterList() } that {
                get(0) and {
                    get { codePoints } and {
                        containsExactly(
                            CodePoint("‾"),
                            CodePoint("͟"),
                            CodePoint("͟"),
                            CodePoint("͞"),
                        )
                    }
                }
                hasSize(1)
            }
        }

        @TestFactory
        fun `should handle multi-codepoint clusters`() = testEach<CharSequence.() -> List<GraphemeCluster>>(
            { asGraphemeClusterSequence().toList() },
            { toGraphemeClusterList() },
        ) { fn ->
            expecting { "‾͟͟͞(((ꎤ ✧曲✧)̂—̳͟͞͞O HIT!".fn() } that {
                containsExactly(
                    "‾͟͟͞".toGraphemeClusterList().single(),
                    "(".toGraphemeClusterList().single(),
                    "(".toGraphemeClusterList().single(),
                    "(".toGraphemeClusterList().single(),
                    "ꎤ".toGraphemeClusterList().single(),
                    " ".toGraphemeClusterList().single(),
                    "✧".toGraphemeClusterList().single(),
                    "曲".toGraphemeClusterList().single(),
                    "✧".toGraphemeClusterList().single(),
                    ")̂".toGraphemeClusterList().single(),
                    "—̳͟͞͞".toGraphemeClusterList().single(),
                    "O".toGraphemeClusterList().single(),
                    " ".toGraphemeClusterList().single(),
                    "H".toGraphemeClusterList().single(),
                    "I".toGraphemeClusterList().single(),
                    "T".toGraphemeClusterList().single(),
                    "!".toGraphemeClusterList().single(),
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
        "😀",
        "👨🏾",
        "👩‍👩‍👧‍👧",
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
        "😀" to 1,
        "👨🏾" to 1,
        "👩‍👩‍👧‍👧" to 1,
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
        expecting { string.mapGraphemeClusters { it.codePoints.size } } that {
            isEqualTo(expectedCount)
            get { sumOf { it } }.isEqualTo(string.codePointCount)
        }
    }
}
