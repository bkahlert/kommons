package koodies.io.file

import koodies.io.path.Defaults.DEFAULT_APPEND_OPTIONS
import koodies.io.path.Defaults.DEFAULT_WRITE_OPTIONS
import koodies.text.LineSeparators
import koodies.text.LineSeparators.LF
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.util.ArrayList

/**
 * Returns a new [InputStreamReader] for reading the content of this file.
 */
fun Path.reader(charset: Charset = Charsets.UTF_8): InputStreamReader =
    inputStream().reader(charset)

/**
 * Returns a new [BufferedReader] for reading the content of this file.
 *
 * @param bufferSize necessary size of the buffer.
 */
fun Path.bufferedReader(charset: Charset = Charsets.UTF_8, bufferSize: Int = 8096): BufferedReader =
    reader(charset).buffered(bufferSize)

/**
 * Returns a new [OutputStreamWriter] for writing the content of this file.
 */
fun Path.writer(charset: Charset = Charsets.UTF_8): OutputStreamWriter =
    outputStream().writer(charset)

/**
 * Returns a new [BufferedWriter] for writing the content of this file.
 *
 * @param bufferSize necessary size of the buffer.
 */
fun Path.bufferedWriter(charset: Charset = Charsets.UTF_8, bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedWriter =
    writer(charset).buffered(bufferSize)

/**
 * Returns a new [PrintWriter] for writing the content of this file.
 */
fun Path.printWriter(charset: Charset = Charsets.UTF_8): PrintWriter =
    PrintWriter(bufferedWriter(charset))


/**
 * Sets the content of this file as an [array] of bytes.
 * If this file already exists, it becomes overwritten.
 *
 * @param array byte array to write into this file.
 */
fun Path.writeBytes(array: ByteArray): Path =
    apply { Files.newOutputStream(this, *DEFAULT_WRITE_OPTIONS).use { it.write(array) } }

/**
 * Appends an [array] of bytes to the content of this file.
 *
 * @param array byte array to append to this file.
 */
fun Path.appendBytes(array: ByteArray): Path =
    apply { Files.newOutputStream(this, *DEFAULT_APPEND_OPTIONS).use { it.write(array) } }

/**
 * Sets the content of this file as [text] encoded using UTF-8 or specified [charset].
 * If this file exists, it becomes overwritten.
 *
 * @param text text to write into file.
 * @param charset character set to use.
 */
fun Path.writeText(text: String, charset: Charset = Charsets.UTF_8): Path =
    writeBytes(text.toByteArray(charset))


/**
 * Sets the content of this file as [text] encoded using UTF-8 or specified [charset]
 * and a trailing [lineSeparator].
 * If this file exists, it becomes overwritten.
 *
 * @param text text to write into file.
 * @param charset character set to use.
 * @param lineSeparator the line separator to use.
 *
 * @see LineSeparators
 */
fun Path.writeLine(text: String, charset: Charset = Charsets.UTF_8, lineSeparator: String = LF): Path =
    writeText("$text$lineSeparator", charset)

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset].
 *
 * @param text text to append to file.
 * @param charset character set to use.
 */
fun Path.appendText(text: String, charset: Charset = Charsets.UTF_8) =
    appendBytes(text.toByteArray(charset))

/**
 * Appends [text] to the content of this file using UTF-8 or the specified [charset]
 * and a trailing [lineSeparator].
 *
 * @param text text to append to file.
 * @param charset character set to use.
 * @param lineSeparator the line separator to use.
 *
 * @see LineSeparators
 */
fun Path.appendLine(text: String, charset: Charset = Charsets.UTF_8, lineSeparator: String = LF) =
    appendText("$text$lineSeparator", charset)

/**
 * Reads file by byte blocks and calls [action] for each block read.
 * Block has default size which is implementation-dependent.
 * This functions passes the byte array and amount of bytes in the array to the [action] function.
 *
 * You can use this function for huge files.
 *
 * @param action function to process file blocks.
 */
fun Path.forEachBlock(action: (buffer: ByteArray, bytesRead: Int) -> Unit): Path =
    apply { forEachBlock(DEFAULT_BUFFER_SIZE / 2, action) }

/**
 * Reads file by byte blocks and calls [action] for each block read.
 * This functions passes the byte array and amount of bytes in the array to the [action] function.
 *
 * You can use this function for huge files.
 *
 * @param action function to process file blocks.
 * @param blockSize size of a block, replaced by 512 if it's less, 4096 by default.
 */
fun Path.forEachBlock(blockSize: Int, action: (buffer: ByteArray, bytesRead: Int) -> Unit): Path =
    apply {
        val arr = ByteArray(blockSize.coerceAtLeast(DEFAULT_BUFFER_SIZE / 16))

        inputStream().use { input ->
            do {
                val size = input.read(arr)
                if (size <= 0) break
                action(arr, size)
            } while (true)
        }
    }

/**
 * Reads this file line by line using the specified [charset] and calls [action] for each line.
 * Default charset is UTF-8.
 *
 * You may use this function on huge files.
 *
 * @param charset character set to use.
 * @param action function to process file lines.
 */
fun Path.forEachLine(charset: Charset = Charsets.UTF_8, action: (line: String) -> Unit): Path =
    apply {
        // Note: close is called at forEachLine
        BufferedReader(InputStreamReader(this.inputStream(), charset)).forEachLine(action)
    }

/**
 * Constructs a new [InputStream] of this file and returns it as a result.
 */
fun Path.inputStream(): InputStream = Files.newInputStream(this)

/**
 * Constructs a new [OutputStream] of this file and returns it as a result.
 */
fun Path.outputStream(): OutputStream = Files.newOutputStream(this)

/**
 * Reads the file content as a list of lines.
 *
 * Do not use this function for huge files.
 *
 * @param charset character set to use. By default uses UTF-8 charset.
 * @return list of file lines.
 */
fun Path.readLines(charset: Charset = Charsets.UTF_8): List<String> {
    val result = ArrayList<String>()
    forEachLine(charset) { result.add(it); }
    return result
}

/**
 * Calls the [block] callback giving it a sequence of all the lines in this file and closes the reader once
 * the processing is complete.

 * @param charset character set to use. By default uses UTF-8 charset.
 * @return the value returned by [block].
 */
inline fun <T> Path.useLines(charset: Charset = Charsets.UTF_8, block: (Sequence<String>) -> T): T =
    bufferedReader(charset).use { block(it.lineSequence()) }
