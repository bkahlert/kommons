package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat

@Execution(CONCURRENT)
class ContainsAnyKtTest {
    val string = "foo bar"

    @Test
    fun `should return true if any of the others is case-matching substring`() {
        expectThat(string).containsAny("baz", "o b", "abc")
    }

    @Test
    fun `should return true if any of the others is non-case-matching substring but case is ignored`() {
        expectThat(string).containsAny("baz", "O B", "abc", ignoreCase = true)
    }

    @Test
    fun `should return false if none of the others is no case-matching substring`() {
        expectThat(string).not { containsAny("baz", "O B", "abc") }
    }

    @Test
    fun `should return false if none of the others is substring`() {
        expectThat(string).not { containsAny("baz", "---", "abc") }
    }
}


/**
 * Asserts that the subject contains any of the [expected] substrings.
 */
fun <T : CharSequence> Assertion.Builder<T>.containsAny(vararg expected: T, ignoreCase: Boolean = false): Assertion.Builder<T> =
    assert("contains any of ${expected.map { it.quoted }}") {
        if (it.containsAny(expected, ignoreCase = ignoreCase)) {
            pass()
        } else {
            fail("does not contain any of ${expected.map { it.quoted }}")
        }
    }
