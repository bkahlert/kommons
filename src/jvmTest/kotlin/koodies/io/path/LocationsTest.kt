package koodies.io.path

import koodies.io.path.Locations.ls
import koodies.test.Fixtures.copyTo
import koodies.test.HtmlFile
import koodies.test.TextFile
import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.exists
import strikt.assertions.isEmpty

@Execution(CONCURRENT)
class LocationsTest {

    @Nested
    inner class Ls {
        @Test
        fun `should list matching files`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            listOf(resolve("a.html"), resolve("b/c.html"), resolve("b/d.html")).map { HtmlFile.copyTo(it) }
            listOf(resolve("a.txt"), resolve("b/c.txt"), resolve("b/d.txt")).map { TextFile.copyTo(it) }
            expectThat(ls("**/*.html")).containsExactly(resolve("b/c.html"), resolve("b/d.html"))
        }

        @Test
        fun `should return empty list on no matching files`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            listOf(resolve("a.txt"), resolve("b/c.txt"), resolve("b/d.txt")).map { TextFile.copyTo(it) }
            expectThat(ls("**/*.html")).isEmpty()
        }
    }

    @Test
    fun `should resolve WorkingDirectory`() {
        expectThat(Locations.WorkingDirectory).exists()
    }

    @Test
    fun `should resolve HomeDirectory`() {
        expectThat(Locations.HomeDirectory).exists()
    }

    @Test
    fun `should resolve Temp`() {
        expectThat(Locations.Temp).exists()
    }
}
