package com.bkahlert.kommons.exec

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons.text.LineSeparators.removeTrailingLineSeparator
import com.bkahlert.kommons.text.startSpaced
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.charset.Charset
import java.time.Instant
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.jvm.isAccessible
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** [java.lang.Process] wrapper with extended features such as access to its [state]. */
public class Process(
    private val processBuilder: ProcessBuilder,
) : java.lang.Process() {
    /** Moment the process started. */
    public val start: Instant = Now
    private val process: java.lang.Process = processBuilder.start()

    @Suppress("KDocMissingDocumentation")
    override fun getOutputStream(): OutputStream = process.outputStream

    @Suppress("KDocMissingDocumentation")
    override fun getInputStream(): InputStream = process.inputStream

    @Suppress("KDocMissingDocumentation")
    override fun getErrorStream(): InputStream = process.errorStream

    @Suppress("KDocMissingDocumentation")
    override fun waitFor(): Int = process.waitFor()

    @Suppress("KDocMissingDocumentation")
    override fun exitValue(): Int = process.exitValue()

    @Suppress("KDocMissingDocumentation")
    override fun destroy(): Unit = process.destroy()

    /** Native process ID of the process. */
    public val pid: Long? by lazy {
        kotlin.runCatching {
            process::class.members.firstOrNull { it.name == "pid" }
                ?.apply { isAccessible = true }
                ?.run { call(process).toString().toLong() }
        }.recover {
            process::class.memberFunctions.firstOrNull { it.name == "pid" }
                ?.apply { isAccessible = true }
                ?.run { call(process).toString().toLong() }
        }.getOrNull()
    }

    override fun toString(): String = asString {
        pid?.also { put(::pid.name, it) }
        put("state", exitState?.let { it::class.simpleName?.lowercase() } ?: "running")
        put("commandLine", processBuilder.commandLine)
    }

    /** Exit state of the process or `null` if the process is still running. */
    public var exitState: ExitState? = null
        get() {
            if (field == null) {
                field = try {
                    if (exitValue() == 0) Succeeded()
                    else Failed()
                } catch (e: IllegalThreadStateException) {
                    null
                }
            }
            return field
        }
        private set

    /** State of a process that terminated with an [exitCode]. */
    public sealed interface ExitState {

        /** Process this state describes. */
        public val process: Process

        /** Moment the process started. */
        public val start: Instant get() = process.start

        /** Textual representation of this state. */
        public val status: String

        /** Moment the process terminated. */
        public val end: Instant

        /** [Duration] the process took to execute. */
        public val runtime: Duration get() = (end.toEpochMilli() - start.toEpochMilli()).milliseconds

        /** Exit code the process terminated with. */
        public val exitCode: Int get() = process.exitValue()

        /**
         * Returns the read standard output if the process [Succeeded] or
         * throws an exception with the read standard error if the process [Failed].
         *
         * ***Note:** This convenience method is only safe to call
         * if the corresponding input streams aren't already closed.
         * That is, **it can't be used if the output was already read or logged.***
         *
         * @param charset used to read the standard error if the process [Failed]
         */
        public fun readBytesOrThrow(charset: Charset = Charsets.UTF_8): ByteArray

        /**
         * Returns the read standard output if the process [Succeeded] or
         * throws an exception with the read standard error if the process [Failed].
         *
         * ***Note:** This convenience method is only safe to call
         * if the corresponding input streams aren't already closed.
         * That is, **it can't be used if the output was already read or logged.***
         *
         * @param charset used to read the standard output respectively the standard error
         */
        public fun readTextOrThrow(charset: Charset = Charsets.UTF_8): String =
            String(readBytesOrThrow(charset), charset)

        /**
         * Returns the read standard output if the process [Succeeded] or
         * throws an exception with the read standard error if the process [Failed].
         *
         * ***Note:** This convenience method is only safe to call
         * if the corresponding input streams aren't already closed.
         * That is, **it can't be used if the output was already read or logged.***
         *
         * @param charset used to read the standard output respectively the standard error
         */
        public fun readLinesOrThrow(charset: Charset = Charsets.UTF_8): List<String> =
            readTextOrThrow(charset).removeTrailingLineSeparator().lines()
    }

    /** State of a process that terminated successfully. */
    public inner class Succeeded : ExitState {
        override val process: Process get() = this@Process
        override val end: Instant = Now
        override val status: String = "Process${pid?.toString().startSpaced} terminated successfully within $runtime"
        override fun toString(): String = status

        private val bytes by lazy { inputStream.readBytes().also { inputStream.close() } }
        override fun readBytesOrThrow(charset: Charset): ByteArray = bytes
    }

    /** State of a process that terminated erroneously. */
    public inner class Failed : ExitState {
        override val process: Process get() = this@Process
        override val end: Instant = Now
        override val status: String = "Process${pid?.toString().startSpaced} terminated after $runtime with exit code $exitCode"
        override fun toString(): String = status

        private val bytes by lazy { errorStream.readBytes().also { errorStream.close() } }
        override fun readBytesOrThrow(charset: Charset): ByteArray {
            if (bytes.isEmpty()) throw IOException(status)
            else throw IOException(status + ":\n" + String(bytes, charset).removeTrailingLineSeparator())
        }
    }

    public companion object
}
