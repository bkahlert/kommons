package koodies.io.path

import koodies.text.randomString
import koodies.time.Now
import java.io.BufferedWriter
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.FileTime
import kotlin.io.path.appendLines
import kotlin.io.path.bufferedWriter
import kotlin.io.path.createDirectories
import kotlin.io.path.createFile
import kotlin.io.path.exists
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.setLastModifiedTime
import kotlin.io.path.writeLines
import kotlin.io.path.writeText
import kotlin.streams.asSequence
import kotlin.streams.toList
import kotlin.time.Duration
import kotlin.time.milliseconds

/**
 * Default [StandardOpenOption] arguments for append operations such as [Path.writeLines].
 *
 * The effect is that a file will be created if it does not exist and otherwise overridden.
 */
val DEFAULT_WRITE_OPTIONS: Array<StandardOpenOption> =
    arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)

/**
 * Default [StandardOpenOption] arguments for append operations such as [Path.appendLines].
 *
 * The effect is that a file will be created if it does not exist and otherwise appended to.
 */
val DEFAULT_APPEND_OPTIONS: Array<StandardOpenOption> =
    arrayOf(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND)

/**
 * Returns a new [BufferedWriter] for writing the content of this file
 * encoded using [Charsets.UTF_8] and using the specified [DEFAULT_BUFFER_SIZE] by default.
 */
fun Path.bufferedWriter(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter =
    bufferedWriter(charset, bufferSize, *DEFAULT_WRITE_OPTIONS)

/**
 * Returns a new [BufferedWriter] for appending to the content of this file
 * encoded using [Charsets.UTF_8] and using the specified [DEFAULT_BUFFER_SIZE] by default.
 */
fun Path.bufferedAppendingWriter(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter =
    bufferedWriter(charset, bufferSize, *DEFAULT_APPEND_OPTIONS)

/**
 * Sets the content of this file as [text]
 * encoded using [Charsets.UTF_8] by default.
 *
 * The file will be overwritten if it already exists.
 */
fun Path.writeText(text: String, charset: Charset = Charsets.UTF_8) =
    writeText(text, charset, *DEFAULT_WRITE_OPTIONS)

/**
 * Write the specified [line] to a file terminating it with the platform's line separator
 * encoded using [Charsets.UTF_8] by default.
 *
 * The file will be overwritten if it already exists.
 */
fun Path.writeLine(line: CharSequence, charset: Charset = Charsets.UTF_8): Path =
    writeLines(listOf(line), charset, *DEFAULT_WRITE_OPTIONS)

/**
 * Write the specified [lines] to a file terminating each one with the platform's line separator
 * encoded using [Charsets.UTF_8] by default.
 *
 * The file will be overwritten if it already exists.
 */
fun Path.writeLines(vararg lines: CharSequence, charset: Charset = Charsets.UTF_8): Path =
    writeLines(lines.asIterable(), charset, *DEFAULT_WRITE_OPTIONS)

/**
 * Appends the specified [line] to a file terminating it with the platform's line separator
 * using [Charsets.UTF_8] by default.
 *
 * The file will be appended to if it already exists.
 */
fun Path.appendLine(line: CharSequence, charset: Charset = Charsets.UTF_8): Path =
    appendLines(listOf(line), charset)

/**
 * Appends the specified [lines] to a file terminating each one with the platform's line separator
 * using [Charsets.UTF_8] by default.
 *
 * The file will be appended to if it already exists.
 */
fun Path.appendLines(vararg lines: CharSequence, charset: Charset = Charsets.UTF_8): Path =
    appendLines(lines.asIterable(), charset)


private fun Path.getPathMatcher(glob: String): PathMatcher? {
    // avoid creating a matcher if all entries are required.
    if (glob == "*" || glob == "**" || glob == "**/*") return null

    // create a matcher and return a filter that uses it.
    return fileSystem.getPathMatcher("glob:$glob")
}

/**
 * Returns a list of the entries in this directory and its sub directories
 * optionally filtered by matching against the specified [glob] pattern.
 *
 * @param glob the globbing pattern. The syntax is specified by the [FileSystem.getPathMatcher] method.
 *
 * @throws java.util.regex.PatternSyntaxException if the glob pattern is invalid.
 * @throws NotDirectoryException If this path does not refer to a directory.
 * @throws IOException If an I/O error occurs.
 *
 * @see Files.walk
 */
fun Path.listDirectoryEntriesRecursively(glob: String = "*"): List<Path> =
    getPathMatcher(glob)?.let {
        Files.walk(this).filter { path -> it.matches(path) }.toList()
    } ?: Files.walk(this).toList()

/**
 * Calls the [block] callback with a sequence of all entries in this directory
 * and its sub directories optionally filtered by matching against the specified [glob] pattern.
 *
 * @param glob the globbing pattern. The syntax is specified by the [FileSystem.getPathMatcher] method.
 *
 * @throws java.util.regex.PatternSyntaxException if the glob pattern is invalid.
 * @throws NotDirectoryException If this path does not refer to a directory.
 * @throws IOException If an I/O error occurs.
 * @return the value returned by [block].
 *
 * @see Files.walk
 */
fun <T> Path.useDirectoryEntriesRecursively(glob: String = "*", block: (Sequence<Path>) -> T): T =
    (getPathMatcher(glob)?.let {
        Files.walk(this).filter { path -> it.matches(path) }
    } ?: Files.walk(this)).use { block(it.asSequence()) }

/**
 * Performs the given [action] on each entry in this directory and its sub directories
 * optionally filtered by matching against the specified [glob] pattern.
 *
 * @param glob the globbing pattern. The syntax is specified by the [FileSystem.getPathMatcher] method.
 *
 * @throws java.util.regex.PatternSyntaxException if the glob pattern is invalid.
 * @throws NotDirectoryException If this path does not refer to a directory.
 * @throws IOException If an I/O error occurs.
 *
 * @see Files.walk
 */
fun Path.forEachDirectoryEntryRecursively(glob: String = "*", action: (Path) -> Unit) =
    (getPathMatcher(glob)?.let {
        Files.walk(this).filter { path -> it.matches(path) }
    } ?: Files.walk(this)).use { it.forEach(action) }


/**
 * Deletes this file or empty directory.
 *
 * Returns the deletes path.
 */
fun Path.delete(): Path =
    apply {
        if (exists()) {
            Files.delete(this)
        }
    }

/**
 * Deletes this file or directory recursively.
 *
 * Returns the deletes path.
 */
fun Path.deleteRecursively(): Path =
    apply {
        if (exists()) {
            if (isDirectory()) forEachDirectoryEntry { it.deleteRecursively() }
            delete()
        }
    }


/**
 * Returns this [Path] with a random path segment added.
 */
fun Path.randomPath(base: String, extension: String): Path {
    val minLength = 6
    val length = base.length + extension.length
    val randomLength = (minLength - length).coerceAtLeast(3)
    return resolve("$base${randomString(randomLength)}$extension")
}

/**
 * Creates a  this [Path] with a random file name added
 */
fun Path.randomFile(base: String = randomString(4), extension: String = ".tmp"): Path =
    randomPath(base, extension).createFile()

/**
 * Returns this [Path] with all parent directories created.
 *
 * Example: If directory `/some/where` existed and this method was called on `/some/where/resides/a/file`,
 * the missing directories `/some/where/resides` and `/some/where/resides/a` would be created.
 */
fun Path.withDirectoriesCreated() = also { parent?.createDirectories() }

/**
 * Contains since when this file was last modified.
 */
var Path.age: Duration
    get() :Duration = (Now.millis - getLastModifiedTime().toMillis()).milliseconds
    set(value) {
        setLastModifiedTime(FileTime.from(Now.minus(value)))
    }
