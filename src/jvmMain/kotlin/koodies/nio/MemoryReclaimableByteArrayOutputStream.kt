package koodies.nio

import koodies.debug.replaceNonPrintableCharacters
import java.io.ByteArrayOutputStream
import koodies.math.BigInteger

/**
 * A [ByteArrayOutputStream] implementation that allows to remove
 * bytes from the start—reclaiming otherwise wasted memory.
 */
public class MemoryReclaimableByteArrayOutputStream(private val initialSize: Int = 1024) : ByteArrayOutputStream(initialSize) {
    public companion object {
        public val MAX_ARRAY_SIZE: BigInteger = (Int.MAX_VALUE - 8).toBigInteger()
    }

    /**
     * Takes and returns the first [bytes] which increases the available capacity by
     * the same amount without increasing the reserved memory.
     */
    public fun take(bytes: Int): ByteArray {
        check(bytes <= count)
        return ByteArray(bytes)
            .also { buf.copyInto(it, 0, 0, bytes) }
            .also { drop(bytes) }
    }

    /**
     * Drops the first [bytes] which increases the available capacity by
     * the same amount without increasing the reserved memory.
     */
    public fun drop(bytes: Int) {
        check(bytes <= count)
        buf.copyInto(buf, 0, bytes, count)
        count -= bytes
    }

    /**
     * Increases the capacity [by] bytes.
     */
    public fun grow(by: Int) {
        check(by > 0) { "$by must be greater than 0" }
        val newSize = (buf.size.toBigInteger() + by.toBigInteger()).also {
            check(it < MAX_ARRAY_SIZE) { "New capacity $it must be smaller than $MAX_ARRAY_SIZE" }
        }
        val newBuf = ByteArray(newSize.toInt())
        buf.copyInto(newBuf, 0, 0, count)
        buf = newBuf
        count -= by
    }

    public fun ensureSpace() {
        if (remaining < initialSize) grow(initialSize)
    }

    public val remaining: Int get() = buf.size - count

    override fun toString(): String = "$count/${buf.size} (${count / buf.size}% used): " + buf.take(count)
        .joinToString(separator = "", limit = 100) { it.toChar().toString().replaceNonPrintableCharacters() }

    public operator fun get(pos: Int): Byte = buf[pos]
}

