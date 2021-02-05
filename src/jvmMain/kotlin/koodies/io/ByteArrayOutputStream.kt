package koodies.io

import koodies.io.AbstractByteArrayOutputStream.Companion.EOF
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Implements a ThreadSafe version of [AbstractByteArrayOutputStream] using instance synchronization.
 */
class ByteArrayOutputStream @JvmOverloads constructor(size: Int = DEFAULT_BUFFER_SIZE) : AbstractByteArrayOutputStream() {

    private val lock = ReentrantLock()

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (off < 0
            || off > b.size
            || len < 0
            || off + len > b.size
            || off + len < 0
        ) {
            throw IndexOutOfBoundsException()
        } else if (len == 0) {
            return
        }
        lock.withLock { writeImpl(b, off, len) }
    }

    override fun write(b: Int) {
        lock.withLock { writeImpl(b) }
    }

    @Throws(IOException::class) override fun write(`in`: InputStream?): Int {
        return lock.withLock { writeImpl(`in`!!) }
    }

    override fun size(): Int {
        return lock.withLock { count }
    }

    /**
     * @see java.io.ByteArrayOutputStream.reset
     */
    override fun reset() {
        lock.withLock { resetImpl() }
    }

    @Throws(IOException::class) override fun writeTo(out: OutputStream?) {
        lock.withLock { writeToImpl(out!!) }
    }

    override fun toInputStream(): InputStream {
        return lock.withLock { toInputStream { buffer, offset, length -> ByteArrayInputStream(buffer, offset, length) } }
    }

    override fun toByteArray(): ByteArray {
        return lock.withLock { toByteArrayImpl() }
    }

    override fun toString(): String {
        return super.toString()
    }

    companion object {
        /**
         * Fetches entire contents of an `InputStream` and represent
         * same data as result InputStream.
         *
         *
         * This method is useful where,
         *
         *
         *  * Source InputStream is slow.
         *  * It has network resources associated, so we cannot keep it open for
         * long time.
         *  * It has network timeout associated.
         *
         * It can be used in favor of [.toByteArray], since it
         * avoids unnecessary allocation and copy of byte[].<br></br>
         * This method buffers the input internally, so there is no need to use a
         * `BufferedInputStream`.
         *
         * @param input Stream to be fully buffered.
         *
         * @return A fully buffered stream.
         *
         * @throws IOException if an I/O error occurs
         * @since 2.0
         */
        @JvmOverloads @Throws(IOException::class) fun toBufferedInputStream(input: InputStream, size: Int = DEFAULT_BUFFER_SIZE): InputStream {
            ByteArrayOutputStream(size).use { output ->
                output.write(input)
                return output.toInputStream()
            }
        }
    }
    /**
     * Creates a new byte array output stream, with a buffer capacity of
     * the specified size, in bytes.
     *
     * @param size the initial size
     *
     * @throws IllegalArgumentException if size is negative
     */
    /**
     * Creates a new byte array output stream.
     */
    init {
        require(size >= 0) { "Negative initial size: $size" }
        lock.withLock { needNewBuffer(size) }
    }
}


/**
 * This is the base class for implementing an output stream in which the data
 * is written into a byte array. The buffer automatically grows as data
 * is written to it.
 *
 *
 * The data can be retrieved using `toByteArray()` and
 * `toString()`.
 * Closing an `AbstractByteArrayOutputStream` has no effect. The methods in
 * this class can be called after the stream has been closed without
 * generating an `IOException`.
 *
 *
 *
 * This is the base for an alternative implementation of the
 * [java.io.ByteArrayOutputStream] class. The original implementation
 * only allocates 32 bytes at the beginning. As this class is designed for
 * heavy duty it starts at {@value #DEFAULT_SIZE} bytes. In contrast to the original it doesn't
 * reallocate the whole memory block but allocates additional buffers. This
 * way no buffers need to be garbage collected and the contents don't have
 * to be copied to the new buffer. This class is designed to behave exactly
 * like the original. The only exception is the deprecated
 * [java.io.ByteArrayOutputStream.toString] method that has been
 * ignored.
 *
 *
 * @since 2.7
 */
abstract class AbstractByteArrayOutputStream : OutputStream() {
    /** The list of buffers, which grows and never reduces.  */
    private val buffers: MutableList<ByteArray> = ArrayList()

    /** The index of the current buffer.  */
    private var currentBufferIndex = 0

    /** The total count of bytes in all the filled buffers.  */
    private var filledBufferSum = 0

    /** The current buffer.  */
    private var currentBuffer: ByteArray? = null

    /** The total count of bytes written.  */
    protected var count = 0

    /** Flag to indicate if the buffers can be reused after reset  */
    private var reuseBuffers = true

    /**
     * Makes a new buffer available either by allocating
     * a new one or re-cycling an existing one.
     *
     * @param newcount  the size of the buffer if one is created
     */
    protected fun needNewBuffer(newcount: Int) {
        if (currentBufferIndex < buffers.size - 1) {
            //Recycling old buffer
            filledBufferSum += currentBuffer!!.size
            currentBufferIndex++
            currentBuffer = buffers[currentBufferIndex]
        } else {
            //Creating new buffer
            val newBufferSize: Int
            if (currentBuffer == null) {
                newBufferSize = newcount
                filledBufferSum = 0
            } else {
                newBufferSize = Math.max(
                    currentBuffer!!.size shl 1,
                    newcount - filledBufferSum)
                filledBufferSum += currentBuffer!!.size
            }
            currentBufferIndex++
            currentBuffer = ByteArray(newBufferSize)
            buffers.add(currentBuffer!!)
        }
    }

    /**
     * Writes the bytes to the byte array.
     * @param b the bytes to write
     * @param off The start offset
     * @param len The number of bytes to write
     */
    abstract override fun write(b: ByteArray, off: Int, len: Int)

    /**
     * Writes the bytes to the byte array.
     * @param b the bytes to write
     * @param off The start offset
     * @param len The number of bytes to write
     */
    protected fun writeImpl(b: ByteArray?, off: Int, len: Int) {
        val newcount = count + len
        var remaining = len
        var inBufferPos = count - filledBufferSum
        while (remaining > 0) {
            val part = Math.min(remaining, currentBuffer!!.size - inBufferPos)
            System.arraycopy(b, off + len - remaining, currentBuffer, inBufferPos, part)
            remaining -= part
            if (remaining > 0) {
                needNewBuffer(newcount)
                inBufferPos = 0
            }
        }
        count = newcount
    }

    /**
     * Write a byte to byte array.
     * @param b the byte to write
     */
    abstract override fun write(b: Int)

    /**
     * Write a byte to byte array.
     * @param b the byte to write
     */
    protected fun writeImpl(b: Int) {
        var inBufferPos = count - filledBufferSum
        if (inBufferPos == currentBuffer!!.size) {
            needNewBuffer(count + 1)
            inBufferPos = 0
        }
        currentBuffer!![inBufferPos] = b.toByte()
        count++
    }

    /**
     * Writes the entire contents of the specified input stream to this
     * byte stream. Bytes from the input stream are read directly into the
     * internal buffers of this streams.
     *
     * @param in the input stream to read from
     * @return total number of bytes read from the input stream
     * (and written to this stream)
     * @throws IOException if an I/O error occurs while reading the input stream
     * @since 1.4
     */
    @Throws(IOException::class) abstract fun write(`in`: InputStream?): Int

    /**
     * Writes the entire contents of the specified input stream to this
     * byte stream. Bytes from the input stream are read directly into the
     * internal buffers of this streams.
     *
     * @param in the input stream to read from
     * @return total number of bytes read from the input stream
     * (and written to this stream)
     * @throws IOException if an I/O error occurs while reading the input stream
     * @since 2.7
     */
    @Throws(IOException::class) protected fun writeImpl(`in`: InputStream): Int {
        var readCount = 0
        var inBufferPos = count - filledBufferSum
        var n = `in`.read(currentBuffer, inBufferPos, currentBuffer!!.size - inBufferPos)
        while (n != EOF) {
            readCount += n
            inBufferPos += n
            count += n
            if (inBufferPos == currentBuffer!!.size) {
                needNewBuffer(currentBuffer!!.size)
                inBufferPos = 0
            }
            n = `in`.read(currentBuffer, inBufferPos, currentBuffer!!.size - inBufferPos)
        }
        return readCount
    }

    /**
     * Returns the current size of the byte array.
     *
     * @return the current size of the byte array
     */
    abstract fun size(): Int

    /**
     * Closing a `ByteArrayOutputStream` has no effect. The methods in
     * this class can be called after the stream has been closed without
     * generating an `IOException`.
     *
     * @throws IOException never (this method should not declare this exception
     * but it has to now due to backwards compatibility)
     */
    @Throws(IOException::class) override fun close() {
        //nop
    }

    /**
     * @see java.io.ByteArrayOutputStream.reset
     */
    abstract fun reset()

    /**
     * @see java.io.ByteArrayOutputStream.reset
     */
    protected fun resetImpl() {
        count = 0
        filledBufferSum = 0
        currentBufferIndex = 0
        if (reuseBuffers) {
            currentBuffer = buffers[currentBufferIndex]
        } else {
            //Throw away old buffers
            currentBuffer = null
            val size: Int = buffers[0].size
            buffers.clear()
            needNewBuffer(size)
            reuseBuffers = true
        }
    }

    /**
     * Writes the entire contents of this byte stream to the
     * specified output stream.
     *
     * @param out  the output stream to write to
     * @throws IOException if an I/O error occurs, such as if the stream is closed
     * @see java.io.ByteArrayOutputStream.writeTo
     */
    @Throws(IOException::class) abstract fun writeTo(out: OutputStream?)

    /**
     * Writes the entire contents of this byte stream to the
     * specified output stream.
     *
     * @param out  the output stream to write to
     * @throws IOException if an I/O error occurs, such as if the stream is closed
     * @see java.io.ByteArrayOutputStream.writeTo
     */
    @Throws(IOException::class) protected fun writeToImpl(out: OutputStream) {
        var remaining = count
        for (buf in buffers) {
            val c = Math.min(buf.size, remaining)
            out.write(buf, 0, c)
            remaining -= c
            if (remaining == 0) {
                break
            }
        }
    }

    /**
     * Gets the current contents of this byte stream as a Input Stream. The
     * returned stream is backed by buffers of `this` stream,
     * avoiding memory allocation and copy, thus saving space and time.<br></br>
     *
     * @return the current contents of this output stream.
     * @see java.io.ByteArrayOutputStream.toByteArray
     * @see .reset
     * @since 2.5
     */
    abstract fun toInputStream(): InputStream?

    /**
     * Gets the current contents of this byte stream as a Input Stream. The
     * returned stream is backed by buffers of `this` stream,
     * avoiding memory allocation and copy, thus saving space and time.<br></br>
     *
     * @param <T> the type of the InputStream which makes up
     * the [SequenceInputStream].
     * @param isConstructor A constructor for an InputStream which makes
     * up the [SequenceInputStream].
     *
     * @return the current contents of this output stream.
     * @see java.io.ByteArrayOutputStream.toByteArray
     * @see .reset
     * @since 2.7
    </T> */
    protected fun <T : InputStream?> toInputStream(factory: (ByteArray, Int, Int) -> T): InputStream {
        var remaining = count
        if (remaining == 0) {
            return ClosedInputStream.CLOSED_INPUT_STREAM
        }
        val list: MutableList<T> = ArrayList(buffers.size)
        for (buffer in buffers) {
            val size = buffer.size.coerceAtMost(remaining)
            list.add(factory(buffer, 0, size))
            remaining -= size
            if (remaining == 0) {
                break
            }
        }
        reuseBuffers = false
        return SequenceInputStream(Collections.enumeration(list))
    }

    /**
     * Gets the current contents of this byte stream as a byte array.
     * The result is independent of this stream.
     *
     * @return the current contents of this output stream, as a byte array
     * @see java.io.ByteArrayOutputStream.toByteArray
     */
    abstract fun toByteArray(): ByteArray?

    /**
     * Gets the current contents of this byte stream as a byte array.
     * The result is independent of this stream.
     *
     * @return the current contents of this output stream, as a byte array
     * @see java.io.ByteArrayOutputStream.toByteArray
     */
    protected fun toByteArrayImpl(): ByteArray {
        var remaining = count
        if (remaining == 0) {
            return EMPTY_BYTE_ARRAY
        }
        val newbuf = ByteArray(remaining)
        var pos = 0
        for (buf in buffers) {
            val c = Math.min(buf.size, remaining)
            System.arraycopy(buf, 0, newbuf, pos, c)
            pos += c
            remaining -= c
            if (remaining == 0) {
                break
            }
        }
        return newbuf
    }

    /**
     * Gets the current contents of this byte stream as a string
     * using the specified encoding.
     *
     * @param charset  the character encoding
     * @return the string converted from the byte array
     * @see java.io.ByteArrayOutputStream.toString
     * @since 2.5
     */
    fun toString(charset: Charset): String {
        return String(toByteArray()!!, charset)
    }

    override fun toString(): String {
        return toString(Charsets.UTF_8)
    }

    companion object {
        const val EOF = -1
        const val DEFAULT_SIZE = 1024

        /** A singleton empty byte array.  */
        private val EMPTY_BYTE_ARRAY = ByteArray(0)
    }
}


/**
 * Closed input stream. This stream returns EOF to all attempts to read
 * something from the stream.
 *
 *
 * Typically uses of this class include testing for corner cases in methods
 * that accept input streams and acting as a sentinel value instead of a
 * `null` input stream.
 *
 *
 * @since 1.4
 */
class ClosedInputStream : InputStream() {
    /**
     * Returns -1 to indicate that the stream is closed.
     *
     * @return always -1
     */
    override fun read(): Int {
        return EOF
    }

    companion object {
        /**
         * A singleton.
         */
        val CLOSED_INPUT_STREAM = ClosedInputStream()
    }
}

