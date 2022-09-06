package com.bkahlert.kommons.io

import com.bkahlert.kommons.IsolatedProcess
import com.bkahlert.kommons.Now
import com.bkahlert.kommons.SystemLocations
import com.bkahlert.kommons.io.DeleteOnExecTestHelper.Variant.Default
import com.bkahlert.kommons.io.DeleteOnExecTestHelper.Variant.NonRecursively
import com.bkahlert.kommons.io.DeleteOnExecTestHelper.Variant.Recursively
import com.bkahlert.kommons.minus
import com.bkahlert.kommons.plus
import com.bkahlert.kommons.test.OneMinuteTimeout
import com.bkahlert.kommons.test.createAnyFile
import com.bkahlert.kommons.test.createDirectoryWithFiles
import com.bkahlert.kommons.test.createTempJarFile
import com.bkahlert.kommons.test.createTempJarFileSystem
import com.bkahlert.kommons.test.fixtures.SvgImageFixture
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.test.toNewJarFileSystem
import com.bkahlert.kommons.text.Unicode.NULL
import com.bkahlert.kommons.toFileTime
import io.kotest.assertions.asClue
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.throwables.shouldThrowUnit
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import io.kotest.matchers.paths.shouldBeADirectory
import io.kotest.matchers.paths.shouldBeEmptyDirectory
import io.kotest.matchers.paths.shouldBeSymbolicLink
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.paths.shouldNotBeEmptyDirectory
import io.kotest.matchers.paths.shouldNotExist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URI
import java.net.URL
import java.nio.channels.SeekableByteChannel
import java.nio.file.AccessMode
import java.nio.file.CopyOption
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.DirectoryStream
import java.nio.file.DirectoryStream.Filter
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileStore
import java.nio.file.FileSystem
import java.nio.file.FileSystemNotFoundException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.LinkOption
import java.nio.file.LinkOption.NOFOLLOW_LINKS
import java.nio.file.NoSuchFileException
import java.nio.file.NotDirectoryException
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.WatchEvent.Kind
import java.nio.file.WatchEvent.Modifier
import java.nio.file.WatchKey
import java.nio.file.WatchService
import java.nio.file.attribute.BasicFileAttributes
import java.nio.file.attribute.FileAttribute
import java.nio.file.attribute.FileAttributeView
import java.nio.file.attribute.FileTime
import java.nio.file.attribute.UserPrincipalLookupService
import java.nio.file.spi.FileSystemProvider
import kotlin.io.path.appendText
import kotlin.io.path.createDirectories
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.io.path.toPath
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class PathsKtTest {

    @Test fun to_path_or_null(@TempDir tempDir: Path) = testAll {
        tempDir.pathString.cs.toPathOrNull()?.pathString shouldBe tempDir.pathString
        tempDir.pathString.toPathOrNull()?.pathString shouldBe tempDir.pathString
        tempDir.toUri().toURL().toPathOrNull()?.pathString shouldBe tempDir.pathString
        tempDir.toUri().toPathOrNull()?.pathString shouldBe tempDir.pathString
        tempDir.toFile().toPathOrNull()?.pathString shouldBe tempDir.pathString

        NULL.toString().cs.toPathOrNull() shouldBe null
        NULL.toString().toPathOrNull() shouldBe null
        URL("https://example.com/path").toPathOrNull() shouldBe null
        URI("https://example.com/path").toPathOrNull() shouldBe null
        File("\u0000").toPathOrNull() shouldBe null
    }

    @Test fun to_path(@TempDir tempDir: Path) = testAll {
        tempDir.pathString.cs.toPath().pathString shouldBe tempDir.pathString
        tempDir.pathString.toPath().pathString shouldBe tempDir.pathString
        tempDir.toUri().toURL().toPath().pathString shouldBe tempDir.pathString
        tempDir.toUri().toPath().pathString shouldBe tempDir.pathString
        tempDir.toFile().toPath().pathString shouldBe tempDir.pathString

        shouldThrow<InvalidPathException> { NULL.toString().cs.toPath() }
        shouldThrow<InvalidPathException> { NULL.toString().toPath() }
        shouldThrow<FileSystemNotFoundException> { URL("https://example.com/path").toPath() }
        shouldThrow<FileSystemNotFoundException> { URI("https://example.com/path").toPath() }
        shouldThrow<IllegalArgumentException> { File("\u0000").toPath() }
    }

    @Test fun to_file_or_null(@TempDir tempDir: File) = testAll {
        tempDir.path.cs.toFileOrNull()?.path shouldBe tempDir.path
        tempDir.path.toFileOrNull()?.path shouldBe tempDir.path
        tempDir.toPath().toUri().toURL().toFileOrNull()?.path shouldBe tempDir.path
        tempDir.toPath().toUri().toFileOrNull()?.path shouldBe tempDir.path
        tempDir.toPath().toFileOrNull()?.path shouldBe tempDir.path

        shouldNotThrowAny { "$NULL".cs.toFileOrNull() }
        shouldNotThrowAny { "$NULL".toFileOrNull() }
        URL("https://example.com/path").toFileOrNull() shouldBe null
        tempDir.toPath().createJarAndResolve().toUri().toFileOrNull() shouldBe null
        tempDir.toPath().createJarAndResolve().toFileOrNull() shouldBe null
    }

    @Test fun to_file(@TempDir tempDir: File) = testAll {
        tempDir.path.cs.toFile().path shouldBe tempDir.path
        tempDir.path.toFile().path shouldBe tempDir.path
        tempDir.toPath().toUri().toURL().toFile().path shouldBe tempDir.path
        tempDir.toPath().toUri().toFile().path shouldBe tempDir.path
        tempDir.toPath().toFile().path shouldBe tempDir.path

        // NULL.toString().cs.toFile() // does not even throw
        // NULL.toString().toFile() // does not even throw
        shouldThrow<FileSystemNotFoundException> { URL("https://example.com/path").toFile() }
        shouldThrow<UnsupportedOperationException> { tempDir.toPath().createJarAndResolve().toUri().toFile() }
        shouldThrow<UnsupportedOperationException> { tempDir.toPath().createJarAndResolve().toFile() }
    }


    @Test fun create_temp_file(@TempDir tempDir: Path) = testAll {
        tempDir.createTempFile() should {
            it.shouldExist()
            it.isRegularFile()
            it.parent.pathString shouldBe kotlin.io.path.createTempFile(tempDir).parent.pathString
        }
    }

    @Test fun create_temp_directory(@TempDir tempDir: Path) = testAll {
        tempDir.createTempDirectory() should {
            it.shouldExist()
            it.isDirectory()
            it.parent.pathString shouldBe kotlin.io.path.createTempDirectory(tempDir).parent.pathString
        }
    }

    @Test fun create_temp_text_file(@TempDir tempDir: Path) = testAll {
        createTempTextFile("text") should {
            it.shouldExist()
            it.readText() shouldBe "text"
        }
        tempDir.createTempTextFile("text") should {
            it.shouldExist()
            it.readText() shouldBe "text"
        }
    }

    @Test fun create_temp_binary_file(@TempDir tempDir: Path) = testAll {
        createTempBinaryFile("string".encodeToByteArray()) should {
            it.shouldExist()
            it.readBytes() shouldBe "string".encodeToByteArray()
        }
        tempDir.createTempBinaryFile("string".encodeToByteArray()) should {
            it.shouldExist()
            it.readBytes() shouldBe "string".encodeToByteArray()
        }
    }

    @Test fun create_text_file(@TempDir tempDir: Path) = testAll {
        tempDir.resolve("file.txt").createTextFile("text") should {
            it.shouldExist()
            it.readText() shouldBe "text"
        }
    }

    @Test fun create_binary_file(@TempDir tempDir: Path) = testAll {
        tempDir.resolve("file").createBinaryFile("string".encodeToByteArray()) should {
            it.shouldExist()
            it.readBytes() shouldBe "string".encodeToByteArray()
        }
    }

    @Test fun is_normalized_directory(@TempDir tempDir: Path) = testAll {
        tempDir.isNormalizedDirectory() shouldBe true
        (tempDir / "foo" / "..").isNormalizedDirectory() shouldBe true
        (tempDir / "foo").isNormalizedDirectory() shouldBe false
    }

    @Test fun require_normalized_directory(@TempDir tempDir: Path) = testAll {
        tempDir should { requireNormalizedDirectory(it) shouldBe it }
        tempDir / "foo" / ".." should { requireNormalizedDirectory(it) shouldBe it }
        shouldThrow<IllegalArgumentException> { requireNormalizedDirectory(tempDir / "foo") }
    }

    @Test fun check_normalized_directory(@TempDir tempDir: Path) = testAll {
        tempDir should { checkNormalizedDirectory(it) shouldBe it }
        tempDir / "foo" / ".." should { checkNormalizedDirectory(it) shouldBe it }
        shouldThrow<IllegalStateException> { checkNormalizedDirectory(tempDir / "foo") }
    }

    @Test fun require_normalized_no_directory(@TempDir tempDir: Path) = testAll {
        shouldThrow<IllegalArgumentException> { requireNoDirectoryNormalized(tempDir) }
        shouldThrow<IllegalArgumentException> { requireNoDirectoryNormalized(tempDir / "foo" / "..") }
        tempDir / "foo" should { requireNoDirectoryNormalized(it) shouldBe it }
    }

    @Test fun check_normalized_no_directory(@TempDir tempDir: Path) = testAll {
        shouldThrow<IllegalStateException> { checkNoDirectoryNormalized(tempDir) }
        shouldThrow<IllegalStateException> { checkNoDirectoryNormalized(tempDir / "foo" / "..") }
        tempDir / "foo" should { checkNoDirectoryNormalized(it) shouldBe it }
    }

    @Nested
    inner class IsSubPathOf {

        @Test
        fun `should return true if child`(@TempDir tempDir: Path) = testAll {
            (tempDir / "child").isInside(tempDir) shouldBe true
            (tempDir / "child").isSubPathOf(tempDir) shouldBe true
        }

        @Test
        fun `should return true if descendent`(@TempDir tempDir: Path) = testAll {
            (tempDir / "child1" / "child2").isInside(tempDir) shouldBe true
            (tempDir / "child1" / "child2").isSubPathOf(tempDir) shouldBe true
        }

        @Test
        fun `should return true if path is obscure`(@TempDir tempDir: Path) = testAll {
            (tempDir / "child1" / ".." / "child2").isInside(tempDir) shouldBe true
            (tempDir / "child1" / ".." / "child2").isSubPathOf(tempDir) shouldBe true
        }

        @Test
        fun `should return true if same`(@TempDir tempDir: Path) = testAll {
            tempDir.isInside(tempDir) shouldBe true
            tempDir.isSubPathOf(tempDir) shouldBe true
        }

        @Test
        fun `should return false if not inside`(@TempDir tempDir: Path) = testAll {
            tempDir.isInside(tempDir / "child") shouldBe false
            tempDir.isSubPathOf(tempDir / "child") shouldBe false
        }
    }

    @Test fun create_parent_directories(@TempDir tempDir: Path) = testAll {
        val file = tempDir.resolve("some/dir/some/file")
        file.createParentDirectories().parent should {
            it.shouldExist()
            it.shouldBeADirectory()
            it.shouldBeEmptyDirectory()
        }
    }

    @Test
    fun age(@TempDir tempDir: Path) = testAll {
        with(tempDir.resolve("existing").createFile().apply { lastModified -= 1.days }) {
            age should {
                it shouldBeGreaterThanOrEqualTo (1.days - 5.seconds)
                it shouldBeLessThanOrEqualTo (1.days + 5.seconds)
            }
            age = 42.days
            age should {
                it shouldBeGreaterThanOrEqualTo (42.days - 5.seconds)
                it shouldBeLessThanOrEqualTo (42.days + 5.seconds)
            }
        }

        shouldThrow<NoSuchFileException> { tempDir.resolve("missing").age }
        shouldThrowUnit<NoSuchFileException> { tempDir.resolve("missing").age = 42.days }
    }


    @Nested
    inner class CreatedKtTest {

        @Test
        fun `should read created`(@TempDir tempDir: Path) = testAll {
            tempDir.createTempFile().created.toInstant() should {
                it shouldBeLessThan (Now + 1.minutes)
                it shouldBeGreaterThan (Now - 1.minutes)
            }
        }

        @Test
        fun `should write created`(@TempDir tempDir: Path) = testAll {
            tempDir.createTempFile().apply {
                created = (Now - 20.minutes).toFileTime()
            }.created should {
                it shouldBeLessThan (Now + 21.minutes).toFileTime()
                it shouldBeGreaterThan (Now - 21.minutes).toFileTime()
            }
        }
    }

    @Nested
    inner class LastAccessedKtTest {

        @Test
        fun `should read last accessed`(@TempDir tempDir: Path) = testAll {
            tempDir.createTempFile().lastAccessed.toInstant() should {
                it shouldBeLessThan (Now + 1.minutes)
                it shouldBeGreaterThan (Now - 1.minutes)
            }
        }

        @Test
        fun `should write last accessed`(@TempDir tempDir: Path) = testAll {
            tempDir.createTempFile().apply {
                lastAccessed = FileTime.from(Now - 20.minutes)
            }.lastAccessed.toInstant() should {
                it shouldBeLessThan (Now + 21.minutes)
                it shouldBeGreaterThan (Now - 21.minutes)
            }
        }
    }

    @Nested
    inner class LastModifiedKtTest {

        @Test
        fun `should read last modified`(@TempDir tempDir: Path) = testAll {
            tempDir.createTempFile().lastModified.toInstant() should {
                it shouldBeLessThan (Now + 1.minutes)
                it shouldBeGreaterThan (Now - 1.minutes)
            }
        }

        @Test
        fun `should write last modified`(@TempDir tempDir: Path) = testAll {
            tempDir.createTempFile().apply {
                lastModified = FileTime.from(Now - 20.minutes)
            }.lastModified.toInstant() should {
                it shouldBeLessThan (Now + 21.minutes)
                it shouldBeGreaterThan (Now - 21.minutes)
            }
        }
    }


    @Test fun resolve_between_file_systems(@TempDir tempDir: Path) {
        // same filesystem
        tempDir.createTempJarFileSystem().use { jarFileSystem ->
            val receiverJarPath: Path = jarFileSystem.rootDirectories.first().createTempDirectory().createTempDirectory()
            val relativeJarPath: Path = receiverJarPath.parent.relativize(receiverJarPath)
            receiverJarPath.resolveBetweenFileSystems(relativeJarPath)
                .shouldBe(receiverJarPath.resolve(receiverJarPath.last()))
        }
        // same filesystem
        with(tempDir.createTempDirectory()) {
            val receiverFilePath = createTempDirectory()
            val relativeFilePath: Path = receiverFilePath.parent.relativize(receiverFilePath)
            receiverFilePath.resolveBetweenFileSystems(relativeFilePath)
                .shouldBe(receiverFilePath.resolve(receiverFilePath.last()))
        }

        // absolute other path
        tempDir.createTempJarFileSystem().use { jarFileSystem ->
            val receiverJarPath: Path = jarFileSystem.rootDirectories.first().createTempDirectory().createTempFile()
            val absoluteJarPath: Path = jarFileSystem.rootDirectories.first()
            receiverJarPath.resolveBetweenFileSystems(absoluteJarPath)
                .shouldBe(absoluteJarPath)
        }
        // absolute other path
        tempDir.createTempJarFileSystem().use { jarFileSystem ->
            val receiverFilePath: Path = tempDir.createTempDirectory().createTempFile()
            val absoluteJarPath: Path = jarFileSystem.rootDirectories.first()
            receiverFilePath.resolveBetweenFileSystems(absoluteJarPath)
                .shouldBe(absoluteJarPath)
        }
        // absolute other path
        tempDir.createTempJarFileSystem().use { jarFileSystem ->
            val receiverJarPath: Path = jarFileSystem.rootDirectories.first().createTempDirectory().createTempFile()
            val otherFileAbsPath: Path = tempDir.createTempDirectory()
            receiverJarPath.resolveBetweenFileSystems(otherFileAbsPath)
                .shouldBe(otherFileAbsPath)
        }
        // absolute other path
        with(tempDir) {
            val receiverFilePath = createTempDirectory().createTempFile()
            val otherFileAbsPath: Path = createTempDirectory()
            receiverFilePath.resolveBetweenFileSystems(otherFileAbsPath)
                .shouldBe(otherFileAbsPath)
        }

        // relative other path
        with(tempDir) {
            val receiverFilePath: Path = createTempDirectory().createTempFile()
            createTempJarFileSystem().use { jarFileSystem ->
                val relativeJarPath: Path = jarFileSystem.rootDirectories.first().createTempDirectory().createTempFile()
                    .let { absPath -> absPath.parent.relativize(absPath) }
                receiverFilePath.resolveBetweenFileSystems(relativeJarPath)
                    .shouldBe(receiverFilePath.resolve(relativeJarPath.first().toString()))
            }
        }
        // relative other path
        with(tempDir) {
            val relativeFilePath: Path = createTempDirectory().createTempFile()
                .let { absPath -> absPath.parent.relativize(absPath) }
            createTempJarFileSystem().use { jarFileSystem ->
                val receiverJarPath: Path = jarFileSystem.rootDirectories.first().createTempDirectory().createTempFile()
                receiverJarPath.resolveBetweenFileSystems(relativeFilePath)
                    .shouldBe(receiverJarPath.resolve(relativeFilePath.first().toString()))
            }
        }
    }

    @Test
    fun resolve_random(@TempDir tempDir: Path) = testAll {
        tempDir.resolveRandom() should {
            it.shouldNotExist()
        }
        tempDir.resolveRandom("prefix", "suffix") should {
            it.shouldNotExist()
            it.fileName.pathString shouldStartWith "prefix"
            it.fileName.pathString shouldEndWith "suffix"
        }
    }

    @Test
    fun resolve_file(@TempDir tempDir: Path) = testAll {
        tempDir.resolveFile { Paths.get("dir", "file") } shouldBe tempDir / "dir" / "file"
        tempDir.resolveFile(Paths.get("dir", "file")) shouldBe tempDir / "dir" / "file"
        tempDir.resolveFile("dir/file") shouldBe tempDir / "dir" / "file"
        shouldThrow<IllegalStateException> { tempDir.resolveFile { Paths.get("dir", "..") } }
        shouldThrow<IllegalStateException> { tempDir.resolveFile(Paths.get("dir", "..")) }
        shouldThrow<IllegalStateException> { tempDir.resolveFile("dir/..") }
    }

    @Test
    fun list_directory_entries_recursively(@TempDir tempDir: Path) = testAll {
        val dir = tempDir.createDirectoryWithFiles()

        dir.listDirectoryEntriesRecursively()
            .map { it.pathString } shouldContainExactlyInAnyOrder listOf(
            dir.resolve("pixels.gif").pathString,
            dir.resolve("kommons.svg").pathString,
            dir.resolve("docs").pathString,
            dir.resolve("docs/hello-world.html").pathString,
            dir.resolve("docs/unicode.txt").pathString,
        )

        dir.listDirectoryEntriesRecursively("**/*.*")
            .map { it.pathString } shouldContainExactlyInAnyOrder listOf(
            dir.resolve("pixels.gif").pathString,
            dir.resolve("kommons.svg").pathString,
            dir.resolve("docs/hello-world.html").pathString,
            dir.resolve("docs/unicode.txt").pathString,
        )

        shouldThrow<NotDirectoryException> { tempDir.createTempFile().listDirectoryEntriesRecursively() }
    }

    @Nested
    inner class ListDirectoryEntriesRecursivelyOperation {

        @Test
        fun `should delete directory contents`(@TempDir tempDir: Path) = testAll {
            val dir = tempDir.resolve("dir").createDirectories()
            dir.createDirectoryWithFiles()

            dir.deleteDirectoryEntriesRecursively().shouldExist()
            dir.shouldBeEmptyDirectory()
        }

        @Test
        fun `should delete filtered directory contents`(@TempDir tempDir: Path) = testAll {
            val dir = tempDir.resolve("dir").createDirectories()
            val exception = dir.createDirectoryWithFiles().listDirectoryEntriesRecursively().first()

            dir.deleteDirectoryEntriesRecursively { it != exception && !it.isDirectory() }.shouldExist()
            dir.listDirectoryEntries().map { it.pathString }.shouldNotContain(exception.pathString)
        }

        @Test
        fun `should throw on non-directory`(@TempDir tempDir: Path) = testAll {
            val file = tempDir.resolve("file").createFile()
            shouldThrow<NotDirectoryException> { file.deleteDirectoryEntriesRecursively() }
        }
    }

    @Test
    fun use_directory_entries_recursively(@TempDir tempDir: Path) = testAll {
        val dir = tempDir.createDirectoryWithFiles()

        dir.useDirectoryEntriesRecursively { seq -> seq.map { it.fileName.pathString }.sorted().joinToString() }
            .shouldBe("docs, hello-world.html, kommons.svg, pixels.gif, unicode.txt")

        dir.useDirectoryEntriesRecursively("**/*.*") { seq -> seq.map { it.fileName.pathString }.sorted().joinToString() }
            .shouldBe("hello-world.html, kommons.svg, pixels.gif, unicode.txt")

        shouldThrow<NotDirectoryException> { tempDir.createTempFile().useDirectoryEntriesRecursively { } }
    }

    @Test
    fun for_each_directory_entries_recursively(@TempDir tempDir: Path) = testAll {
        val dir = tempDir.createDirectoryWithFiles()

        buildList { dir.forEachDirectoryEntryRecursively { add(it) } }
            .map { it.pathString } shouldContainExactlyInAnyOrder listOf(
            dir.resolve("pixels.gif").pathString,
            dir.resolve("kommons.svg").pathString,
            dir.resolve("docs").pathString,
            dir.resolve("docs/hello-world.html").pathString,
            dir.resolve("docs/unicode.txt").pathString,
        )

        buildList { dir.forEachDirectoryEntryRecursively("**/*.*") { add(it) } }
            .map { it.pathString } shouldContainExactlyInAnyOrder listOf(
            dir.resolve("pixels.gif").pathString,
            dir.resolve("kommons.svg").pathString,
            dir.resolve("docs/hello-world.html").pathString,
            dir.resolve("docs/unicode.txt").pathString,
        )

        shouldThrow<NotDirectoryException> { tempDir.createTempFile().forEachDirectoryEntryRecursively { } }
    }

    @Test fun copy_to_directory(@TempDir tempDir: Path) = testAll {
        val file = tempDir.createAnyFile("file")
        val dir = tempDir.resolve("dir")

        shouldThrow<NoSuchFileException> { file.copyToDirectory(dir) }

        file.copyToDirectory(dir, createDirectories = true) should {
            it.parent.fileName.pathString shouldBe "dir"
            it.fileName.pathString shouldBe "file"
            it.readText() shouldBe SvgImageFixture.contents
        }

        file.appendText("-overwritten")
        shouldThrow<FileAlreadyExistsException> { file.copyToDirectory(dir) }

        file.copyToDirectory(dir, overwrite = true) should {
            it.parent.fileName.pathString shouldBe "dir"
            it.fileName.pathString shouldBe "file"
            it.readText() shouldBe "${SvgImageFixture.contents}-overwritten"
        }
    }


    @Nested
    inner class Delete {

        @Test
        fun `should delete file`(@TempDir tempDir: Path) = testAll {
            val file = tempDir.createAnyFile()
            file.delete().shouldNotExist()
            tempDir.shouldBeEmptyDirectory()
        }

        @Test
        fun `should delete empty directory`(@TempDir tempDir: Path) = testAll {
            val dir = tempDir.resolve("dir").createDirectory()
            dir.delete().shouldNotExist()
            tempDir.shouldBeEmptyDirectory()
        }

        @Test
        fun `should throw on non-empty directory`(@TempDir tempDir: Path) = testAll {
            val dir = tempDir.resolve("dir").createDirectory().apply { createAnyFile() }
            shouldThrow<DirectoryNotEmptyException> { dir.delete() }
        }

        @Test
        fun `should delete non-existing file`(@TempDir tempDir: Path) = testAll {
            val file = tempDir.resolve("file")
            file.delete().asClue { it.exists() shouldBe false }
            tempDir.shouldBeEmptyDirectory()
        }

        @Nested
        inner class WithNoFollowLinks {

            @Test
            fun `should delete symbolic link itself`(@TempDir tempDir: Path) = testAll {
                val symbolicLink = tempDir.symbolicLink()
                symbolicLink.delete()

                symbolicLink.delete(NOFOLLOW_LINKS).asClue { it.exists(NOFOLLOW_LINKS) shouldBe false }
                tempDir.shouldBeEmptyDirectory()
            }
        }

        @Nested
        inner class WithoutNoFollowLinks {

            @Test
            fun `should not delete symbolic link itself`(@TempDir tempDir: Path) = testAll {
                val symbolicLink = tempDir.symbolicLink()

                symbolicLink.shouldBeSymbolicLink()
                tempDir.shouldNotBeEmptyDirectory()
            }
        }
    }

    @Nested
    inner class DeleteRecursively {

        @Test
        fun `should delete file`(@TempDir tempDir: Path) = testAll {
            tempDir.createAnyFile().deleteRecursively().shouldNotExist()
            tempDir.shouldBeEmptyDirectory()
        }

        @Test
        fun `should delete empty directory`(@TempDir tempDir: Path) = testAll {
            tempDir.resolve("dir").createDirectory().deleteRecursively().shouldNotExist()
            tempDir.shouldBeEmptyDirectory()
        }

        @Test
        fun `should delete non-empty directory`(@TempDir tempDir: Path) = testAll {
            tempDir.resolve("dir").createDirectory().apply { createAnyFile() }.deleteRecursively().shouldNotExist()
            tempDir.shouldBeEmptyDirectory()
        }

        @Test
        fun `should delete non-existing file`(@TempDir tempDir: Path) = testAll {
            tempDir.resolve("file").deleteRecursively().shouldNotExist()
            tempDir.shouldBeEmptyDirectory()
        }

        @Test
        fun `should delete complex file tree`(@TempDir tempDir: Path) = testAll {
            val dir = tempDir.resolve("dir").createDirectory()
            dir.createDirectoryWithFiles().symbolicLink()

            dir.deleteRecursively().shouldNotExist()
            tempDir.shouldBeEmptyDirectory()
        }

        @Test
        fun `should delete filtered files`(@TempDir tempDir: Path) = testAll {
            val dir = tempDir.resolve("dir").createDirectory()
            val exception = dir.createDirectoryWithFiles().listDirectoryEntriesRecursively().first()

            dir.deleteRecursively { it != exception && !it.isDirectory() }.shouldExist()
            dir.listDirectoryEntries().map { it.pathString }.shouldNotContain(exception.pathString)
        }
    }

    @OneMinuteTimeout
    @Test fun delete_on_exit(@TempDir tempDir: Path) = testAll {
        tempDir.createAnyFile("file-delete-default").asClue {
            IsolatedProcess.exec(DeleteOnExecTestHelper::class, Default.name, it.pathString) shouldBe 0
            it.shouldNotExist()
        }
        tempDir.createAnyFile("file-delete-recursively").asClue {
            IsolatedProcess.exec(DeleteOnExecTestHelper::class, Recursively.name, it.pathString) shouldBe 0
            it.shouldNotExist()
        }
        tempDir.createAnyFile("file-delete-non-recursively").asClue {
            IsolatedProcess.exec(DeleteOnExecTestHelper::class, NonRecursively.name, it.pathString) shouldBe 0
            it.shouldNotExist()
        }

        tempDir.createDirectoryWithFiles("dir-delete-default").asClue {
            IsolatedProcess.exec(DeleteOnExecTestHelper::class, Default.name, it.pathString) shouldBe 0
            it.shouldNotExist()
        }
        tempDir.createDirectoryWithFiles("dir-delete-recursively").asClue {
            IsolatedProcess.exec(DeleteOnExecTestHelper::class, Recursively.name, it.pathString) shouldBe 0
            it.shouldNotExist()
        }
        tempDir.createDirectoryWithFiles("dir-delete-non-recursively").asClue {
            IsolatedProcess.exec(DeleteOnExecTestHelper::class, NonRecursively.name, it.pathString) shouldBe 0
            it.shouldExist()
        }

        tempDir.resolve("missing").asClue {
            IsolatedProcess.exec(DeleteOnExecTestHelper::class, Default.name, it.pathString) shouldBe 0
            it.shouldNotExist()
        }
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


    @Test fun use_input_stream(@TempDir tempDir: Path) = testAll {
        tempDir.createAnyFile().useInputStream { it.readBytes().decodeToString() } shouldBe SvgImageFixture.contents
        shouldThrow<NoSuchFileException> { tempDir.resolveRandom().useInputStream {} }
    }

    @Test fun use_buffered_input_stream(@TempDir tempDir: Path) = testAll {
        tempDir.createAnyFile().useBufferedInputStream { it.readBytes().decodeToString() } shouldBe SvgImageFixture.contents
        shouldThrow<NoSuchFileException> { tempDir.resolveRandom().useBufferedInputStream {} }
    }

    @Test fun use_reader(@TempDir tempDir: Path) = testAll {
        tempDir.createAnyFile().useReader { it.readText() } shouldBe SvgImageFixture.contents
        shouldThrow<NoSuchFileException> { tempDir.resolveRandom().useReader {} }
    }

    @Test fun use_buffered_reader(@TempDir tempDir: Path) = testAll {
        tempDir.createAnyFile().useBufferedReader { it.readText() } shouldBe SvgImageFixture.contents
        shouldThrow<NoSuchFileException> { tempDir.resolveRandom().useBufferedReader {} }
    }

    @Test fun use_output_stream(@TempDir tempDir: Path) = testAll {
        tempDir.resolve("file").useOutputStream { it.write("abc".encodeToByteArray()) }.readText() shouldBe "abc"
        shouldThrow<NoSuchFileException> { tempDir.resolveRandom().useOutputStream(TRUNCATE_EXISTING) {} }
    }

    @Test fun use_buffered_output_stream(@TempDir tempDir: Path) = testAll {
        tempDir.resolve("file").useBufferedOutputStream { it.write("abc".encodeToByteArray()) }.readText() shouldBe "abc"
        shouldThrow<NoSuchFileException> { tempDir.resolveRandom().useBufferedOutputStream(TRUNCATE_EXISTING) {} }
    }

    @Test fun use_writer(@TempDir tempDir: Path) = testAll {
        tempDir.resolve("file").useWriter { it.write("abc") }.readText() shouldBe "abc"
        shouldThrow<NoSuchFileException> { tempDir.resolveRandom().useWriter(TRUNCATE_EXISTING) {} }
    }

    @Test fun use_buffered_writer(@TempDir tempDir: Path) = testAll {
        tempDir.resolve("file").useBufferedWriter { it.write("abc") }.readText() shouldBe "abc"
        shouldThrow<NoSuchFileException> { tempDir.resolveRandom().useBufferedWriter(TRUNCATE_EXISTING) {} }
    }
}

class DeleteOnExecTestHelper {
    enum class Variant {
        Default {
            override fun deleteOnExit(path: Path) {
                path.deleteOnExit()
            }
        },
        Recursively {
            override fun deleteOnExit(path: Path) {
                path.deleteOnExit(recursively = true)
            }
        },
        NonRecursively {
            override fun deleteOnExit(path: Path) {
                path.deleteOnExit(recursively = false)
            }
        };

        abstract fun deleteOnExit(path: Path)
    }

    companion object {
        @JvmStatic
        fun main(vararg args: String) {
            kotlin.runCatching {
                val operation = Variant.valueOf(args.first())::deleteOnExit
                val file = Paths.get(args.last())
                require(file.isSubPathOf(SystemLocations.Temp))
                operation(file)
            }.onFailure { exitProcess(1) }
        }
    }
}

fun Path.symbolicLink(): Path = resolveRandom().apply {
    Files.createSymbolicLink(this, resolveRandom())
    check(exists(NOFOLLOW_LINKS)) { "Failed to create symbolic link $this." }
}

fun Path.createJarAndResolve(): Path =
    createTempJarFile().toNewJarFileSystem().getPath("file")


internal object UnsupportedFileSystemProvider : FileSystemProvider() {
    override fun getScheme(): String = "unsupported"
    override fun newFileSystem(uri: URI, env: MutableMap<String, *>): FileSystem = UnsupportedFileSystem
    override fun getFileSystem(uri: URI): FileSystem = UnsupportedFileSystem
    override fun getPath(uri: URI): Path = UnsupportedPath
    override fun newByteChannel(path: Path, options: MutableSet<out OpenOption>, vararg attrs: FileAttribute<*>): SeekableByteChannel =
        throw UnsupportedOperationException()

    override fun newDirectoryStream(dir: Path, filter: Filter<in Path>): DirectoryStream<Path> = throw UnsupportedOperationException()
    override fun createDirectory(dir: Path, vararg attrs: FileAttribute<*>) = throw UnsupportedOperationException()
    override fun delete(path: Path) = throw UnsupportedOperationException()
    override fun copy(source: Path, target: Path, vararg options: CopyOption) = throw UnsupportedOperationException()
    override fun move(source: Path, target: Path, vararg options: CopyOption) = throw UnsupportedOperationException()
    override fun isSameFile(path: Path, path2: Path): Boolean = path == UnsupportedPath && path2 == UnsupportedPath
    override fun isHidden(path: Path): Boolean = false
    override fun getFileStore(path: Path): FileStore = throw UnsupportedOperationException()
    override fun checkAccess(path: Path, vararg modes: AccessMode) = Unit
    override fun <V : FileAttributeView> getFileAttributeView(path: Path, type: Class<V>, vararg options: LinkOption): V =
        throw UnsupportedOperationException()

    override fun <A : BasicFileAttributes> readAttributes(path: Path, type: Class<A>, vararg options: LinkOption): A = throw UnsupportedOperationException()
    override fun readAttributes(path: Path, attributes: String, vararg options: LinkOption): MutableMap<String, Any> = throw UnsupportedOperationException()
    override fun setAttribute(path: Path, attribute: String, value: Any, vararg options: LinkOption) = throw UnsupportedOperationException()
}

internal object UnsupportedFileSystem : FileSystem() {
    override fun close(): Unit = Unit
    override fun provider(): FileSystemProvider = UnsupportedFileSystemProvider
    override fun isOpen(): Boolean = true
    override fun isReadOnly(): Boolean = true
    override fun getSeparator(): String = "/"
    override fun getRootDirectories(): MutableIterable<Path> = throw UnsupportedOperationException()
    override fun getFileStores(): MutableIterable<FileStore> = throw UnsupportedOperationException()
    override fun supportedFileAttributeViews(): MutableSet<String> = mutableSetOf()
    override fun getPath(first: String, vararg more: String): Path = UnsupportedPath
    override fun getPathMatcher(syntaxAndPattern: String): PathMatcher = throw UnsupportedOperationException()
    override fun getUserPrincipalLookupService(): UserPrincipalLookupService = throw UnsupportedOperationException()
    override fun newWatchService(): WatchService = throw UnsupportedOperationException()
}

internal object UnsupportedPath : Path {
    override fun toString(): String = "com.bkahlert.kommons.path.UnsupportedPath"
    override fun hashCode(): Int = 1
    override fun equals(other: Any?): Boolean = other is UnsupportedPath
    override fun compareTo(other: Path): Int = throw UnsupportedOperationException()
    override fun getFileSystem(): FileSystem = UnsupportedFileSystem
    override fun isAbsolute(): Boolean = true
    override fun getRoot(): Path = throw UnsupportedOperationException()
    override fun getFileName(): Path = throw UnsupportedOperationException()
    override fun getParent(): Path = throw UnsupportedOperationException()
    override fun getNameCount(): Int = throw UnsupportedOperationException()
    override fun getName(index: Int): Path = throw UnsupportedOperationException()
    override fun subpath(beginIndex: Int, endIndex: Int): Path = throw UnsupportedOperationException()
    override fun startsWith(other: Path): Boolean = throw UnsupportedOperationException()
    override fun startsWith(other: String): Boolean = throw UnsupportedOperationException()
    override fun endsWith(other: Path): Boolean = throw UnsupportedOperationException()
    override fun endsWith(other: String): Boolean = throw UnsupportedOperationException()
    override fun normalize(): Path = throw UnsupportedOperationException()
    override fun resolve(other: Path): Path = throw UnsupportedOperationException()
    override fun resolve(other: String): Path = throw UnsupportedOperationException()
    override fun resolveSibling(other: Path): Path = throw UnsupportedOperationException()
    override fun resolveSibling(other: String): Path = throw UnsupportedOperationException()
    override fun relativize(other: Path): Path = throw UnsupportedOperationException()
    override fun toFile(): File = throw UnsupportedOperationException()
    override fun toUri(): URI = URI("${UnsupportedFileSystemProvider.scheme}:/path")
    override fun toAbsolutePath(): Path = throw UnsupportedOperationException()
    override fun toRealPath(vararg options: LinkOption?): Path = throw UnsupportedOperationException()
    override fun iterator(): MutableIterator<Path> = throw UnsupportedOperationException()
    override fun register(watcher: WatchService, vararg events: Kind<*>): WatchKey = throw UnsupportedOperationException()
    override fun register(watcher: WatchService, events: Array<out Kind<*>>, vararg modifiers: Modifier?): WatchKey = throw UnsupportedOperationException()
}

private val String.cs: CharSequence get() = StringBuilder(this)
