package koodies.io.file

import koodies.io.randomFile
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.time.Now
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.nio.file.attribute.FileTime
import kotlin.time.Duration
import kotlin.time.minutes

class LastModifiedKtTest {

    @Test
    fun `should read last modified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(randomFile().lastModified.toInstant())
            .isLessThan(Now.plus(Duration.minutes(1)))
            .isGreaterThan(Now.minus(Duration.minutes(1)))
    }

    @Test
    fun `should write last modified`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile()
        file.lastModified = FileTime.from(Now.minus(Duration.minutes(20)))
        expectThat(file.lastModified.toInstant())
            .isLessThan(Now.plus(Duration.minutes(21)))
            .isGreaterThan(Now.minus(Duration.minutes(21)))
    }
}
