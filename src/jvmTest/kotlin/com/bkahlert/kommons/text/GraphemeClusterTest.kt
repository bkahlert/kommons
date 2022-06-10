package com.bkahlert.kommons.text

import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.text.LineSeparators.CR
import com.bkahlert.kommons.text.LineSeparators.CRLF
import com.bkahlert.kommons.text.LineSeparators.LF
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestFactory
import strikt.assertions.containsExactly
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.last
import com.bkahlert.kommons.text.Unicode.ESCAPE as e

class GraphemeClusterTest {

    @Nested
    inner class AsGraphemeClusterSequence {

        @TestFactory
        fun `should handle empty string`() = testEachOld<CharSequence.() -> List<GraphemeCluster>>(
            { asGraphemeClusterSequence().toList() },
            { toGraphemeClusterList() },
        ) { fn ->
            expecting { "".fn() } that { isEmpty() }
        }

        @TestFactory
        fun `should handle control characters`() = testEachOld<CharSequence.() -> List<GraphemeCluster>>(
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

        @TextWidthRequiring @TestFactory
        fun `should handle multi-codepoint clusters`() = testEachOld<CharSequence.() -> List<GraphemeCluster>>(
            { asGraphemeClusterSequence().toList() },
            { toGraphemeClusterList() },
        ) { fn ->
            expecting { "a̳o".fn() } that {
                containsExactly(
                    "a̳".toGraphemeClusterList().single(),
                    "o".toGraphemeClusterList().single(),
                )
            }
        }

        @TestFactory
        fun `should handle multi-chars codepoints`() = testEachOld<CharSequence.() -> List<GraphemeCluster>>(
            { asGraphemeClusterSequence().toList() },
            { toGraphemeClusterList() },
        ) { fn ->
            expecting { "😀".fn() } that {
                containsExactly(
                    "😀".toGraphemeClusterList().first(),
                )
            }
        }
    }

    @TestFactory
    fun `should return input string`() = testEachOld(
        "A",
        "曲",
        "🟥",
        "a̠",
        "😀",
        "👨🏾",
        "👩‍👩‍👧‍👧",
    ) { input ->
        val graphemeCluster = input.asGraphemeClusterSequence().single()
        expecting { graphemeCluster.toString() } that { isEqualTo(input) }
    }

    @TestFactory
    fun `should return grapheme cluster count`() = testEachOld(
        "" to 0,
        "$e" to 1,
        "${e}M" to 2,
        "x😀" to 2,
        "😀" to 1,
        "👨🏾" to 1,
        "👩‍👩‍👧‍👧" to 1,
    ) { (string, expectedCount) ->
        expecting { string.graphemeClusterCount } that { isEqualTo(expectedCount) }
    }

    @TextWidthRequiring @TestFactory
    fun `should return grapheme cluster count for combining characters`() = testEachOld(
        "a̳o" to 2,
    ) { (string, expectedCount) ->
        expecting { string.graphemeClusterCount } that { isEqualTo(expectedCount) }
    }

    @TestFactory
    fun `should map grapheme clusters`() = testEachOld(
        "" to emptyList(),
        "$e" to listOf(1),
        "${e}M" to listOf(1, 1),
        "x😀" to listOf(1, 1),
        "👨🏾‍" to listOf(3),
    ) { (string, expectedCount) ->
        expecting { string.mapGraphemeClusters { it.codePoints.size } } that {
            isEqualTo(expectedCount)
            get { sumOf { it } }.isEqualTo(string.codePointCount)
        }
    }

    @TextWidthRequiring @TestFactory
    fun `should map grapheme clusters for combining characters`() = testEachOld(
        "a̳o" to listOf(2, 1),
    ) { (string, expectedCount) ->
        expecting { string.mapGraphemeClusters { it.codePoints.size } } that {
            isEqualTo(expectedCount)
            get { sumOf { it } }.isEqualTo(string.codePointCount)
        }
    }
}
