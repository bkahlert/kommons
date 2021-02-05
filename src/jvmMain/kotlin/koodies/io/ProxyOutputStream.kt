package koodies.io

import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream


/**
 * A Proxy stream which acts as expected, that is it passes the method
 * calls on to the proxied stream and doesn't change which methods are
 * being called. It is an alternative base class to FilterOutputStream
 * to increase reusability.
 *
 *
 * See the protected methods for ways in which a subclass can easily decorate
 * a stream with custom pre-, post- or error processing functionality.
 *
 */
open class ProxyOutputStream
/**
 * Constructs a new ProxyOutputStream.
 *
 * @param proxy  the OutputStream to delegate to
 */
    (proxy: OutputStream?) : FilterOutputStream(proxy) {
    /**
     * Invokes the delegate's `write(int)` method.
     * @param idx the byte to write
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun write(idx: Int) {
        try {
            beforeWrite(1)
            out.write(idx)
            afterWrite(1)
        } catch (e: IOException) {
            handleIOException(e)
        }
    }

    /**
     * Invokes the delegate's `write(byte[])` method.
     * @param bts the bytes to write
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun write(bts: ByteArray) {
        try {
            val len = IOUtils.length(bts)
            beforeWrite(len)
            out.write(bts)
            afterWrite(len)
        } catch (e: IOException) {
            handleIOException(e)
        }
    }

    /**
     * Invokes the delegate's `write(byte[])` method.
     * @param bts the bytes to write
     * @param st The start offset
     * @param end The number of bytes to write
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun write(bts: ByteArray, st: Int, end: Int) {
        try {
            beforeWrite(end)
            out.write(bts, st, end)
            afterWrite(end)
        } catch (e: IOException) {
            handleIOException(e)
        }
    }

    /**
     * Invokes the delegate's `flush()` method.
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun flush() {
        try {
            out.flush()
        } catch (e: IOException) {
            handleIOException(e)
        }
    }

    /**
     * Invokes the delegate's `close()` method.
     * @throws IOException if an I/O error occurs
     */
    @Throws(IOException::class) override fun close() {
        IOUtils.close(out, { e: IOException? -> handleIOException(e) })
    }

    /**
     * Invoked by the write methods before the call is proxied. The number
     * of bytes to be written (1 for the [.write] method, buffer
     * length for [.write], etc.) is given as an argument.
     *
     *
     * Subclasses can override this method to add common pre-processing
     * functionality without having to override all the write methods.
     * The default implementation does nothing.
     *
     * @since 2.0
     * @param n number of bytes to be written
     * @throws IOException if the pre-processing fails
     */
    @Throws(IOException::class) protected fun beforeWrite(n: Int) {
        // noop
    }

    /**
     * Invoked by the write methods after the proxied call has returned
     * successfully. The number of bytes written (1 for the
     * [.write] method, buffer length for [.write],
     * etc.) is given as an argument.
     *
     *
     * Subclasses can override this method to add common post-processing
     * functionality without having to override all the write methods.
     * The default implementation does nothing.
     *
     * @since 2.0
     * @param n number of bytes written
     * @throws IOException if the post-processing fails
     */
    @Throws(IOException::class) protected fun afterWrite(n: Int) {
        // noop
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
