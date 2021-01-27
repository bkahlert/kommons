package koodies.time

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.time.hours

class DurationsKtTest {
    @Nested
    inner class ToIntMilliseconds {
        @Test
        fun `should correspond to long value`() {
            val duration = 14.5.hours
            expectThat(duration.toIntMilliseconds().toLong()).isEqualTo(duration.toLongMilliseconds())
        }
    }
}
