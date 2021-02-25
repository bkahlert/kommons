package koodies.io

import java.io.InputStream
import java.io.OutputStream

open class TeeInputStream @JvmOverloads constructor(
    input: InputStream?,
    private val branch: OutputStream,
    private val closeBranch: Boolean = false,
) : ProxyInputStream(input) {

    override fun close() {
        try {
            super.close()
        } finally {
            if (closeBranch) {
                branch.close()
            }
        }
    }

    override fun read(): Int {
        val byte = super.read()
        if (byte != EOF) {
            branch.write(byte)
        }
        return byte
    }

    override fun read(bytes: ByteArray, offset: Int, length: Int): Int {
        val n = super.read(bytes, offset, length)
        if (n != EOF) {
            branch.write(bytes, offset, n)
        }
        return n
    }

    override fun read(bytes: ByteArray): Int {
        val n = super.read(bytes)
        if (n != EOF) {
            branch.write(bytes, 0, n)
        }
        return n
    }
}
