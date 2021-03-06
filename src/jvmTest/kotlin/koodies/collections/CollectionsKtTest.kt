package koodies.collections

import koodies.test.test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure

@Execution(SAME_THREAD)
class CollectionsKtTest {

    @Nested
    inner class DropLast {

        private val list = listOf("first", "second", "third")

        @TestFactory
        fun `should drop last elements`() = test(list) {
            expectThrowing { dropLast(-1) }.that { isFailure().isA<IllegalArgumentException>() }
            expect { dropLast(0) }.that { containsExactly("first", "second", "third") }
            expect { dropLast() }.that { containsExactly("first", "second") }
            expect { dropLast(1) }.that { containsExactly("first", "second") }
            expect { dropLast(2) }.that { containsExactly("first") }
            expect { dropLast(3) }.that { isEmpty() }
            expect { dropLast(4) }.that { isEmpty() }
        }
    }

    @Nested
    inner class ToLinkedSet {

        private val list = listOf("first", "second", "first")

        @Test
        fun `should return linked set`() {
            expectThat(list.toLinkedSet()).isA<LinkedHashSet<String>>()
        }

        @Test
        fun `should contain elements only once`() {
            expectThat(list.toLinkedSet()).isEqualTo(LinkedHashSet(listOf("first", "second")))
        }
    }

    @Nested
    inner class RequireContainsSingleOfType {

        private val list = listOf("first", 2, 3.3)

        @Test
        fun `should return single element`() {
            expectThat(list.requireContainsSingleOfType<Int>()).isEqualTo(2)
        }

        @Test
        fun `should throw on missing element of type`() {
            expectCatching { list.requireContainsSingleOfType<Float>() }.isFailure().isA<NoSuchElementException>()
        }

        @Test
        fun `should throw on multiple elements of type`() {
            expectCatching { list.requireContainsSingleOfType<Number>() }.isFailure().isA<NoSuchElementException>()
        }
    }
}
