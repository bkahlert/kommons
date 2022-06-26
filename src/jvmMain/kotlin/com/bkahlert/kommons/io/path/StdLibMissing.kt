package com.bkahlert.kommons.io.path

import com.bkahlert.kommons.io.path.StandardOpenOptions.DEFAULT_APPEND_OPTIONS
import com.bkahlert.kommons.io.path.StandardOpenOptions.DEFAULT_WRITE_OPTIONS
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URI
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.bufferedWriter
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.io.path.pathString
import kotlin.io.path.useLines
import kotlin.io.path.writeLines
import kotlin.io.path.writeText
import kotlin.io.path.appendBytes as kotlinAppendBytes
import kotlin.io.path.writeBytes as kotlinWriteBytes

// TODO migrate
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
 * String representation of this path's [Path.getFileName] that does **not** rely on [toString].
 */
public val Path.fileNameString: String get() = fileName.pathString

/**
 * String representation of this path's [URI].
 */
public val Path.uriString: String get() = toUri().toString()
