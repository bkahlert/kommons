package koodies.exec

import koodies.Exceptions.ISE
import koodies.exception.toCompactString
import koodies.exec.Process.ExitState
import koodies.exec.Process.ProcessState.Running
import koodies.exec.Process.ProcessState.Terminated
import koodies.logging.ReturnValue
import koodies.text.LineSeparators
import koodies.text.Semantics.formattedAs
import koodies.text.takeUnlessBlank
import koodies.text.withSuffix
import koodies.time.Now
import java.io.BufferedWriter
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
     * Representation of the state of a [Process].
     *
     * The types are distinguished (which may have more differentiated specializations):
     * - [Running] for already started and not yet terminated processes
     * - [Terminated] for started and no more running processes
     */
    public sealed class ProcessState(
        /**
         * Textual representation of this state.
         */
        public val status: String,
        /**
         * Whether this state represents a successful or failed state.
         *
         * Can be `null` if currently unknown (i.e. while running).
         */
        public override val successful: Boolean?,
    ) : ReturnValue {
        override fun toString(): String = status
        override val textRepresentation: String?
            get() = when (successful) {
                true -> null
                null -> "async computation"
                false -> status
            }

        /**
         * State of a process that already started but not terminated, yet.
         */
        public class Running(
            /**
             * PID of the running process.
             */
            public val pid: Long,
            status: String = "Process $pid is running.",
        ) : ProcessState(status, null)

        /**
         * State of a process that started and is no longer running.
         */
        public open class Terminated(
            /**
             * PID of the terminated process at the time is was still running.
             */
            public val pid: Long,
            /**
             * Code the terminated process exited with.
             */
            public val exitCode: Int,
            /**
             * All [IO] that was logged while the process was running.
             */
            public val io: IOSequence<IO>,
            status: String = "Process $pid terminated with exit code $exitCode.",
        ) : ProcessState(status, exitCode == 0)
    }

    /**
     * The current state of this process.
     */
    public val state: ProcessState

    /**
     * The state this process exited with. Only set, if [state] is [ProcessState.Terminated].
     */
    public val exitState: ExitState?

    /**
     * Representation of the exit state of a [ProcessState.Terminated] [Process].
     *
     * The types are distinguished (which may have more differentiated specializations):
     * - [Running] for already started and not yet terminated processes
     * - [Terminated] for started and no more running processes
     */
    public sealed class ExitState(exitCode: Int, pid: Long, io: IOSequence<IO>, status: String) :
        Terminated(pid, exitCode, io, status), ReturnValue {

        /**
         * Implementors are used to delegate the creation of the [ExitState] to.
         */
        public fun interface ExitStateHandler {

            /**
             * Returns the [ExitState] of a [Process] based on the
             * given [terminated] [ProcessState].
             */
            public fun handle(terminated: Terminated): ExitState
        }

        /**
         * State of a process that [Terminated] successfully.
         */
        public open class Success(
            pid: Long,
            io: IOSequence<IO>,
            status: String = "Process ${pid.formattedAs.input} terminated successfully at $Now.",
        ) : ExitState(0, pid, io, status)

        /**
         * State of a process that [Terminated] erroneously.
         */
        public open class Failure(
            exitCode: Int,
            pid: Long,
            private val relevantFiles: List<URI> = emptyList(),

            /**
             * Detailed information about the circumstances of a process's failed termination.
             */
            public val dump: String? = null,
            io: IOSequence<IO> = IOSequence.EMPTY,
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

        /**
         * State of a process that [Terminated] with a technical [exception].
         */
        public open class Fatal(
            /**
             * Unexpected exception that lead to a process's termination.
             */
            public val exception: Throwable,
            exitCode: Int,
            pid: Long,
            /**
             * Detailed information about the circumstances of a process's unexpected termination.
             */
            public val dump: String,
            io: IOSequence<IO>,
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
 * Returns whether `this` [Process] [isRunning].
 */
public val Process.alive: Boolean get() = state is Running

/**
 * Returns whether `this` [Process] is [Running].
 */
public val Process.isRunning: Boolean get() = state is Running

/**
 * Returns `this` process's [ExitState.exitCode] if it terminated
 * or `null` otherwise.
 */
public val Process.exitCodeOrNull: Int? get() = exitState?.exitCode

/**
 * Returns `this` process's [ExitState.exitCode].
 *
 * Throws an [IllegalStateException] if the process has not terminated.
 */
public val Process.exitCode: Int get() = exitState?.exitCode ?: throw ISE("Process $pid has not terminated.")

/**
 * Writes the given [input] strings with a slight delay between
 * each input on the [Process]'s [InputStream].
 */
public fun Process.enter(vararg input: String, delay: Duration = milliseconds(10)): Unit =
    inputStream.enter(*input, delay = delay)

/**
 * Writes the given [input] strings with a slight delay between
 * each input on the [Process]'s [InputStream].
 */
public fun Process.input(vararg input: String, delay: Duration = milliseconds(10)): Unit =
    inputStream.enter(*input, delay = delay)

/**
 * Writes the given [input] strings with a slight delay between
 * each input on the [Process]'s [InputStream].
 */
public fun OutputStream.enter(vararg input: String, delay: Duration = milliseconds(10)) {
    val stdin = BufferedWriter(OutputStreamWriter(this))
    input.forEach {
        TimeUnit.MILLISECONDS.sleep(delay.inWholeMilliseconds)
        stdin.write(it.withSuffix(LineSeparators.CRLF))
        stdin.flush()
    }
}

// PROCESS BUILDER EXTENSIONS

/**
 * Whether built [Process] instances redirect their standard error to standard output.
 */
public var ProcessBuilder.redirectErrorStream: Boolean
    get() = redirectErrorStream()
    set(value) {
        redirectErrorStream(value)
    }

/**
 * This process builder's environment;
 * used by built [Process] instances as their environment.
 */
public val ProcessBuilder.environment: MutableMap<String, String>
    get() = environment()

/**
 * This process builder's working directory;
 * used by built [Process] instances as their working directory.
 *
 * If not working directory is set, the working directory of the current
 * Java process is used (usually the directory named by the system property `user.dir`).
 */
public var ProcessBuilder.workingDirectory: Path?
    get() = directory()?.toPath()
    set(value) {
        value?.toAbsolutePath()?.run {
            require(exists()) { "Working directory $this does not exist." }
            require(isDirectory()) { "Working directory $this is no directory." }
            directory(toFile())
        }
    }
