package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.io.path.PosixFilePermissions.OWNER_ALL_PERMISSIONS
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
import strikt.assertions.containsExactlyInAnyOrder
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

    @Nested
    inner class DefaultLocations {
        @TestFactory
        fun paths() = testEach(
            Locations.Default.Work,
            Locations.Default.Home,
            Locations.Default.Temp,
        ) { path ->
            expecting { path.isAbsolute } that { isTrue() }
            expecting { path.exists() } that { isTrue() }
        }

        @Test
        fun `should resolve HomeDirectory`() {
            expectThat(Locations.Default.Home).exists()
        }

        @Test
        fun `should resolve Temp`() {
            expectThat(Locations.Default.Temp).exists()
        }
    }

    @Nested
    inner class IsSubPathOf {

        @Test
        fun `should return true if child`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(resolve("child")).isSubPathOf(this)
        }

        @Test
        fun `should return true if descendent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(resolve("child1/child2")).isSubPathOf(this)
        }

        @Test
        fun `should return true if path is obscure`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(resolve("child1/../child2")).isSubPathOf(this)
        }

        @Test
        fun `should return true if same`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(this).isSubPathOf(this)
        }

        @Test
        fun `should return false if not inside`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(Locations.Default.Home).not { isSubPathOf(this@withTempDir) }
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
            expectThat(randomPath()).isSubPathOf(this)
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
            expectThat(randomDirectory()).isSubPathOf(this)
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
            expectThat(randomFile()).isSubPathOf(this)
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
            expectThat(tempDir().deleteOnExit()).isSubPathOf(Locations.Default.Temp)
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
                .isSubPathOf(this)
                .isDirectory()
                .hasPosixPermissions(OWNER_ALL_PERMISSIONS)
        }

        @Test
        fun `should create directory inside non-existent parent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val nonExistentParent = randomPath()
            expectThat(nonExistentParent.tempDir())
                .isSubPathOf(nonExistentParent)
                .isDirectory()
                .hasPosixPermissions(OWNER_ALL_PERMISSIONS)
        }
    }

    @Nested
    inner class TempFile {

        @Test
        fun `should create inside temp directory`() {
            expectThat(tempFile().deleteOnExit()).isSubPathOf(Locations.Default.Temp)
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
                .isSubPathOf(this)
                .isRegularFile()
                .hasPosixPermissions(OWNER_ALL_PERMISSIONS)
        }

        @Test
        fun `should create file inside non-existent parent`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val nonExistentParent = randomPath()
            expectThat(nonExistentParent.tempFile())
                .isSubPathOf(nonExistentParent)
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
                    isSubPathOf(Locations.Default.Temp)
                }
                this
            }
            expectThat(tempDir).not { exists() }
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

fun Builder<Path>.isSubPathOf(path: Path): Builder<Path> =
    assert("is inside $path") {
        when (it.isSubPathOf(path)) {
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
