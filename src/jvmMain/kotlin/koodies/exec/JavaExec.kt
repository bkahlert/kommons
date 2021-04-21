package koodies.exec

import koodies.collections.synchronizedSetOf
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO.META.FILE
import koodies.concurrent.process.IO.META.STARTING
import koodies.concurrent.process.IO.META.TERMINATED
import koodies.concurrent.process.IOLog
import koodies.concurrent.process.ShutdownHookUtils
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
import koodies.jvm.thenAlso
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators
import koodies.text.Semantics.formattedAs
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

/**
 * Java-based [Exec] implementation.
 */
public open class JavaExec(
    protected val commandLine: CommandLine,
    protected val exitStateHandler: ExitStateHandler? = null,
    protected val execTerminationCallback: ExecTerminationCallback? = {},
    protected val destroyOnShutdown: Boolean = true,
) : Exec {
    public companion object;

    private val startLock = ReentrantLock()
    private var javaProcess: java.lang.Process? = null
    override fun start(): Exec {
        if (javaProcess != null) return this
        return startLock.withLock {
            if (javaProcess != null) return this
            kotlin.runCatching {
                javaProcess = commandLine.toJavaProcess().apply {
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
                this
            }.onFailure {
                kotlin.runCatching { execTerminationCallback?.invoke(it) }
            }.getOrThrow()
        }
    }

    private fun startImplicitly(): java.lang.Process = run { start(); javaProcess!! }

    override val pid: Long by lazy { startImplicitly().pid() }
    override val started: Boolean get() = javaProcess != null
    override val alive: Boolean get() = javaProcess?.isAlive == true
    override val exitValue: Int get() = startImplicitly().exitValue() // TODO
    override fun waitFor(): ExitState = exitState ?: onExit.join()
    override fun stop(): Exec = also { startImplicitly().destroy() }
    override fun kill(): Exec = also { startImplicitly().destroyForcibly() }

    override val workingDirectory: Path get() = commandLine.workingDirectory

    override fun waitForTermination(): ExitState = onExit.join()

    override val state: ProcessState get() = exitState ?: run { if (javaProcess == null) Prepared() else Running(pid) }

    override var exitState: ExitState? = null
        protected set

    private val capturingInputStream: OutputStream by lazy { TeeOutputStream(startImplicitly().outputStream, RedirectingOutputStream { io.input + it }) }
    private val capturingOutputStream: InputStream by lazy { start(); TeeInputStream(startImplicitly().inputStream, RedirectingOutputStream { io.out + it }) }
    private val capturingErrorStream: InputStream by lazy { start(); TeeInputStream(startImplicitly().errorStream, RedirectingOutputStream { io.err + it }) }

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

        callbackStage.thenCombine(startImplicitly().onExit()) { _, _ ->
            io.flush()
            process
        }.handle { _, throwable ->
            when {

                throwable != null -> {
                    val cause: Throwable = (throwable as? CompletionException)?.cause ?: throwable
                    val dump = createDump("Process ${commandLine.summary} terminated with ${cause.toCompactString()}.")
                    Fatal(cause, exitValue, pid, dump.ansiRemoved, io.toList())
                }

                exitStateHandler != null ->
                    kotlin.runCatching {
                        exitStateHandler.handle(Terminated(pid, exitValue, io.toList()))
                    }.getOrElse { ex ->
                        val message =
                            "Unexpected error terminating process ${pid.formattedAs.input} with exit code ${exitValue.formattedAs.input}:${LineSeparators.LF}\t" +
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

    override fun toString(): String {
        val delegateString =
            if (javaProcess != null) "${javaProcess.toString().replaceFirst('[', '(').dropLast(1) + ")"}, successful=${successful.asEmoji}"
            else "not yet started"
        return "${this::class.simpleName ?: "object"}(delegate=$delegateString, started=${started.asEmoji})".substringBeforeLast(")") +
            ", commandLine=${commandLine.commandLine.toCompactString().truncate(50, MIDDLE, " … ")}" +
            ", processTerminationCallback=${execTerminationCallback.asEmoji}" +
            ", destroyOnShutdown=${destroyOnShutdown.asEmoji})"
    }
}
