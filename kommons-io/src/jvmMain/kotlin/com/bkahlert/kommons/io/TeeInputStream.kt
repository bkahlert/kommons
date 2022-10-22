package com.bkahlert.kommons.io

import java.io.FilterInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * An [InputStream] writes everything read from the specified [input]
 * to the specified [branches].
 */
public class TeeInputStream(
    private val input: InputStream?,
    private vararg val branches: OutputStream,
    private val closeBranches: Boolean = false,
) : FilterInputStream(input) {

    private val lock = ReentrantLock()

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int = lock.withLock {
        super.read(bytes, offset, length)
            .also { read ->
                if (read != -1) branches.forEach { it.write(bytes, offset, read) }
            }
    }

    override fun read(): Int = lock.withLock {
        super.read()
            .also { byte ->
                if (byte != -1) branches.forEach { it.write(byte) }
            }
    }

    override fun close(): Unit = lock.withLock {
        try {
            super.close()
        } finally {
            if (closeBranches) {
                branches.forEach { kotlin.runCatching { it.close() } }
            }
        }
    }
}
