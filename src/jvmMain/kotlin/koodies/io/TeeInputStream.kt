package koodies.io

import koodies.io.AbstractByteArrayOutputStream.Companion.EOF
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


/**
 * InputStream proxy that transparently writes a copy of all bytes read
 * from the proxied stream to a given OutputStream. Using [.skip]
 * or [.mark]/[.reset] on the stream will result on some
 * bytes from the input stream being skipped or duplicated in the output
 * stream.
 *
 *
 * The proxied input stream is closed when the [.close] method is
 * called on this proxy. You may configure whether the input stream closes the
 * output stream.
 *
 *
 * @since 1.4
 * @see ObservableInputStream
 */
open class TeeInputStream @JvmOverloads constructor(
    input: InputStream?,
    /**
     * The output stream that will receive a copy of all bytes read from the
     * proxied input stream.
     */
    private val branch: OutputStream,
    /**
     * Flag for closing the associated output stream when this stream is closed.
     */
    private val closeBranch: Boolean = false,
) : ProxyInputStream(input) {
    /**
     * Closes the proxied input stream and, if so configured, the associated
     * output stream. An exception thrown from one stream will not prevent
     * closing of the other stream.
     *
     * @throws IOException if either of the streams could not be closed
     */
    @Throws(IOException::class) override fun close() {
        try {
            super.close()
        } finally {
            if (closeBranch) {
                branch.close()
            }
        }
    }

    /**
     * Reads a single byte from the proxied input stream and writes it to
     * the associated output stream.
     *
     * @return next byte from the stream, or -1 if the stream has ended
     * @throws IOException if the stream could not be read (or written)
     */
    @Throws(IOException::class) override fun read(): Int {
        val ch = super.read()
        if (ch != EOF) {
            branch.write(ch)
        }
        return ch
    }

    /**
     * Reads bytes from the proxied input stream and writes the read bytes
     * to the associated output stream.
     *
     * @param bts byte buffer
     * @param st start offset within the buffer
     * @param end maximum number of bytes to read
     * @return number of bytes read, or -1 if the stream has ended
     * @throws IOException if the stream could not be read (or written)
     */
    @Throws(IOException::class) override fun read(bts: ByteArray, st: Int, end: Int): Int {
        val n = super.read(bts!!, st, end)
        if (n != EOF) {
            branch.write(bts, st, n)
        }
        return n
    }

    /**
     * Reads bytes from the proxied input stream and writes the read bytes
     * to the associated output stream.
     *
     * @param bts byte buffer
     * @return number of bytes read, or -1 if the stream has ended
     * @throws IOException if the stream could not be read (or written)
     */
    @Throws(IOException::class) override fun read(bts: ByteArray): Int {
        val n = super.read(bts!!)
        if (n != EOF) {
            branch.write(bts, 0, n)
        }
        return n
    }
}
