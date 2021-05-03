package koodies.time

import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.attribute.FileTime


class ToFileTimeKtTest {
    @Test
    fun `should return FileType`() {
        val now = Now.instant
        expectThat(now.toFileTime()).isEqualTo(FileTime.from(now))
    }
}
