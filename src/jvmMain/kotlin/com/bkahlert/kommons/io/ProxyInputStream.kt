package com.bkahlert.kommons.io

import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

@Suppress("MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
public abstract class ProxyInputStream(proxy: InputStream?) : FilterInputStream(proxy) {

    private val lock: ReentrantLock = ReentrantLock()
    private val inputStream = `in`

    override fun read(): Int = try {
        beforeRead(1)
        val byte = inputStream.read()
        afterRead(if (byte != EOF) 1 else EOF)
        byte
    } catch (e: IOException) {
        handleIOException(e)
        EOF
    }

    override fun read(bytes: ByteArray): Int = try {
        beforeRead(bytes.size)
        val length = inputStream.read(bytes)
        afterRead(length)
        length
    } catch (e: IOException) {
        handleIOException(e)
        EOF
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int = try {
        beforeRead(length)
        val count = inputStream.read(bytes, offset, length)
        afterRead(count)
        count
    } catch (e: IOException) {
        handleIOException(e)
        EOF
    }

    override fun skip(count: Long): Long = try {
        inputStream.skip(count)
    } catch (e: IOException) {
        handleIOException(e)
        0
    }

    override fun available(): Int = try {
        super.available()
    } catch (e: IOException) {
        handleIOException(e)
        0
    }

    override fun close(): Unit = try {
        inputStream.close()
    } catch (e: IOException) {
        handleIOException(e)
    }

    override fun mark(readlimit: Int): Unit = lock.withLock { inputStream.mark(readlimit) }

    override fun reset() {
        lock.withLock {
            try {
                inputStream.reset()
            } catch (e: IOException) {
                handleIOException(e)
            }
        }
    }

    override fun markSupported(): Boolean {
        return inputStream.markSupported()
    }

    protected fun beforeRead(count: Int) {
        // no-op
    }

    protected fun afterRead(count: Int) {
        // no-op
    }

    protected fun handleIOException(e: IOException) {
        throw e
    }
}
