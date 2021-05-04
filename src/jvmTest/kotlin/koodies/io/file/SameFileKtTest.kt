package koodies.io.file

import koodies.text.randomString
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.java.exists

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
