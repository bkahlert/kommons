package com.bkahlert.kommons.exec

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons.debug.trace
import com.bkahlert.kommons.exec.IO.Output
import com.bkahlert.kommons.exec.io.RedirectingOutputStream
import com.bkahlert.kommons.exec.io.TeeInputStream
import com.bkahlert.kommons.text.startSpaced
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.Collections
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

    private val _io: MutableList<IO> = Collections.synchronizedList(mutableListOf())
    public val io: List<IO> = _io

    @Suppress("KDocMissingDocumentation")
    override fun getOutputStream(): OutputStream = process.outputStream

    private val teeInputStream: InputStream = TeeInputStream(process.inputStream, RedirectingOutputStream {
        it.trace("recording")
        _io.add(Output(it))
    })
    private val teeErrorStream: InputStream = TeeInputStream(process.errorStream, RedirectingOutputStream { _io.add(IO.Error(it)) })

    @Suppress("KDocMissingDocumentation")
    override fun getInputStream(): InputStream = teeInputStream

    @Suppress("KDocMissingDocumentation")
    override fun getErrorStream(): InputStream = teeErrorStream

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
        pid?.also { put("pid", it) }
        put("state", state::class.simpleName?.lowercase())
        put("commandLine", processBuilder.commandLine)
    }

    /** State of the process. */
    public val state: State get() = exitState ?: Running()

    /** Exit state of the process or `null` if the process is still running. */
    public var exitState: ExitState? = null
        get() {
            if (field == null) {
                field = try {
                    val exitValue = exitValue()
                    // TODO read lazily
                    // TODO succeeded.toString -> output
                    // TODO failed.toString -> error
                    kotlin.runCatching { _io.add(IO.Output(process.inputStream.readBytes())) }
                    kotlin.runCatching { _io.add(IO.Error(process.errorStream.readBytes())) }
                    if (exitValue == 0) Succeeded()
                    else Failed()
                } catch (e: IllegalThreadStateException) {
                    null
                }
            }
            return field
        }
        private set

    /** Representation of the state of a [Process]. */
    public sealed interface State {

        /** Process this state describes. */
        public val process: Process

        /** Moment the process started. */
        public val start: Instant get() = process.start

        /** Textual representation of this state. */
        public val status: String
    }

    /** State of a process that already started but not terminated, yet. */
    public inner class Running(
        override val status: String = "Process${pid?.toString().startSpaced} is running",
    ) : State {
        override val process: Process get() = this@Process
        override fun toString(): String = status
    }

    /** State of a process that terminated with an [exitCode]. */
    public sealed interface ExitState : State {

        /** Moment the process terminated. */
        public val end: Instant

        /** [Duration] the process took to execute. */
        public val runtime: Duration get() = (end.toEpochMilli() - start.toEpochMilli()).milliseconds

        /** Exit code the process terminated with. */
        public val exitCode: Int get() = process.exitValue()
    }

    /** State of a process that terminated successfully. */
    public inner class Succeeded : ExitState {
        override val process: Process get() = this@Process
        override val end: Instant = Now
        override val status: String = "Process${process.pid?.toString().startSpaced} terminated successfully within $runtime"
        override fun toString(): String = status
    }

    /** State of a process that terminated erroneously. */
    public inner class Failed : ExitState {
        override val process: Process get() = this@Process
        override val end: Instant = Now
        override val status: String = "Process${process.pid?.toString().startSpaced} terminated after $runtime with exit code $exitCode"
        override fun toString(): String = status
    }

    public companion object
}
