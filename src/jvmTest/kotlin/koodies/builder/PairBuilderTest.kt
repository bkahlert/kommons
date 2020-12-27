package koodies.builder

import koodies.test.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class PairBuilderTest {

    @TestFactory
    fun `should build pair`() = listOf<PairBuilderInit<String, Int>>(
        { "three" to 4 },
        { "three" and 4 },
    ).test { init ->
        expectThat(init.buildPair()).isEqualTo("three" to 4)
    }

    @Test
    fun `should only consider returned pair`() {
        expectThat(with(PairBuilder) {
            "two" to 3
            "three" and 4
        }).isEqualTo("three" to 4)
    }
}
