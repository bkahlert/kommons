package koodies.exec

import koodies.Exceptions.ISE
import koodies.concurrent.process.IO
import koodies.exception.toCompactString
import koodies.exec.Process.ExitState
import koodies.exec.Process.ProcessState.Prepared
import koodies.exec.Process.ProcessState.Running
import koodies.exec.Process.ProcessState.Terminated
import koodies.logging.ReturnValue
import koodies.text.LineSeparators
import koodies.text.Semantics.formattedAs
import koodies.text.takeUnlessBlank
import koodies.time.Now
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.CompletableFuture

/**
 * Platform independent representation of a running program.
 */
public interface Process : ReturnValue {

    /**
     * Stream of this program's meta logs.
     */
    public val metaStream: MetaStream

    /**
     * This program's input stream, that is,
     * the stream you can write to in order
     * to provide input for this program.
     *
     * *In contrast to the extremely misleading naming used in the Java world,
     * this property is named after its purpose—not its type.*
     */
    public val inputStream: OutputStream

    /**
     * This program's output stream, that is,
     * the stream you can read in order to find
     * out what [IO] this process produces.
     *
     * *In contrast to the extremely misleading naming used in the Java world,
     * this property is named after its purpose—not its type.*
     */
    public val outputStream: InputStream

    /**
     * This program's error stream, that is,
     * the stream you can read in order to find
     * out what goes wrong.
     */
    public val errorStream: InputStream

    /**
     * The identifier of this process.
     */
    public val pid: Long

    /**
     * Starts the program represented by this process.
     */
    public fun start(): Process

    public sealed class ProcessState(
        public val status: String,
        public override val successful: Boolean?,
    ) : ReturnValue {
        override fun toString(): String = status
        override val textRepresentation: String?
            get() = when (successful) {
                true -> null
                null -> "async computation"
                false -> status
            }

        public class Prepared(
            status: String = "Process has not yet started.",
        ) : ProcessState(status, null)

        public class Running(
            public val pid: Long,
            status: String = "Process $pid is running.",
        ) : ProcessState(status, null)

        public open class Terminated(
            public val pid: Long,
            public val exitCode: Int,
            public val io: List<IO>,
            status: String = "Process $pid terminated with exit code $exitCode.",
        ) : ProcessState(status, exitCode == 0)
    }

    public val state: ProcessState

    public val exitState: ExitState?

    public sealed class ExitState(exitCode: Int, pid: Long, io: List<IO>, status: String) :
        Terminated(pid, exitCode, io, status), ReturnValue {

        public fun interface ExitStateHandler {
            public fun handle(terminated: Terminated): ExitState
        }

        public open class Success(
            pid: Long,
            io: List<IO>,
            status: String = "Process ${pid.formattedAs.input} terminated successfully at $Now.",
        ) : ExitState(0, pid, io, status)

        public open class Failure(
            exitCode: Int,
            pid: Long,
            private val relevantFiles: List<URI> = emptyList(),
            public val dump: String? = null,
            io: List<IO> = emptyList(),
            status: String = "Process ${pid.formattedAs.input} terminated with exit code ${exitCode.formattedAs.error}.",
        ) : ExitState(exitCode, pid, io, status) {
            override val textRepresentation: String? get() = toString()
            override fun toString(): String =
                StringBuilder(status).apply {
                    relevantFiles.forEach {
                        append(LineSeparators.LF)
                        append(it)
                    }
                    dump?.takeUnlessBlank()?.let {
                        append(LineSeparators.LF)
                        append(dump)
                    }
                }.toString()
        }

        public open class Fatal(
            public val exception: Throwable,
            exitCode: Int,
            pid: Long,
            public val dump: String,
            io: List<IO>,
            status: String = "Process ${pid.formattedAs.input} fatally failed with ${exception.toCompactString()}",
        ) : ExitState(exitCode, pid, io, status)
    }

    /**
     * Whether the process terminated successfully or failed.
     *
     * `null` is the process has not terminated, yet.
     */
    override val successful: Boolean? get() = exitState?.successful
    override val symbol: String get() = exitState?.symbol ?: state.symbol
    override val textRepresentation: String? get() = exitState?.textRepresentation ?: state.textRepresentation

    /**
     * A completable future that returns an instances of this process once
     * the program represented by this process terminated.
     */
    public val onExit: CompletableFuture<out ExitState>

    /**
     * Blocking method that waits until the program represented by this process
     * terminates and returns its [exitValue].
     */
    public fun waitFor(): ExitState = onExit.join()

    /**
     * Returns the exit code of the program represented by process process once
     * it terminates. If the program has not terminated yet, it throws an
     * [IllegalStateException].
     */
    @Deprecated("use exitCode", ReplaceWith("exitCode", "koodies.exec.exitCode")) public val exitValue: Int get() = exitCode

    /**
     * Blocking method that waits until the program represented by this process
     * terminates and returns its [exitValue].
     */
    @Deprecated("use waitFor", ReplaceWith("waitFor()")) public fun waitForTermination(): Terminated = waitFor()

    /**
     * Gracefully attempts to stop the execution of the program represented by this process.
     */
    public fun stop(): Process

    /**
     * Forcefully stops the execution of the program represented by this process.
     */
    public fun kill(): Process
}

/**
 * Returns whether `this` [Process] was started.
 *
 * Contrary to [alive] this property stays `true` even after the process terminated.
 */
public val Process.started: Boolean get() = state !is Prepared

/**
 * Returns whether `this` [Process] [isRunning].
 *
 * Contrary to [started] this property stays turns `false` again after the process terminated.
 */
public val Process.alive: Boolean get() = state is Running

/**
 * Returns whether `this` [Process] is running.
 *
 * Contrary to [started] this property stays turns `false` again after the process terminated.
 */
public val Process.isRunning: Boolean get() = state is Running

/**
 * Returns `this` process's [ExitState.exitCode] if it terminated
 * or `null` otherwise.
 */
public val Process.exitCodeOrNull: Int? get() = also { it.start() }.exitState?.exitCode

/**
 * Returns `this` process's [ExitState.exitCode].
 *
 * Throws an [IllegalStateException] if the process has not terminated.
 */
public val Process.exitCode: Int get() = also { it.start() }.exitState?.exitCode ?: throw ISE("Process $pid has not terminated.")
