package koodies.io


import koodies.io.AbstractByteArrayOutputStream.Companion.EOF
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


/**
 * A Proxy stream which acts as expected, that is it passes the method
 * calls on to the proxied stream and doesn't change which methods are
 * being called.
 *
 *
 * It is an alternative base class to FilterInputStream
 * to increase reusability, because FilterInputStream changes the
 * methods being called, such as read(byte[]) to read(byte[], int, int).
 *
 *
 *
 * See the protected methods for ways in which a subclass can easily decorate
 * a stream with custom pre-, post- or error processing functionality.
 *
 */
abstract class ProxyInputStream
/**
 * Constructs a new ProxyInputStream.
 *
 * @param proxy  the InputStream to delegate to
 */
    (proxy: InputStream?) : FilterInputStream(proxy) {

    private val lock: ReentrantLock = ReentrantLock()

    /**
     * Invokes the delegate's `read()` method.
     * @return the byte read or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun read(): Int {
        return try {
            beforeRead(1)
            val b = `in`.read()
            afterRead(if (b != EOF) 1 else EOF)
            b
        } catch (e: IOException) {
            handleIOException(e)
            EOF
        }
    }

    /**
     * Invokes the delegate's `read(byte[])` method.
     * @param bts the buffer to read the bytes into
     * @return the number of bytes read or EOF if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun read(bts: ByteArray): Int {
        return try {
            beforeRead(IOUtils.length(bts))
            val n = `in`.read(bts)
            afterRead(n)
            n
        } catch (e: IOException) {
            handleIOException(e)
            EOF
        }
    }

    /**
     * Invokes the delegate's `read(byte[], int, int)` method.
     * @param bts the buffer to read the bytes into
     * @param off The start offset
     * @param len The number of bytes to read
     * @return the number of bytes read or -1 if the end of stream
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun read(bts: ByteArray, off: Int, len: Int): Int {
        return try {
            beforeRead(len)
            val n = `in`.read(bts, off, len)
            afterRead(n)
            n
        } catch (e: IOException) {
            handleIOException(e)
            EOF
        }
    }

    /**
     * Invokes the delegate's `skip(long)` method.
     * @param ln the number of bytes to skip
     * @return the actual number of bytes skipped
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun skip(ln: Long): Long {
        return try {
            `in`.skip(ln)
        } catch (e: IOException) {
            handleIOException(e)
            0
        }
    }

    /**
     * Invokes the delegate's `available()` method.
     * @return the number of available bytes
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun available(): Int {
        return try {
            super.available()
        } catch (e: IOException) {
            handleIOException(e)
            0
        }
    }

    /**
     * Invokes the delegate's `close()` method.
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun close() {
        IOUtils.close(`in`) { e: IOException? -> handleIOException(e) }
    }

    /**
     * Invokes the delegate's `mark(int)` method.
     * @param readlimit read ahead limit
     */
    override fun mark(readlimit: Int) {
        lock.withLock { `in`.mark(readlimit) }
    }

    /**
     * Invokes the delegate's `reset()` method.
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun reset() {
        lock.withLock {
            try {
                `in`.reset()
            } catch (e: IOException) {
                handleIOException(e)
            }
        }
    }

    /**
     * Invokes the delegate's `markSupported()` method.
     * @return true if mark is supported, otherwise false
     */
    override fun markSupported(): Boolean {
        return `in`.markSupported()
    }

    /**
     * Invoked by the read methods before the call is proxied. The number
     * of bytes that the caller wanted to read (1 for the [.read]
     * method, buffer length for [.read], etc.) is given as
     * an argument.
     *
     *
     * Subclasses can override this method to add common pre-processing
     * functionality without having to override all the read methods.
     * The default implementation does nothing.
     *
     *
     * Note this method is *not* called from [.skip] or
     * [.reset]. You need to explicitly override those methods if
     * you want to add pre-processing steps also to them.
     *
     * @since 2.0
     * @param n number of bytes that the caller asked to be read
     * @throws IOException if the pre-processing fails
     */
    @Throws(IOException::class) protected fun beforeRead(n: Int) {
        // no-op
    }

    /**
     * Invoked by the read methods after the proxied call has returned
     * successfully. The number of bytes returned to the caller (or -1 if
     * the end of stream was reached) is given as an argument.
     *
     *
     * Subclasses can override this method to add common post-processing
     * functionality without having to override all the read methods.
     * The default implementation does nothing.
     *
     *
     * Note this method is *not* called from [.skip] or
     * [.reset]. You need to explicitly override those methods if
     * you want to add post-processing steps also to them.
     *
     * @since 2.0
     * @param n number of bytes read, or -1 if the end of stream was reached
     * @throws IOException if the post-processing fails
     */
    @Throws(IOException::class) protected fun afterRead(n: Int) {
        // no-op
    }

    /**
     * Handle any IOExceptions thrown.
     *
     *
     * This method provides a point to implement custom exception
     * handling. The default behavior is to re-throw the exception.
     * @param e The IOException thrown
     * @throws IOException if an I/O error occurs
     * @since 2.0
     */
    @Throws(IOException::class) protected fun handleIOException(e: IOException?) {
        throw e!!
    }
}
