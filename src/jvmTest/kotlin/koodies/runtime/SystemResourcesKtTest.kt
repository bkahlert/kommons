package koodies.runtime

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class SystemResourcesKtTest {

    @Test
    fun `should resolve to valid URL`() {
        expectThat("junit-platform.properties".asSystemResourceUrl().readBytes().size).isEqualTo(871)
    }
}
