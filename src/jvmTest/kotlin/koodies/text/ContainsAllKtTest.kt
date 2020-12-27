package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class ContainsAllKtTest {
    val string = "foo bar"

    @Test
    fun `should return true if all of the others is case-matching substring`() {
        expectThat(string.containsAll(listOf("foo ", " bar"))).isTrue()
    }

    @Test
    fun `should return true if all of the others is non-case-matching substring but case is ignored`() {
        expectThat(string.containsAll(listOf("foo ", "BAR"), ignoreCase = true)).isTrue()
    }

    @Test
    fun `should return false if none of the others is no case-matching substring`() {
        expectThat(string.containsAll(listOf("baz", "O B", "abc"))).isFalse()
    }

    @Test
    fun `should return false if none of the others is substring`() {
        expectThat(string.containsAll(listOf("baz", "---", "abc"))).isFalse()
    }
}
