package koodies.io.file

import koodies.io.randomFile
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.time.Now
import koodies.time.toFileTime
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import kotlin.time.Duration
import kotlin.time.minutes

class CreatedKtTest {

    @Test
    fun `should read created`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(randomFile().created.toInstant())
            .isLessThan(Now.plus(Duration.minutes(1)))
            .isGreaterThan(Now.minus(Duration.minutes(1)))
    }

    @Test
    fun `should write created`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile()
        file.created = Now.minus(Duration.minutes(20)).toFileTime()
        expectThat(file.created)
            .isLessThan(Now.plus(Duration.minutes(21)).toFileTime())
            .isGreaterThan(Now.minus(Duration.minutes(21)).toFileTime())
    }
}
