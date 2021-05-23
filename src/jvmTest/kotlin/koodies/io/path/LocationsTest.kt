package koodies.io.path

import koodies.io.Locations
import koodies.io.Locations.ls
import koodies.io.copyTo
import koodies.io.file.pathString
import koodies.io.path
import koodies.test.HtmlFixture
import koodies.test.TextFixture
import koodies.test.UniqueId
import koodies.test.testEach
import koodies.test.toStringContains
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isTrue
import strikt.assertions.last
import strikt.java.exists
import java.nio.file.Path
import kotlin.io.path.exists

class LocationsTest {

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

    @Test
    fun `should resolve InternalTemp`() {
        expectThat(Locations.InternalTemp) {
            path.toStringContains("koodies")
            path.isInside(Locations.Temp)
        }
    }

    @Test
    fun `should resolve ExecTemp`() {
        expectThat(Locations.ExecTemp) {
            path.last().pathString.isEqualTo("exec")
            path.isInside(Locations.InternalTemp.path)
        }
    }

    @Test
    fun `should resolve FilesTemp`() {
        expectThat(Locations.FilesTemp) {
            path.last().pathString.isEqualTo("files")
            path.isInside(Locations.InternalTemp.path)
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
