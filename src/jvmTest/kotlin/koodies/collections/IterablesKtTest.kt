package koodies.collections

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(ExecutionMode.CONCURRENT)
class IterablesKtTest {

    @Nested
    inner class MaxOrThrowKtTest {
        @Test
        fun `should find max`() {
            expectThat(listOf(1, 3, 2).maxOrThrow()).isEqualTo(3)
        }

        @Test
        fun `should throw on empty`() {
            expectCatching { emptyList<Int>().maxOrThrow() }.isFailure().isA<NoSuchElementException>()
        }
    }

    @Nested
    inner class MinOrThrowKtTest {
        @Test
        fun `should find min`() {
            expectThat(listOf(3, 1, 2).minOrThrow()).isEqualTo(1)
        }

        @Test
        fun `should throw on empty`() {
            expectCatching { emptyList<Int>().minOrThrow() }.isFailure().isA<NoSuchElementException>()
        }
    }

    @Nested
    inner class ZipWithDefaultKtTest {

        @Nested
        inner class ListBased {
            @Test
            fun `should zip collections of same length`() {
                expectThat(listOf("a", "b").zipWithDefault(listOf(1, 2), "" to 0) { left, right -> "$right-$left" })
                    .containsExactly("1-a", "2-b")
            }

            @Test
            fun `should zip collections of with shorter first collection`() {
                expectThat(listOf("a", "b").zipWithDefault(listOf(1, 2, 3), "" to 0) { left, right -> "$right-$left" })
                    .containsExactly("1-a", "2-b", "3-")
            }

            @Test
            fun `should zip collections of with shorter second collection`() {
                expectThat(listOf("a", "b", "c").zipWithDefault(listOf(1, 2), "" to 0) { left, right -> "$right-$left" })
                    .containsExactly("1-a", "2-b", "0-c")
            }
        }

        @Nested
        inner class SequenceBased {
            @Test
            fun `should zip collections of same length`() {
                expectThat(sequenceOf("a", "b").zipWithDefault(sequenceOf(1, 2), "" to 0) { left, right -> "$right-$left" }.toList())
                    .containsExactly("1-a", "2-b")
            }

            @Test
            fun `should zip collections of with shorter first collection`() {
                expectThat(sequenceOf("a", "b").zipWithDefault(sequenceOf(1, 2, 3), "" to 0) { left, right -> "$right-$left" }.toList())
                    .containsExactly("1-a", "2-b", "3-")
            }

            @Test
            fun `should zip collections of with shorter second collection`() {
                expectThat(sequenceOf("a", "b", "c").zipWithDefault(sequenceOf(1, 2), "" to 0) { left, right -> "$right-$left" }.toList())
                    .containsExactly("1-a", "2-b", "0-c")
            }
        }
    }

}
