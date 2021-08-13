package koodies.exec

import koodies.collections.synchronizedSetOf
import koodies.debug.asEmoji
import koodies.exception.toCompactString
import koodies.exec.Exec.Companion.createDump
import koodies.exec.Exec.Companion.fallbackExitStateHandler
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.exec.Process.State
import koodies.exec.Process.State.Excepted
import koodies.exec.Process.State.Running
import koodies.io.TeeInputStream
import koodies.io.TeeOutputStream
import koodies.runtime.addShutDownHook
import koodies.runtime.removeShutdownHook
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.Symbols.Computation
import koodies.text.Semantics.formattedAs
import koodies.text.truncate
import koodies.time.Now
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.concurrent.thread
import koodies.io.RedirectingOutputStream as ReOutputStream
import java.lang.Process as JavaProcess

/**
 * Java-based [Exec] implementation.
 */
public class JavaExec(

    private val process: JavaProcess,

    /**
     * The working directory to be used during execution.
     */
    public override val workingDirectory: Path?,

    /**
     * The command and its arguments to execute.
     */
    public override val commandLine: CommandLine,

    /**
     * If set, the creation of the [ExitState] is delegated to it.
     */
    private val exitStateHandler: ExitStateHandler? = null,

    /**
     * Called the moment the [Exec] terminates—no matter if the [Exec] succeeds or fails
     */
    private val execTerminationCallback: ExecTerminationCallback? = {},

    /**
     * Whether to kill this [Exec] if it's still running during VM shutdown.
     */
    private val destroyOnShutdown: Boolean = true,
) : Exec {
    public companion object;

    override val start: Instant = Now.instant
    override val end: Instant? get() = (state as? ExitState)?.end
    override val pid: Long = process.pid()
    override fun waitFor(): ExitState = (_state as? ExitState) ?: onExit.join()
    override fun stop(): Exec = also { process.destroy() }
    override fun kill(): Exec = also { process.destroyForcibly() }

    private var _state: State = Running(start, pid)

    // onExit needs to be triggered at some point so it updates the state
    // consequently callbacks registered after that moment will likely not be triggered
    override val state: State
        get() {
            onExit
            return _state
        }

    override val metaStream: MetaStream = MetaStream({ ioLog + it })
    override val inputStream: OutputStream = TeeOutputStream(process.outputStream, ReOutputStream { ioLog.input + it })
    override val outputStream: InputStream = TeeInputStream(process.inputStream, ReOutputStream { ioLog.output + it })
    override val errorStream: InputStream = TeeInputStream(process.errorStream, ReOutputStream { ioLog.error + it })

    private val ioLog = IOLog()
    override val io: IOSequence<IO> get() = IOSequence(ioLog)

    private val preTerminationCallbacks = synchronizedSetOf<Exec.() -> Unit>()
    override fun addPreTerminationCallback(callback: Exec.() -> Unit): Exec =
        apply { preTerminationCallbacks.add(callback) }

    private val postTerminationCallbacks = synchronizedSetOf<Exec.(ExitState) -> Unit>()
    public override fun addPostTerminationCallback(callback: Exec.(ExitState) -> Unit): Exec =
        apply { postTerminationCallbacks.add(callback) }

    private val cachedOnExit: CompletableFuture<out ExitState> by lazy {
        CompletableFuture.supplyAsync {
            preTerminationCallbacks.mapNotNull { callback ->
                runCatching { this.callback() }.exceptionOrNull()
            }.firstOrNull()?.let { throw it }
        }.thenCombine(process.onExit()) { _, _ ->
        }.handle { _, throwable ->
            ioLog.flush()
            val exitValue = process.exitValue()

            val exitState: ExitState = if (throwable != null) {
                val cause: Throwable = (throwable as? CompletionException)?.cause ?: throwable
                val dump = createDump("Process ${pid.formattedAs.input} terminated with ${cause.toCompactString()}.")
                Excepted(start, Now.instant, pid, exitValue, io, cause, dump.ansiRemoved)
            } else {
                kotlin.runCatching {
                    with(exitStateHandler ?: fallbackExitStateHandler()) { handle(pid, exitValue, io) }
                }.getOrElse { exception ->
                    val formattedPid = pid.formattedAs.input
                    val formattedExitCode = exitValue.formattedAs.input
                    val formattedException = exception.message.formattedAs.error
                    val message = "Unexpected error terminating process $formattedPid with exit code $formattedExitCode:$LF\t$formattedException"
                    Excepted(start, Now.instant, pid, exitValue, io, exception, createDump(message), message)
                }
            }

            _state = exitState

            postTerminationCallbacks.mapNotNull { callback ->
                runCatching { this.callback(exitState) }.exceptionOrNull()
            }.firstOrNull()?.printStackTrace()
            exitState
        }
    }

    override val onExit: CompletableFuture<out ExitState> get() = (_state as? ExitState)?.let { CompletableFuture.completedFuture(it) } ?: cachedOnExit

    override fun toString(): String {
        val delegateString = "${process.toString().replaceFirst('[', '(').dropLast(1) + ")"}, successful=${successfulOrNull?.asEmoji ?: Computation}"
        return "${this::class.simpleName ?: "object"}(process=$delegateString)".substringBeforeLast(")") +
            ", commandLine=${commandLine.shellCommand.toCompactString().truncate(50, " … ")}" +
            ", execTerminationCallback=${(execTerminationCallback != null).asEmoji}" +
            ", destroyOnShutdown=${destroyOnShutdown.asEmoji})"
    }

    init {
        try {
            if (destroyOnShutdown) {
                val shutdownHook = thread(start = false, name = "shutdown hook for $pid", contextClassLoader = null) { process.destroy() }
                addShutDownHook(shutdownHook)

                process.onExit().handle { _, _ -> removeShutdownHook(shutdownHook) }
            }

            execTerminationCallback?.let { callback ->
                process.onExit().handle { _, ex -> runCatching { callback.invoke(ex) } }
            }
        } catch (ex: Throwable) {
            kotlin.runCatching { execTerminationCallback?.invoke(ex) }
        }
    }
}
