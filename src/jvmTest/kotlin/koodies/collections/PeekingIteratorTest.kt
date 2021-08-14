package koodies.collections

import koodies.test.expecting
import koodies.test.testEach
import koodies.test.tests
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThrows
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class PeekingIteratorTest {

    private val list: List<Int> = listOf(100, 200, 300)

    @Test
    fun `should iterate`() {
        expecting { list.peekingIterator().asSequence().toList() } that { containsExactly(100, 200, 300) }
    }

    @TestFactory
    fun `should return peeked on valid index`() = testEach(0 to 100, 1 to 200, 2 to 300) { (index, expected) ->
        expecting { list.peekingIterator().peekOrNull(index) } that { isEqualTo(expected) }
    }

    @TestFactory
    fun `should peek index 0 by default`() = tests {
        expecting { list.peekingIterator().peekOrNull() } that { isEqualTo(100) }
        expecting { list.peekingIterator().peek() } that { isEqualTo(100) }
    }

    @Test
    fun `should return null on invalid index`() {
        expecting { list.peekingIterator().peekOrNull(3) } that { isNull() }
    }

    @Test
    fun `should throw on invalid index`() {
        expectThrows<IndexOutOfBoundsException> { list.peekingIterator().peek(3) }
    }

    @TestFactory
    fun `should iterate after peek`() = testEach(0, 1, 2, 3) { index ->
        expecting {
            val iter = list.peekingIterator()
            iter.peekOrNull(index)
            iter.asSequence().toList()
        } that { containsExactly(100, 200, 300) }
    }

    @Test
    fun `should peek lesser index than already peeked`() {
        val iter = list.peekingIterator()
        expecting { iter.peekOrNull(1) } that { isEqualTo(200) }
        expecting { iter.peekOrNull(0) } that { isEqualTo(100) }
        expecting { iter.asSequence().toList() } that { containsExactly(100, 200, 300) }
    }
}
