package koodies.io

import koodies.io.path.age
import koodies.io.path.isInside
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.junit.UniqueId
import koodies.test.HtmlFixture
import koodies.test.TextFixture
import koodies.test.expecting
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.time.days
import koodies.time.hours
import koodies.time.minutes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.size
import strikt.java.exists
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.time.Duration

class LocationsKtTest {

    @TestFactory
    fun paths() = testEach(
        Locations.WorkingDirectory,
        Locations.HomeDirectory,
        Locations.Temp,
    ) { path ->
        expecting { path.isAbsolute } that { isTrue() }
        expecting { path.exists() } that { isTrue() }
    }

    @Test
    fun `should resolve HomeDirectory`() {
        expectThat(Locations.HomeDirectory).exists()
    }

    @Test
    fun `should resolve Temp`() {
        expectThat(Locations.Temp).exists()
    }

    @Nested
    inner class Ls {

        @Test
        fun `should list matching files`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            listOf(resolve("a.html"), resolve("b/c.html"), resolve("b/d.html")).map { HtmlFixture.copyTo(it) }
            listOf(resolve("a.txt"), resolve("b/c.txt"), resolve("b/d.txt")).map { TextFixture.copyTo(it) }
            expectThat(ls("**/*.html")).containsExactly(resolve("b/c.html"), resolve("b/d.html"))
        }

        @Test
        fun `should return empty list on no matching files`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            listOf(resolve("a.txt"), resolve("b/c.txt"), resolve("b/d.txt")).map { TextFixture.copyTo(it) }
            expectThat(ls("**/*.html")).isEmpty()
        }
    }

    @Nested
    inner class AutoCleanup {

        @Test
        fun `should delete if empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expecting { cleanUp(Duration.ZERO, 0) } that { not { exists() } }
        }

        @Test
        fun `should keep at most specified number of files`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            (1..10).forEach { _ -> tempFile() }
            expecting { listDirectoryEntriesRecursively() } that { size.isEqualTo(10) }
            expecting { cleanUp(Duration.ZERO, 5).listDirectoryEntriesRecursively() } that { size.isEqualTo(5) }
        }

        @Test
        fun `should not delete if less files than maximum`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            (1..10).forEach { _ -> tempFile() }
            expecting { listDirectoryEntriesRecursively() } that { size.isEqualTo(10) }
            expecting { cleanUp(Duration.ZERO, 100).listDirectoryEntriesRecursively() } that { size.isEqualTo(10) }
        }

        @Test
        fun `should not delete files younger than specified age`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val a = tempFile("a").also { it.age = 30.minutes }
            val b = tempFile("b").also { it.age = 1.5.hours }
            tempFile("c").also { it.age = 1.days }
            expecting { cleanUp(2.hours, 0).listDirectoryEntriesRecursively() } that { containsExactlyInAnyOrder(a, b) }
        }

        @Test
        fun `should delete empty directories`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val emptyDir = tempDir("empty")
            val file = tempFile()
            expecting { cleanUp(2.hours, 0).listDirectoryEntriesRecursively() } that {
                not { contains(emptyDir) }
                contains(file)
            }
        }
    }
}

fun Builder<Path>.isInside(path: Path): Builder<Path> =
    assert("is inside $path") {
        when (it.isInside(path)) {
            true -> pass()
            false -> fail()
        }
    }
