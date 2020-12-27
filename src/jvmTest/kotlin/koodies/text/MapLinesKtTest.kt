package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class MapLinesKtTest {

    val transform = { s: CharSequence -> "$s" + s.reversed() }

    @Test
    fun `should transform single line`() {
        expectThat("AB".mapLines(ignoreTrailingSeparator = true, transform)).isEqualTo("ABBA")
    }

    @Test
    fun `should transform multi line`() {
        @Suppress("SpellCheckingInspection")
        expectThat("AB\nBA".mapLines(ignoreTrailingSeparator = true, transform)).isEqualTo("ABBA\nBAAB")
    }

    @Test
    fun `should keep trailing line`() {
        expectThat("AB\nBA\n".mapLines { "X" }).isEqualTo("X\nX\n")
    }

    @Test
    fun `should map empty string`() {
        expectThat("".mapLines { "X" }).isEqualTo("X")
    }

    @Test
    fun `should map empty string and keep trailing line`() {
        expectThat("\n".mapLines { "X" }).isEqualTo("X\n")
    }

    @Test
    fun `should map trailing empty line if not ignored`() {
        expectThat("\n".mapLines(ignoreTrailingSeparator = false) { "X" }).isEqualTo("X\nX")

    }
}
