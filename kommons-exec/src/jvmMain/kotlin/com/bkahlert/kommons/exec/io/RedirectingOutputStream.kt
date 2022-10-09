package com.bkahlert.kommons.exec.io

import com.bkahlert.kommons.debug.asString
import java.io.OutputStream

/**
 * An [OutputStream] that redirects everything written to it to the [redirection].
 */
public class RedirectingOutputStream(private val redirection: (ByteArray) -> Unit) : OutputStream() {

    override fun write(b: ByteArray): Unit =
        redirection.invoke(b)

    override fun write(b: ByteArray, off: Int, len: Int): Unit =
        redirection.invoke(b.copyOfRange(off, off + len))

    override fun write(b: Int): Unit =
        write(byteArrayOf((b and 0xFF).toByte()))

    override fun toString(): String = asString {
        put(::redirection, redirection)
    }
}
