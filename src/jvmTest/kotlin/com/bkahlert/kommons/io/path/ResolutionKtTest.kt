package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.createTempDirectory
import com.bkahlert.kommons.createTempFile
import com.bkahlert.kommons.randomString
import com.bkahlert.kommons.test.createTempJarFileSystem
import com.bkahlert.kommons.test.junit.UniqueId
import com.bkahlert.kommons.test.testEachOld
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.Unicode
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.Assertion.Builder
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotNull
import strikt.java.exists
import java.net.URI
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.readText
import kotlin.io.path.toPath

class ResolutionKtTest {

    @Nested
    inner class ToPath {

        @Test
        fun `should return regular path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val path = createTempFile("file", ".txt")
            expectThat("$path".asPath())
                .isEqualTo(path)
                .not { isA<WrappedPath>() }
        }

        @Test
        fun `should not check existence`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val path = resolve("file.txt")
            expectThat("$path".asPath())
                .isEqualTo(path)
                .not { exists() }
        }
    }


    @Nested
    inner class ToMappedPath {

        @Test
        fun `should map regular path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val randomContent = randomString()
            val uri = createTempFile().writeText(randomContent).toUri()
            val readContent = uri.toMappedPath { it.readText() }
            expectThat(readContent).isEqualTo(randomContent)
        }

        @Test
        fun `should map stdlib class path`() {
            val url = Regex::class.java.getResource("Regex.class")
            val siblingFileNames = url?.toMappedPath { it.readText() }
            expectThat(siblingFileNames)
                .isNotNull()
                .contains("Matcher")
                .contains("MatchResult")
                .contains("getRange")
                .contains(" ")
                .contains("")
                .contains(Unicode.START_OF_HEADING.toString())
        }
    }

    @Nested
    inner class ResolveBetweenFileSystemsKtTest {

        @Nested
        inner class WithSameFileSystem {

            @Test
            fun `should return relative jar path resolved against jar path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                createTempJarFileSystem().use { jarFileSystem ->
                    val receiverJarPath: Path = jarFileSystem.rootDirectories.first().createTempDirectory().createTempDirectory()
                    val relativeJarPath: Path = receiverJarPath.parent.relativize(receiverJarPath)
                    expectThat(receiverJarPath.resolveBetweenFileSystems(relativeJarPath))
                        .isEqualTo(receiverJarPath.resolve(receiverJarPath.last()))
                }
            }

            @Test
            fun `should return relative file path resolved against file path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val receiverFilePath = createTempDirectory().createTempDirectory()
                val relativeFilePath: Path = receiverFilePath.parent.relativize(receiverFilePath)
                expectThat(receiverFilePath.resolveBetweenFileSystems(relativeFilePath))
                    .isEqualTo(receiverFilePath.resolve(receiverFilePath.last()))
            }
        }

        @Nested
        inner class WithAbsoluteOtherPath {

            @Test
            fun `should return absolute jar path resolved against jar path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                createTempJarFileSystem().use { jarFileSystem ->
                    val receiverJarPath: Path = jarFileSystem.rootDirectories.first().createTempDirectory().createTempFile()
                    val absoluteJarPath: Path = jarFileSystem.rootDirectories.first()
                    expectThat(receiverJarPath.resolveBetweenFileSystems(absoluteJarPath)).isEqualTo(absoluteJarPath)
                }
            }

            @Test
            fun `should return absolute jar path resolved against file path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val receiverFilePath: Path = createTempDirectory().createTempFile()
                createTempJarFileSystem().use { jarFileSystem ->
                    val absoluteJarPath: Path = jarFileSystem.rootDirectories.first()
                    expectThat(receiverFilePath.resolveBetweenFileSystems(absoluteJarPath)).isEqualTo(absoluteJarPath)
                }
            }

            @Test
            fun `should return absolute file path resolved against jar path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val otherFileAbsPath: Path = createTempDirectory()
                createTempJarFileSystem().use { jarFileSystem ->
                    val receiverJarPath: Path = jarFileSystem.rootDirectories.first().createTempDirectory().createTempFile()
                    expectThat(receiverJarPath.resolveBetweenFileSystems(otherFileAbsPath)).isEqualTo(otherFileAbsPath)
                }
            }

            @Test
            fun `should return absolute file path resolved against file path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val receiverFilePath = createTempDirectory().createTempFile()
                val otherFileAbsPath: Path = createTempDirectory()
                expectThat(receiverFilePath.resolveBetweenFileSystems(otherFileAbsPath)).isEqualTo(otherFileAbsPath)
            }
        }


        @Nested
        inner class WithRelativeOtherPath {

            @Test
            fun `should return file path on relative jar path resolved against file path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val receiverFilePath: Path = createTempDirectory().createTempFile()
                createTempJarFileSystem().use { jarFileSystem ->
                    val relativeJarPath: Path = jarFileSystem.rootDirectories.first().createTempDirectory().createTempFile()
                        .let { absPath -> absPath.parent.relativize(absPath) }
                    expectThat(receiverFilePath.resolveBetweenFileSystems(relativeJarPath))
                        .isEqualTo(receiverFilePath.resolve(relativeJarPath.first().toString()))
                }
            }

            @Test
            fun `should return jar path on relative file path resolved against jar path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val relativeFilePath: Path = createTempDirectory().createTempFile()
                    .let { absPath -> absPath.parent.relativize(absPath) }
                createTempJarFileSystem().use { jarFileSystem ->
                    val receiverJarPath: Path = jarFileSystem.rootDirectories.first().createTempDirectory().createTempFile()
                    expectThat(receiverJarPath.resolveBetweenFileSystems(relativeFilePath))
                        .isEqualTo(receiverJarPath.resolve(relativeFilePath.first().toString()))
                }
            }
        }
    }

    @Nested
    inner class ResolveSiblingKtTest {

        @Test
        fun `should resolve sibling path`() {
            expectThat(Paths.get("/a/b/c").resolveSibling { resolveSibling(fileName.pathString + "-x") }).pathString.isEqualTo("/a/b-x/c")
        }

        @Test
        fun `should resolve with returned multi-segment path`() {
            expectThat(Paths.get("/a/b/c.d").resolveSibling { resolveSibling("1/e") }).pathString.isEqualTo("/a/1/e/c.d")
        }

        @TestFactory
        fun `should apply order`() = listOf(
            0 to "/a/b/c-x",
            1 to "/a/b-x/c",
            2 to "/a-x/b/c",
        ).testEachOld { (order, expected) ->
            expecting { Paths.get("/a/b/c").resolveSibling(order) { resolveSibling(fileName.pathString + "-x") } } that { pathString.isEqualTo(expected) }
        }

        @Test
        fun `should throw on more levels requested than present`() {
            expectCatching { Paths.get("/a/b/c").resolveSibling(order = 3) { this } }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should throw on negative order`() {
            expectCatching { Paths.get("/a/b/c").resolveSibling(order = -1) { this } }.isFailure().isA<IllegalArgumentException>()
        }
    }
}

/**
 * Converts the subject to a [Path].
 */
fun Builder<out CharSequence>.asPath(): Builder<Path> =
    get("as path") { toString().asPath() }

/**
 * Converts the subject to a [Path].
 */
@JvmName("asPathURI")
fun Builder<out URI>.asPath(): Builder<Path> =
    get("as path") { toPath() }

fun <T : Path> Builder<T>.isSiblingOf(expected: Path, order: Int = 1) =
    assert("is sibling of order $order") { actual ->
        val actualNames = actual.map { name -> name.pathString }.toList()
        val otherNames = expected.map { name -> name.pathString }.toList()
        val actualIndex = actualNames.size - order - 1
        val otherIndex = otherNames.size - order - 1
        val missing = (actualIndex - otherNames.size + 1)
        if (missing > 0) {
            fail("$expected is too short. At least $missing segments are missing to be able to be sibling.")
        }
        if (missing <= 0) {
            val evaluation = actualNames.zip(otherNames).mapIndexed { index, namePair ->
                val match = if (index == actualIndex || index == otherIndex) true
                else namePair.first == namePair.second
                namePair to match
            }
            val matches = evaluation.takeWhile { (_, match) -> match }.map { (namePair, _) -> namePair.first }
            val misMatch = evaluation.getOrNull(matches.size)?.let { (namePair, _) -> namePair }
            if (misMatch != null) fail("Paths match up to $matches, then mismatch $misMatch")
            else pass()
        }
    }
