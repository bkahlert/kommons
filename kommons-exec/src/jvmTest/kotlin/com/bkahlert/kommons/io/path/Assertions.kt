package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.debug.replaceNonPrintableCharacters
import com.bkahlert.kommons.io.listDirectoryEntriesRecursively
import com.bkahlert.kommons.quoted
import com.bkahlert.kommons.io.resolveBetweenFileSystems
import com.bkahlert.kommons.text.LineSeparators.LF
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.contentEquals
import strikt.assertions.isEqualTo
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.pathString
import kotlin.io.path.readBytes
import kotlin.io.path.readText

fun Builder<File>.exists() =
    assert("exists") {
        when (it.exists()) {
            true -> pass()
            else -> fail()
        }
    }


fun <T : Path> Builder<T>.hasContent(expectedContent: String): Builder<T> =
    and { textContent.isEqualTo(expectedContent) }

fun <T : Path> Builder<T>.hasContent(expectedContent: ByteArray): Builder<T> =
    and { byteContent.contentEquals(expectedContent) }

val <T : Path> Builder<T>.textContent: Builder<String>
    get() = get("text content") { readText() }

fun Builder<Path>.textContent(assertion: Builder<String>.() -> Unit): Builder<Path> =
    with("text content", { readText() }, assertion)

val <T : Path> Builder<T>.byteContent: Builder<ByteArray>
    get() = get("get bytes") { readBytes() }

fun Builder<Path>.byteContent(assertion: Builder<ByteArray>.() -> Unit): Builder<Path> =
    with("byte content", { readBytes() }, assertion)


fun <T : Path> Builder<T>.hasEqualContent(other: Path) =
    assert("has equal content as \"$other\"") {
        val actualContent = it.readBytes()
        val expectedContent = other.readBytes()
        when (actualContent.contentEquals(expectedContent)) {
            true -> pass()
            else -> fail(
                "was ${actualContent.size} instead of ${expectedContent.size} bytes.$LF" +
                    "Actual content:$LF" + String(actualContent).replaceNonPrintableCharacters() + LF +
                    "Expected content:$LF" + String(expectedContent).replaceNonPrintableCharacters() + LF
            )
        }
    }

fun <T : Path> Builder<Pair<T, Path>>.haveEqualContent() =
    assert("have same content") {
        val firstContent = it.first.readBytes()
        val lastContent = it.second.readBytes()
        when (firstContent.contentEquals(lastContent)) {
            true -> pass()
            else -> fail(
                "was ${firstContent.size} instead of ${lastContent.size} bytes.$LF" +
                    "Content #1:$LF" + String(firstContent).replaceNonPrintableCharacters() + LF +
                    "Content #2:$LF" + String(lastContent).replaceNonPrintableCharacters() + LF
            )
        }
    }

fun <T : Path> Builder<T>.hasSameFiles(other: Path) =
    assert("has same files as ${other.pathString.quoted}") { actual ->
        expectThat(actual).containsAllFiles(other)
        expectThat(other).containsAllFiles(actual)
    }


fun <T : Path> Builder<T>.containsAllFiles(other: Path) =
    assert("contains all files as ${other.pathString.quoted}") { actual ->
        if (!actual.isDirectory()) fail("$actual is no directory")
        if (!other.isDirectory()) fail("$other is no directory")
        other.listDirectoryEntriesRecursively().filter { it.isRegularFile() }.forEach { otherPath ->
            val relativePath = other.relativize(otherPath)
            val actualPath = actual.resolveBetweenFileSystems(relativePath)
            if (actualPath.readText() != otherPath.readText()) {
                fail("$actualPath and $otherPath have different content:\nactual: ${actual.readText()}\nexpected:${otherPath.readText()}")
            }
        }
        pass()
    }

val <T : Path> Builder<T>.pathString
    get() = get("path as string") { pathString }

/**
 * Asserts that the subject is writable.
 *
 * @see Files.isWritable
 */
fun <T : Path> Builder<T>.isWritable(): Builder<T> =
    assertThat("is writable") { Files.isWritable(it) }
