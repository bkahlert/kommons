package koodies.builder

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD

@Execution(SAME_THREAD)
class ListBuilderTest {

    @Test
    fun `should build`() {
        val list = ListBuilder.provideDefault<String>().invoke { +"d" }
    }
}
