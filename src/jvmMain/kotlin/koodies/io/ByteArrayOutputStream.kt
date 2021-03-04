package koodies.io

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream
import java.nio.charset.Charset
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thread-safe version of [AbstractByteArrayOutputStream] using [ReentrantLock] locking.
 */
public class ByteArrayOutputStream @JvmOverloads constructor(size: Int = DEFAULT_BUFFER_SIZE) : AbstractByteArrayOutputStream() {

    private val lock = ReentrantLock()

    init {
        require(size >= 0) { "$size must be positive" }
        lock.withLock { increaseCapacity(size) }
    }

    private fun checkBounds(offset: Int, bytes: ByteArray, length: Int) {
        if (length < 0 || offset !in 0..bytes.size || offset + length !in 0..bytes.size) throw IndexOutOfBoundsException()
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) {
        checkBounds(offset, bytes, length)
        if (length > 0) lock.withLock { writeToBuffers(bytes, offset, length) }
    }

    override fun write(bytes: Int): Unit = lock.withLock { writeToBuffers(bytes) }
    override fun write(inputStream: InputStream): Int = lock.withLock { writeToBuffers(inputStream) }
    override fun size(): Int = lock.withLock { totalBytesWritten }
    override fun reset(): Unit = lock.withLock { resetBuffers() }
    override fun writeTo(out: OutputStream): Unit = lock.withLock { writeBytesTo(out) }
    override fun toInputStream(): InputStream = lock.withLock { toInputStream { buffer, offset, length -> ByteArrayInputStream(buffer, offset, length) } }
    override fun toByteArray(): ByteArray = lock.withLock { toByteArrayImpl() }
}


/**
 * An [OutputStream] implementation that does not need
 * to know the necessary space but dynamically grows if needed.
 */
public abstract class AbstractByteArrayOutputStream : OutputStream() {
    private val buffers: MutableList<ByteArray> = ArrayList()
    private var bufferIndex = 0
    private var bytesWritten = 0
    private var currentBuffer: ByteArray? = null
    protected var totalBytesWritten: Int = 0
    private var reuseBuffersAfterReset = true

    protected val requireBuffer: ByteArray get() = checkNotNull(currentBuffer)

    /**
     * Provides a new buffer either by allocating
     * a new one (in which case the resulting capacity will be [newCapacityOnAllocation])
     * or re-cycling an existing one.
     */
    protected fun increaseCapacity(newCapacityOnAllocation: Int) {
        val canRecycle = bufferIndex < buffers.size - 1
        if (canRecycle) {
            bytesWritten += currentBuffer!!.size
            bufferIndex++
            currentBuffer = buffers[bufferIndex]
        } else {
            val newBufferSize: Int = currentBuffer?.run {
                (size shl 1).coerceAtLeast(newCapacityOnAllocation - bytesWritten).also { bytesWritten += size }
            } ?: newCapacityOnAllocation.also { bytesWritten = 0 }

            bufferIndex++
            currentBuffer = ByteArray(newBufferSize).also { buffers.add(it) }
        }
    }

    abstract override fun write(bytes: ByteArray, offset: Int, length: Int)
    protected fun writeToBuffers(bytes: ByteArray, offset: Int, length: Int) {
        val totalBytesWrittenAfterwards = totalBytesWritten + length
        var remaining = length
        var inBufferPos = totalBytesWritten - bytesWritten
        while (remaining > 0) {
            val part = remaining.coerceAtMost(requireBuffer.size - inBufferPos)
            System.arraycopy(bytes, offset + length - remaining, requireBuffer, inBufferPos, part)
            remaining -= part
            if (remaining > 0) {
                increaseCapacity(totalBytesWrittenAfterwards)
                inBufferPos = 0
            }
        }
        totalBytesWritten = totalBytesWrittenAfterwards
    }

    abstract override fun write(bytes: Int)
    protected fun writeToBuffers(bytes: Int) {
        var inBufferPos = totalBytesWritten - bytesWritten
        if (inBufferPos == requireBuffer.size) {
            increaseCapacity(totalBytesWritten + 1)
            inBufferPos = 0
        }
        requireBuffer[inBufferPos] = bytes.toByte()
        totalBytesWritten++
    }

    public abstract fun write(inputStream: InputStream): Int
    protected fun writeToBuffers(inputStream: InputStream): Int {
        var readCount = 0
        var inBufferPos = totalBytesWritten - bytesWritten
        var n = inputStream.read(requireBuffer, inBufferPos, requireBuffer.size - inBufferPos)
        while (n != EOF) {
            readCount += n
            inBufferPos += n
            totalBytesWritten += n
            if (inBufferPos == requireBuffer.size) {
                increaseCapacity(requireBuffer.size)
                inBufferPos = 0
            }
            n = inputStream.read(requireBuffer, inBufferPos, requireBuffer.size - inBufferPos)
        }
        return readCount
    }

    public abstract fun size(): Int
    override fun close(): Unit = Unit

    public abstract fun reset()
    protected fun resetBuffers() {
        totalBytesWritten = 0
        bytesWritten = 0
        bufferIndex = 0
        if (reuseBuffersAfterReset) {
            currentBuffer = buffers[bufferIndex]
        } else {
            currentBuffer = null
            buffers[0].size.also { buffers.clear() }.let { increaseCapacity(it) }
            reuseBuffersAfterReset = true
        }
    }

    public abstract fun writeTo(out: OutputStream)
    protected fun writeBytesTo(out: OutputStream) {
        var remaining = totalBytesWritten
        buffers.forEach { buf ->
            val length = buf.size.coerceAtMost(remaining)
            out.write(buf, 0, length)
            remaining -= length
            if (remaining == 0) return
        }
    }

    public abstract fun toInputStream(): InputStream
    protected fun <T : InputStream?> toInputStream(factory: (ByteArray, Int, Int) -> T): InputStream {
        var remaining = totalBytesWritten
        if (remaining == 0) return ClosedInputStream
        val list: MutableList<T> = ArrayList(buffers.size)
        for (buffer in buffers) {
            val size = buffer.size.coerceAtMost(remaining)
            list.add(factory(buffer, 0, size))
            remaining -= size
            if (remaining == 0) break
        }
        reuseBuffersAfterReset = false
        return SequenceInputStream(Collections.enumeration(list))
    }

    public abstract fun toByteArray(): ByteArray
    protected fun toByteArrayImpl(): ByteArray {
        var remaining = totalBytesWritten
        if (remaining == 0) return EmptyByteArray
        val newBuffer = ByteArray(remaining)
        var pos = 0
        for (buffer in buffers) {
            val length = buffer.size.coerceAtMost(remaining)
            System.arraycopy(buffer, 0, newBuffer, pos, length)
            pos += length
            remaining -= length
            if (remaining == 0) break
        }
        return newBuffer
    }

    public fun toString(charset: Charset): String = String(toByteArray(), charset)
    override fun toString(): String = toString(Charsets.UTF_8)
}

public val EOF: Int = -1

public object ClosedInputStream : InputStream() {
    override fun read(): Int = EOF
}

private val EmptyByteArray = ByteArray(0)
