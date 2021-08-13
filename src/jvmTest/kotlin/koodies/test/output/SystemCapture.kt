package koodies.test.output

import koodies.exec.IO
import java.io.OutputStream
import java.io.PrintStream
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * A capture session that captures [System.out] and [System.err].
 */
open class SystemCapture(print: Boolean) {
    private val lock = ReentrantLock()
    private val out: PrintStreamCapture
    private val err: PrintStreamCapture

    private val capturedStrings: MutableList<IO> = arrayListOf()

    init {
        out = PrintStreamCapture(if (print) System.out else PrintStream(OutputStream.nullOutputStream())) { string: String -> captureOut(string) }
        err = PrintStreamCapture(if (print) System.err else PrintStream(OutputStream.nullOutputStream())) { string: String -> captureErr(string) }
        System.setOut(out)
        System.setErr(err)
    }

    fun release() {
        System.setOut(out.parent)
        System.setErr(err.parent)
    }

    private fun captureOut(string: String) {
        lock.withLock { capturedStrings.add(IO.Output typed string) }
    }

    private fun captureErr(string: String) {
        lock.withLock { capturedStrings.add(IO.Error typed string) }
    }

    fun useCapturedStrings(transform: (IO) -> Unit) {
        lock.withLock {
            capturedStrings.forEach { transform(it) }
        }
    }

    fun reset() {
        lock.withLock { capturedStrings.clear() }
    }
}
