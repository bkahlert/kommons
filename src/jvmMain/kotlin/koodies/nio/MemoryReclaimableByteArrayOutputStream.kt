package koodies.nio

import com.ionspin.kotlin.bignum.integer.BigInteger
import koodies.debug.replaceNonPrintableCharacters
import koodies.number.toBigInteger
import java.io.ByteArrayOutputStream

/**
 * A [ByteArrayOutputStream] implementation that allows to remove
 * bytes from the start—reclaiming otherwise wasted memory.
 */
class MemoryReclaimableByteArrayOutputStream(private val initialSize: Int = 1024) : ByteArrayOutputStream(initialSize) {
    companion object {
        val MAX_ARRAY_SIZE: BigInteger = (Int.MAX_VALUE - 8).toBigInteger()
    }

    /**
     * Drops the first [bytes] which increases the available capacity by
     * the same amount without increasing the reserved memory.
     */
    fun drop(bytes: Int) {
        check(bytes <= count)
        buf.copyInto(buf, 0, bytes, count)
        count -= bytes
    }

    /**
     * Increases the capacity [by] bytes.
     */
    fun grow(by: Int) {
        check(by > 0) { "$by must be greater than 0" }
        val newSize = (buf.size.toBigInteger() + by.toBigInteger()).also {
            check(it < MAX_ARRAY_SIZE) { "New capacity $it must be smaller than $MAX_ARRAY_SIZE" }
        }
        val newBuf = ByteArray(newSize.intValue(true))
        buf.copyInto(newBuf, 0, 0, count)
        buf = newBuf
        count -= by
    }

    fun ensureSpace() {
        if (remaining < initialSize) grow(initialSize)
    }

    val remaining get() = buf.size - count

    override fun toString() = "$count/${buf.size} (${count / buf.size}% used): " + buf.take(count)
        .joinToString(separator = "", limit = 100) { it.toChar().toString().replaceNonPrintableCharacters() }

    operator fun get(pos: Int): Byte = buf[pos]
}

