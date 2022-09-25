package com.bkahlert.kommons_deprecated.io

import java.io.FilterOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Output stream that delegates all calls to the specified [proxy].
 */
@Suppress("MemberVisibilityCanBePrivate")
public open class ProxyOutputStream(private val proxy: OutputStream) : FilterOutputStream(proxy) {
    /**
     * Writes the specified [byte] to the proxied [out] and
     * passes an eventually thrown [IOException] to [handleIOException].
     */
    override fun write(byte: Int): Unit =
        out.runExceptionHandling {
            try {
                beforeWrite(1)
                write(byte)
            } finally {
                afterWrite(1)
            }
        }

    /**
     * Writes [bytes] to the proxied [out] and
     * passes an eventually thrown [IOException] to [handleIOException].
     */
    override fun write(bytes: ByteArray): Unit =
        out.runExceptionHandling {
            try {
                beforeWrite(bytes.size)
                write(bytes)
            } finally {
                afterWrite(bytes.size)
            }
        }

    /**
     * Writes [length] [bytes] starting at [offset] to the proxied [out] and
     * passes an eventually thrown [IOException] to [handleIOException].
     */
    override fun write(bytes: ByteArray, offset: Int, length: Int): Unit =
        out.runExceptionHandling {
            try {
                beforeWrite(length)
                write(bytes, offset, length)
            } finally {
                afterWrite(length)
            }
        }

    /**
     * Flushes the proxied [out] and passes an eventually thrown [IOException]
     * to [handleIOException].
     */
    override fun flush(): Unit = out.runExceptionHandling { flush() }

    /**
     * Closes the proxied [out] and passes an eventually thrown [IOException]
     * to [handleIOException].
     */
    override fun close(): Unit = out.runExceptionHandling { close() }

    /**
     * Callback before [length] bytes are written.
     */
    protected fun beforeWrite(@Suppress("UNUSED_PARAMETER") length: Int): Unit = Unit

    /**
     * Callback after [length] were written.
     */
    protected fun afterWrite(@Suppress("UNUSED_PARAMETER") length: Int): Unit = Unit

    /**
     * Callback in the event of an [IOException].
     */
    protected fun handleIOException(exception: IOException): Unit = throw exception

    /**
     * Runs the specified [operation] on the proxied [out] and passes an eventually thrown [IOException]
     * to [handleIOException].
     */
    protected fun OutputStream.runExceptionHandling(operation: OutputStream.() -> Unit) {
        runCatching { operation() }.recover { exception ->
            exception.let { it as? IOException }?.let { handleIOException(it) } ?: throw exception
        }
    }
}
