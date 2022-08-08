package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class TuplesKtTest {

    @Test fun quadruple() = testAll {
        Quadruple("a", 1, listOf(1.2, 3.4), Unit) should {
            it.first shouldBe "a"
            it.second shouldBe 1
            it.third shouldBe listOf(1.2, 3.4)
            it.fourth shouldBe Unit
        }
    }

    @Test fun quintuple() = testAll {
        Quintuple("a", 1, listOf(1.2, 3.4), Unit, 5.1f) should {
            it.first shouldBe "a"
            it.second shouldBe 1
            it.third shouldBe listOf(1.2, 3.4)
            it.fourth shouldBe Unit
            it.fifth shouldBe 5.1f
        }
    }

    @Test fun too() = testAll {
        "a" to 1 too listOf(1.2, 3.4) shouldBe Triple("a", 1, listOf(1.2, 3.4))
        "a" to 1 too listOf(1.2, 3.4) too Unit shouldBe Quadruple("a", 1, listOf(1.2, 3.4), Unit)
        "a" to 1 too listOf(1.2, 3.4) too Unit too 5.1f shouldBe Quintuple("a", 1, listOf(1.2, 3.4), Unit, 5.1f)
    }

    @Test fun map() = testAll {
        Pair("a", 1).map { it.toString() } shouldBe Pair("a", "1")
        Triple("a", 1, listOf(1.2, 3.4)).map { it.toString() } shouldBe Triple("a", "1", "[1.2, 3.4]")
        Quadruple("a", 1, listOf(1.2, 3.4), Unit).map { it.toString() } shouldBe Quadruple("a", "1", "[1.2, 3.4]", "kotlin.Unit")
        Quintuple("a", 1, listOf(1.2, 3.4), Unit, 5.1f).map { it.toString() } shouldBe Quintuple("a", "1", "[1.2, 3.4]", "kotlin.Unit", "5.1")
    }
}
