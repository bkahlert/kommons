package com.bkahlert.kommons

import com.bkahlert.kommons.test.createAnyFile
import com.bkahlert.kommons.test.junit.testEach
import com.bkahlert.kommons.test.testAll
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.paths.shouldBeADirectory
import io.kotest.matchers.paths.shouldBeAbsolute
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.pathString
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

class SystemLocationsTest {

    @TestFactory fun locations() = testEach(
        SystemLocations.Work,
        SystemLocations.Home,
        SystemLocations.Temp,
        SystemLocations.JavaHome,
    ) {
        it.shouldBeAbsolute()
        it.shouldExist()
        it.shouldBeADirectory()
    }

    @Nested
    inner class WithTempDirectory {

        @Test
        fun `should run inside temp dir`() {
            val tempDir: Path = withTempDirectory {
                this should {
                    it.isSubPathOf(SystemLocations.Temp) shouldBe true
                    it.shouldBeADirectory()
                }
                this
            }
            tempDir.shouldNotExist()
        }
    }

    @Nested
    inner class AutoCleanup {

        @Test
        fun `should delete if empty`(@TempDir tempDir: Path) = testAll {
            tempDir.cleanUp(Duration.ZERO, 0).shouldNotExist()
        }

        @Test
        fun `should keep at most specified number of files`(@TempDir tempDir: Path) = testAll {
            (1..10).forEach { i -> tempDir.createAnyFile("file-$i") }
            tempDir.listDirectoryEntriesRecursively() shouldHaveSize 10
            tempDir.cleanUp(Duration.ZERO, 5).listDirectoryEntriesRecursively() shouldHaveSize 5
        }

        @Test
        fun `should not delete if less files than maximum`(@TempDir tempDir: Path) = testAll {
            (1..10).forEach { i -> tempDir.createAnyFile("file-$i") }
            tempDir.listDirectoryEntriesRecursively() shouldHaveSize 10
            tempDir.cleanUp(Duration.ZERO, 100).listDirectoryEntriesRecursively() shouldHaveSize 10
        }

        @Test
        fun `should not delete files younger than specified age`(@TempDir tempDir: Path) = testAll {
            val a = tempDir.createAnyFile("a").apply { age = 30.minutes }
            val b = tempDir.createAnyFile("b").apply { age = 1.5.hours }
            tempDir.createAnyFile("c").apply { age = 1.days }
            tempDir.cleanUp(2.hours, 0).listDirectoryEntriesRecursively().map { it.pathString }.shouldContainExactlyInAnyOrder(
                a.pathString,
                b.pathString
            )
        }

        @Test
        fun `should delete empty directories`(@TempDir tempDir: Path) = testAll {
            val emptyDir = tempDir.createTempDirectory("empty")
            val file = tempDir.createAnyFile()
            tempDir.cleanUp(2.hours, 0).listDirectoryEntriesRecursively().map { it.pathString } should {
                it shouldNotContain emptyDir.pathString
                it shouldContain file.pathString
            }
        }
    }
}
