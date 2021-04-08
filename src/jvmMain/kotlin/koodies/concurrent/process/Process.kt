package koodies.concurrent.process

import koodies.collections.synchronizedListOf
import koodies.concurrent.process.Process.ExitState
import koodies.concurrent.process.Process.ProcessState.Terminated
import koodies.debug.asEmoji
import koodies.exception.toCompactString
import koodies.logging.ReturnValue
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.formattedAs
import koodies.text.takeUnlessBlank
import koodies.time.Now
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.concurrent.CompletableFuture
import java.lang.Process as JavaProcess


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
                        append(LF)
                        append(it)
                    }
                    dump?.takeUnlessBlank()?.let {
                        append(LF)
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
     * Returns whether [start] was called.
     *
     * Contrary to [alive] this property will never return `false` once [start] was called.
     */
    @Deprecated("use state") public val started: Boolean

    /**
     * Returns whether the program represented by this process
     * is currently running.
     *
     * Contrary to [started] this property reflects the actual running state of
     * the program represented by this process.
     */
    @Deprecated("use state") public val alive: Boolean

    /**
     * Returns the exit code of the program represented by process process once
     * it terminates. If the program has not terminated yet, it throws an
     * [IllegalStateException].
     */
    @Deprecated("use exit state") public val exitValue: Int

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
     * Blocking method that waits until the program represented by this process
     * terminates and returns its [exitValue].
     */
    @Deprecated("use waitFor") public fun waitForTermination(): Terminated = onExit.join()

    /**
     * Gracefully attempts to stop the execution of the program represented by this process.
     */
    public fun stop(): Process

    /**
     * Forcefully stops the execution of the program represented by this process.
     */
    public fun kill(): Process
}

public class MetaStream(vararg listeners: (IO.META) -> Unit) {
    private val history: MutableList<IO.META> = synchronizedListOf()
    private val listeners: MutableList<(IO.META) -> Unit> = synchronizedListOf(*listeners)

    public fun subscribe(listener: (IO.META) -> Unit): Unit {
        history.forEach { listener(it) }
        listeners.add(listener)
    }

    public fun emit(meta: IO.META): Unit {
        history.add(meta)
        listeners.forEach { it(meta) }
    }
}
// TODO
///**
// * Returns whether [start] was called.
// *
// * Contrary to [alive] this property will never return `false` once [start] was called.
// */
//public val Process.started: Boolean
//    get() = when (state) {
//        is Prepared -> false
//        is Running -> true
//        is Terminated -> true
//    }

/**
 * A process that delegates to the [JavaProcess] provided by the specified [processProvider].
 */
public abstract class DelegatingProcess(private val processProvider: Process.() -> JavaProcess) : Process {
    override val metaStream: MetaStream = MetaStream()
    override val inputStream: OutputStream by lazy { javaProcess.outputStream }
    override val outputStream: InputStream by lazy { javaProcess.inputStream }
    override val errorStream: InputStream by lazy { javaProcess.errorStream }
    override val pid: Long by lazy { javaProcess.pid() }

    /**
     * The Java process this process delegates to.
     */
    protected val javaProcess: JavaProcess by lazy { processProvider().also { _started = true } }
    override fun start(): Process = also { javaProcess.pid() }
    private var _started: Boolean = false
    override val started: Boolean get() = _started
    override val alive: Boolean get() = started && javaProcess.isAlive
    override val exitValue: Int get() = javaProcess.exitValue()
    abstract override val onExit: CompletableFuture<out ExitState>
    override fun waitFor(): ExitState = exitState ?: onExit.join()
    override fun stop(): Process = also { javaProcess.destroy() }
    override fun kill(): Process = also { javaProcess.destroyForcibly() }

    override fun toString(): String {
        val delegateString =
            if (started) "${javaProcess.toString().replaceFirst('[', '(').dropLast(1) + ")"}, successful=${successful.asEmoji}"
            else "not yet started"
        return "${this::class.simpleName ?: "object"}(delegate=$delegateString, started=${started.asEmoji})"
    }
}
