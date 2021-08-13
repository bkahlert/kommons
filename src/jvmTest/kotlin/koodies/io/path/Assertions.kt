package koodies.io.path

import koodies.debug.replaceNonPrintableCharacters
import koodies.io.file.lastModified
import koodies.io.file.quoted
import koodies.io.file.resolveBetweenFileSystems
import koodies.text.LineSeparators.LF
import koodies.text.truncate
import koodies.time.Now
import koodies.time.minus
import strikt.api.Assertion.Builder
import strikt.api.expectThat
import strikt.assertions.contentEquals
import strikt.assertions.isEqualTo
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.time.Duration

fun Builder<File>.exists() =
    assert("exists") {
        when (it.exists()) {
            true -> pass()
            else -> fail()
        }
    }

fun <T : CharSequence> Builder<T>.containsOnlyCharacters(chars: CharArray) =
    assert("contains only the characters " + chars.toString().truncate(20)) {
        val unexpectedCharacters = it.filter { char: Char -> !chars.contains(char) }
        when (unexpectedCharacters.isEmpty()) {
            true -> pass()
            else -> fail("contained unexpected characters: " + unexpectedCharacters.toString().truncate(20))
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
                    "Expected content:$LF" + String(expectedContent).replaceNonPrintableCharacters() + LF)
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
                    "Content #2:$LF" + String(lastContent).replaceNonPrintableCharacters() + LF)
        }
    }

fun <T : Path> Builder<T>.hasSameFiles(other: Path) =
    assert("has same files as ${other.quoted}") { actual ->
        expectThat(actual).containsAllFiles(other)
        expectThat(other).containsAllFiles(actual)
    }


fun <T : Path> Builder<T>.containsAllFiles(other: Path) =
    assert("contains all files as ${other.quoted}") { actual ->
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

fun <T : Path> Builder<T>.absolutePathMatches(regex: Regex) =
    assert("matches ${regex.pattern}") {
        when (it.toAbsolutePath().toString().matches(regex)) {
            true -> pass()
            else -> fail()
        }
    }

fun <T : Path> Builder<T>.isEmptyDirectory() =
    assert("is empty directory") { self ->
        val files = self.listDirectoryEntriesRecursively().filter { current -> current != self }
        when (files.isEmpty()) {
            true -> pass()
            else -> fail("contained $files")
        }
    }

fun <T : Path> Builder<T>.lastModified(duration: Duration) =
    assert("was last modified at most $duration ago") {
        val now = Now.fileTime
        val recent = now - duration
        when (it.lastModified) {
            in (recent..now) -> pass()
            else -> fail()
        }
    }
