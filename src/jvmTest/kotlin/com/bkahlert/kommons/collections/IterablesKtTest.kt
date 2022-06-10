package com.bkahlert.kommons.collections

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.containsExactly

class IterablesKtTest {

    @Nested
    inner class ZipWithDefaultKtTest {

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
