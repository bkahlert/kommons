package koodies.exec

import koodies.collections.synchronizedSetOf
import koodies.debug.asEmoji
import koodies.exception.toCompactString
import koodies.exec.Exec.Companion.createDump
import koodies.exec.Exec.Companion.fallbackExitStateHandler
import koodies.exec.IO.Meta.File
import koodies.exec.IO.Meta.Starting
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.exec.Process.ExitState.Fatal
import koodies.exec.Process.ProcessState
import koodies.exec.Process.ProcessState.Running
import koodies.exec.Process.ProcessState.Terminated
import koodies.io.TeeInputStream
import koodies.io.TeeOutputStream
import koodies.jvm.addShutDownHook
import koodies.jvm.removeShutdownHook
import koodies.jvm.thenAlso
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators
import koodies.text.Semantics.Symbols.Computation
import koodies.text.Semantics.formattedAs
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.concurrent.thread
import koodies.io.RedirectingOutputStream as ReOutputStream
import java.lang.Process as JavaProcess

/**
 * Java-based [Exec] implementation.
 */
public class JavaExec(

    private val javaProcess: JavaProcess,

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
    protected val execTerminationCallback: ExecTerminationCallback? = {},

    /**
     * Whether to kill this [Exec] if it's still running during VM shutdown.
     */
    private val destroyOnShutdown: Boolean = true,
) : Exec {
    public companion object;

    override val pid: Long by lazy { javaProcess.pid() }
    override fun waitFor(): ExitState = exitState ?: onExit.join()
    override fun stop(): Exec = also { javaProcess.destroy() }
    override fun kill(): Exec = also { javaProcess.destroyForcibly() }

    override val state: ProcessState get() = exitState ?: Running(pid)

    override var exitState: ExitState? = null
        protected set

    private val capturingInputStream: OutputStream by lazy { TeeOutputStream(javaProcess.outputStream, ReOutputStream { ioLog.input + it }) }
    private val capturingOutputStream: InputStream by lazy { TeeInputStream(javaProcess.inputStream, ReOutputStream { ioLog.output + it }) }
    private val capturingErrorStream: InputStream by lazy { TeeInputStream(javaProcess.errorStream, ReOutputStream { ioLog.error + it }) }

    override val metaStream: MetaStream = MetaStream({ ioLog + it })
    override val inputStream: OutputStream get() = capturingInputStream
    override val outputStream: InputStream get() = capturingOutputStream
    override val errorStream: InputStream get() = capturingErrorStream

    private val ioLog by lazy { IOLog() }
    override val io: IOSequence<IO> get() = IOSequence(ioLog)

    private val preTerminationCallbacks = synchronizedSetOf<Exec.() -> Unit>()
    override fun addPreTerminationCallback(callback: Exec.() -> Unit): Exec =
        apply { preTerminationCallbacks.add(callback) }

    private val cachedOnExit: CompletableFuture<out ExitState> by lazy<CompletableFuture<out ExitState>> {
        val process: Exec = this@JavaExec
        val callbackStage: CompletableFuture<Process> = preTerminationCallbacks.mapNotNull {
            runCatching { process.it() }.exceptionOrNull()
        }.firstOrNull()?.let { CompletableFuture.failedFuture(it) }
            ?: CompletableFuture.completedFuture(process)

        callbackStage.thenCombine(javaProcess.onExit()) { _, _ ->
            ioLog.flush()
            process
        }.handle { _, throwable ->
            val exitValue = javaProcess.exitValue()

            run {
                if (throwable != null) {
                    val cause: Throwable = (throwable as? CompletionException)?.cause ?: throwable
                    val dump = createDump("Process ${commandLine.summary} terminated with ${cause.toCompactString()}.")

                    Fatal(cause, exitValue, pid, dump.ansiRemoved, io)
                } else {
                    kotlin.runCatching {
                        val exitStateHandler = exitStateHandler ?: fallbackExitStateHandler()
                        val terminated = Terminated(pid, exitValue, io)

                        exitStateHandler.handle(terminated)
                    }.getOrElse { ex ->
                        val message =
                            "Unexpected error terminating process ${pid.formattedAs.input} with exit code ${exitValue.formattedAs.input}:${LineSeparators.LF}\t" +
                                ex.message.formattedAs.error

                        Fatal(ex, exitValue, pid, createDump(message), io, message)
                    }
                }

            }.also { exitState = it }

        }.thenAlso { term, ex ->
            postTerminationCallbacks.forEach {
                process.it(term ?: Fatal(ex!!, javaProcess.exitValue(), pid, "Unexpected exception in process termination handling.", io))
            }
        }
    }

    private val postTerminationCallbacks = synchronizedSetOf<Exec.(ExitState) -> Unit>()
    public override fun addPostTerminationCallback(callback: Exec.(ExitState) -> Unit): Exec =
        apply { postTerminationCallbacks.add(callback) }

    override val onExit: CompletableFuture<out ExitState> get() = exitState?.let { CompletableFuture.completedFuture(it) } ?: cachedOnExit

    override fun toString(): String {
        val delegateString = "${javaProcess.toString().replaceFirst('[', '(').dropLast(1) + ")"}, successful=${successful?.asEmoji ?: Computation}"
        return "${this::class.simpleName ?: "object"}(delegate=$delegateString)".substringBeforeLast(")") +
            ", commandLine=${commandLine.shellCommand.toCompactString().truncate(50, MIDDLE, " … ")}" +
            ", execTerminationCallback=${(execTerminationCallback != null).asEmoji}" +
            ", destroyOnShutdown=${destroyOnShutdown.asEmoji})"
    }

    init {
        try {
            metaStream.emit(Starting(commandLine))
            commandLine.includedFiles.forEach { metaStream.emit(File(it)) }

            if (destroyOnShutdown) {
                val shutdownHook = thread(start = false, name = "shutdown hook for $this", contextClassLoader = null) { javaProcess.destroy() }
                addShutDownHook(shutdownHook)

                javaProcess.onExit().handle { _, _ -> removeShutdownHook(shutdownHook) }
            }

            execTerminationCallback?.let { callback ->
                javaProcess.onExit().handle { _, ex -> runCatching { callback.invoke(ex) } }
            }
        } catch (ex: Throwable) {
            kotlin.runCatching { execTerminationCallback?.invoke(ex) }
        }
    }
}
