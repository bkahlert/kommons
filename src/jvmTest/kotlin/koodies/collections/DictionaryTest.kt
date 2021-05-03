package koodies.collections

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo


class DictionaryTest {

    private val dict = dictOf(
        "known" to 42,
        "negative" to -1,
    ) { _ -> Int.MAX_VALUE }

    @Test
    fun `should get value on match`() {
        expectThat(dict["known"]).isEqualTo(42)
    }

    @Test
    fun `should get default on mismatch`() {
        expectThat(dict["unknown"]).isEqualTo(Int.MAX_VALUE)
    }
}
