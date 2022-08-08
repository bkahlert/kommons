package com.bkahlert.kommons

import com.bkahlert.kommons.test.testAll
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.throwable.shouldHaveMessage
import kotlin.test.Test

class CollectionsKtTest {

    @Test fun require_not_empty() = testAll {
        requireNotEmpty(collection) shouldBe collection
        requireNotEmpty(collection) { "error" } shouldBe collection
        requireNotEmpty(array) shouldBe array
        requireNotEmpty(array) { "error" } shouldBe array
        shouldThrow<IllegalArgumentException> { requireNotEmpty(emptyCollection) }
        shouldThrow<IllegalArgumentException> { requireNotEmpty(emptyCollection) { "error" } } shouldHaveMessage "error"
        shouldThrow<IllegalArgumentException> { requireNotEmpty(emptyArray) }
        shouldThrow<IllegalArgumentException> { requireNotEmpty(emptyArray) { "error" } } shouldHaveMessage "error"
    }

    @Test fun check_not_empty() = testAll {
        checkNotEmpty(collection) shouldBe collection
        checkNotEmpty(collection) { "error" } shouldBe collection
        checkNotEmpty(array) shouldBe array
        checkNotEmpty(array) { "error" } shouldBe array
        shouldThrow<IllegalStateException> { checkNotEmpty(emptyCollection) }
        shouldThrow<IllegalStateException> { checkNotEmpty(emptyCollection) { "error" } } shouldHaveMessage "error"
        shouldThrow<IllegalStateException> { checkNotEmpty(emptyArray) }
        shouldThrow<IllegalStateException> { checkNotEmpty(emptyArray) { "error" } } shouldHaveMessage "error"
    }

    @Test fun take_if_not_empty() = testAll {
        collection.takeIfNotEmpty() shouldBe collection
        array.takeIfNotEmpty() shouldBe array
        emptyCollection.takeIfNotEmpty() shouldBe null
        emptyArray.takeIfNotEmpty() shouldBe null
    }

    @Test fun take_unless_empty() = testAll {
        collection.takeUnlessEmpty() shouldBe collection
        array.takeUnlessEmpty() shouldBe array
        emptyCollection.takeUnlessEmpty() shouldBe null
        emptyArray.takeUnlessEmpty() shouldBe null
    }

    @Test fun head() = testAll {
        collection.head shouldBe "array"
        shouldThrow<NoSuchElementException> { emptyCollection.head }
    }

    @Test fun head_or_null() = testAll {
        collection.headOrNull shouldBe "array"
        emptyCollection.headOrNull.shouldBeNull()
    }

    @Test fun tail() = testAll {
        listOf("head", "tail").tail shouldBe listOf("tail")
        collection.tail shouldBe emptyList()
        emptyCollection.tail shouldBe emptyList()
    }

    @Test fun locate() = testAll {
        listOf("foo", "bar") should {
            shouldThrow<IndexOutOfBoundsException> { it.locate(-1, CharSequence::length) }.message shouldBe "index out of range: -1"
            it.locate(0, CharSequence::length) shouldBe (0 to 0)
            it.locate(1, CharSequence::length) shouldBe (0 to 1)
            it.locate(2, CharSequence::length) shouldBe (0 to 2)
            it.locate(3, CharSequence::length) shouldBe (1 to 0)
            it.locate(4, CharSequence::length) shouldBe (1 to 1)
            it.locate(5, CharSequence::length) shouldBe (1 to 2)
            shouldThrow<IndexOutOfBoundsException> { it.locate(6, CharSequence::length) }.message shouldBe "index out of range: 6"

            shouldThrow<IndexOutOfBoundsException> { it.locate(-1, 2, CharSequence::length) }.message shouldBe "index out of range: -1"
            it.locate(1, 2, CharSequence::length) shouldBe (0..0 to 1 too 2)
            it.locate(1, 6, CharSequence::length) shouldBe (0..1 to 1 too 3)
            it.locate(4, 6, CharSequence::length) shouldBe (1..1 to 1 too 3)
            shouldThrow<IndexOutOfBoundsException> { it.locate(4, 7, CharSequence::length) }.message shouldBe "index out of range: 6"
            shouldThrow<IndexOutOfBoundsException> { it.locate(2, 1, CharSequence::length) }.message shouldBe "begin 2, end 1"
        }
    }
}

internal val collection: Collection<String> = listOf("array")
internal val emptyCollection: Collection<String> = emptyList()

internal val array: Array<String> = arrayOf("array")
internal val emptyArray: Array<String> = emptyArray()
