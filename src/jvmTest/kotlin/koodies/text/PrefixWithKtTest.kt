package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class PrefixWithKtTest {

    @Test
    fun `should add prefix if not there`() {
        @Suppress("SpellCheckingInspection")
        expectThat("foo".prefixWith("bar")).isEqualTo("barfoo")
    }

    @Test
    fun `should add prefix if already there`() {
        @Suppress("SpellCheckingInspection")
        expectThat("barfoo".prefixWith("bar")).isEqualTo("barbarfoo")
    }
}
