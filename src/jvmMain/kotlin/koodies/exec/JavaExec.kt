package koodies.exec

import koodies.collections.synchronizedSetOf
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.DelegatingProcess
import koodies.concurrent.process.IO.META.FILE
import koodies.concurrent.process.IO.META.STARTING
import koodies.concurrent.process.IO.META.TERMINATED
import koodies.concurrent.process.IOLog
import koodies.concurrent.process.ShutdownHookUtils
import koodies.jvm.thenAlso
import koodies.debug.asEmoji
import koodies.exception.toCompactString
import koodies.exec.Exec.Companion.createDump
import koodies.exec.Process.ExitState
import koodies.exec.Process.ExitState.ExitStateHandler
import koodies.exec.Process.ExitState.Failure
import koodies.exec.Process.ExitState.Fatal
import koodies.exec.Process.ExitState.Success
import koodies.exec.Process.ProcessState
import koodies.exec.Process.ProcessState.Prepared
import koodies.exec.Process.ProcessState.Running
import koodies.exec.Process.ProcessState.Terminated
import koodies.io.RedirectingOutputStream
import koodies.io.TeeInputStream
import koodies.io.TeeOutputStream
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.LineSeparators
import koodies.text.Semantics.formattedAs
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.concurrent.thread

/**
 * Java-based [Exec] implementation.
 */
public open class JavaExec(
    protected val commandLine: CommandLine,
    protected val exitStateHandler: ExitStateHandler? = null,
    protected val execTerminationCallback: ExecTerminationCallback? = {},
    protected val destroyOnShutdown: Boolean = true,
) : DelegatingProcess({
    kotlin.runCatching {
        commandLine.toJavaProcess().apply {
            metaStream.emit(STARTING(commandLine))
            commandLine.includedFiles.forEach { metaStream.emit(FILE(it)) }

            if (destroyOnShutdown) {
                val shutdownHook = thread(start = false, name = "shutdown hook for $this", contextClassLoader = null) { destroy() }
                ShutdownHookUtils.addShutDownHook(shutdownHook)

                onExit().handle { _, _ -> ShutdownHookUtils.removeShutdownHook(shutdownHook) }
            }

            execTerminationCallback?.let { callback ->
                onExit().handle { _, ex -> runCatching { callback.invoke(ex) } }
            }
        }
    }.onFailure {
        kotlin.runCatching { execTerminationCallback?.invoke(it) }
    }.getOrThrow()
}), Exec {
    public companion object;

    override val workingDirectory: Path get() = commandLine.workingDirectory

    override fun start(): Exec = also { super.start() }
    override fun waitForTermination(): ExitState = onExit.join()

    override val state: ProcessState get() = exitState ?: run { if (!started) Prepared() else Running(pid) }

    override var exitState: ExitState? = null
        protected set

    private val capturingInputStream: OutputStream by lazy { TeeOutputStream(javaProcess.outputStream, RedirectingOutputStream { io.input + it }) }
    private val capturingOutputStream: InputStream by lazy { TeeInputStream(javaProcess.inputStream, RedirectingOutputStream { io.out + it }) }
    private val capturingErrorStream: InputStream by lazy { TeeInputStream(javaProcess.errorStream, RedirectingOutputStream { io.err + it }) }

    final override val metaStream: MetaStream = MetaStream({ io + it })
    final override val inputStream: OutputStream get() = capturingInputStream
    final override val outputStream: InputStream get() = capturingOutputStream
    final override val errorStream: InputStream get() = capturingErrorStream

    override val io: IOLog by lazy { IOLog() }

    private val preTerminationCallbacks = synchronizedSetOf<Exec.() -> Unit>()
    override fun addPreTerminationCallback(callback: Exec.() -> Unit): Exec =
        apply { preTerminationCallbacks.add(callback) }

    protected val cachedOnExit: CompletableFuture<out ExitState> by lazy<CompletableFuture<out ExitState>> {
        val process: Exec = this@JavaExec
        val callbackStage: CompletableFuture<Process> = preTerminationCallbacks.mapNotNull {
            runCatching { process.it() }.exceptionOrNull()
        }.firstOrNull()?.let { CompletableFuture.failedFuture(it) }
            ?: CompletableFuture.completedFuture(process)

        callbackStage.thenCombine(javaProcess.onExit()) { _, _ ->
            io.flush()
            process
        }.handle { _, throwable ->
            when {

                throwable != null -> {
                    val cause: Throwable = (throwable as? CompletionException)?.cause ?: throwable
                    val dump = createDump("Process ${commandLine.summary} terminated with ${cause.toCompactString()}.")
                    Fatal(cause, exitValue, pid, dump.removeEscapeSequences(), io.toList())
                }

                exitStateHandler != null ->
                    kotlin.runCatching {
                        exitStateHandler.handle(Terminated(pid, exitValue, io.toList()))
                    }.getOrElse { ex ->
                        val message = "Unexpected error terminating process ${pid.formattedAs.input} with exit code ${exitValue.formattedAs.input}:${LineSeparators.LF}\t" +
                            ex.message.formattedAs.error
                        Fatal(ex, exitValue, pid, createDump(message), io.toList(), message)
                    }

                exitValue != 0 -> {
                    val dump = createDump(
                        "Process ${pid.formattedAs.input} terminated with exit code ${exitValue.formattedAs.input}",
                        *commandLine.includedFiles.map { FILE(it).formatted }.toTypedArray()
                    )
                    Failure(exitValue, pid, commandLine.includedFiles.map { it.toUri() }, dump, io.toList())
                }

                else -> {
                    metaStream.emit(TERMINATED(process))
                    Success(pid, io.toList())
                }

            }.also { exitState = it }
        }.thenAlso { term, ex ->
            postTerminationCallbacks.forEach {
                process.it(term ?: Fatal(ex!!, exitValue, pid, "Unexpected exception in process termination handling.", io.toList()))
            }
        }
    }

    private val postTerminationCallbacks = synchronizedSetOf<Exec.(ExitState) -> Unit>()
    public override fun addPostTerminationCallback(callback: Exec.(ExitState) -> Unit): Exec =
        apply { postTerminationCallbacks.add(callback) }

    override val onExit: CompletableFuture<out ExitState> get() = exitState?.let { CompletableFuture.completedFuture(it) } ?: cachedOnExit

    override fun toString(): String =
        super.toString().substringBeforeLast(")") +
            ", commandLine=${commandLine.commandLine.toCompactString().truncate(50, MIDDLE, " … ")}" +
            ", processTerminationCallback=${execTerminationCallback.asEmoji}" +
            ", destroyOnShutdown=${destroyOnShutdown.asEmoji})"
}
