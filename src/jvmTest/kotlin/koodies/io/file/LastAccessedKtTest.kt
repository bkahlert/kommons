package koodies.io.file

import koodies.io.randomFile
import koodies.junit.UniqueId
import koodies.test.withTempDir
import koodies.time.Now
import koodies.time.minutes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.nio.file.attribute.FileTime

class LastAccessedKtTest {

    @Test
    fun `should read last accessed`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(randomFile().lastAccessed.toInstant())
            .isLessThan(Now + 1.minutes)
            .isGreaterThan(Now - 1.minutes)
    }

    @Test
    fun `should write last accessed`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile()
        file.lastAccessed = FileTime.from(Now - 20.minutes)
        expectThat(file.lastAccessed.toInstant())
            .isLessThan(Now + 21.minutes)
            .isGreaterThan(Now - 21.minutes)
    }
}
