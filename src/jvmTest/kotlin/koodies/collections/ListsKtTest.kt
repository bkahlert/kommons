package koodies.collections

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class ListsKtTest {

    @Nested
    inner class PairwiseAllKtTest {

        @Test
        fun `should success on matching size all filters`() {
            expectThat(listOf("A", "B").pairwiseAll({ it == "A" }, { it.length < 10 })).isTrue()
            expectThat(arrayOf("A", "B").pairwiseAll({ it == "A" }, { it.length < 10 })).isTrue()
        }

        @Test
        fun `should fail on size mismatch`() {
            expectThat(listOf("A", "B").pairwiseAll({ it == "A" })).isFalse()
            expectThat(arrayOf("A", "B").pairwiseAll({ it == "A" })).isFalse()
        }

        @Test
        fun `should fail on negative filter result`() {
            expectThat(listOf("A", "B").pairwiseAll({ it == "A" }, { it == "A" })).isFalse()
            expectThat(arrayOf("A", "B").pairwiseAll({ it == "A" }, { it == "A" })).isFalse()
        }
    }

    @Nested
    inner class RemoveFirstKtTest {
        @Test
        fun `should remove first elements`() {
            val list = mutableListOf("a", "b", "c")
            val first2 = list.removeFirst(2)
            expectThat(first2).containsExactly("a", "b")
            expectThat(list).containsExactly("c")
        }

        @Test
        fun `should throw and leave list unchanged on too few elements`() {
            val list = mutableListOf("a", "b", "c")
            expectCatching { list.removeFirst(4) }.isFailure().isA<IllegalArgumentException>()
            expectThat(list).containsExactly("a", "b", "c")
        }
    }

    @Nested
    inner class WithNegativeIndicesKtTest {
        val list = listOf("a", "b", "c").withNegativeIndices()

        @Test
        fun `should support negative indices`() {
            expectThat(list).get { get(-1) }.isEqualTo("c")
        }

        @Test
        fun `should support negative overflow`() {
            expectThat(list).get { get(-5) }.isEqualTo("b")
        }

        @Test
        fun `should support positive overflow`() {
            expectThat(list).get { get(6) }.isEqualTo("a")
        }
    }
}
