package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue


class AnyContainsAllKtTest {

    val stringList = listOf("foo bar", "BAR BAZ")

    @Test
    fun `should return true if any of the others is case-matching substring`() {
        expectThat(stringList.anyContainsAll(listOf("foo ", "bar"))).isTrue()
    }

    @Test
    fun `should return true if any of the others is non-case-matching substring but case is ignored`() {
        expectThat(stringList.anyContainsAll(listOf("foo ", "BAR"), ignoreCase = true)).isTrue()
    }

    @Test
    fun `should return false if none of the others is no case-matching substring`() {
        expectThat(stringList.anyContainsAll(listOf("baz", "Baz", "abc"))).isFalse()
    }

    @Test
    fun `should return false if none of the others is substring`() {
        expectThat(stringList.anyContainsAll(listOf("@@@", "---", "!!!"))).isFalse()
    }
}
