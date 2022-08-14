package com.bkahlert.kommons.io

import com.bkahlert.kommons.test.fixtures.EmojiTextDocumentFixture
import com.bkahlert.kommons.test.testAll
import com.bkahlert.kommons.test.url
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.inspectors.forAll
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldEndWith
import io.kotest.matchers.longs.shouldBeGreaterThan
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.paths.shouldExist
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.IOException
import java.net.URI
import java.nio.channels.NonWritableChannelException
import java.nio.file.AccessMode
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.ReadOnlyFileSystemException
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.copyTo
import kotlin.io.path.fileStore
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.toPath

class ClassPathFileSystemProviderTest {

    private val provider = ClassPathFileSystemProvider()

    @Test fun schema() = testAll {
        ClassPathFileSystemProvider.URI_SCHEME should {
            it shouldBe "classpath"
            it shouldBe provider.scheme
        }
    }

    @Test fun new_file_system() = testAll(
        fileBasedFileClassPathUri,
        jarBasedFileClassPathUri,
        jarBasedClassClassPathUri,
        URI("classPath:$fileBasedFileResource"),
    ) {
        shouldThrow<UnsupportedOperationException> { provider.newFileSystem(it) }
    }

    @Test fun get_file_system() = testAll {
        provider.getFileSystem(fileBasedFileClassPathUri) should { fileSystem ->
            fileSystem shouldBe provider.getPath(fileBasedFileClassPathUri).fileSystem
            fileSystem shouldBe provider.getFileSystem(URI("classPath:$fileBasedFileResource"))
        }

        shouldThrow<IOException> { provider.getFileSystem(URI("classpath:missing.file")) }
        shouldThrow<IllegalArgumentException> { provider.getFileSystem(URI("illegal:file")) }
    }

    @Test fun get_path() = testAll {
        withClue("file-based file") {
            provider.getPath(fileBasedFileClassPathUri) should { path ->
                path.fileSystem.provider().scheme shouldBe "file"
                path.shouldExist()
                path shouldBe fileBasedFileUrl.toPath()
                path.readBytes() shouldBe fileBasedFileBytes
            }
        }

        withClue("jar-based file") {
            provider.getPath(jarBasedFileClassPathUri) should { path ->
                path.fileSystem.provider().scheme shouldBe "jar"
                path.shouldExist()
                path shouldBe jarBasedFileUrl.toPath()
                path.readBytes() shouldBe jarBasedFileBytes
            }
        }

        withClue("jar-based class") {
            provider.getPath(jarBasedClassClassPathUri) should { path ->
                path.fileSystem.provider().scheme shouldBe "jar"
                path.shouldExist()
                path shouldBe jarBasedClassUrl.toPath()
                path.readBytes() shouldBe jarBasedClassBytes
            }
        }

        withClue("ignore scheme case") {
            provider.getPath(URI("classPath:$fileBasedFileResource")) shouldBe provider.getPath(fileBasedFileClassPathUri)
        }

        shouldThrow<IOException> { provider.getPath(URI("classpath:missing.file")) }
        shouldThrow<IllegalArgumentException> { provider.getPath(URI("illegal:file")) }
    }

    @Test fun paths_get() = testAll(
        fileBasedFileClassPathUri,
        jarBasedFileClassPathUri,
        jarBasedClassClassPathUri,
    ) { uri: URI ->
        Paths.get(uri) should { path ->
            path shouldBe provider.getPath(uri)
            path shouldBe uri.toPath()
        }
    }

    @Test fun new_byte_channel() = testAll {
        listOf(
            fileBasedFileClassPathUri to fileBasedFileBytes,
            jarBasedFileClassPathUri to jarBasedFileBytes,
            jarBasedClassClassPathUri to jarBasedClassBytes,
        ).forAll { (uri: URI, bytes) ->
            provider.newByteChannel(uri.toPath()).readBytes() shouldBe bytes
        }

        shouldThrow<NonWritableChannelException> {
            provider.newByteChannel(jarBasedFileClassPathUri.toPath()).newOutputStream().write(jarBasedFileBytes)
        }

        shouldThrow<IllegalArgumentException> { provider.newByteChannel(UnsupportedPath) }
            .message shouldBe "URI scheme unsupported is not supported. Only paths returned by getPath are supported."
    }


    @Test fun new_directory_stream() = testAll {
        listOf(
            fileBasedFileClassPathUri,
            jarBasedFileClassPathUri,
            jarBasedClassClassPathUri,
        ).forAll { uri: URI ->
            val path = uri.toPath()
            provider.newDirectoryStream(path.parent).toList().forAny { entry ->
                entry.shouldEndWith(path.fileName)
            }
        }

        shouldThrow<IllegalArgumentException> { provider.newDirectoryStream(UnsupportedPath) }
            .message shouldBe "URI scheme unsupported is not supported. Only paths returned by getPath are supported."
    }

    @Test fun create_directory() = testAll {
        shouldThrow<ReadOnlyFileSystemException> { provider.createDirectory(jarBasedFileClassPathUri.toPath()) }
        shouldThrow<IllegalArgumentException> { provider.createDirectory(UnsupportedPath) }
    }

    @Test fun delete() = testAll {
        shouldThrow<ReadOnlyFileSystemException> { provider.delete(jarBasedFileClassPathUri.toPath()) }
        shouldThrow<IllegalArgumentException> { provider.delete(UnsupportedPath) }
    }

    @Test fun copy(@TempDir tempDir: Path) = testAll {
        listOf(
            fileBasedFileClassPathUri to fileBasedFileBytes,
            jarBasedFileClassPathUri to jarBasedFileBytes,
            jarBasedClassClassPathUri to jarBasedClassBytes,
        ).forAll { (uri: URI, bytes) ->
            val source = uri.toPath()
            val target = tempDir.resolve(source.fileName.pathString)
            provider.copy(source, target)
            target.readBytes() shouldBe bytes
        }

        shouldThrow<IllegalArgumentException> { provider.copy(UnsupportedPath, UnsupportedPath) }
            .message shouldBe "URI scheme unsupported is not supported. Only paths returned by getPath are supported."
    }

    @Test fun move() = testAll {
        shouldThrow<ReadOnlyFileSystemException> { provider.move(jarBasedFileClassPathUri.toPath(), UnsupportedPath) }
        shouldThrow<IllegalArgumentException> { provider.move(UnsupportedPath, UnsupportedPath) }
    }

    @Test fun is_same_file() = testAll {
        listOf(
            fileBasedFileClassPathUri,
            jarBasedFileClassPathUri,
            jarBasedClassClassPathUri,
        ).forAll { uri: URI ->
            provider.isSameFile(uri.toPath(), uri.toPath()) shouldBe true
            provider.isSameFile(uri.toPath(), UnsupportedPath) shouldBe false
        }

        shouldThrow<IllegalArgumentException> { provider.isSameFile(UnsupportedPath, UnsupportedPath) }
            .message shouldBe "URI scheme unsupported is not supported. Only paths returned by getPath are supported."
    }

    @Test fun is_hidden() = testAll {
        listOf(
            fileBasedFileClassPathUri,
            jarBasedFileClassPathUri,
            jarBasedClassClassPathUri,
        ).forAll { uri: URI ->
            provider.isHidden(uri.toPath()) shouldBe false
        }

        shouldThrow<IllegalArgumentException> { provider.isHidden(UnsupportedPath) }
            .message shouldBe "URI scheme unsupported is not supported. Only paths returned by getPath are supported."
    }

    @Test fun get_file_store() = testAll {
        provider.getFileStore(jarBasedFileClassPathUri.toPath()).type() shouldBe jarBasedFileClassPathUri.toPath().fileStore().type()
        shouldThrow<IllegalArgumentException> { provider.getFileStore(UnsupportedPath) }
    }

    @Test fun check_access() = testAll {
        listOf(
            fileBasedFileClassPathUri,
            jarBasedFileClassPathUri,
            jarBasedClassClassPathUri,
        ).forAll { uri: URI ->
            shouldNotThrowAny { provider.checkAccess(uri.toPath()) }
            shouldThrow<ReadOnlyFileSystemException> { provider.checkAccess(uri.toPath(), AccessMode.WRITE) }
        }

        shouldThrow<IllegalArgumentException> { provider.checkAccess(UnsupportedPath) }
            .message shouldBe "URI scheme unsupported is not supported. Only paths returned by getPath are supported."
        shouldThrow<IllegalArgumentException> { provider.checkAccess(UnsupportedPath, AccessMode.WRITE) }
            .message shouldBe "URI scheme unsupported is not supported. Only paths returned by getPath are supported."
    }

    @Test fun get_file_attribute_view() = testAll {
        listOf(
            fileBasedFileClassPathUri,
            jarBasedFileClassPathUri,
            jarBasedClassClassPathUri,
        ).forAll { uri: URI ->
            val view = provider.getFileAttributeView(uri.toPath(), BasicFileAttributeView::class).shouldNotBeNull()
            view.readAttributes().size() shouldBeGreaterThan 0L
        }

        shouldThrow<IllegalArgumentException> { provider.getFileAttributeView(UnsupportedPath, BasicFileAttributeView::class) }
            .message shouldBe "URI scheme unsupported is not supported. Only paths returned by getPath are supported."
    }

    @Test fun read_attributes() = testAll {
        listOf(
            fileBasedFileClassPathUri,
            jarBasedFileClassPathUri,
            jarBasedClassClassPathUri,
        ).forAll { uri: URI ->
            provider.readAttributes(uri.toPath(), BasicFileAttributes::class).size() shouldBeGreaterThan 0L
            provider.readAttributes(uri.toPath(), "*")["size"].shouldBeInstanceOf<Long>() shouldBeGreaterThan 0L
        }

        shouldThrow<IllegalArgumentException> { provider.readAttributes(UnsupportedPath, BasicFileAttributes::class) }
            .message shouldBe "URI scheme unsupported is not supported. Only paths returned by getPath are supported."
        shouldThrow<IllegalArgumentException> { provider.readAttributes(UnsupportedPath, "*") }
            .message shouldBe "URI scheme unsupported is not supported. Only paths returned by getPath are supported."
    }

    @Test fun set_attribute() = testAll {
        shouldThrow<ReadOnlyFileSystemException> { provider.setAttribute(jarBasedFileClassPathUri.toPath(), "custom", true) }
        shouldThrow<IllegalArgumentException> { provider.setAttribute(UnsupportedPath, "custom", true) }
    }


    @Test fun class_path() = testAll {
        listOf(
            fileBasedFileResource,
            jarBasedFileResource,
            jarBasedClassResource,
        ).forAll { resource: String ->
            ClassPath(resource) should {
                it.shouldExist()
                it shouldBe ClassPath("classpath:$resource")
                it shouldBe Paths.get(URI("classpath:$resource"))
            }
        }

        shouldThrow<IOException> { ClassPath("invalid.file") }
    }


    @Test fun manually_copy_class_path(@TempDir tempDir: Path) = testAll {
        ClassPath(fileBasedFileResource).useBufferedInputStream {
            tempDir.resolve("classPathTextFile-streamed-copy").useOutputStream { out -> it.copyTo(out) }
        }.readBytes() shouldBe fileBasedFileBytes

        ClassPath(jarBasedClassResource).useBufferedInputStream {
            tempDir.resolve("standardLibraryClassPathClass-streamed-copy").useOutputStream { out -> it.copyTo(out) }
        }.readBytes() shouldBe jarBasedClassBytes
    }

    @Test fun kotlin_copy_class_path(@TempDir tempDir: Path) = testAll {
        ClassPath(fileBasedFileResource).copyTo(tempDir.resolve("classPathTextFile-kotlin-copy")) should {
            it.pathString shouldBe tempDir.resolve("classPathTextFile-kotlin-copy").pathString
            it.readBytes() shouldBe fileBasedFileBytes
        }

        ClassPath(jarBasedClassResource).copyTo(tempDir.resolve("standardLibraryClassPathClass-kotlin-copy")) should {
            it.pathString shouldBe tempDir.resolve("standardLibraryClassPathClass-kotlin-copy").pathString
            it.readBytes() shouldBe jarBasedClassBytes
        }
    }

    @Test fun kotlin_copy_class_path_to_directory(@TempDir tempDir: Path) = testAll {
        ClassPath(fileBasedFileResource).copyToDirectory(tempDir) should {
            it.pathString shouldBe tempDir.resolve(fileBasedFileResource).pathString
            it.readBytes() shouldBe fileBasedFileBytes
        }

        ClassPath(jarBasedClassResource).copyToDirectory(tempDir) should {
            it.pathString shouldBe tempDir.resolve("Regex.class").pathString
            it.readBytes() shouldBe jarBasedClassBytes
        }
    }

    @Test fun kotlin_copy_class_path_to_directory__multiple(@TempDir tempDir: Path) = testAll {
        for (i in 0..20) {
            ClassPath(fileBasedFileResource).copyTo(tempDir.resolve(fileBasedFileResource + i)) should {
                it.pathString shouldBe tempDir.resolve(fileBasedFileResource + i).pathString
                it.readBytes() shouldBe fileBasedFileBytes
            }

            ClassPath(jarBasedClassResource).copyTo(tempDir.resolve("Regex.class$i")) should {
                it.pathString shouldBe tempDir.resolve("Regex.class$i").pathString
                it.readBytes() shouldBe jarBasedClassBytes
            }
        }
    }
}

internal const val fileBasedFileResource = "61C285F09D95930D0AE298B00AF09F918B0A.txt"
internal val fileBasedFileClassPathUri = URI("classpath:$fileBasedFileResource")
internal val fileBasedFileUrl = checkNotNull(Thread.currentThread().contextClassLoader.getResource(fileBasedFileResource)) {
    "unable to locate $fileBasedFileResource"
}
internal val fileBasedFileBytes = fileBasedFileUrl.readBytes()

internal val jarBasedFileResource = "fixtures/${EmojiTextDocumentFixture.name}"
internal val jarBasedFileClassPathUri = URI("classpath:$jarBasedFileResource")
internal val jarBasedFileUrl = EmojiTextDocumentFixture.url
internal val jarBasedFileBytes = jarBasedFileUrl.readBytes()

internal const val jarBasedClassResource = "kotlin.text.Regex.class"
internal val jarBasedClassClassPathUri = URI("classpath:$jarBasedClassResource")
internal val jarBasedClassUrl = checkNotNull(Regex::class.java.getResource("Regex.class")) {
    "unable to locate $jarBasedClassResource"
}
internal val jarBasedClassBytes = jarBasedClassUrl.readBytes()
