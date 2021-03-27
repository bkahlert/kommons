@file:Suppress("PublicApiImplicitType")

package koodies.io.path

import koodies.debug.debug
import koodies.debug.replaceNonPrintableCharacters
import koodies.io.file.lastModified
import koodies.io.file.quoted
import koodies.io.file.resolveBetweenFileSystems
import koodies.regex.countMatches
import koodies.text.LineSeparators
import koodies.text.quoted
import koodies.text.truncate
import koodies.time.Now
import koodies.time.minus
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.hasSize
import java.io.File
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes
import kotlin.io.path.readText
import kotlin.time.Duration

fun <T : CharSequence> Assertion.Builder<T>.isEqualToByteWise(other: CharSequence) =
    assert("is equal to byte-wise") { value ->
        val thisString = value.toList()
        val otherString = other.toList()
        when (thisString.containsAll(otherString)) {
            true -> pass()
            else -> fail("\nwas        ${otherString.debug}" +
                "\ninstead of ${thisString.debug}.")
        }
    }

@Suppress("unused")
fun <T : CharSequence> Assertion.Builder<T>.containsAtLeast(value: CharSequence, lowerLimit: Int = 1) =
    assert("contains ${value.quoted} at least ${lowerLimit}x") {
        val actual = Regex.fromLiteral("$value").countMatches(it)
        if (actual >= lowerLimit) pass()
        else fail("but actually contains it only ${actual}x")
    }

fun <T : CharSequence> Assertion.Builder<T>.containsAtMost(value: CharSequence, limit: Int = 1) =
    assert("contains ${value.quoted} at most ${limit}x") {
        val actual = Regex.fromLiteral(value.toString()).countMatches(it)
        if (actual <= limit) pass()
        else fail("but actually contains it even ${actual}x")
    }

fun <T : CharSequence> Assertion.Builder<T>.containsExactly(value: CharSequence, expectedCount: Int) =
    assert("contains ${value.quoted} exactly ${expectedCount}x") {
        val actual = Regex.fromLiteral(value.toString()).countMatches(it)
        if (actual == expectedCount) pass()
        else fail("but actually contains it ${actual}x")
    }

fun <T : CharSequence> Assertion.Builder<T>.notContainsLineSeparator() =
    assert("contains line separator") { value ->
        val matchedSeparators = LineSeparators.filter { value.contains(it) }
        if (matchedSeparators.isEmpty()) pass()
        else fail("but the following have been found: $matchedSeparators")
    }


fun Assertion.Builder<String>.prefixes(value: String) =
    assert("prefixed by $value") { prefix ->
        if (value.startsWith(prefix)) pass()
        else fail("$value is not prefixed by ${prefix.debug}")
    }


fun <T> Assertion.Builder<List<T>>.single(assertion: Assertion.Builder<T>.() -> Unit) {
    hasSize(1).and { get { this[0] }.run(assertion) }
}

fun Assertion.Builder<File>.exists() =
    assert("exists") {
        when (it.exists()) {
            true -> pass()
            else -> fail()
        }
    }


fun <T : CharSequence> Assertion.Builder<T>.containsOnlyCharacters(chars: CharArray) =
    assert("contains only the characters " + chars.toString().truncate(20)) {
        val unexpectedCharacters = it.filter { char: Char -> !chars.contains(char) }
        when (unexpectedCharacters.isEmpty()) {
            true -> pass()
            else -> fail("contained unexpected characters: " + unexpectedCharacters.toString().truncate(20))
        }
    }

fun <T : Path> Assertion.Builder<T>.hasContent(expectedContent: String) =
    assert("has content ${expectedContent.debug}") {
        val actualContent = it.readText()
        when (actualContent.contentEquals(expectedContent)) {
            true -> pass()
            else -> fail("was ${actualContent.debug}")
        }
    }

val <T : Path> Assertion.Builder<T>.text: Assertion.Builder<String>
    get() = get("get text") { readText() }

val <T : Path> Assertion.Builder<T>.bytes: Assertion.Builder<ByteArray>
    get() = get("get bytes") { readBytes() }

fun <T : Path> Assertion.Builder<T>.hasEqualContent(other: Path) =
    assert("has equal content as \"$other\"") {
        val actualContent = it.readBytes()
        val expectedContent = other.readBytes()
        when (actualContent.contentEquals(expectedContent)) {
            true -> pass()
            else -> fail(
                "was ${actualContent.size} instead of ${expectedContent.size} bytes.\n" +
                    "Actual content:\n" + String(actualContent).replaceNonPrintableCharacters() + "\n" +
                    "Expected content:\n" + String(expectedContent).replaceNonPrintableCharacters() + "\n")
        }
    }

fun <T : Path> Assertion.Builder<Pair<T, Path>>.haveEqualContent() =
    assert("have same content") {
        val firstContent = it.first.readBytes()
        val lastContent = it.second.readBytes()
        when (firstContent.contentEquals(lastContent)) {
            true -> pass()
            else -> fail(
                "was ${firstContent.size} instead of ${lastContent.size} bytes.\n" +
                    "Content #1:\n" + String(firstContent).replaceNonPrintableCharacters() + "\n" +
                    "Content #2:\n" + String(lastContent).replaceNonPrintableCharacters() + "\n")
        }
    }

fun <T : Path> Assertion.Builder<T>.hasSameFiles(other: Path) =
    assert("has same files as ${other.quoted}") { actual ->
        expectThat(actual).containsAllFiles(other)
        expectThat(other).containsAllFiles(actual)
    }


fun <T : Path> Assertion.Builder<T>.containsAllFiles(other: Path) =
    assert("contains all files as ${other.quoted}") { actual ->
        if (!actual.isDirectory()) fail("$actual is no directory")
        if (!other.isDirectory()) fail("$other is no directory")
        other.listDirectoryEntriesRecursively().filter { it.isRegularFile() }.forEach { otherPath ->
            val relativePath = other.relativize(otherPath)
            val actualPath = actual.resolveBetweenFileSystems(relativePath)
            if (actualPath.readText() != otherPath.readText()) fail("$actualPath and $otherPath have different content:\nactual: ${actual.readText()}\nexpected:${otherPath.readText()}")
        }
        pass()
    }

fun <T : Path> Assertion.Builder<T>.absolutePathMatches(regex: Regex) =
    assert("matches ${regex.pattern}") {
        when (it.toAbsolutePath().toString().matches(regex)) {
            true -> pass()
            else -> fail()
        }
    }

fun <T : Path> Assertion.Builder<T>.isEmptyDirectory() =
    assert("is empty directory") { self ->
        val files = self.listDirectoryEntriesRecursively().filter { current -> current != self }
        when (files.isEmpty()) {
            true -> pass()
            else -> fail("contained $files")
        }
    }

fun <T : Path> Assertion.Builder<T>.lastModified(duration: Duration) =
    assert("was last modified at most $duration ago") {
        val now = Now.fileTime
        val recent = now - duration
        when (it.lastModified) {
            in (recent..now) -> pass()
            else -> fail()
        }
    }
