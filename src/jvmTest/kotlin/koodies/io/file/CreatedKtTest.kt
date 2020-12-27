package koodies.io.file

import koodies.io.path.randomFile
import koodies.test.withTempDir
import koodies.time.Now
import koodies.time.toFileTime
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import kotlin.time.minutes

@Execution(CONCURRENT)
class CreatedKtTest {

    @Test
    fun `should read created`() = withTempDir {
        expectThat(randomFile().created.toInstant())
            .isLessThan(Now.plus(1.minutes))
            .isGreaterThan(Now.minus(1.minutes))
    }

    @Test
    fun `should write created`() = withTempDir {
        val file = randomFile()
        file.created = Now.minus(20.minutes).toFileTime()
        expectThat(file.created)
            .isLessThan(Now.plus(21.minutes).toFileTime())
            .isGreaterThan(Now.minus(21.minutes).toFileTime())
    }
}

