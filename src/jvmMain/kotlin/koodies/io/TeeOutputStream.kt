package koodies.io

import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


open class TeeOutputStream(out: OutputStream, private var branch: OutputStream) : ProxyOutputStream(out) {

    private val lock = ReentrantLock()

    override fun write(bytes: ByteArray) = lock.withLock {
        super.write(bytes)
        branch.write(bytes)
    }

    override fun write(bytes: ByteArray, offset: Int, length: Int) = lock.withLock {
        super.write(bytes, offset, length)
        branch.write(bytes, offset, length)
    }

    override fun write(byte: Int) = lock.withLock {
        super.write(byte)
        branch.write(byte)
    }

    override fun flush() = lock.withLock {
        super.flush()
        branch.flush()
    }

    override fun close() = lock.withLock {
        try {
            super.close()
        } finally {
            branch.close()
        }
    }
}
