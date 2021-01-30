package koodies.io.path

import koodies.io.noDirectory
import koodies.io.path.Defaults.DEFAULT_APPEND_OPTIONS
import koodies.io.path.Defaults.DEFAULT_WRITE_OPTIONS
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.NotDirectoryException
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.StandardOpenOption
import java.util.stream.Stream
import kotlin.io.path.bufferedWriter
import kotlin.io.path.exists
import kotlin.io.path.forEachDirectoryEntry
import kotlin.io.path.inputStream
import kotlin.io.path.isDirectory
import kotlin.io.path.outputStream
import kotlin.io.path.useLines
import kotlin.io.path.writeLines
import kotlin.io.path.writeText
import kotlin.streams.asSequence
import kotlin.streams.toList
import kotlin.io.path.appendBytes as kotlinAppendBytes
import kotlin.io.path.writeBytes as kotlinWriteBytes

/*
 * Opinionated list of missing StdLib extension functions.
 */

/**
 * Constructs a buffered input stream wrapping a new [FileInputStream] of this file and returns it as a result.
 */
fun Path.bufferedInputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream = inputStream().buffered(bufferSize)

/**
 * Constructs a buffered output stream wrapping a new [FileOutputStream] of this file and returns it as a result.
 */
fun Path.bufferedOutputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedOutputStream = outputStream().buffered(bufferSize)


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
 * Writes an [array] of bytes to this file.
 *
 * By default, the file will be overwritten if it already exists, but you can control this behavior
 * with [options].
 *
 * @param array byte array to write into this file.
 * @param options options to determine how the file is opened.
 */
fun Path.writeBytes(array: ByteArray): Path =
    apply { kotlinWriteBytes(array) }

/**
 * Appends an [array] of bytes to the content of this file.
 *
 * @param array byte array to append to this file.
 */
fun Path.appendBytes(array: ByteArray): Path =
    apply { kotlinAppendBytes(array) }

/**
 * Sets the content of this file as [text]
 * encoded using [Charsets.UTF_8] by default.
 *
 * The file will be overwritten if it already exists.
 */
fun Path.writeText(text: String, charset: Charset = Charsets.UTF_8): Path =
    apply { writeText(text, charset, *DEFAULT_WRITE_OPTIONS) }

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset].
 *
 * @param text text to append to file.
 * @param charset character set to use for writing text, UTF-8 by default.
 */
fun Path.appendText(text: String, charset: Charset = Charsets.UTF_8, vararg options: StandardOpenOption = DEFAULT_APPEND_OPTIONS): Path =
    apply { outputStream(*options).writer(charset).use { it.write(text) } }

/**
 * Write the specified [line] to a file terminating it with the platform's line separator
 * encoded using [Charsets.UTF_8] by default.
 *
 * The file will be overwritten if it already exists.
 */
fun Path.writeLine(line: CharSequence, charset: Charset = Charsets.UTF_8): Path =
    writeLines(listOf(line), charset, *DEFAULT_WRITE_OPTIONS)

/**
 * Reads the file content as a list of lines and returns
 * the line with the specified [index]—starting to count with `1`.
 *
 * @param charset character set to use for reading text, UTF-8 by default.
 * @return list of file lines.
 */
fun Path.readLine(index: Int, charset: Charset = Charsets.UTF_8): String? {
    require(index > 0) { "Index $index must be positive. The first line has index 1." }
    return useLines(charset) { it.drop(index - 1).firstOrNull() }
}

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

/**
 * Appends the specified collection of char sequences [lines] to a file terminating each one with the platform's line separator
 * using [Charsets.UTF_8] by default.
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 */
fun Path.appendLines(lines: Iterable<CharSequence>, charset: Charset = Charsets.UTF_8): Path =
    Files.write(this, lines, charset, *DEFAULT_APPEND_OPTIONS)

/**
 * Appends the specified sequence of char sequences [lines] to a file terminating each one with the platform's line separator
 * using [Charsets.UTF_8] by default.
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 */
fun Path.appendLines(lines: Sequence<CharSequence>, charset: Charset = Charsets.UTF_8): Path =
    Files.write(this, lines.asIterable(), charset, *DEFAULT_APPEND_OPTIONS)


/**
 * String representation of this path that does **not** rely on [toString].
 */
fun Path.asString(): String = "${resolve("")}"


private fun Path.getPathMatcher(glob: String): PathMatcher? {
    // avoid creating a matcher if all entries are required.
    if (glob == "*" || glob == "**" || glob == "**/*") return null

    // create a matcher and return a filter that uses it.
    return fileSystem.getPathMatcher("glob:$glob")
}

private fun Path.streamContentsRecursively(glob: String = "*"): Stream<Path> {
    if (!isDirectory()) throw noDirectory()
    val walk = Files.walk(this).filter { it != this }
    return getPathMatcher(glob)
        ?.let { matcher -> walk.filter { path -> matcher.matches(path) } }
        ?: walk
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
    streamContentsRecursively(glob).toList()

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
    streamContentsRecursively(glob).use { block(it.asSequence()) }

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
    streamContentsRecursively(glob).use { it.forEach(action) }


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
 * Contains whether this path belongs to the default [FileSystem].
 *
 * @see [FileSystems.getDefault]
 */
fun Path.isDefaultFileSystem(): Boolean = fileSystem == FileSystems.getDefault()
