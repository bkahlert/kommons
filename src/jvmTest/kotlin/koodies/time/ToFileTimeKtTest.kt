package koodies.time

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.attribute.FileTime

@Execution(CONCURRENT)
class ToFileTimeKtTest {
    @Test
    fun `should return FileType`() {
        val now = Now.instant
        expectThat(now.toFileTime()).isEqualTo(FileTime.from(now))
    }
}
