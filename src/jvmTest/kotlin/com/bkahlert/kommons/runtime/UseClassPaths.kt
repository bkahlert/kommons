package com.bkahlert.kommons.runtime

import com.bkahlert.kommons.io.classPath
import com.bkahlert.kommons.io.path.WrappedPath
import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.copyToDirectory
import com.bkahlert.kommons.io.path.isCopyOf
import com.bkahlert.kommons.io.readClassPathBytes
import com.bkahlert.kommons.io.readClassPathText
import com.bkahlert.kommons.io.requireClassPathBytes
import com.bkahlert.kommons.io.requireClassPathText
import com.bkahlert.kommons.io.useClassPath
import com.bkahlert.kommons.io.useClassPaths
import com.bkahlert.kommons.io.useRequiredClassPath
import com.bkahlert.kommons.listDirectoryEntriesRecursively
import com.bkahlert.kommons.randomDirectory
import com.bkahlert.kommons.randomPath
import com.bkahlert.kommons.test.Fixture61C285F09D95930D0AE298B00AF09F918B0A
import com.bkahlert.kommons.test.Fixture61C285F09D95930D0AE298B00AF09F918B0A.data
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEach
import com.bkahlert.kommons.test.tests
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.unit.Size
import com.bkahlert.kommons.unit.size
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
                it.listDirectoryEntriesRecursively().any { it.fileName.toString().endsWith(".class") }
            }).filter { true }.size.isGreaterThan(2)
        }

        @Test
        fun `should map resource on matching path`() {
            expectThat(useClassPaths(Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString) { it.readText() }).all { isEqualTo(data.decodeToString()) }
        }

        @Test
        fun `should map resource on non-matching path`() {
            expectThat(useClassPaths("invalid.file") { this }).isEmpty()
        }

        @TestFactory
        fun `should support different notations`() = testEach(
            Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString,
            "/${Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString}",
            "classpath:${Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString}",
            "classpath:/${Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString}",
        ) { expecting { useClassPaths(this) { it.readText() } } that { all { isEqualTo(data.decodeToString()) } } }

        @Test
        fun `should map read-only root`() {
            expectThat(useClassPaths("") { it::class.qualifiedName!! }).filter { it.contains("ReadOnly") }.size.isGreaterThan(2)
        }

        @Test
        fun `should map read-only resource`() {
            expectThat(useClassPaths(Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString) { it::class.qualifiedName!! })
                .filter { it.contains("ReadOnly") }.size.isGreaterThanOrEqualTo(1)
        }

        @Test
        fun `should list read-only resources`() {
            expectThat(useClassPaths("") {
                it.listDirectoryEntries().map { it::class.qualifiedName }.toList()
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
                    expectThat(it.exists()).isTrue()
                    expectThrows<ReadOnlyFileSystemException> { operation(it) }
                    expectThat(it.exists()).isTrue()
                    expectThat(it.readBytes()).isEqualTo(data)
                }
            }
        }
    }


    @Nested
    inner class UseClassPath {

        @Test
        fun `should map root with no provided path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThat(useClassPath("") {
                it.listDirectoryEntriesRecursively().any { it.fileName.toString().endsWith(".class") }
            }).isTrue()
        }

        @Test
        fun `should map resource on matching path`() {
            expectThat(useClassPath(Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString) { it.readText() }).isEqualTo(data.decodeToString())
        }

        @Test
        fun `should map resource on non-matching path`() {
            expectThat(useClassPath("invalid.file") { this }).isNull()
        }

        @TestFactory
        fun `should support different notations`() = testEach(
            Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString,
            "/${Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString}",
            "classpath:${Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString}",
            "classpath:/${Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString}",
        ) { expecting { useClassPath(this) { it.readText() } } that { isEqualTo(data.decodeToString()) } }

        @Test
        fun `should map read-only root`() {
            expectThat(useClassPath("") { it::class.qualifiedName }).isNotNull().contains("ReadOnly")
        }

        @Test
        fun `should map read-only resource`() {
            expectThat(useClassPath(Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString) { it::class.qualifiedName }).isNotNull().contains("ReadOnly")
        }

        @Test
        fun `should list read-only resources`() {
            expectThat(useClassPath("") { it.listDirectoryEntries().mapNotNull { it::class.qualifiedName }.toList() }).isNotNull().all { contains("ReadOnly") }
        }

        @TestFactory
        fun `should throw on write access`(uniqueId: UniqueId) = mapOf<String, Path.(Path) -> Unit>(
            "outputStream" to { Files.newBufferedWriter(it) },
            "move" to { Files.move(it, randomPath()) },
            "delete" to { Files.delete(it) },
        ).map { (name, operation) ->
            dynamicTest(name) {
                useClassPath("try.it") {
                    expectThat(it.exists()).isTrue()
                    expectCatching {
                        withTempDir(uniqueId) {
                            val target = this
                            target.operation(it)
                        }
                    }.isFailure().isA<ReadOnlyFileSystemException>()
                    expectThat(it.exists()).isTrue()
                    expectThat(it.readBytes()).isEqualTo(data)
                }
            }
        }

        @Nested
        inner class UsingDelegatesProperty {
            @Test
            fun `should copy class path directory`() {
                val fixtures by classPath("img")
                useClassPath("img") {
                    fixtures.copyToDirectory(it.randomDirectory("copy1")) to it.copyToDirectory(it.randomDirectory("copy2"))
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
        expecting { useRequiredClassPath(Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString) { it.readText() } } that { isEqualTo(data.decodeToString()) }
        expectThrows<NoSuchFileException> { useRequiredClassPath("invalid.file") {} }
    }

    @TestFactory
    fun `read class path`() = tests {
        expecting { readClassPathText(Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString) } that { isEqualTo(data.decodeToString()) }
        expecting { readClassPathBytes(Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString) } that { isEqualTo(data) }
        expecting { readClassPathText("invalid.file") } that { isNull() }
        expecting { readClassPathBytes("invalid.file") } that { isNull() }
    }

    @TestFactory
    fun `require class path`() = tests {
        expecting { requireClassPathText(Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString) } that { isEqualTo(data.decodeToString()) }
        expecting { requireClassPathBytes(Fixture61C285F09D95930D0AE298B00AF09F918B0A.pathString) } that { isEqualTo(data) }
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
