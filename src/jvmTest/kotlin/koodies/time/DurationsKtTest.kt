package koodies.time

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import kotlin.time.hours

@Execution(SAME_THREAD)
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
