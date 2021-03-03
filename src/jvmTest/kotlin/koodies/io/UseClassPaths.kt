package koodies.io

import koodies.io.file.WrappedPath
import koodies.io.path.asPath
import koodies.io.path.copyToDirectory
import koodies.io.path.isCopyOf
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.randomDirectory
import koodies.io.path.randomPath
import koodies.test.FixturePath61C285F09D95930D0AE298B00AF09F918B0A.fixtureContent
import koodies.test.UniqueId
import koodies.test.testWithTempDir
import koodies.test.toStringContains
import koodies.test.withTempDir
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.filter
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import strikt.assertions.isGreaterThanOrEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isTrue
import strikt.assertions.size
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.ReadOnlyFileSystemException
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes
import kotlin.io.path.readText

@Execution(CONCURRENT)
class ClassPathsKtTest {

    @Nested
    inner class UseClassPaths {

        @Test fun `should map root with no provided path`() {
            expectThat(useClassPaths("") {
                listDirectoryEntriesRecursively().any { it.fileName.toString().endsWith(".class") }
            }).filter { true }.size.isGreaterThan(2)
        }

        @Test
        fun `should map resource on matching path`() {
            expectThat(useClassPaths("junit-platform.properties") { readText() }).all { contains("LessUglyDisplayNameGenerator") }
        }

        @Test
        fun `should map resource on non-matching path`() {
            expectThat(useClassPaths("invalid.file") { this }).isEmpty()
        }

        @TestFactory
        fun `should support different notations`() = listOf(
            "junit-platform.properties",
            "/junit-platform.properties",
            "classpath:junit-platform.properties",
            "classpath:/junit-platform.properties",
            "ClassPath:junit-platform.properties",
            "ClassPath:/junit-platform.properties",
        ).map { dynamicTest(it) { expectThat(useClassPaths(it) { readText() }).all { contains("LessUglyDisplayNameGenerator") } } }

        @Test
        fun `should map read-only root`() {
            expectThat(useClassPaths("") { this::class.qualifiedName!! }).filter { it.contains("ReadOnly") }.size.isGreaterThan(2)
        }

        @Test
        fun `should map read-only resource`() {
            expectThat(useClassPaths("junit-platform.properties") { this::class.qualifiedName!! }).filter { it.contains("ReadOnly") }.size.isGreaterThanOrEqualTo(
                1)
        }

        @Test
        fun `should list read-only resources`() {
            expectThat(useClassPaths("") {
                listDirectoryEntries().map { it::class.qualifiedName }.toList()
            }).filter { it.all { pathType -> pathType!!.contains("ReadOnly") } }.size.isGreaterThanOrEqualTo(2)
        }

        @TestFactory
        fun `should throw on write access`(uniqueId: UniqueId) = mapOf<String, Path.(Path) -> Unit>(
            "outputStream" to { Files.newBufferedWriter(it) },
            "move" to { Files.move(it, this) },
            "delete" to { Files.delete(it) },
        ).testWithTempDir(uniqueId) { (_, operation) ->
            useClassPaths("try.it") {
                expectThat(exists()).isTrue()
                expectCatching {
                    val target = this
                    target.operation(this@useClassPaths)
                }.isFailure().isA<ReadOnlyFileSystemException>()
                expectThat(exists()).isTrue()
                expectThat(readBytes()).isEqualTo(fixtureContent)
            }
        }
    }


    @Nested
    inner class UseClassPath {

        @Test
        fun `should map root with no provided path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(useClassPath("") {
                listDirectoryEntriesRecursively().any { it.fileName.toString().endsWith(".class") }
            }).isTrue()
        }

        @Test
        fun `should map resource on matching path`() {
            expectThat(useClassPath("junit-platform.properties") { readText() }).toStringContains("LessUglyDisplayNameGenerator")
        }

        @Test
        fun `should map resource on non-matching path`() {
            expectThat(useClassPath("invalid.file") { this }).isNull()
        }

        @TestFactory
        fun `should support different notations`() = listOf(
            "junit-platform.properties",
            "/junit-platform.properties",
            "classpath:junit-platform.properties",
            "classpath:/junit-platform.properties",
            "ClassPath:junit-platform.properties",
            "ClassPath:/junit-platform.properties",
        ).map { dynamicTest(it) { expectThat(useClassPath(it) { readText() }).toStringContains("LessUglyDisplayNameGenerator") } }

        @Test
        fun `should map read-only root`() {
            expectThat(useClassPath("") { this::class.qualifiedName }).isNotNull().contains("ReadOnly")
        }

        @Test
        fun `should map read-only resource`() {
            expectThat(useClassPath("junit-platform.properties") { this::class.qualifiedName }).isNotNull().contains("ReadOnly")
        }

        @Test
        fun `should list read-only resources`() {
            expectThat(useClassPath("") { listDirectoryEntries().mapNotNull { it::class.qualifiedName }.toList() }).isNotNull().all { contains("ReadOnly") }
        }

        @TestFactory
        fun `should throw on write access`(uniqueId: UniqueId) = mapOf<String, Path.(Path) -> Unit>(
            "outputStream" to { Files.newBufferedWriter(it) },
            "move" to { Files.move(it, randomPath()) },
            "delete" to { Files.delete(it) },
        ).map { (name, operation) ->
            dynamicTest(name) {
                useClassPath("try.it") {
                    expectThat(exists()).isTrue()
                    expectCatching {
                        withTempDir(uniqueId) {
                            val target = this
                            target.operation(this@useClassPath)
                        }
                    }.isFailure().isA<ReadOnlyFileSystemException>()
                    expectThat(exists()).isTrue()
                    expectThat(readBytes()).isEqualTo(fixtureContent)
                }
            }
        }

        @Nested
        inner class UsingDelegatesProperty {
            @Test
            fun `should copy class path directory`() {
                val fixtures by classPath("img")
                useClassPath("img") {
                    fixtures.copyToDirectory(randomDirectory("copy1")) to copyToDirectory(randomDirectory("copy2"))
                }?.also { (copy1, copy2) ->
                    expectThat(copy1) {
                        size.isGreaterThan(0)
                        isCopyOf(fixtures)
                        isCopyOf(copy2)
                    }
                }
            }
        }
    }

    @Nested
    inner class UseRequiredClassPath {

        @Test
        fun `should map resource on matching path`() {
            expectThat(useRequiredClassPath("junit-platform.properties") { readText() }).toStringContains("LessUglyDisplayNameGenerator")
        }

        @Test
        fun `should throw on non-matching path`() {
            expectCatching { useRequiredClassPath("invalid.file") { this } }.isFailure().isA<NoSuchFileException>()
        }
    }

    @Nested
    inner class IsClassPath {

        @Nested
        inner class CharSequenceBased {
            @Test
            fun `should return true on class path`() {
                expectThat("classpath:path/file").isClassPath()
            }


            @Test
            fun `should return false on regular path`() {
                expectThat("path/file").not { isClassPath() }
            }


            @Test
            fun `should return false on illegal path`() {
                expectThat("!!!").not { isClassPath() }
            }
        }

        @Nested
        inner class PathBased {

            @Test
            fun `should return false on regular path`() {
                expectThat("path/file".asPath()).not { isClassPath() }
            }


            @Test
            fun `should return false on illegal path`() {
                expectThat("!!!".asPath()).not { isClassPath() }
            }
        }
    }
}


@JvmName("charSequenceBasedIsClassPath")
fun <T : CharSequence> Assertion.Builder<T>.isClassPath() =
    assert("is class path") { value ->
        when (value.startsWith("classpath" + ":")) {
            true -> pass()
            else -> fail("is no class path")
        }
    }

fun <T : Path> Assertion.Builder<T>.isClassPath() =
    assert("is class path") { value ->
        when (value is WrappedPath) {
            true -> pass()
            else -> fail("is no class path")
        }
    }
