package koodies.text

import koodies.test.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class ContainsAllKtTest {
    val string = "foo bar"

    @TestFactory
    fun `should return true if all of the others is case-matching substring`() = test(string) {
        expect { containsAll(listOf("foo ", " bar")) }.that { isTrue() }
        expect { containsAll("foo ", " bar") }.that { isTrue() }
    }

    @TestFactory
    fun `should return true if all of the others is non-case-matching substring but case is ignored`() = test(string) {
        expect { containsAll(listOf("foo ", "BAR"), ignoreCase = true) }.that { isTrue() }
        expect { containsAll("foo ", "BAR", ignoreCase = true) }.that { isTrue() }
    }

    @TestFactory
    fun `should return false if none of the others is no case-matching substring`() = test(string) {
        expect { containsAll(listOf("baz", "O B", "abc")) }.that { isFalse() }
        expect { containsAll("baz", "O B", "abc") }.that { isFalse() }
    }

    @TestFactory
    fun `should return false if none of the others is substring`() = test(string) {
        expect { containsAll(listOf("baz", "---", "abc")) }.that { isFalse() }
        expect { containsAll("baz", "---", "abc") }.that { isFalse() }
    }
}
