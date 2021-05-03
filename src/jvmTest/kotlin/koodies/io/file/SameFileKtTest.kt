package koodies.io.file

import koodies.text.randomString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.java.exists
import strikt.assertions.isEqualTo


class SameFileKtTest {
    @Test
    fun `should always return same path`() {
        val random = randomString(17)
        expectThat(sameFile(random)).isEqualTo(sameFile(random))
    }

    @Test
    fun `should not implicitly create file`() {
        val random = randomString(17)
        expectThat(sameFile(random)).not { exists() }
    }
}
