package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.iterator.shouldBeEmpty
import io.kotest.matchers.sequences.shouldBeEmpty
import io.kotest.matchers.sequences.shouldContainExactly
import kotlin.test.Test

class IteratorsKtTest {

    @Test fun to_list() = testAll {
        emptyList<Any>().iterator().toList().shouldBeEmpty()
        listOf(2, 5, 7).iterator().toList().asSequence().shouldContainExactly(2, 5, 7)
    }

    @Test fun map() = testAll {
        emptyList<Int>().iterator().map { it * 2 }.shouldBeEmpty()
        listOf(2, 5, 7).iterator().map { it * 2 }.asSequence().shouldContainExactly(4, 10, 14)
    }

    @Suppress("EmptyRange")
    @Test fun map_to_ranges() = testAll {
        emptyList<Int>().iterator().mapToRanges().asSequence().shouldBeEmpty()
        listOf(2).iterator().mapToRanges().asSequence().shouldContainExactly(0..1)
        listOf(2, 5, 7).iterator().mapToRanges().asSequence().shouldContainExactly(0..1, 2..4, 5..6)
        listOf(2, 5, 7).iterator().mapToRanges(start = 1).asSequence().shouldContainExactly(1..1, 2..4, 5..6)
        listOf(2, 5, 7).iterator().mapToRanges(start = 2).asSequence().shouldContainExactly(2..1, 2..4, 5..6)
    }
}
