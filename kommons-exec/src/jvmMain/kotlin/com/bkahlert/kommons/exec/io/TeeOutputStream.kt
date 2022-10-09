package com.bkahlert.kommons.exec.io

import com.bkahlert.kommons.debug.asString
import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

internal class TeeOutputStream(
    private val output: OutputStream,
    private vararg val branches: OutputStream,
) : FilterOutputStream(output) {

    private val lock = ReentrantLock()

    override fun write(byte: Int): Unit = lock.withLock {
        super.write(byte)
        branches.forEach { it -> it.write(byte) }
    }

    override fun flush(): Unit = lock.withLock {
        super.flush()
        branches.forEach { it.flush() }
    }

    override fun close(): Unit = lock.withLock {
        try {
            super.close()
        } finally {
            branches.forEach { kotlin.runCatching { it.close() } }
        }
    }

    override fun toString(): String = asString {
        put(::output, output)
        put(::branches, branches.toList())
    }
}
