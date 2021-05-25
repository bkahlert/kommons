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

class AutoCleaningDirectoryTest {

    @Test
    fun `should be sub directory`() {
        expecting { Locations.Temp.autoCleaning("com.bkahlert.koodies.app-specific") } that {
            isEqualTo(AutoCleaningDirectory(Locations.Temp.resolve("com.bkahlert.koodies.app-specific")))
            toStringContains("com.bkahlert.koodies.app-specific")
        }
    }

    @Test
    fun `should throw on non-temp location`() {
        expectThrows<IllegalArgumentException> { AutoCleaningDirectory(Locations.WorkingDirectory.resolve("directory")) }
    }

    @Test
    fun `should throw if already exists but no directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = resolve("file").also { it.touch() }
        expectThrows<IllegalArgumentException> { AutoCleaningDirectory(path) }
    }

    @Test
    fun `should create if not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { autoCleaning(uniqueId.value) } that {
            path.exists()
            path.isDirectory()
        }
    }

    @Test
    fun `should set POSIX permissions to 700`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { autoCleaning(uniqueId.value).path } that {
            permissions.containsExactlyInAnyOrder(OWNER_ALL_PERMISSIONS)
        }
    }

    @Test
    fun `should not delete files younger than 1h by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { autoCleaning("test") } that { keepAge.isEqualTo(1.hours) }
    }

    @Test
    fun `should keep at most 100 files by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { autoCleaning("test") } that { keepCount.isEqualTo(100) }
    }
}

val Assertion.Builder<AutoCleaningDirectory>.path: DescribeableBuilder<Path>
    get() = get("path") { path }
val Assertion.Builder<AutoCleaningDirectory>.keepAge: DescribeableBuilder<Duration>
    get() = get("keep age") { keepAge }
val Assertion.Builder<AutoCleaningDirectory>.keepCount: DescribeableBuilder<Int>
    get() = get("keep count") { keepCount }
