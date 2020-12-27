package koodies.io.file

import koodies.io.path.randomFile
import koodies.test.withTempDir
import koodies.time.Now
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import java.nio.file.attribute.FileTime
import kotlin.time.minutes

@Execution(CONCURRENT)
class LastModifiedKtTest {

    @Test
    fun `should read last modified`() = withTempDir {
        expectThat(randomFile().lastModified.toInstant())
            .isLessThan(Now.plus(1.minutes))
            .isGreaterThan(Now.minus(1.minutes))
    }

    @Test
    fun `should write last modified`() = withTempDir {
        val file = randomFile()
        file.lastModified = FileTime.from(Now.minus(20.minutes))
        expectThat(file.lastModified.toInstant())
            .isLessThan(Now.plus(21.minutes))
            .isGreaterThan(Now.minus(21.minutes))
    }
}
