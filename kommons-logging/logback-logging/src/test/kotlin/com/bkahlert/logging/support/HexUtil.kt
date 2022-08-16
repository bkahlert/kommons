package com.bkahlert.logging.support

/**
 * Utility for dealing with the hexadecimal system.
 *
 * @author Björn Kahlert
 */
object HexUtil {
    val HEX = charArrayOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'b', 'c', 'd', 'e', 'f'
    )
    val HEX_BYTES = HEX.size * HEX.size - 1
    val HEX_BYTE = HEX.size - 1

    /**
     * Converts the given decimal value to its 16-character hex form.
     *
     * @param decimal value
     *
     * @return hex representation
     */
    fun longToHex(decimal: Long): String {
        val byteCount = getByteCount(java.lang.Long.SIZE)
        val data = CharArray(byteCount)
        writeHexLong(data, decimal)
        return String(data)
    }

    /**
     * Converts the given decimal value to its 16-character hex form.
     *
     * @param decimal value
     *
     * @return hex representation
     */
    fun intToHex(decimal: Long): String {
        val byteCount = getByteCount(Integer.SIZE)
        val data = CharArray(byteCount)
        writeHexLong(data, decimal)
        return String(data)
    }

    fun getByteCount(size: Int): Int {
        return Math.toIntExact(Math.round(size / (StrictMath.log(HEX.size.toDouble()) / StrictMath.log(2.0))))
    }

    fun writeHexLong(data: CharArray, v: Long) {
        val maxShift = data.size - 2 shl 2
        var pos = 0
        val n = data.size
        while (pos < n) {
            writeHexByte(data, pos, (v ushr maxShift - (pos shl 2) and HEX_BYTES.toLong()).toByte())
            pos += 2
        }
    }

    fun writeHexByte(data: CharArray, pos: Int, b: Byte) {
        data[pos] = HEX[b.toInt() shr 4 and HEX_BYTE]
        data[pos + 1] = HEX[b.toInt() and HEX_BYTE]
    }
}
