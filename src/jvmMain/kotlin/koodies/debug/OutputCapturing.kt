package koodies.debug


import koodies.collections.withNegativeIndices
import koodies.io.ByteArrayOutputStream
import koodies.text.LineSeparators.lines
import java.io.OutputStream
import java.io.PrintStream

public interface CapturedOutput : CharSequence {
    public val all: String
    public val out: String
    public val err: String

    public val allLines: List<String> get() = all.splitLines().withNegativeIndices()
    public val outLines: List<String> get() = out.splitLines().withNegativeIndices()
    public val errLines: List<String> get() = err.splitLines().withNegativeIndices()
}

private fun String.splitLines(): List<String> = lines().dropLastWhile { it.isBlank() }

/**
 * Allows to capture a pair of output and error stream like [System.out] and [System.err]
 * without applying any modifications - namely ANSI escaping removal - as does Spring Boot's `OutputCapture`.
 */
public class AdHocOutputCapture : CapturedOutput {
    private val allStream = ByteArrayOutputStream()
    private val outStream = ByteArrayOutputStream()
    private val errStream = ByteArrayOutputStream()
    private var oldOut: PrintStream? = null
    private var oldErr: PrintStream? = null

    @Suppress("SameParameterValue")
    private fun startCapturing(redirect: Boolean = false, out: PrintStream = System.out, err: PrintStream = System.err) {
        oldOut = out
        oldErr = err
        System.setOut(TeePrintStream(if (redirect) OutputStream.nullOutputStream() else out, outStream, allStream))
        System.setErr(TeePrintStream(if (redirect) OutputStream.nullOutputStream() else err, errStream, allStream))
    }

    private fun stopCapturing() {
        System.setOut(oldOut)
        System.setErr(oldErr)
    }

    public fun <T> runCapturing(redirect: Boolean = false, runnable: () -> T): T {
        startCapturing(redirect, System.out, System.err)
        val result = runnable.runCatching { invoke() }
        stopCapturing()
        return result.getOrThrow()
    }

    override val all: String get() = allStream.toString(Charsets.UTF_8)
    override val out: String get() = outStream.toString(Charsets.UTF_8)
    override val err: String get() = errStream.toString(Charsets.UTF_8)

    /**
     * Print stream that forwards all calls to a [TeeOutputStream] of the given output streams
     */
    private class TeePrintStream(vararg streams: OutputStream) :
        PrintStream(TeeOutputStream(*streams), false, Charsets.UTF_8.name())

    /**
     * Output stream that forwards all output calls to the given output streams
     */
    private class TeeOutputStream(vararg streams: OutputStream) : OutputStream() {
        private var streams: List<OutputStream> = streams.toList()
        override fun write(byte_: Int) = streams.forEach { it.write(byte_) }
        override fun write(buffer: ByteArray) = streams.forEach { it.write(buffer) }
        override fun write(buf: ByteArray, off: Int, len: Int) = streams.forEach { it.write(buf, off, len) }
        override fun flush() = streams.forEach { it.flush() }
        override fun close() = streams.forEach { it.close() }
    }

    public companion object {
        /**
         * Runs the executable with [System.out] and [System.err] redirected
         * and provides access to the redirected output through [CapturedOutput].
         *
         * @param runnable the executable of which the produced output get captured
         *
         * @return the captured output
         */
        public fun <T> captureOutput(redirect: Boolean = false, runnable: () -> T): Pair<T, CapturedOutput> {
            val capture = AdHocOutputCapture()
            val returnValue = capture.runCapturing(redirect, runnable)
            return returnValue to capture
        }
    }


    override val length: Int get() = all.length
    override fun get(index: Int): Char = all[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = all.subSequence(startIndex, endIndex)
    override fun toString(): String = all
}
