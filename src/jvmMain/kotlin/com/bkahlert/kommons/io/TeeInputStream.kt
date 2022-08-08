package com.bkahlert.kommons.io

import com.bkahlert.kommons.debug.asEmoji
import com.bkahlert.kommons.debug.renderType
import com.bkahlert.kommons.text.asString
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

public open class TeeInputStream @JvmOverloads constructor(
    private val input: InputStream?,
    private vararg val branches: OutputStream,
    private val closeBranches: Boolean = false,
) : ProxyInputStream(input) {

    private val lock = ReentrantLock()

    protected fun each(read: Int, block: (OutputStream).(Int) -> Unit): Int = lock.withLock {
        read.apply { if (read != EOF) branches.forEach { it.block(read) } }
    }

    override fun read(bytes: ByteArray): Int =
        each(super.read(bytes)) { write(bytes, 0, it) }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int =
        each(super.read(bytes, offset, length)) { write(bytes, offset, it) }

    override fun read(): Int =
        each(super.read()) { write(it) }

    override fun close(): Unit = lock.withLock {
        try {
            super.close()
        } finally {
            if (closeBranches) {
                branches.forEach { runCatching { it.close() } }
            }
        }
    }

    override fun toString(): String = asString {
        put(::input, input?.renderType())
        put(::branches, branches.toList())
        put(::closeBranches, closeBranches.asEmoji())
    }
}
