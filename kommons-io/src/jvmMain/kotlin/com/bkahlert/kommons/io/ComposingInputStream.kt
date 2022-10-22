package com.bkahlert.kommons.io

import java.io.InputStream

/**
 * An [InputStream] that is backed by the specified [bytes].
 */
public class ComposingInputStream(
    private val bytes: List<ByteArray>,
) : InputStream() {
    public constructor(vararg bytes: ByteArray) : this(bytes.asList())

    private var currentBytesIndex = 0
    private val currentBytes get() = bytes.getOrNull(currentBytesIndex)
    private var pos = 0

    override fun available(): Int = currentBytes?.size ?: 0

    override fun read(): Int {
        val currentBytes = currentBytes ?: return -1
        return if (pos < currentBytes.size) {
            currentBytes[pos++].toInt() and 0xff
        } else {
            currentBytesIndex++
            pos = 0
            read()
        }
    }
}
