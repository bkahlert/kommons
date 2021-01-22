package koodies

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expect
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class ConstrainedFunctionTest {
    private var callCounter: Int = 0
    private val fn by callable(atMost = 2) { ++callCounter }

    @Test
    fun `should call implementation only twice`() {
        expect {
            that(fn()).isEqualTo(1)
            that(fn()).isEqualTo(2)
            that(fn()).isEqualTo(2)
            that(callCounter).isEqualTo(2)
        }
    }
}
