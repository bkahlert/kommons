package com.bkahlert.kommons.test.output

import java.io.OutputStream
import java.io.PrintStream
import java.util.function.Consumer

/**
 * An [OutputStream] implementation that captures written strings.
 */
class OutputStreamCapture(private val systemStream: PrintStream, private val copy: Consumer<String>) : OutputStream() {
    override fun write(b: Int) = write(byteArrayOf((b and 0xFF).toByte()))

    override fun write(b: ByteArray, off: Int, len: Int) {
        copy.accept(String(b, off, len))
        systemStream.write(b, off, len)
    }

    override fun flush() = systemStream.flush()
}
