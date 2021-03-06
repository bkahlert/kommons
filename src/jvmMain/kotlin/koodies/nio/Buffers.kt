package koodies.nio

import java.nio.ByteBuffer

/**
 * If this byte buffer if backed by an byte array
 * this field contains a copy of its current content.
 *
 * The offset is already correcty, that the payload
 * starts at position `0` and ends at [ByteArray.size]`-1`.
 */
public val ByteBuffer.bytes: ByteArray
    get() {
        check(hasArray()) { "This buffer has no backing byte array." }
        val backingArray = array()
        val backingArrayOffset = arrayOffset()
        return ByteArray(position()) { backingArray[backingArrayOffset + it] }
    }
