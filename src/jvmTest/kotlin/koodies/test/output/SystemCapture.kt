package koodies.test.output

import koodies.concurrent.process.IO
import koodies.concurrent.synchronized

/**
 * A capture session that captures [System.out] and [System.err].
 */
open class SystemCapture {
    private val monitor = Any()
    private val out: PrintStreamCapture
    private val err: PrintStreamCapture

    private val capturedStrings: MutableList<CapturedString> = arrayListOf<CapturedString>().synchronized()

    init {
        out = PrintStreamCapture(System.out) { string: String -> captureOut(string) }
        err = PrintStreamCapture(System.err) { string: String -> captureErr(string) }
        System.setOut(out)
        System.setErr(err)
    }

    fun release() {
        System.setOut(out.parent)
        System.setErr(err.parent)
    }

    private fun captureOut(string: String) {
        synchronized(monitor) { capturedStrings.add(CapturedString(IO.Type.OUT, string)) }
    }

    private fun captureErr(string: String) {
        synchronized(monitor) { capturedStrings.add(CapturedString(IO.Type.ERR, string)) }
    }

    fun append(builder: StringBuilder, filter: (IO.Type) -> Boolean) {
        synchronized(monitor) {
            capturedStrings
                .asSequence()
                .filter { filter(it.type) }
                .forEach { builder.append(it) }
        }
    }

    fun reset() {
        synchronized(monitor) { capturedStrings.clear() }
    }

    private class CapturedString(val type: IO.Type, private val string: String) {
        override fun toString(): String = string
    }
}
