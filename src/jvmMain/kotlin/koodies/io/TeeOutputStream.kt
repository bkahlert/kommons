package koodies.io

import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


open class TeeOutputStream(out: OutputStream, private vararg val branches: OutputStream) : ProxyOutputStream(out) {

    private val lock = ReentrantLock()
    protected fun each(block: (OutputStream).() -> Unit) =
        branches.forEach { it.block() }

    override fun write(bytes: ByteArray) = lock.withLock {
        super.write(bytes)
        each { write(bytes) }
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) = lock.withLock {
        super.write(bytes, offset, length)
        each { write(bytes, offset, length) }
    }

    override fun write(byte: Int) = lock.withLock {
        super.write(byte)
        each { write(byte) }
    }

    override fun flush() = lock.withLock {
        super.flush()
        each { flush() }
    }

    override fun close() = lock.withLock {
        try {
            super.close()
        } finally {
            each { runCatching { close() } }
        }
    }
}
