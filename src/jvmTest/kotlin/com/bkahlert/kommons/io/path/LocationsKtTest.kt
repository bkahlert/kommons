package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.io.copyTo
import com.bkahlert.kommons.io.path.PosixFilePermissions.OWNER_ALL_PERMISSIONS
import com.bkahlert.kommons.test.HtmlFixture
import com.bkahlert.kommons.test.TextFixture
import com.bkahlert.kommons.test.expecting
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.time.days
import com.bkahlert.kommons.time.hours
import com.bkahlert.kommons.time.minutes
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
import strikt.assertions.isNotNull
import strikt.assertions.isTrue
import strikt.assertions.size
import strikt.java.exists
import strikt.java.isDirectory
import strikt.java.isRegularFile
import strikt.java.parent
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermission
import kotlin.io.path.exists
import kotlin.io.path.getPosixFilePermissions
import kotlin.time.Duration

class LocationsKtTest {

    @TestFactory
    fun paths() = testEach(
        Locations.work,
        Locations.home,
        Locations.temp,
    ) { path ->
        expecting { path.isAbsolute } that { isTrue() }
        expecting { path.exists() } that { isTrue() }
    }

    @Test
    fun `should resolve HomeDirectory`() {
        expectThat(Locations.home).exists()
    }

    @Test
    fun `should resolve Temp`() {
        expectThat(Locations.temp).exists()
    }

    @Nested
    inner class IsInside {

        @Test
        fun `should return true if child`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(resolve("child")).isInside(this)
        }

        @Test
        fun `should return true if descendent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(resolve("child1/child2")).isInside(this)
        }

        @Test
        fun `should return true if path is obscure`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(resolve("child1/../child2")).isInside(this)
        }

        @Test
        fun `should return true if same`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(this).isInside(this)
        }

        @Test
        fun `should return false if not inside`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(Locations.home).not { isInside(this@withTempDir) }
        }
    }

    @Nested
    inner class CreateParentDirectories {

        @Test
        fun `should create missing directories`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val file = resolve("some/dir/some/file")
            expectThat(file.createParentDirectories()).parent.isNotNull() and {
                exists()
                isDirectory()
                isEmpty()
            }
        }
    }

    @Nested
    inner class RandomPath {

        @Test
        fun `should create inside receiver path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomPath()).isInside(this)
        }

        @Test
        fun `should not exist`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomPath()).not { exists() }
        }
    }

    @Nested
    inner class RandomDirectory {

        @Test
        fun `should create inside receiver path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomDirectory()).isInside(this)
        }

        @Test
        fun `should create directory`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomDirectory()).isDirectory()
        }

        @Test
        fun `should create directory inside non-existent parent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomPath().randomDirectory()).isDirectory()
        }
    }

    @Nested
    inner class RandomFile {

        @Test
        fun `should create inside receiver path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomFile()).isInside(this)
        }

        @Test
        fun `should create regular file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomFile()).isRegularFile()
        }

        @Test
        fun `should create regular file inside non-existent parent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(randomPath().randomFile()).isRegularFile()
        }
    }

    @Nested
    inner class TempDirectory {

        @Test
        fun `should create inside temp directory`() {
            expectThat(tempDir().deleteOnExit()).isInside(Locations.temp)
        }

        @Test
        fun `should create directory`() {
            expectThat(tempDir().deleteOnExit()).isDirectory()
        }

        @Test
        fun `should have exclusive rights`() {
            expectThat(tempDir().deleteOnExit()).hasPosixPermissions(OWNER_ALL_PERMISSIONS)
        }

        @Test
        fun `should create directory inside receiver path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(tempDir())
                .isInside(this)
                .isDirectory()
                .hasPosixPermissions(OWNER_ALL_PERMISSIONS)
        }

        @Test
        fun `should create directory inside non-existent parent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val nonExistentParent = randomPath()
            expectThat(nonExistentParent.tempDir())
                .isInside(nonExistentParent)
                .isDirectory()
                .hasPosixPermissions(OWNER_ALL_PERMISSIONS)
        }
    }

    @Nested
    inner class TempFile {

        @Test
        fun `should create inside temp directory`() {
            expectThat(tempFile().deleteOnExit()).isInside(Locations.temp)
        }

        @Test
        fun `should create file`() {
            expectThat(tempFile().deleteOnExit()).isRegularFile()
        }

        @Test
        fun `should have exclusive rights`() {
            expectThat(tempFile().deleteOnExit()).hasPosixPermissions(OWNER_ALL_PERMISSIONS)
        }

        @Test
        fun `should create file inside receiver path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(tempFile())
                .isInside(this)
                .isRegularFile()
                .hasPosixPermissions(OWNER_ALL_PERMISSIONS)
        }

        @Test
        fun `should create file inside non-existent parent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val nonExistentParent = randomPath()
            expectThat(nonExistentParent.tempFile())
                .isInside(nonExistentParent)
                .isRegularFile()
                .hasPosixPermissions(OWNER_ALL_PERMISSIONS)
        }
    }

    @Nested
    inner class RunWithTempDir {

        @Test
        fun `should run inside temp dir`() {
            val tempDir: Path = runWithTempDir {
                expectThat(this) {
                    isDirectory()
                    isInside(Locations.temp)
                }
                this
            }
            expectThat(tempDir).not { exists() }
        }
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


fun Builder<Path>.hasPosixPermissions(permissions: Set<PosixFilePermission>): Builder<Path> =
    assert("has POSIX permissions") {
        when (it.getPosixFilePermissions()) {
            permissions -> pass()
            else -> fail()
        }
    }
