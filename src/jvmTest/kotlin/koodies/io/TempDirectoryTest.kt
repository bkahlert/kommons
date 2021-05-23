package koodies.io

import koodies.io.path.Defaults.OWNER_ALL_PERMISSIONS
import koodies.io.path.age
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.touch
import koodies.test.UniqueId
import koodies.test.expectThrows
import koodies.test.expecting
import koodies.test.toStringContains
import koodies.test.withTempDir
import koodies.time.days
import koodies.time.hours
import koodies.time.minutes
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.DescribeableBuilder
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEqualTo
import strikt.assertions.size
import strikt.java.exists
import strikt.java.isDirectory
import strikt.java.isRegularFile
import strikt.java.parent
import java.nio.file.Path
import kotlin.time.Duration

class TempDirectoryTest {

    @Test
    fun `should be creatable with Locations object`() {
        expecting { Locations.Temp("com.bkahlert.koodies.app-specific") } that {
            isEqualTo(TempDirectory(Locations.Temp.resolve("com.bkahlert.koodies.app-specific")))
            toStringContains("com.bkahlert.koodies.app-specific")
        }
    }

    @Test
    fun `should throw on non-temp location`() {
        expectThrows<IllegalArgumentException> { TempDirectory(Locations.WorkingDirectory.resolve("directory")) }
    }

    @Test
    fun `should throw if already exists but no directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = resolve("file").also { it.touch() }
        expectThrows<IllegalArgumentException> { TempDirectory(path) }
    }

    private fun Path.temp() = TempDirectory(this)
    private fun Path.temp(minAge: Duration) = TempDirectory(this, minAge)
    private fun Path.temp(maximumFileCount: Int) = TempDirectory(this, maximumFileCount = maximumFileCount)
    private fun Path.temp(minAge: Duration, maximumFileCount: Int) = TempDirectory(this, minAge, maximumFileCount)

    @Test
    fun `should create if not exists`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { temp() } that {
            path.exists()
            path.isDirectory()
        }
    }

    @Test
    fun `should set POSIX permissions to 700`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expecting { temp().path } that {
            permissions.containsExactlyInAnyOrder(OWNER_ALL_PERMISSIONS)
        }
    }

    @Test
    fun `should delete if empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val temporaryDirectory = temp()
        expecting { temporaryDirectory.cleanUp() } that { not { exists() } }
    }

    @Test
    fun `should create directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val temporaryDirectory = temp()
        expecting { temporaryDirectory.tempDir() } that {
            isDirectory()
            parent.isEqualTo(temporaryDirectory.path)
        }
    }

    @Test
    fun `should create file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val temporaryDirectory = temp()
        expecting { temporaryDirectory.tempFile() } that {
            isRegularFile()
            parent.isEqualTo(temporaryDirectory.path)
        }
    }

    @Test
    fun `should keep at most 100 files by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val temporaryDirectory = temp(Duration.ZERO)
        (1..110).forEach { temporaryDirectory.tempFile() }
        expecting { temporaryDirectory.path.listDirectoryEntriesRecursively() } that { size.isEqualTo(110) }
        expecting { temporaryDirectory.cleanUp().listDirectoryEntriesRecursively() } that { size.isEqualTo(100) }
    }

    @Test
    fun `should keep at most specified number of files`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val temporaryDirectory = temp(Duration.ZERO, 5)
        (1..10).forEach { temporaryDirectory.tempFile() }
        expecting { temporaryDirectory.path.listDirectoryEntriesRecursively() } that { size.isEqualTo(10) }
        expecting { temporaryDirectory.cleanUp().listDirectoryEntriesRecursively() } that { size.isEqualTo(5) }
    }

    @Test
    fun `should not delete if less files than maximum`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val temporaryDirectory = temp(100)
        (1..10).forEach { temporaryDirectory.tempFile() }
        expecting { temporaryDirectory.path.listDirectoryEntriesRecursively() } that { size.isEqualTo(10) }
        expecting { temporaryDirectory.cleanUp().listDirectoryEntriesRecursively() } that { size.isEqualTo(10) }
    }

    @Test
    fun `should not delete files younger than 1h by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val temporaryDirectory = temp(0)
        val a = temporaryDirectory.tempFile("a").also { it.age = 30.minutes }
        temporaryDirectory.tempFile("b").also { it.age = 3.hours }
        val c = temporaryDirectory.tempFile("c")
        expecting { temporaryDirectory.cleanUp().listDirectoryEntriesRecursively() } that { containsExactlyInAnyOrder(a, c) }
    }

    @Test
    fun `should not delete files younger than specified age`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val temporaryDirectory = temp(2.hours, 0)
        val a = temporaryDirectory.tempFile("a").also { it.age = 30.minutes }
        val b = temporaryDirectory.tempFile("b").also { it.age = 1.5.hours }
        temporaryDirectory.tempFile("c").also { it.age = 1.days }
        expecting { temporaryDirectory.cleanUp().listDirectoryEntriesRecursively() } that { containsExactlyInAnyOrder(a, b) }
    }

    @Test
    fun `should delete empty directories`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val temporaryDirectory = temp()
        val emptyDir = temporaryDirectory.tempDir("empty")
        val notEmptyDir = temporaryDirectory.tempDir("not-empty").also { it.tempFile() }
        expecting { temporaryDirectory.cleanUp().listDirectoryEntriesRecursively() } that {
            not { contains(emptyDir) }
            contains(notEmptyDir)
        }
    }
}

val Assertion.Builder<TempDirectory>.path: DescribeableBuilder<Path>
    get() = get("path") { path }
