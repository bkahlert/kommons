package koodies.io

import koodies.io.path.PosixFilePermissions.OWNER_ALL_PERMISSIONS
import koodies.io.path.touch
import koodies.junit.UniqueId
import koodies.test.toStringContains
import koodies.test.withTempDir
import koodies.time.hours
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.java.exists
import strikt.java.isDirectory
import java.nio.file.Path
import kotlin.time.Duration

class SelfCleaningDirectoryTest {

    @Test
    fun `should take receiver directory`() {
        expectThat(Locations.Temp.resolve("koodies.app-specific").selfCleaning()) {
            isEqualTo(SelfCleaningDirectory(Locations.Temp.resolve("koodies.app-specific")))
            toStringContains("koodies.app-specific")
        }
    }

    @Test
    fun `should create missing directories`() {
        expectThat(Locations.Temp.resolve("koodies.app-specific/A/B/C").selfCleaning()) {
            isEqualTo(SelfCleaningDirectory(Locations.Temp.resolve("koodies.app-specific/A/B/C")))
            toStringContains("koodies.app-specific/A/B/C")
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
        expectThat(resolve(uniqueId.value).selfCleaning()) {
            path.exists()
            path.isDirectory()
        }
    }

    @Test
    fun `should set POSIX permissions to 700`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(resolve(uniqueId.value).selfCleaning().path) {
            permissions.containsExactlyInAnyOrder(OWNER_ALL_PERMISSIONS)
        }
    }

    @Test
    fun `should not delete files younger than 1h by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(selfCleaning()) { keepAge.isEqualTo(1.hours) }
    }

    @Test
    fun `should keep at most 100 files by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(selfCleaning()) { keepCount.isEqualTo(100) }
    }
}

val Assertion.Builder<SelfCleaningDirectory>.path: DescribeableBuilder<Path>
    get() = get("path") { path }
val Assertion.Builder<SelfCleaningDirectory>.keepAge: DescribeableBuilder<Duration>
    get() = get("keep age") { keepAge }
val Assertion.Builder<SelfCleaningDirectory>.keepCount: DescribeableBuilder<Int>
    get() = get("keep count") { keepCount }
