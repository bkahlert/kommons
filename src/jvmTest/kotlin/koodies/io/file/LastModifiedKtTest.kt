package koodies.io.file

import koodies.io.path.randomFile
import koodies.test.junit.UniqueId
import koodies.test.withTempDir
import koodies.time.Now
import koodies.time.minutes
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.nio.file.attribute.FileTime

class LastModifiedKtTest {

    @Test
    fun `should read last modified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(randomFile().lastModified.toInstant())
            .isLessThan(Now + 1.minutes)
            .isGreaterThan(Now - 1.minutes)
    }

    @Test
    fun `should write last modified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile()
        file.lastModified = FileTime.from(Now - 20.minutes)
        expectThat(file.lastModified.toInstant())
            .isLessThan(Now + 21.minutes)
            .isGreaterThan(Now - 21.minutes)
    }
}
