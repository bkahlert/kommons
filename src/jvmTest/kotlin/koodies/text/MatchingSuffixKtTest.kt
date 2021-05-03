package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNullOrBlank


class MatchingSuffixKtTest {

    @Test
    fun `should find matching suffix`() {
        expectThat("Prom!§\$%&/())pt".matchingSuffix("pt", "/())pt", "om", "&/())p")).isEqualTo("/())pt")
    }

    @Test
    fun `should not find non-matching suffix`() {
        expectThat("Prompt!".matchingSuffix("abc", "def")).isNullOrBlank()
    }
}
