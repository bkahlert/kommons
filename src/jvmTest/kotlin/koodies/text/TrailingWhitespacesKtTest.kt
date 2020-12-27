package koodies.text

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.hasLength
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class TrailingWhitespacesKtTest {

    @Test
    fun `should find single whitespace`() {
        expectThat("abc ".trailingWhitespaces).isEqualTo(" ")
    }

    @Test
    fun `should find untypical whitespaces`() {
        expectThat(Unicode.whitespaces.joinToString("").trailingWhitespaces).hasLength(20)
    }

    @Test
    fun `should find only last whitespaces`() {
        expectThat("ab   c  ".trailingWhitespaces).isEqualTo("  ")
    }


    @Test
    fun `should not find non trailing whitespaces`() {
        expectThat("abc  x".trailingWhitespaces).isEqualTo("")
    }
}
