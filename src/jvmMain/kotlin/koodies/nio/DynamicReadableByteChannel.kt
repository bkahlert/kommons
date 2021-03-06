package koodies.nio

import koodies.debug.asEmoji
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.ReadableByteChannel
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * A readable byte channel of which the data are dynamically
 * provided by [yield].
 *
 * Calling [read] will read as much data as possible (limited by
 * availability of data and capacity of provided buffer).
 * If not data are available, no bytes are read, though later
 * data might become available.
 *
 * This channel [isOpen] as long as [close] has not been called
 * or there are still unread data. In other words: Only if all data
 * have been read and [close] was called this channel is effectively closed.
 *
 * Providing data using [yield] is possible as long [close] was not called.
 * There are no restrictions in terms of yielded data as the used buffer
 * dynamically grows in size.
 */
public class DynamicReadableByteChannel : ReadableByteChannel {

    private val buffer: MemoryReclaimableByteArrayOutputStream = MemoryReclaimableByteArrayOutputStream()
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()

    public val available: Int get() = buffer.size()
    private val depleted: Boolean get() = buffer.size() == 0
    private var closed: Boolean = false

    override fun isOpen(): Boolean = lock.read { !closed || !depleted }

    override fun read(dst: ByteBuffer): Int = lock.write {
        if (!isOpen) return -1
        val readable = available.coerceAtMost(dst.capacity())
        check(readable >= 0)
        if (readable > 0) dst.put(buffer.take(readable))
        readable
    }

    override fun close(): Unit = lock.write { closed = true }

    override fun toString(): String = "${this::class.simpleName}(open=${isOpen.asEmoji}; buffer=$buffer)"

    public fun yield(bytes: ByteArray): Unit = lock.write {
        if (closed) throw ClosedChannelException()
        buffer.writeBytes(bytes)
    }

    internal val bytes: ByteArray get() = buffer.toByteArray()

}
