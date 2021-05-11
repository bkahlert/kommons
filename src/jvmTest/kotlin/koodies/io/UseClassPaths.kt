package koodies.io

import koodies.io.file.WrappedPath
import koodies.io.path.asPath
import koodies.io.path.copyToDirectory
import koodies.io.path.isCopyOf
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.randomDirectory
import koodies.io.path.randomPath
import koodies.test.Fixture61C285F09D95930D0AE298B00AF09F918B0A
import koodies.test.Fixture61C285F09D95930D0AE298B00AF09F918B0A.data
import koodies.test.Fixture61C285F09D95930D0AE298B00AF09F918B0A.text
import koodies.test.UniqueId
import koodies.test.testEach
import koodies.test.tests
import koodies.test.withTempDir
import koodies.unit.Size
import koodies.unit.size
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
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
            expectThat(useClassPaths(Fixture61C285F09D95930D0AE298B00AF09F918B0A.path) { readText() }).all { isEqualTo(text) }
        }

        @Test
        fun `should map resource on non-matching path`() {
            expectThat(useClassPaths("invalid.file") { this }).isEmpty()
        }

        @TestFactory
        fun `should support different notations`() = testEach(
            Fixture61C285F09D95930D0AE298B00AF09F918B0A.path,
            "/${Fixture61C285F09D95930D0AE298B00AF09F918B0A.path}",
            "classpath:${Fixture61C285F09D95930D0AE298B00AF09F918B0A.path}",
            "classpath:/${Fixture61C285F09D95930D0AE298B00AF09F918B0A.path}",
            "ClassPath:${Fixture61C285F09D95930D0AE298B00AF09F918B0A.path}",
            "ClassPath:/${Fixture61C285F09D95930D0AE298B00AF09F918B0A.path}",
        ) { expecting { useClassPaths(this) { readText() } } that { all { isEqualTo(text) } } }

        @Test
        fun `should map read-only root`() {
            expectThat(useClassPaths("") { this::class.qualifiedName!! }).filter { it.contains("ReadOnly") }.size.isGreaterThan(2)
        }

        @Test
        fun `should map read-only resource`() {
            expectThat(useClassPaths(Fixture61C285F09D95930D0AE298B00AF09F918B0A.path) { this::class.qualifiedName!! }).filter { it.contains("ReadOnly") }.size.isGreaterThanOrEqualTo(
                1)
        }

        @Test
        fun `should list read-only resources`() {
            expectThat(useClassPaths("") {
                listDirectoryEntries().map { it::class.qualifiedName }.toList()
            }).filter { it.all { pathType -> pathType!!.contains("ReadOnly") } }.size.isGreaterThanOrEqualTo(2)
        }

        @TestFactory
        fun `should throw on write access`(uniqueId: UniqueId) = testEach<Pair<String, Path.(Path) -> Unit>>(
            "outputStream" to { Files.newBufferedWriter(it) },
            "move" to { Files.move(it, this) },
            "delete" to { Files.delete(it) },
        ) { (_, operation) ->
            withTempDir(uniqueId) {
                useClassPaths("try.it") {
                    expectThat(exists()).isTrue()
                    expectThrows<ReadOnlyFileSystemException> { operation(this@useClassPaths) }
                    expectThat(exists()).isTrue()
                    expectThat(readBytes()).isEqualTo(data)
                }
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
            expectThat(useClassPath(Fixture61C285F09D95930D0AE298B00AF09F918B0A.path) { readText() }).isEqualTo(text)
        }

        @Test
        fun `should map resource on non-matching path`() {
            expectThat(useClassPath("invalid.file") { this }).isNull()
        }

        @TestFactory
        fun `should support different notations`() = testEach(
            Fixture61C285F09D95930D0AE298B00AF09F918B0A.path,
            "/${Fixture61C285F09D95930D0AE298B00AF09F918B0A.path}",
            "classpath:${Fixture61C285F09D95930D0AE298B00AF09F918B0A.path}",
            "classpath:/${Fixture61C285F09D95930D0AE298B00AF09F918B0A.path}",
            "ClassPath:${Fixture61C285F09D95930D0AE298B00AF09F918B0A.path}",
            "ClassPath:/${Fixture61C285F09D95930D0AE298B00AF09F918B0A.path}",
        ) { expecting { useClassPath(this) { readText() } } that { isEqualTo(text) } }

        @Test
        fun `should map read-only root`() {
            expectThat(useClassPath("") { this::class.qualifiedName }).isNotNull().contains("ReadOnly")
        }

        @Test
        fun `should map read-only resource`() {
            expectThat(useClassPath(Fixture61C285F09D95930D0AE298B00AF09F918B0A.path) { this::class.qualifiedName }).isNotNull().contains("ReadOnly")
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
                    expectThat(readBytes()).isEqualTo(data)
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
                        size.isGreaterThan(Size.ZERO)
                        isCopyOf(fixtures)
                        isCopyOf(copy2)
                    }
                }
            }
        }
    }

    @TestFactory
    fun `use required class path`() = tests {
        expecting { useRequiredClassPath(Fixture61C285F09D95930D0AE298B00AF09F918B0A.path) { readText() } } that { isEqualTo(text) }
        expectThrows<NoSuchFileException> { useRequiredClassPath("invalid.file") {} }
    }

    @TestFactory
    fun `read class path`() = tests {
        expecting { readClassPathText(Fixture61C285F09D95930D0AE298B00AF09F918B0A.path) } that { isEqualTo(text) }
        expecting { readClassPathBytes(Fixture61C285F09D95930D0AE298B00AF09F918B0A.path) } that { isEqualTo(data) }
        expecting { readClassPathText("invalid.file") } that { isNull() }
        expecting { readClassPathBytes("invalid.file") } that { isNull() }
    }

    @TestFactory
    fun `require class path`() = tests {
        expecting { requireClassPathText(Fixture61C285F09D95930D0AE298B00AF09F918B0A.path) } that { isEqualTo(text) }
        expecting { requireClassPathBytes(Fixture61C285F09D95930D0AE298B00AF09F918B0A.path) } that { isEqualTo(data) }
        expectThrows<NoSuchFileException> { requireClassPathText("invalid.file") }
        expectThrows<NoSuchFileException> { requireClassPathBytes("invalid.file") }
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
