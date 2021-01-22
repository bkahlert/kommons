package koodies

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expect
import strikt.assertions.isEqualTo

@Execution(SAME_THREAD)
class LazyFunctionTest {
    private var callCounter: Int = 0
    private val fn by constrained { ++callCounter }

    @Test
    fun `should call implementation only once`() {
        expect {
            that(fn()).isEqualTo(1)
            that(fn()).isEqualTo(1)
            that(callCounter).isEqualTo(1)
        }
    }
}
