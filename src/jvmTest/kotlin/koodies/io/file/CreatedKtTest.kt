package koodies.io.file

import koodies.io.randomFile
import koodies.junit.UniqueId
import koodies.test.withTempDir
import koodies.time.Now
import koodies.time.minutes
import koodies.time.toFileTime
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan

class CreatedKtTest {

    @Test
    fun `should read created`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(randomFile().created.toInstant())
            .isLessThan(Now + 1.minutes)
            .isGreaterThan(Now - 1.minutes)
    }

    @Test
    fun `should write created`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val file = randomFile()
        file.created = (Now - 20.minutes).toFileTime()
        expectThat(file.created)
            .isLessThan((Now + 21.minutes).toFileTime())
            .isGreaterThan((Now - 21.minutes).toFileTime())
    }
}
