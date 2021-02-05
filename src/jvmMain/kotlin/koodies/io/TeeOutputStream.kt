package koodies.io

import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 * Classic splitter of [OutputStream]. Named after the Unix 'tee' command. It allows a stream to be branched off so there
 * are now two streams.
 */
open class TeeOutputStream
/**
 * Constructs a TeeOutputStream.
 * @param out the main OutputStream
 * @param branch the second OutputStream
 */(
    out: OutputStream?,
    /** the second OutputStream to write to  */
    protected var branch //TODO consider making this private
    : OutputStream,
) : ProxyOutputStream(out) {

    private val lock = ReentrantLock()

    /**
     * Write the bytes to both streams.
     * @param b the bytes to write
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun write(b: ByteArray) {
        lock.withLock {
            super.write(b)
            branch.write(b)
        }
    }

    /**
     * Write the specified bytes to both streams.
     * @param b the bytes to write
     * @param off The start offset
     * @param len The number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun write(b: ByteArray, off: Int, len: Int) {
        lock.withLock {
            super.write(b, off, len)
            branch.write(b, off, len)
        }
    }

    /**
     * Write a byte to both streams.
     * @param b the byte to write
     * @throws IOException if an I/O error occurs
     */
    @Synchronized @Throws(IOException::class) override fun write(b: Int) {
        lock.withLock {
            super.write(b)
            branch.write(b)
        }
    }

    /**
     * Flushes both streams.
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun flush() {
        lock.withLock {
            super.flush()
            branch.flush()
        }
    }

    /**
     * Closes both output streams.
     *
     * If closing the main output stream throws an exception, attempt to close the branch output stream.
     *
     * If closing the main and branch output streams both throw exceptions, which exceptions is thrown by this method is
     * currently unspecified and subject to change.
     *
     * @throws IOException
     * if an I/O error occurs
     */
    @Throws(IOException::class) override fun close() {
        lock.withLock {
            try {
                super.close()
            } finally {
                branch.close()
            }
        }
    }
}
