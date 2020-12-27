package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class FlatMapLinesKtTest {

    val transform = { s: CharSequence -> listOf("$s" + s.reversed(), "${s.reversed()}" + "$s") }

    @Test
    fun `should transform single line`() {
        expectThat("AB".flatMapLines(ignoreTrailingSeparator = true, transform)).isEqualTo("ABBA\nBAAB")
    }

    @Test
    fun `should transform multi line`() {
        @Suppress("SpellCheckingInspection")
        expectThat("AB\nBA".flatMapLines(ignoreTrailingSeparator = true, transform)).isEqualTo("ABBA\nBAAB\nBAAB\nABBA")
    }

    @Test
    fun `should keep trailing line`() {
        expectThat("AB\nBA\n".flatMapLines { listOf("X", "Y") }).isEqualTo("X\nY\nX\nY\n")
    }

    @Test
    fun `should map empty string`() {
        expectThat("".flatMapLines { listOf("X", "Y") }).isEqualTo("X\nY")
    }

    @Test
    fun `should map empty string and keep trailing line`() {
        expectThat("\n".flatMapLines { listOf("X", "Y") }).isEqualTo("X\nY\n")
    }

    @Test
    fun `should map trailing empty line if not ignored`() {
        expectThat("\n".flatMapLines(ignoreTrailingSeparator = false) { listOf("X", "Y") }).isEqualTo("X\nY\nX\nY")

    }
}

