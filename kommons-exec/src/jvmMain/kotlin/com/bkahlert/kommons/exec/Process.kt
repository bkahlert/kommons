package com.bkahlert.kommons.exec

import com.bkahlert.kommons.Now
import com.bkahlert.kommons.debug.asString
import com.bkahlert.kommons.exec.Process.State.Running
import com.bkahlert.kommons.exec.Process.State.Terminated.Failed
import com.bkahlert.kommons.exec.Process.State.Terminated.Succeeded
import com.bkahlert.kommons.text.startSpaced
import java.io.InputStream
import java.io.OutputStream
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
        pid?.also { put("pid", it) }
        put("state", state::class.simpleName?.lowercase())
        put("commandLine", processBuilder.commandLine)
    }

    /** State of the process. */
    public val state: State
        get() =
            try {
                if (exitValue() == 0) Succeeded(this)
                else Failed(this, processBuilder.commandLine)
            } catch (e: IllegalThreadStateException) {
                Running(this)
            }

    /** Representation of the state of a [Process]. */
    public sealed interface State {

        /** Process this state describes. */
        public val process: Process

        /** Moment the process started. */
        public val start: Instant get() = process.start

        /** Textual representation of this state. */
        public val status: String

        /** State of a process that already started but not terminated, yet. */
        public class Running(
            override val process: Process,
            override val status: String = "Process${process.pid?.toString().startSpaced} is running",
        ) : State {
            override fun toString(): String = status
        }

        /** State of a process that terminated with an [exitCode]. */
        public sealed interface Terminated : State {

            /** Moment the process terminated. */
            public val end: Instant

            /** [Duration] the process took to execute. */
            public val runtime: Duration get() = (end.toEpochMilli() - start.toEpochMilli()).milliseconds

            /** Exit code the process terminated with. */
            public val exitCode: Int get() = process.exitValue()

            /** State of a process that [Terminated] successfully. */
            public class Succeeded(
                override val process: Process,
            ) : Terminated {
                override val end: Instant = Now
                override val status: String = "Process${process.pid?.toString().startSpaced} terminated successfully within $runtime"
                override fun toString(): String = status
            }

            /** State of a process that [Terminated] erroneously. */
            public class Failed(
                override val process: Process,
                /** [CommandLine] that was used to start the [process]. */
                public val commandLine: CommandLine?,
            ) : Terminated {
                override val end: Instant = Now
                override val status: String = buildString {
                    append("Process${process.pid?.toString().startSpaced} terminated after $runtime with exit code $exitCode")
                    if (commandLine != null) {
                        append("; command line: ")
                        append(commandLine)
                    }
                }

                override fun toString(): String = status
            }
        }
    }

    public companion object
}
