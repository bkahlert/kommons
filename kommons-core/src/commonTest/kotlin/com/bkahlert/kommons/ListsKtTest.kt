package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class ListsKtTest {

    @Test fun sub_list() = testAll {
        val list = listOf("a", "b", "c").withNegativeIndices()
        list.subList(0..0) shouldBe list.subList(0, 1)
        list.subList(1..2) shouldBe list.subList(1, 3)
    }

    @Test fun with_negative_indices() = testAll {
        val list = listOf("a", "b", "c").withNegativeIndices()
        list[-1] shouldBe "c"
        list[-5] shouldBe "b"
        list[6] shouldBe "a"
    }

    @Test
    fun predecessor() = testAll {
        list.predecessor { false }.shouldBeEmpty()
        list.predecessor { it == "foo" }.shouldContainExactly("baz")
        list.predecessor { it == "bar" }.shouldContainExactly("foo")
        list.predecessor { it == "baz" }.shouldContainExactly("bar")
        list.predecessor { it != "bar" }.shouldContainExactly("baz", "bar")
        list.predecessor { true }.shouldContainExactly("baz", "foo", "bar")
        listOf("foo", "bar").predecessor { true }.shouldContainExactly("bar", "foo")
        listOf("foo").predecessor { true }.shouldContainExactly("foo")
        emptyList<String>().predecessor { true }.shouldBeEmpty()
    }

    @Test
    fun successor() = testAll {
        list.successor { false }.shouldBeEmpty()
        list.successor { it == "foo" }.shouldContainExactly("bar")
        list.successor { it == "bar" }.shouldContainExactly("baz")
        list.successor { it == "baz" }.shouldContainExactly("foo")
        list.successor { it != "bar" }.shouldContainExactly("bar", "foo")
        list.successor { true }.shouldContainExactly("bar", "baz", "foo")
        listOf("foo", "bar").successor { true }.shouldContainExactly("bar", "foo")
        listOf("foo").successor { true }.shouldContainExactly("foo")
        emptyList<String>().successor { true }.shouldBeEmpty()
    }
}

val list = listOf("foo", "bar", "baz")
