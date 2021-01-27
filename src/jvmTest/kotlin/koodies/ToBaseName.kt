package koodies

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.hasLength
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class BaseNameKtTest {

    @Nested
    inner class ToBaseName {

        @Test
        fun `should make first char alphanumeric`() {
            expectThat("-YZ--abc".toBaseName()).isEqualTo("XYZ--abc")
        }

        @Test
        fun `should keep alphanumeric period underscore and dash`() {
            expectThat("aB3._-xx".toBaseName()).isEqualTo("aB3._-xx")
        }

        @Test
        fun `should replace whitespace with dash`() {
            expectThat("abc- \n\t\r".toBaseName()).isEqualTo("abc-----")
        }

        @Test
        fun `should replace with underscore by default`() {
            expectThat("a𝕓☰👋⻦𐦀︎".toBaseName()).isEqualTo("a_______")
        }

        @Test
        fun `should fill up to min length`() {
            expectThat("abc".toBaseName(6)).hasLength(6)
        }

        @Test
        fun `should fill up to 8 chars by default`() {
            expectThat("abc".toBaseName()).hasLength(8)
        }

        @Test
        fun `should create random string in case of null`() {
            expectThat(null.toBaseName()).hasLength(8)
        }
    }
}
