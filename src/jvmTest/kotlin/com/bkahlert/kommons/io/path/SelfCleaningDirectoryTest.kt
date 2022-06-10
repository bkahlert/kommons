package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons.io.path.PosixFilePermissions.OWNER_ALL_PERMISSIONS
import com.bkahlert.kommons.io.permissions
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.toStringContains
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.time.hours
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
    fun `should create missing directories`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(resolve("kommons/a/b/c").selfCleaning()) {
            isEqualTo(SelfCleaningDirectory(resolve("kommons/a/b/c")))
            toStringContains("kommons/a/b/c")
        }
    }

    @Test
    fun `should throw on non-temp location`() {
        expectThrows<IllegalArgumentException> { SelfCleaningDirectory(SystemLocations.Work.resolve("directory")) }
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
