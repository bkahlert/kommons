package koodies.io.file

import koodies.io.path.copyTo
import koodies.io.path.delete
import koodies.io.path.executable
import koodies.io.path.hasContent
import koodies.io.path.randomFile
import koodies.io.path.randomPath
import koodies.runtime.deleteOnExit
import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.ReadOnlyFileSystemException
import kotlin.io.path.exists
import kotlin.io.path.isReadable
import kotlin.io.path.isWritable
import kotlin.io.path.moveTo

@Execution(CONCURRENT)
class AsReadyOnlyKtTest {

    @TestFactory
    fun `should allow`(uniqueId: UniqueId) = listOf(
        allowedFileOperation(
            "readable", uniqueId,
            { isReadable() },
            { Files.isReadable(this) },
        ) { isTrue() },

        allowedFileOperation(
            "writable", uniqueId,
            { isWritable() },
            { Files.isWritable(this) },
        ) { isFalse() },

        allowedFileOperation(
            "executable", uniqueId,
            { executable },
            { Files.isExecutable(this) },
        ) { },

        allowedFileOperation(
            "exists", uniqueId,
            { exists() },
            { Files.exists(this) },
        ) { isTrue() },

        allowedFileOperation(
            "copy", uniqueId,
            { copyTo(sameFile("$fileName").deleteOnExit()) },
            { sameFile("$fileName").deleteOnExit().also { Files.copy(this, it) } },
        ) { get { sameFile("$fileName") }.hasContent("line #1\nline #2\n") },

        allowedFileOperation(
            "buffered reading", uniqueId,
            { bufferedInputStream().bufferedReader().readText() },
            { Files.newBufferedReader(this).readText() },
        ) { isEqualTo("line #1\nline #2\n") },
    )

    @TestFactory
    fun `should disallow`(uniqueId: UniqueId) = listOf(

        disallowedFileOperation(
            "move", uniqueId,
            { withTempDir(uniqueId) { moveTo(randomPath()) } },
            { withTempDir(uniqueId) { Files.move(this, randomPath()) } },
        ) { isA<java.nio.file.FileSystemException>() },

        disallowedFileOperation(
            "any type of output stream", uniqueId,
            { outputStream() },
            { Files.newBufferedWriter(this) },
        ) { isA<ReadOnlyFileSystemException>() },

        disallowedFileOperation(
            "delete", uniqueId,
            { delete() },
            { Files.delete(this) },
        ) { isA<ReadOnlyFileSystemException>() },
    )
}

internal inline fun <reified T> allowedFileOperation(
    name: String,
    uniqueId: UniqueId,
    vararg variants: Path.() -> T,
    crossinline validator: Assertion.Builder<T>.() -> Unit,
): DynamicContainer {
    return dynamicContainer("call to $name", variants.map { variant ->
        dynamicTest("$variant") {
            withTempDir(uniqueId) {
                val tempFile = randomFile().writeText("line #1\nline #2\n").asReadOnly()
                expectThat(variant(tempFile)).validator()
            }
        }
    })
}

internal inline fun disallowedFileOperation(
    name: String,
    uniqueId: UniqueId,
    vararg variants: Path.() -> Unit,
    crossinline validator: Assertion.Builder<Throwable>.() -> Unit,
): DynamicContainer {
    return dynamicContainer("call to $name", variants.map { variant ->
        dynamicTest("$variant") {
            withTempDir(uniqueId) {
                val tempFile = randomFile().writeText("line #1\nline #2\n").asReadOnly()
                expectCatching { variant(tempFile) }.isFailure().validator()
            }
        }
    })
}
