package koodies

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isNull

class ResultsKtTest {

    @Nested
    inner class GetOrException {

        @Test
        fun `should return result`() {
            val (result, exception) = kotlin.runCatching { 42 }.getOrException()
            expect {
                that(result).isEqualTo(42)
                that(exception).isNull()
            }
        }

        @Test
        fun `should return exception`() {
            val (result, exception) = kotlin.runCatching { throw IllegalStateException() }.getOrException()
            expect {
                that(result).isNull()
                that(exception).isA<IllegalStateException>()
            }
        }
    }
}
