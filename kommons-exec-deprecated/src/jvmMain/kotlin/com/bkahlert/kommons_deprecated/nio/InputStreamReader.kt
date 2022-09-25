package com.bkahlert.kommons_deprecated.nio

import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.charset.Charset
import java.nio.charset.CharsetDecoder
import java.nio.charset.CoderResult
import java.nio.charset.CodingErrorAction
import java.nio.charset.MalformedInputException
import java.nio.charset.UnmappableCharacterException

/**
 * A class for turning a byte stream into a character stream. Data read from the
 * source input stream is converted into characters by either a default or a
 * provided character converter. The default encoding is taken from the
 * "file.encoding" system property. `InputStreamReader` contains a buffer
 * of bytes read from the source stream and converts these into characters as
 * needed. The buffer size is 8K.
 *
 * ***Note for JLine:** the default InputStreamReader that comes from the JRE
 * usually read more bytes than needed from the input stream, which
 * is not usable in a character per character model used in the terminal.
 * We thus use the harmony code which only reads the minimal number of bytes.*
 *
 * @see <a href="https://github.com/jline/jline3/blob/92a63e1494d7e24a9f36cf639257b181080a535f/terminal/src/main/java/org/jline/utils/InputStreamReader.java"
 * >JLine 3 InputStreamReader</a>
 */
public class InputStreamReader(
    inputStream: InputStream,
    charset: Charset = Charsets.UTF_8,
) : Reader(inputStream) {
    private var inputStream: InputStream? = inputStream
    private var endOfInput = false
    private var decoder: CharsetDecoder = charset.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE)
    public var bytes: ByteBuffer = ByteBuffer.allocate(BUFFER_SIZE).apply { limit(0) }
    private var pending: Char = (-1).toChar()

    /**
     * Closes this reader. This implementation closes the source [InputStream] and
     * releases all local storage.
     */
    override fun close() {
        synchronized(lock) {
            inputStream?.close()
            inputStream = null
        }
    }

    /**
     * Returns the name of the encoding used to convert bytes into characters.
     * The value `null` is returned if this reader has been closed.
     *
     * @return the name of the character converter or `null` if this
     * reader is closed.
     */
    public val encoding: String?
        get() = if (!isOpen) null else decoder.charset().name()

    /**
     * Reads a single character from this reader and returns it as an integer
     * with the two higher-order bytes set to 0. Returns -1 if the end of the
     * reader has been reached. The byte value is either obtained from
     * converting bytes in this reader's buffer or by first filling the buffer
     * from the source InputStream and then reading from the buffer.
     *
     * @return the character read or -1 if the end of the reader has been
     * reached.
     * @throws IOException
     * if this reader is closed or some other I/O error occurs.
     */
    override fun read(): Int {
        synchronized(lock) {
            check(isOpen) { "InputStreamReader is closed." }
            if (pending != (-1).toChar()) {
                val c = pending
                pending = (-1).toChar()
                return c.code
            }
            val buf = CharArray(2)
            val nb = read(buf, 0, 2)
            if (nb == 2) {
                pending = buf[1]
            }
            return if (nb > 0) {
                buf[0].code
            } else {
                -1
            }
        }
    }

    /**
     * Reads at most `length` characters from this reader and stores them
     * at position `offset` in the character array `buf`. Returns
     * the number of characters actually read or -1 if the end of the reader has
     * been reached. The bytes are either obtained from converting bytes in this
     * reader's buffer or by first filling the buffer from the source
     * InputStream and then reading from the buffer.
     *
     * @param buf
     * the array to store the characters read.
     * @param offset
     * the initial position in `buf` to store the characters
     * read from this reader.
     * @param length
     * the maximum number of characters to read.
     * @return the number of characters read or -1 if the end of the reader has
     * been reached.
     * @throws IndexOutOfBoundsException
     * if `offset < 0` or `length < 0`, or if
     * `offset + length` is greater than the length of
     * `buf`.
     * @throws IOException
     * if this reader is closed or some other I/O error occurs.
     */
    override fun read(buf: CharArray, offset: Int, length: Int): Int {
        synchronized(lock) {
            if (!isOpen) {
                throw IOException("InputStreamReader is closed.")
            }
            if (offset < 0 || offset > buf.size - length || length < 0) {
                throw IndexOutOfBoundsException()
            }
            if (length == 0) {
                return 0
            }
            val out = CharBuffer.wrap(buf, offset, length)
            var result = CoderResult.UNDERFLOW

            // bytes.remaining() indicates number of bytes in buffer
            // when 1-st time entered, it'll be equal to zero
            var needInput = !bytes.hasRemaining()
            while (out.position() == offset) {
                // fill the buffer if needed
                if (needInput) {
                    try {
                        if (inputStream!!.available() == 0
                            && out.position() > offset
                        ) {
                            // we could return the result without blocking read
                            break
                        }
                    } catch (e: IOException) {
                        // available didn't work so just try the read
                    }
                    val off = bytes.arrayOffset() + bytes.limit()
                    val wasRead = inputStream!!.read(bytes.array(), off, 1)
                    if (wasRead == -1) {
                        endOfInput = true
                        break
                    } else if (wasRead == 0) {
                        break
                    }
                    bytes.limit(bytes.limit() + wasRead)
                }

                // decode bytes
                result = decoder.decode(bytes, out, false)
                needInput = if (result.isUnderflow) {
                    // compact the buffer if no space left
                    if (bytes.limit() == bytes.capacity()) {
                        bytes.compact()
                        bytes.limit(bytes.position())
                        bytes.position(0)
                    }
                    true
                } else {
                    break
                }
            }
            if (result === CoderResult.UNDERFLOW && endOfInput) {
                result = decoder.decode(bytes, out, true)
                decoder.flush(out)
                decoder.reset()
            }
            if (result.isMalformed) {
                throw MalformedInputException(result.length())
            } else if (result.isUnmappable) {
                throw UnmappableCharacterException(result.length())
            }
            return if (out.position() - offset == 0) -1 else out.position() - offset
        }
    }

    /*
     * Whether this InputStreamReader is open.
     */
    private val isOpen: Boolean get() = inputStream != null

    /**
     * Indicates whether this reader is ready to be read without blocking. If
     * the result is `true`, the next `read()` will not block. If
     * the result is `false` then this reader may or may not block when
     * `read()` is called. This implementation returns `true` if
     * there are bytes available in the buffer or the source stream has bytes
     * available.
     *
     * @return `true` if the receiver will not block when `read()`
     * is called, `false` if unknown or blocking will occur.
     * @throws IOException
     * if this reader is closed or some other I/O error occurs.
     */
    override fun ready(): Boolean {
        synchronized(lock) {
            if (inputStream == null) {
                throw IOException("InputStreamReader is closed.")
            }
            return try {
                bytes.hasRemaining() || inputStream!!.available() > 0
            } catch (e: IOException) {
                false
            }
        }
    }

    public companion object {
        private const val BUFFER_SIZE = 4
    }
}
