package koodies.io.path

import koodies.io.noDirectory
import koodies.io.path.Defaults.DEFAULT_APPEND_OPTIONS
import koodies.io.path.Defaults.DEFAULT_WRITE_OPTIONS
import koodies.runtime.onExit
import koodies.time.seconds
import koodies.time.sleep
import koodies.unit.milli
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileSystem
import java.nio.file.FileSystems
import java.nio.file.FileVisitOption
import java.nio.file.Files
import java.nio.file.LinkOption
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
public fun Path.bufferedInputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream = inputStream().buffered(bufferSize)

/**
 * Constructs a buffered output stream wrapping a new [FileOutputStream] of this file and returns it as a result.
 */
public fun Path.bufferedOutputStream(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedOutputStream = outputStream().buffered(bufferSize)


/**
 * Returns a new [BufferedWriter] for writing the content of this file
 * encoded using [Charsets.UTF_8] and using the specified [DEFAULT_BUFFER_SIZE] by default.
 */
public fun Path.bufferedWriter(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter =
    bufferedWriter(charset, bufferSize, *DEFAULT_WRITE_OPTIONS)

/**
 * Returns a new [BufferedWriter] for appending to the content of this file
 * encoded using [Charsets.UTF_8] and using the specified [DEFAULT_BUFFER_SIZE] by default.
 */
public fun Path.bufferedAppendingWriter(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter =
    bufferedWriter(charset, bufferSize, *DEFAULT_APPEND_OPTIONS)


/**
 * Writes an [array] of bytes to this file.
 *
 * @param array byte array to write into this file.
 */
public fun Path.writeBytes(array: ByteArray): Path =
    apply { kotlinWriteBytes(array) }

/**
 * Appends an [array] of bytes to the content of this file.
 *
 * @param array byte array to append to this file.
 */
public fun Path.appendBytes(array: ByteArray): Path =
    apply { kotlinAppendBytes(array) }

/**
 * Sets the content of this file as [text]
 * encoded using [Charsets.UTF_8] by default.
 *
 * The file will be overwritten if it already exists.
 */
public fun Path.writeText(text: String, charset: Charset = Charsets.UTF_8): Path =
    apply { writeText(text, charset, *DEFAULT_WRITE_OPTIONS) }

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset].
 *
 * @param text text to append to file.
 * @param charset character set to use for writing text, UTF-8 by default.
 */
public fun Path.appendText(text: String, charset: Charset = Charsets.UTF_8, vararg options: StandardOpenOption = DEFAULT_APPEND_OPTIONS): Path =
    apply { outputStream(*options).writer(charset).use { it.write(text) } }

/**
 * Write the specified [line] to a file terminating it with the platform's line separator
 * encoded using [Charsets.UTF_8] by default.
 *
 * The file will be overwritten if it already exists.
 */
public fun Path.writeLine(line: CharSequence, charset: Charset = Charsets.UTF_8): Path =
    writeLines(listOf(line), charset, *DEFAULT_WRITE_OPTIONS)

/**
 * Reads the file content as a list of lines and returns
 * the line with the specified [index]—starting to count with `1`.
 *
 * @param charset character set to use for reading text, UTF-8 by default.
 * @return list of file lines.
 */
public fun Path.readLine(index: Int, charset: Charset = Charsets.UTF_8): String? {
    require(index > 0) { "Index $index must be positive. The first line has index 1." }
    return useLines(charset) { it.drop(index - 1).firstOrNull() }
}

/**
 * Write the specified [lines] to a file terminating each one with the platform's line separator
 * encoded using [Charsets.UTF_8] by default.
 *
 * The file will be overwritten if it already exists.
 */
public fun Path.writeLines(vararg lines: CharSequence, charset: Charset = Charsets.UTF_8): Path =
    writeLines(lines.asIterable(), charset, *DEFAULT_WRITE_OPTIONS)

/**
 * Appends the specified [line] to a file terminating it with the platform's line separator
 * using [Charsets.UTF_8] by default.
 *
 * The file will be appended to if it already exists.
 */
public fun Path.appendLine(line: CharSequence, charset: Charset = Charsets.UTF_8): Path =
    appendLines(listOf(line), charset)

/**
 * Appends the specified [lines] to a file terminating each one with the platform's line separator
 * using [Charsets.UTF_8] by default.
 *
 * The file will be appended to if it already exists.
 */
public fun Path.appendLines(vararg lines: CharSequence, charset: Charset = Charsets.UTF_8): Path =
    appendLines(lines.asIterable(), charset)

/**
 * Appends the specified collection of character sequences [lines] to a file terminating each one with the platform's line separator
 * using [Charsets.UTF_8] by default.
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 */
public fun Path.appendLines(lines: Iterable<CharSequence>, charset: Charset = Charsets.UTF_8): Path =
    Files.write(this, lines, charset, *DEFAULT_APPEND_OPTIONS)

/**
 * Appends the specified sequence of character sequences [lines] to a file terminating each one with the platform's line separator
 * using [Charsets.UTF_8] by default.
 *
 * @param charset character set to use for writing text, UTF-8 by default.
 */
public fun Path.appendLines(lines: Sequence<CharSequence>, charset: Charset = Charsets.UTF_8): Path =
    Files.write(this, lines.asIterable(), charset, *DEFAULT_APPEND_OPTIONS)

/**
 * String representation of this path that does **not** rely on [toString].
 */
@Deprecated("use pathString", replaceWith = ReplaceWith("this.pathString", "koodies.io.path.pathString"))
public fun Path.asString(): String = pathString

/**
 * String representation of this path that does **not** rely on [toString].
 */
public val Path.pathString: String get() = "${resolve("")}"

/**
 * String representation of this path's [Path.getFileName] that does **not** rely on [toString].
 */
public val Path.fileNameString: String get() = fileName.pathString

/**
 * String representation of this path's [URI].
 */
public val Path.uriString: String get() = toUri().toString()


private fun Path.getPathMatcher(glob: String): PathMatcher? {
    // avoid creating a matcher if all entries are required.
    if (glob == "*" || glob == "**" || glob == "**/*") return null

    // create a matcher and return a filter that uses it.
    return fileSystem.getPathMatcher("glob:$glob")
}

private fun Path.streamContentsRecursively(glob: String = "*", vararg options: LinkOption): Stream<Path> {
    if (!isDirectory(*options)) throw noDirectory()
    val fileVisitOptions = options.let { if (it.contains(LinkOption.NOFOLLOW_LINKS)) emptyArray() else arrayOf(FileVisitOption.FOLLOW_LINKS) }
    val walk = Files.walk(this, *fileVisitOptions).filter { it != this }
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
public fun Path.listDirectoryEntriesRecursively(glob: String = "*", vararg options: LinkOption): List<Path> =
    streamContentsRecursively(glob, *options).toList()

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
public fun <T> Path.useDirectoryEntriesRecursively(glob: String = "*", vararg options: LinkOption, block: (Sequence<Path>) -> T): T =
    streamContentsRecursively(glob, *options).use { block(it.asSequence()) }

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
public fun Path.forEachDirectoryEntryRecursively(glob: String = "*", vararg options: LinkOption, action: (Path) -> Unit): Unit =
    streamContentsRecursively(glob, *options).use { it.forEach(action) }


/**
 * Deletes this file or empty directory.
 *
 * Returns the deletes path.
 */
public fun Path.delete(vararg options: LinkOption): Path =
    apply { if (exists(*options)) Files.delete(this) }

/**
 * Deletes this file or directory recursively.
 *
 * Symbolic links are not followed but deleted themselves.
 *
 * Returns the deletes path.
 */
public fun Path.deleteRecursively(vararg options: LinkOption, predicate: (Path) -> Boolean = { true }): Path =
    apply {
        if (exists(*options, LinkOption.NOFOLLOW_LINKS)) {
            if (isDirectory(*options, LinkOption.NOFOLLOW_LINKS)) {
                forEachDirectoryEntry { it.deleteRecursively(*options, LinkOption.NOFOLLOW_LINKS, predicate = predicate) }
            }

            if (predicate(this)) {
                var maxAttempts = 3
                var ex: Throwable? = kotlin.runCatching { delete(*options, LinkOption.NOFOLLOW_LINKS) }.exceptionOrNull()
                while (ex != null && maxAttempts > 0) {
                    maxAttempts--
                    if (ex is DirectoryNotEmptyException) {
                        val files = listDirectoryEntriesRecursively(options = options)
                        files.forEach { it.deleteRecursively(*options, predicate = predicate) }
                    }
                    100.milli.seconds.sleep()
                    ex = kotlin.runCatching { delete(*options, LinkOption.NOFOLLOW_LINKS) }.exceptionOrNull()
                }
                if (ex != null) throw ex
            }
        }
    }

/**
 * Deletes the contents of this directory.
 *
 * Throws if this is no directory.
 */
public fun Path.deleteDirectoryEntriesRecursively(predicate: (Path) -> Boolean = { true }): Path =
    apply { listDirectoryEntriesRecursively().forEach { it.deleteRecursively(predicate = predicate) } }

/**
 * Registers this file for deletion the moment this program exits.
 *
 * For safety reasons, [recursively] must be explicitly set to `true`
 * if not-empty directories are to be deleted.
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Path.deleteOnExit(recursively: Boolean = false): Path = apply {
    onExit {
        if (recursively) deleteRecursively()
        else delete()
    }
}

/**
 * Contains whether this path belongs to the default [FileSystem].
 *
 * @see [FileSystems.getDefault]
 */
public fun Path.isDefaultFileSystem(): Boolean = fileSystem == FileSystems.getDefault()
