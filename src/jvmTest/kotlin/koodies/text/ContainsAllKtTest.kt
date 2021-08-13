package koodies.text

import koodies.test.test
import org.junit.jupiter.api.TestFactory
import strikt.assertions.isFalse
import strikt.assertions.isTrue

class ContainsAllKtTest {

    val string = "foo bar"

    @TestFactory
    fun `should return true if all of the others is case-matching substring`() = test(string) {
        expecting { containsAll(listOf("foo ", " bar")) } that { isTrue() }
        expecting { containsAll("foo ", " bar") } that { isTrue() }
    }

    @TestFactory
    fun `should return true if all of the others is non-case-matching substring but case is ignored`() = test(string) {
        expecting { containsAll(listOf("foo ", "BAR"), ignoreCase = true) } that { isTrue() }
        expecting { containsAll("foo ", "BAR", ignoreCase = true) } that { isTrue() }
    }

    @TestFactory
    fun `should return false if none of the others is no case-matching substring`() = test(string) {
        expecting { containsAll(listOf("baz", "O B", "abc")) } that { isFalse() }
        expecting { containsAll("baz", "O B", "abc") } that { isFalse() }
    }

    @TestFactory
    fun `should return false if none of the others is substring`() = test(string) {
        expecting { containsAll(listOf("baz", "---", "abc")) } that { isFalse() }
        expecting { containsAll("baz", "---", "abc") } that { isFalse() }
    }
}
