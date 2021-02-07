@file:Suppress("unused")

package koodies.io


import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.Closeable
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.Writer
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLConnection
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.ReadableByteChannel
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Objects


/**
 * General IO stream manipulation utilities.
 *
 *
 * This class provides static utility methods for input/output operations.
 *
 *  * **[Deprecated]** closeQuietly - these methods close a stream ignoring nulls and exceptions
 *  * toXxx/read - these methods read data from a stream
 *  * write - these methods write data to a stream
 *  * copy - these methods copy all the data from one stream to another
 *  * contentEquals - these methods compare the content of two streams
 *
 *
 *
 * The byte-to-char methods and char-to-byte methods involve a conversion step.
 * Two methods are provided in each case, one that uses the platform default
 * encoding and the other which allows you to specify an encoding. You are
 * encouraged to always specify an encoding because relying on the platform
 * default can lead to unexpected results, for example when moving from
 * development to production.
 *
 *
 * All the methods in this class that read a stream are buffered internally.
 * This means that there is no cause to use a `BufferedInputStream`
 * or `BufferedReader`. The default buffer size of 4K has been shown
 * to be efficient in tests.
 *
 *
 * The various copy methods all delegate the actual copying to one of the following methods:
 *
 *  * [.copyLarge]
 *  * [.copyLarge]
 *  * [.copyLarge]
 *  * [.copyLarge]
 *
 * For example, [.copy] calls [.copyLarge]
 * which calls [.copy] which creates the buffer and calls
 * [.copyLarge].
 *
 *
 * Applications can re-use buffers by using the underlying methods directly.
 * This may improve performance for applications that need to do a lot of copying.
 *
 *
 * Wherever possible, the methods in this class do *not* flush or close
 * the stream. This is to avoid making non-portable assumptions about the
 * streams' origin and further use. Thus the caller is still responsible for
 * closing streams after use.
 *
 *
 * Origin of code: Excalibur.
 */
object IOUtils {
    // NOTE: This class is focused on InputStream, OutputStream, Reader and
    // Writer. Each method should take at least one of these as a parameter,
    // or return one of them.
    private val EMPTY_BYTE_ARRAY = ByteArray(0)

    /**
     * The default buffer size ({@value}) to use in copy methods.
     */
    const val DEFAULT_BUFFER_SIZE = 8192

    /**
     * Represents the end-of-file (or stream).
     * @since 2.5 (made public)
     */
    const val EOF = -1

    /**
     * The default buffer to use for the skip() methods.
     */
    private val SKIP_BYTE_BUFFER = ByteArray(DEFAULT_BUFFER_SIZE)

    // Allocated in the relevant skip method if necessary.
    /*
     * These buffers are static and are shared between threads.
     * This is possible because the buffers are write-only - the contents are never read.
     *
     * N.B. there is no need to synchronize when creating these because:
     * - we don't care if the buffer is created multiple times (the data is ignored)
     * - we always use the same size buffer, so if it it is recreated it will still be OK
     * (if the buffer size were variable, we would need to synch. to ensure some other thread
     * did not create a smaller one)
     */
    private var SKIP_CHAR_BUFFER: CharArray? = null

    /**
     * Returns the given InputStream if it is already a [BufferedInputStream], otherwise creates a
     * BufferedInputStream from the given InputStream.
     *
     * @param inputStream the InputStream to wrap or return (not null)
     * @return the given InputStream or a new [BufferedInputStream] for the given InputStream
     * @throws NullPointerException if the input parameter is null
     * @since 2.5
     */
    // parameter null check
    fun buffer(inputStream: InputStream?): BufferedInputStream {
        // reject null early on rather than waiting for IO operation to fail
        // not checked by BufferedInputStream
        Objects.requireNonNull(inputStream, "inputStream")
        return if (inputStream is BufferedInputStream) inputStream else BufferedInputStream(inputStream)
    }

    /**
     * Returns the given InputStream if it is already a [BufferedInputStream], otherwise creates a
     * BufferedInputStream from the given InputStream.
     *
     * @param inputStream the InputStream to wrap or return (not null)
     * @param size the buffer size, if a new BufferedInputStream is created.
     * @return the given InputStream or a new [BufferedInputStream] for the given InputStream
     * @throws NullPointerException if the input parameter is null
     * @since 2.5
     */
    // parameter null check
    fun buffer(inputStream: InputStream, size: Int): BufferedInputStream {
        // reject null early on rather than waiting for IO operation to fail
        // not checked by BufferedInputStream
        Objects.requireNonNull(inputStream, "inputStream")
        return if (inputStream is BufferedInputStream) inputStream else BufferedInputStream(inputStream, size)
    }

    /**
     * Returns the given OutputStream if it is already a [BufferedOutputStream], otherwise creates a
     * BufferedOutputStream from the given OutputStream.
     *
     * @param outputStream the OutputStream to wrap or return (not null)
     * @return the given OutputStream or a new [BufferedOutputStream] for the given OutputStream
     * @throws NullPointerException if the input parameter is null
     * @since 2.5
     */
    // parameter null check
    fun buffer(outputStream: OutputStream): BufferedOutputStream {
        // reject null early on rather than waiting for IO operation to fail
        // not checked by BufferedInputStream
        Objects.requireNonNull(outputStream, "outputStream")
        return if (outputStream is BufferedOutputStream) outputStream else BufferedOutputStream(outputStream)
    }

    /**
     * Returns the given OutputStream if it is already a [BufferedOutputStream], otherwise creates a
     * BufferedOutputStream from the given OutputStream.
     *
     * @param outputStream the OutputStream to wrap or return (not null)
     * @param size the buffer size, if a new BufferedOutputStream is created.
     * @return the given OutputStream or a new [BufferedOutputStream] for the given OutputStream
     * @throws NullPointerException if the input parameter is null
     * @since 2.5
     */
    // parameter null check
    fun buffer(outputStream: OutputStream, size: Int): BufferedOutputStream {
        // reject null early on rather than waiting for IO operation to fail
        // not checked by BufferedInputStream
        Objects.requireNonNull(outputStream, "outputStream")
        return if (outputStream is BufferedOutputStream) outputStream else BufferedOutputStream(outputStream, size)
    }

    /**
     * Returns the given reader if it is already a [BufferedReader], otherwise creates a BufferedReader from
     * the given reader.
     *
     * @param reader the reader to wrap or return (not null)
     * @return the given reader or a new [BufferedReader] for the given reader
     * @throws NullPointerException if the input parameter is null
     * @since 2.5
     */
    fun buffer(reader: Reader?): BufferedReader {
        return if (reader is BufferedReader) reader else BufferedReader(reader)
    }

    /**
     * Returns the given reader if it is already a [BufferedReader], otherwise creates a BufferedReader from the
     * given reader.
     *
     * @param reader the reader to wrap or return (not null)
     * @param size the buffer size, if a new BufferedReader is created.
     * @return the given reader or a new [BufferedReader] for the given reader
     * @throws NullPointerException if the input parameter is null
     * @since 2.5
     */
    fun buffer(reader: Reader?, size: Int): BufferedReader {
        return if (reader is BufferedReader) reader else BufferedReader(reader, size)
    }

    /**
     * Returns the given Writer if it is already a [BufferedWriter], otherwise creates a BufferedWriter from the
     * given Writer.
     *
     * @param writer the Writer to wrap or return (not null)
     * @return the given Writer or a new [BufferedWriter] for the given Writer
     * @throws NullPointerException if the input parameter is null
     * @since 2.5
     */
    fun buffer(writer: Writer?): BufferedWriter {
        return if (writer is BufferedWriter) writer else BufferedWriter(writer)
    }

    /**
     * Returns the given Writer if it is already a [BufferedWriter], otherwise creates a BufferedWriter from the
     * given Writer.
     *
     * @param writer the Writer to wrap or return (not null)
     * @param size the buffer size, if a new BufferedWriter is created.
     * @return the given Writer or a new [BufferedWriter] for the given Writer
     * @throws NullPointerException if the input parameter is null
     * @since 2.5
     */
    fun buffer(writer: Writer?, size: Int): BufferedWriter {
        return if (writer is BufferedWriter) writer else BufferedWriter(writer, size)
    }

    /**
     * Closes the given [Closeable] as a null-safe operation.
     *
     * @param closeable The resource to close, may be null.
     * @throws IOException if an I/O error occurs.
     * @since 2.7
     */
    @Throws(IOException::class) fun close(closeable: Closeable?) {
        closeable?.close()
    }

    /**
     * Closes the given [Closeable] as a null-safe operation.
     *
     * @param closeables The resource(s) to close, may be null.
     * @throws IOException if an I/O error occurs.
     * @since 2.8.0
     */
    @Throws(IOException::class) fun close(vararg closeables: Closeable?) {
        for (closeable in closeables) {
            close(closeable)
        }
    }

    /**
     * Closes the given [Closeable] as a null-safe operation.
     *
     * @param closeable The resource to close, may be null.
     * @param consumer Consume the IOException thrown by [Closeable.close].
     * @throws IOException if an I/O error occurs.
     * @since 2.7
     */
    @Throws(IOException::class) fun close(closeable: Closeable?, consumer: (IOException) -> Unit) {
        if (closeable != null) {
            try {
                closeable.close()
            } catch (e: IOException) {
                consumer(e)
            }
        }
    }

    /**
     * Closes a URLConnection.
     *
     * @param conn the connection to close.
     * @since 2.4
     */
    fun close(conn: URLConnection?) {
        if (conn is HttpURLConnection) {
            conn.disconnect()
        }
    }

    /**
     * Compares the contents of two Streams to determine if they are equal or
     * not.
     *
     *
     * This method buffers the input internally using
     * `BufferedInputStream` if they are not already buffered.
     *
     *
     * @param input1 the first stream
     * @param input2 the second stream
     * @return true if the content of the streams are equal or they both don't
     * exist, false otherwise
     * @throws NullPointerException if either input is null
     * @throws IOException          if an I/O error occurs
     */
    @Throws(IOException::class) fun contentEquals(input1: InputStream, input2: InputStream): Boolean {
        if (input1 === input2) {
            return true
        }
        val bufferedInput1 = buffer(input1)
        val bufferedInput2 = buffer(input2)
        var ch = bufferedInput1.read()
        while (EOF != ch) {
            val ch2 = bufferedInput2.read()
            if (ch != ch2) {
                return false
            }
            ch = bufferedInput1.read()
        }
        return bufferedInput2.read() == EOF
    }

    /**
     * Copies bytes from an `InputStream` to an
     * `OutputStream`.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedInputStream`.
     *
     *
     *
     * Large streams (over 2GB) will return a bytes copied value of
     * `-1` after the copy has completed since the correct
     * number of bytes cannot be returned as an int. For large streams
     * use the `copyLarge(InputStream, OutputStream)` method.
     *
     *
     * @param input the `InputStream` to read from
     * @param output the `OutputStream` to write to
     * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE, or `0` if `input is null`.
     * @throws NullPointerException if the output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    @Throws(IOException::class) fun copy(input: InputStream?, output: OutputStream): Int {
        val count = copyLarge(input, output)
        return if (count > Int.MAX_VALUE) {
            -1
        } else count.toInt()
    }

    /**
     * Copies bytes from an `InputStream` to an `OutputStream` using an internal buffer of the
     * given size.
     *
     *
     * This method buffers the input internally, so there is no need to use a `BufferedInputStream`.
     *
     *
     * @param input the `InputStream` to read from
     * @param output the `OutputStream` to write to
     * @param bufferSize the bufferSize used to copy from the input to the output
     * @return the number of bytes copied. or `0` if `input is null`.
     * @throws NullPointerException if the output is null
     * @throws IOException if an I/O error occurs
     * @since 2.5
     */
    @Throws(IOException::class) fun copy(input: InputStream?, output: OutputStream, bufferSize: Int): Long {
        return copyLarge(input, output, ByteArray(bufferSize))
    }

    /**
     * Copies bytes from an `InputStream` to chars on a
     * `Writer` using the default character encoding of the platform.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedInputStream`.
     *
     *
     * This method uses [InputStreamReader].
     *
     * @param input the `InputStream` to read from
     * @param output the `Writer` to write to
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    @Deprecated("2.5 use {@link #copy(InputStream, Writer, Charset)} instead") @Throws(IOException::class) fun copy(input: InputStream, output: Writer) {
        copy(input, output, Charsets.UTF_8)
    }

    /**
     * Copies bytes from an `InputStream` to chars on a
     * `Writer` using the specified character encoding.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedInputStream`.
     *
     *
     * This method uses [InputStreamReader].
     *
     * @param input the `InputStream` to read from
     * @param output the `Writer` to write to
     * @param inputCharset the charset to use for the input stream, null means platform default
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.3
     */
    @Throws(IOException::class) fun copy(input: InputStream, output: Writer, inputCharset: Charset = Charsets.UTF_8) {
        copy(InputStreamReader(input, inputCharset), output)
    }

    /**
     * Copies chars from a `Reader` to an `Appendable`.
     *
     *
     * This method uses the provided buffer, so there is no need to use a
     * `BufferedReader`.
     *
     *
     * @param input the `Reader` to read from
     * @param output the `Appendable` to write to
     * @param buffer the buffer to be used for the copy
     * @return the number of characters copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.7
     */
    /**
     * Copies chars from a `Reader` to a `Appendable`.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedReader`.
     *
     *
     * Large streams (over 2GB) will return a chars copied value of
     * `-1` after the copy has completed since the correct
     * number of chars cannot be returned as an int. For large streams
     * use the `copyLarge(Reader, Writer)` method.
     *
     * @param input the `Reader` to read from
     * @param output the `Appendable` to write to
     * @return the number of characters copied, or -1 if &gt; Integer.MAX_VALUE
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.7
     */
    @JvmOverloads @Throws(IOException::class) fun copy(
        input: Reader, output: Appendable,
        buffer: CharBuffer = CharBuffer.allocate(
            DEFAULT_BUFFER_SIZE),
    ): Long {
        var count: Long = 0
        var n: Int
        while (EOF != input.read(buffer).also { n = it }) {
            buffer.flip()
            output.append(buffer, 0, n)
            count += n.toLong()
        }
        return count
    }

    /**
     * Copies chars from a `Reader` to bytes on an
     * `OutputStream` using the default character encoding of the
     * platform, and calling flush.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedReader`.
     *
     *
     * Due to the implementation of OutputStreamWriter, this method performs a
     * flush.
     *
     *
     * This method uses [OutputStreamWriter].
     *
     * @param input the `Reader` to read from
     * @param output the `OutputStream` to write to
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    @Deprecated("2.5 use {@link #copy(Reader, OutputStream, Charset)} instead") @Throws(IOException::class) fun copy(input: Reader, output: OutputStream?) {
        copy(input, output, Charset.defaultCharset())
    }

    /**
     * Copies chars from a `Reader` to bytes on an
     * `OutputStream` using the specified character encoding, and
     * calling flush.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedReader`.
     *
     *
     *
     * Due to the implementation of OutputStreamWriter, this method performs a
     * flush.
     *
     *
     *
     * This method uses [OutputStreamWriter].
     *
     *
     * @param input the `Reader` to read from
     * @param output the `OutputStream` to write to
     * @param outputCharset the charset to use for the OutputStream, null means platform default
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.3
     */
    @Throws(IOException::class) fun copy(input: Reader, output: OutputStream?, outputCharset: Charset = Charsets.UTF_8) {
        val out = OutputStreamWriter(output, outputCharset)
        copy(input, out)
        // XXX Unless anyone is planning on rewriting OutputStreamWriter,
        // we have to flush here.
        out.flush()
    }

    /**
     * Copies chars from a `Reader` to a `Writer`.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedReader`.
     *
     *
     * Large streams (over 2GB) will return a chars copied value of
     * `-1` after the copy has completed since the correct
     * number of chars cannot be returned as an int. For large streams
     * use the `copyLarge(Reader, Writer)` method.
     *
     * @param input the `Reader` to read from
     * @param output the `Writer` to write to
     * @return the number of characters copied, or -1 if &gt; Integer.MAX_VALUE
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    @Throws(IOException::class) fun copy(input: Reader, output: Writer): Int {
        val count = copyLarge(input, output)
        return if (count > Int.MAX_VALUE) {
            -1
        } else count.toInt()
    }

    /**
     * Copies bytes from a large (over 2GB) `InputStream` to an
     * `OutputStream`.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedInputStream`.
     *
     *
     *
     * The buffer size is given by [.DEFAULT_BUFFER_SIZE].
     *
     *
     * @param input the `InputStream` to read from
     * @param output the `OutputStream` to write to
     * @return the number of bytes copied. or `0` if `input is null`.
     * @throws NullPointerException if the output is null
     * @throws IOException if an I/O error occurs
     * @since 1.3
     */
    @Throws(IOException::class) fun copyLarge(input: InputStream?, output: OutputStream): Long {
        return copy(input, output, DEFAULT_BUFFER_SIZE)
    }

    /**
     * Copies bytes from a large (over 2GB) `InputStream` to an
     * `OutputStream`.
     *
     *
     * This method uses the provided buffer, so there is no need to use a
     * `BufferedInputStream`.
     *
     *
     * @param input the `InputStream` to read from
     * @param output the `OutputStream` to write to
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied. or `0` if `input is null`.
     * @throws IOException if an I/O error occurs
     * @since 2.2
     */
    @Throws(IOException::class) fun copyLarge(input: InputStream?, output: OutputStream, buffer: ByteArray?): Long {
        var count: Long = 0
        if (input != null) {
            var n: Int
            while (EOF != input.read(buffer).also { n = it }) {
                output.write(buffer, 0, n)
                count += n.toLong()
            }
        }
        return count
    }
    /**
     * Copies some or all bytes from a large (over 2GB) `InputStream` to an
     * `OutputStream`, optionally skipping input bytes.
     *
     *
     * This method uses the provided buffer, so there is no need to use a
     * `BufferedInputStream`.
     *
     *
     *
     * Note that the implementation uses [.skip].
     * This means that the method may be considerably less efficient than using the actual skip implementation,
     * this is done to guarantee that the correct number of characters are skipped.
     *
     *
     * @param input the `InputStream` to read from
     * @param output the `OutputStream` to write to
     * @param inputOffset : number of bytes to skip from input before copying
     * -ve values are ignored
     * @param length : number of bytes to copy. -ve means all
     * @param buffer the buffer to use for the copy
     * @return the number of bytes copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.2
     */
    /**
     * Copies chars from a large (over 2GB) `Reader` to a `Writer`.
     *
     *
     * This method uses the provided buffer, so there is no need to use a
     * `BufferedReader`.
     *
     *
     *
     * @param input the `Reader` to read from
     * @param output the `Writer` to write to
     * @param buffer the buffer to be used for the copy
     * @return the number of characters copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 2.2
     */
    /**
     * Copies chars from a large (over 2GB) `Reader` to a `Writer`.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedReader`.
     *
     *
     * The buffer size is given by [.DEFAULT_BUFFER_SIZE].
     *
     * @param input the `Reader` to read from
     * @param output the `Writer` to write to
     * @return the number of characters copied
     * @throws NullPointerException if the input or output is null
     * @throws IOException          if an I/O error occurs
     * @since 1.3
     */
    @JvmOverloads @Throws(IOException::class) fun copyLarge(input: Reader, output: Writer, buffer: CharArray? = CharArray(DEFAULT_BUFFER_SIZE)): Long {
        var count: Long = 0
        var n: Int
        while (EOF != input.read(buffer).also { n = it }) {
            output.write(buffer, 0, n)
            count += n.toLong()
        }
        return count
    }

    /**
     * Reads bytes from an input stream.
     * This implementation guarantees that it will read as many bytes
     * as possible before giving up; this may not always be the case for
     * subclasses of [InputStream].
     *
     * @param input where to read input from
     * @param buffer destination
     * @param offset initial offset into buffer
     * @param length length to read, must be &gt;= 0
     * @return actual length read; may be less than requested if EOF was reached
     * @throws IOException if a read error occurs
     * @since 2.2
     */
    /**
     * Reads bytes from an input stream.
     * This implementation guarantees that it will read as many bytes
     * as possible before giving up; this may not always be the case for
     * subclasses of [InputStream].
     *
     * @param input where to read input from
     * @param buffer destination
     * @return actual length read; may be less than requested if EOF was reached
     * @throws IOException if a read error occurs
     * @since 2.2
     */
    @JvmOverloads @Throws(IOException::class) fun read(input: InputStream, buffer: ByteArray, offset: Int = 0, length: Int = buffer.size): Int {
        require(length >= 0) { "Length must not be negative: $length" }
        var remaining = length
        while (remaining > 0) {
            val location = length - remaining
            val count = input.read(buffer, offset + location, remaining)
            if (EOF == count) { // EOF
                break
            }
            remaining -= count
        }
        return length - remaining
    }

    /**
     * Reads bytes from a ReadableByteChannel.
     *
     *
     * This implementation guarantees that it will read as many bytes
     * as possible before giving up; this may not always be the case for
     * subclasses of [ReadableByteChannel].
     *
     * @param input the byte channel to read
     * @param buffer byte buffer destination
     * @return the actual length read; may be less than requested if EOF was reached
     * @throws IOException if a read error occurs
     * @since 2.5
     */
    @Throws(IOException::class) fun read(input: ReadableByteChannel, buffer: ByteBuffer): Int {
        val length = buffer.remaining()
        while (buffer.remaining() > 0) {
            val count = input.read(buffer)
            if (EOF == count) { // EOF
                break
            }
        }
        return length - buffer.remaining()
    }
    /**
     * Reads characters from an input character stream.
     * This implementation guarantees that it will read as many characters
     * as possible before giving up; this may not always be the case for
     * subclasses of [Reader].
     *
     * @param input where to read input from
     * @param buffer destination
     * @param offset initial offset into buffer
     * @param length length to read, must be &gt;= 0
     * @return actual length read; may be less than requested if EOF was reached
     * @throws IOException if a read error occurs
     * @since 2.2
     */
    /**
     * Reads characters from an input character stream.
     * This implementation guarantees that it will read as many characters
     * as possible before giving up; this may not always be the case for
     * subclasses of [Reader].
     *
     * @param input where to read input from
     * @param buffer destination
     * @return actual length read; may be less than requested if EOF was reached
     * @throws IOException if a read error occurs
     * @since 2.2
     */
    @JvmOverloads @Throws(IOException::class) fun read(input: Reader, buffer: CharArray, offset: Int = 0, length: Int = buffer.size): Int {
        require(length >= 0) { "Length must not be negative: $length" }
        var remaining = length
        while (remaining > 0) {
            val location = length - remaining
            val count = input.read(buffer, offset + location, remaining)
            if (EOF == count) { // EOF
                break
            }
            remaining -= count
        }
        return length - remaining
    }
    /**
     * Reads the requested number of bytes or fail if there are not enough left.
     *
     *
     * This allows for the possibility that [InputStream.read] may
     * not read as many bytes as requested (most likely because of reaching EOF).
     *
     * @param input where to read input from
     * @param buffer destination
     * @param offset initial offset into buffer
     * @param length length to read, must be &gt;= 0
     *
     * @throws IOException              if there is a problem reading the file
     * @throws IllegalArgumentException if length is negative
     * @throws EOFException             if the number of bytes read was incorrect
     * @since 2.2
     */

    /**
     * Reads the requested number of characters or fail if there are not enough left.
     *
     *
     * This allows for the possibility that [Reader.read] may
     * not read as many characters as requested (most likely because of reaching EOF).
     *
     * @param input where to read input from
     * @param buffer destination
     * @param offset initial offset into buffer
     * @param length length to read, must be &gt;= 0
     * @throws IOException              if there is a problem reading the file
     * @throws IllegalArgumentException if length is negative
     * @throws EOFException             if the number of characters read was incorrect
     * @since 2.2
     */

    /**
     * Gets the contents of an `InputStream` as a list of Strings,
     * one entry per line, using the default character encoding of the platform.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedInputStream`.
     *
     * @param input the `InputStream` to read from, not null
     * @return the list of Strings, never null
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    @Deprecated("2.5 use {@link #readLines(InputStream, Charset)} instead") @Throws(IOException::class) fun readLines(input: InputStream?): List<String> {
        return readLines(input, Charset.defaultCharset())
    }

    /**
     * Gets the contents of an `InputStream` as a list of Strings,
     * one entry per line, using the specified character encoding.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedInputStream`.
     *
     * @param input the `InputStream` to read from, not null
     * @param charset the charset to use, null means platform default
     * @return the list of Strings, never null
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     * @since 2.3
     */
    @Throws(IOException::class) fun readLines(input: InputStream?, charset: Charset = Charsets.UTF_8): List<String> =
        readLines(InputStreamReader(input, charset))

    /**
     * Gets the contents of a `Reader` as a list of Strings,
     * one entry per line.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedReader`.
     *
     * @param input the `Reader` to read from, not null
     * @return the list of Strings, never null
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     * @since 1.1
     */
    @Throws(IOException::class)  // reader wraps input and is the responsibility of the caller.
    fun readLines(input: Reader?): List<String> {
        val reader = toBufferedReader(input)
        val list: MutableList<String> = ArrayList()
        var line: String
        while (reader.readLine().also { line = it } != null) {
            list.add(line)
        }
        return list
    }

    /**
     * Skips bytes from an input byte stream.
     * This implementation guarantees that it will read as many bytes
     * as possible before giving up; this may not always be the case for
     * skip() implementations in subclasses of [InputStream].
     *
     *
     * Note that the implementation uses [InputStream.read] rather
     * than delegating to [InputStream.skip].
     * This means that the method may be considerably less efficient than using the actual skip implementation,
     * this is done to guarantee that the correct number of bytes are skipped.
     *
     *
     * @param input byte stream to skip
     * @param toSkip number of bytes to skip.
     * @return number of bytes actually skipped.
     * @throws IOException              if there is a problem reading the file
     * @throws IllegalArgumentException if toSkip is negative
     * @see InputStream.skip
     * @see [IO-203 - Add skipFully
     * @since 2.0
    ](https://issues.apache.org/jira/browse/IO-203) */
    @Throws(IOException::class) fun skip(input: InputStream, toSkip: Long): Long {
        require(toSkip >= 0) { "Skip count must be non-negative, actual: $toSkip" }
        /*
         * N.B. no need to synchronize access to SKIP_BYTE_BUFFER: - we don't care if the buffer is created multiple
         * times (the data is ignored) - we always use the same size buffer, so if it it is recreated it will still be
         * OK (if the buffer size were variable, we would need to synch. to ensure some other thread did not create a
         * smaller one)
         */
        var remain = toSkip
        while (remain > 0) {
            // See https://issues.apache.org/jira/browse/IO-203 for why we use read() rather than delegating to skip()
            val n = input.read(SKIP_BYTE_BUFFER, 0, Math.min(remain, SKIP_BYTE_BUFFER.size.toLong()).toInt()).toLong()
            if (n < 0) { // EOF
                break
            }
            remain -= n
        }
        return toSkip - remain
    }

    /**
     * Skips characters from an input character stream.
     * This implementation guarantees that it will read as many characters
     * as possible before giving up; this may not always be the case for
     * skip() implementations in subclasses of [Reader].
     *
     *
     * Note that the implementation uses [Reader.read] rather
     * than delegating to [Reader.skip].
     * This means that the method may be considerably less efficient than using the actual skip implementation,
     * this is done to guarantee that the correct number of characters are skipped.
     *
     *
     * @param input character stream to skip
     * @param toSkip number of characters to skip.
     * @return number of characters actually skipped.
     * @throws IOException              if there is a problem reading the file
     * @throws IllegalArgumentException if toSkip is negative
     * @see Reader.skip
     * @see [IO-203 - Add skipFully
     * @since 2.0
    ](https://issues.apache.org/jira/browse/IO-203) */
    @Throws(IOException::class) fun skip(input: Reader, toSkip: Long): Long {
        require(toSkip >= 0) { "Skip count must be non-negative, actual: $toSkip" }
        /*
         * N.B. no need to synchronize this because: - we don't care if the buffer is created multiple times (the data
         * is ignored) - we always use the same size buffer, so if it it is recreated it will still be OK (if the buffer
         * size were variable, we would need to synch. to ensure some other thread did not create a smaller one)
         */if (SKIP_CHAR_BUFFER == null) {
            SKIP_CHAR_BUFFER = CharArray(SKIP_BYTE_BUFFER.size)
        }
        var remain = toSkip
        while (remain > 0) {
            // See https://issues.apache.org/jira/browse/IO-203 for why we use read() rather than delegating to skip()
            val n = input.read(SKIP_CHAR_BUFFER, 0, Math.min(remain, SKIP_BYTE_BUFFER.size.toLong()).toInt()).toLong()
            if (n < 0) { // EOF
                break
            }
            remain -= n
        }
        return toSkip - remain
    }

    /**
     * Returns the given reader if it is a [BufferedReader], otherwise creates a BufferedReader from the given
     * reader.
     *
     * @param reader the reader to wrap or return (not null)
     * @return the given reader or a new [BufferedReader] for the given reader
     * @throws NullPointerException if the input parameter is null
     * @see .buffer
     * @since 2.2
     */
    fun toBufferedReader(reader: Reader?): BufferedReader {
        return if (reader is BufferedReader) reader else BufferedReader(reader)
    }

    /**
     * Gets the contents of an `InputStream` as a `byte[]`.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedInputStream`.
     *
     *
     * @param input the `InputStream` to read from
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     */
    @Throws(IOException::class) fun toByteArray(input: InputStream?): ByteArray {
        ByteArrayOutputStream().use { output ->
            copy(input, output)
            return output.toByteArray()
        }
    }

    /**
     * Gets the contents of an `InputStream` as a `byte[]`.
     * Use this method instead of `toByteArray(InputStream)`
     * when `InputStream` size is known
     *
     * @param input the `InputStream` to read from
     * @param size the size of `InputStream`
     * @return the requested byte array
     * @throws IOException              if an I/O error occurs or `InputStream` size differ from parameter
     * size
     * @throws IllegalArgumentException if size is less than zero
     * @since 2.1
     */
    @Throws(IOException::class) fun toByteArray(input: InputStream, size: Int): ByteArray {
        require(size >= 0) { "Size must be equal or greater than zero: $size" }
        if (size == 0) {
            return EMPTY_BYTE_ARRAY
        }
        val data = ByteArray(size)
        var offset = 0
        var read: Int = 0
        while (offset < size && input.read(data, offset, size - offset).also { read = it } != EOF) {
            offset += read
        }
        if (offset != size) {
            throw IOException("Unexpected read size. current: $offset, expected: $size")
        }
        return data
    }

    /**
     * Gets contents of an `InputStream` as a `byte[]`.
     * Use this method instead of `toByteArray(InputStream)`
     * when `InputStream` size is known.
     * **NOTE:** the method checks that the length can safely be cast to an int without truncation
     * before using [IOUtils.toByteArray] to read into the byte array.
     * (Arrays can have no more than Integer.MAX_VALUE entries anyway)
     *
     * @param input the `InputStream` to read from
     * @param size the size of `InputStream`
     * @return the requested byte array
     * @throws IOException              if an I/O error occurs or `InputStream` size differ from parameter
     * size
     * @throws IllegalArgumentException if size is less than zero or size is greater than Integer.MAX_VALUE
     * @see IOUtils.toByteArray
     * @since 2.1
     */
    @Throws(IOException::class) fun toByteArray(input: InputStream, size: Long): ByteArray {
        require(size <= Int.MAX_VALUE) { "Size cannot be greater than Integer max value: $size" }
        return toByteArray(input, size.toInt())
    }

    /**
     * Gets the contents of a `Reader` as a `byte[]`
     * using the default character encoding of the platform.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedReader`.
     *
     * @param input the `Reader` to read from
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     */
    @Deprecated("2.5 use {@link #toByteArray(Reader, Charset)} instead") @Throws(IOException::class) fun toByteArray(input: Reader): ByteArray {
        return toByteArray(input, Charset.defaultCharset())
    }

    /**
     * Gets the contents of a `Reader` as a `byte[]`
     * using the specified character encoding.
     *
     *
     * This method buffers the input internally, so there is no need to use a
     * `BufferedReader`.
     *
     * @param input the `Reader` to read from
     * @param charset the charset to use, null means platform default
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs
     * @since 2.3
     */
    @Throws(IOException::class) fun toByteArray(input: Reader, charset: Charset = Charsets.UTF_8): ByteArray {
        ByteArrayOutputStream().use { output ->
            copy(input, output, charset)
            return output.toByteArray()
        }
    }

    /**
     * Gets the contents of a `String` as a `byte[]`
     * using the default character encoding of the platform.
     *
     *
     * This is the same as [String.getBytes].
     *
     * @param input the `String` to convert
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O error occurs (never occurs)
     */
    @Deprecated("2.5 Use {@link String#getBytes()} instead") @Throws(IOException::class) fun toByteArray(input: String): ByteArray {
        // make explicit the use of the default charset
        return input.toByteArray(Charset.defaultCharset())
    }

    /**
     * Gets the contents of a `URI` as a `byte[]`.
     *
     * @param uri the `URI` to read
     * @return the requested byte array
     * @throws NullPointerException if the uri is null
     * @throws IOException          if an I/O exception occurs
     * @since 2.4
     */
    @Throws(IOException::class) fun toByteArray(uri: URI): ByteArray {
        return toByteArray(uri.toURL())
    }

    /**
     * Gets the contents of a `URL` as a `byte[]`.
     *
     * @param url the `URL` to read
     * @return the requested byte array
     * @throws NullPointerException if the input is null
     * @throws IOException          if an I/O exception occurs
     * @since 2.4
     */
    @Throws(IOException::class) fun toByteArray(url: URL): ByteArray {
        val conn = url.openConnection()
        return try {
            toByteArray(conn)
        } finally {
            close(conn)
        }
    }

    /**
     * Gets the contents of a `URLConnection` as a `byte[]`.
     *
     * @param urlConn the `URLConnection` to read
     * @return the requested byte array
     * @throws NullPointerException if the urlConn is null
     * @throws IOException          if an I/O exception occurs
     * @since 2.4
     */
    @Throws(IOException::class) fun toByteArray(urlConn: URLConnection): ByteArray {
        urlConn.getInputStream().use { inputStream -> return toByteArray(inputStream) }
    }


}
