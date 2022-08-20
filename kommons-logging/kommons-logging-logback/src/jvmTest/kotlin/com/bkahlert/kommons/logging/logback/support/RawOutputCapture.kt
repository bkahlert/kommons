package com.bkahlert.kommons.logging.logback.support

import ch.qos.logback.classic.util.StatusViaSLF4JLoggerFactory
import org.assertj.core.api.SoftAssertionsProvider.ThrowingRunnable
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

/**
 * Allows to capture a pair of output and error stream like [System.out] and [System.err]
 * without applying any modifications - namely ANSI escaping removal - as does Spring Boot's `OutputCapture`.
 *
 * @author Björn Kahlert
 */
public class RawOutputCapture : SmartCapturedOutput {
    val allStream = ByteArrayOutputStream()
    val outStream = ByteArrayOutputStream()
    val errStream = ByteArrayOutputStream()

    var oldOut: PrintStream? = null

    var oldErr: PrintStream? = null

    fun startCapturing(captureableOut: PrintStream, captureableErr: PrintStream) {
        oldOut = captureableOut
        oldErr = captureableErr
        System.setOut(TeePrintStream(captureableOut, outStream, allStream))
        System.setErr(TeePrintStream(captureableErr, errStream, allStream))
    }

    fun stopCapturing() {
        System.setOut(oldOut)
        System.setErr(oldErr)
    }

    fun runCapturing(runnable: ThrowingRunnable) {
        startCapturing(System.out, System.err)
        try {
            runnable.run()
        } finally {
            stopCapturing()
        }
    }

    override fun getAll(): String = allStream.toString()
    override fun getOut(): String = outStream.toString()
    override fun getErr(): String = errStream.toString()

    override fun toString(): String {
        return allStream.toString()
    }

    /**
     * Print stream that forwards all calls to a [TeeOutputStream] of the given output streams
     */
    private class TeePrintStream(vararg streams: OutputStream) :
        PrintStream(TeeOutputStream(*streams), false, StandardCharsets.UTF_8.name())

    /**
     * Output stream that forwards all output calls to the given output streams
     */
    private class TeeOutputStream(vararg streams: OutputStream) : OutputStream() {
        val streams: List<OutputStream> = streams.toList()

        private fun applyToAll(consumer: (OutputStream) -> Unit) {
            for (stream in streams) {
                try {
                    consumer(stream)
                } catch (throwable: Throwable) {
                    StatusViaSLF4JLoggerFactory.addError("An error occurred while writing to output stream $stream", TeeOutputStream::class.java.name)
                }
            }
        }

        override fun write(byte_: Int) {
            applyToAll { it.write(byte_) }
        }

        override fun write(buffer: ByteArray) {
            applyToAll { it.write(buffer) }
        }

        override fun write(buf: ByteArray, off: Int, len: Int) {
            applyToAll { it.write(buf, off, len) }
        }

        override fun flush() {
            applyToAll(OutputStream::flush)
        }

        override fun close() {
            applyToAll(OutputStream::close)
        }
    }

    companion object {
        /**
         * Runs the executable with [System.out] and [System.err] redirected
         * and provides access to the redirected output through [SmartCapturedOutput].
         *
         *
         * Important: In contrast to [SmartCaptureSupport] resp. [CapturedOutput]
         * ANSI escapes are not filtered, which allows normally impossible assertions on formatting.
         *
         * @param runnable the executable of which the produced output get captured
         *
         * @return the captured output
         */
        fun capture(runnable: ThrowingRunnable): SmartCapturedOutput {
            val capture = RawOutputCapture()
            capture.runCapturing(runnable)
            return capture
        }
    }
}
