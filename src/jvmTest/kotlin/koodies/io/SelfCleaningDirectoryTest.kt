package koodies.io

import koodies.io.path.Defaults.OWNER_ALL_PERMISSIONS
import koodies.io.path.touch
import koodies.test.UniqueId
import koodies.test.expectThrows
import koodies.test.expecting
import koodies.test.toStringContains
import koodies.test.withTempDir
import koodies.time.hours
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.java.exists
import strikt.java.isDirectory
import java.nio.file.Path
import kotlin.time.Duration

class SelfCleaningDirectoryTest {

    @Test
    fun `should be sub directory`() {
        expecting { Locations.Temp.selfCleaning("com.bkahlert.koodies.app-specific") } that {
            isEqualTo(SelfCleaningDirectory(Locations.Temp.resolve("com.bkahlert.koodies.app-specific")))
            toStringContains("com.bkahlert.koodies.app-specific")
        }
    }

    @Test
    fun `should throw on non-temp location`() {
        expectThrows<IllegalArgumentException> { SelfCleaningDirectory(Locations.WorkingDirectory.resolve("directory")) }
    }

    @Test
    fun `should throw if already exists but no directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = resolve("file").also { it.touch() }
        expectThrows<IllegalArgumentException> { SelfCleaningDirectory(path) }
    }

    @Test
    fun `should create if not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { selfCleaning(uniqueId.value) } that {
            path.exists()
            path.isDirectory()
        }
    }

    @Test
    fun `should set POSIX permissions to 700`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { selfCleaning(uniqueId.value).path } that {
            permissions.containsExactlyInAnyOrder(OWNER_ALL_PERMISSIONS)
        }
    }

    @Test
    fun `should not delete files younger than 1h by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { selfCleaning("test") } that { keepAge.isEqualTo(1.hours) }
    }

    @Test
    fun `should keep at most 100 files by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { selfCleaning("test") } that { keepCount.isEqualTo(100) }
    }
}

val Assertion.Builder<SelfCleaningDirectory>.path: DescribeableBuilder<Path>
    get() = get("path") { path }
val Assertion.Builder<SelfCleaningDirectory>.keepAge: DescribeableBuilder<Duration>
    get() = get("keep age") { keepAge }
val Assertion.Builder<SelfCleaningDirectory>.keepCount: DescribeableBuilder<Int>
    get() = get("keep count") { keepCount }
