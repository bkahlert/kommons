package koodies.concurrent.process

import koodies.collections.synchronizedSetOf
import koodies.concurrent.isScriptFile
import koodies.concurrent.process.ManagedProcess.Evaluated
import koodies.concurrent.process.ManagedProcess.Evaluated.Failed
import koodies.concurrent.process.ManagedProcess.Evaluated.Failed.UnexpectedExitCode
import koodies.concurrent.process.ManagedProcess.Evaluated.Successful
import koodies.concurrent.process.Process.ProcessState
import koodies.concurrent.thenAlso
import koodies.concurrent.toShellScriptFile
import koodies.debug.asEmoji
import koodies.exception.dump
import koodies.exception.toCompactString
import koodies.io.RedirectingOutputStream
import koodies.io.TeeInputStream
import koodies.io.TeeOutputStream
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.logging.ReturnValue
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.withTrailingLineSeparator
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate
import koodies.time.Now
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletableFuture.failedFuture
import java.util.concurrent.CompletionException
import kotlin.concurrent.thread
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import org.codehaus.plexus.util.cli.Commandline as PlexusCommandLine
import java.lang.Process as JavaProcess

public interface ManagedProcess : Process {
    public companion object {
        public fun from(
            commandLine: CommandLine,
            expectedExitValue: Int? = 0,
            processTerminationCallback: ProcessTerminationCallback? = null,
        ): ManagedProcess = ManagedJavaProcess(
            commandLine = commandLine,
            expectedExitValue = expectedExitValue,
            processTerminationCallback = processTerminationCallback)
    }

    public val ioLog: IOLog

    override fun start(): ManagedProcess

    /**
     * Registers the given [callback] in a thread-safe manner
     * to be called before the process termination is handled.
     */
    public fun addPreTerminationCallback(callback: ManagedProcess.() -> Unit): ManagedProcess

    /**
     * Registers the given [callback] in a thread-safe manner
     * to be called after the process termination is handled.
     */
    public fun addPostTerminationCallback(callback: ManagedProcess.(Evaluated) -> Unit): ManagedProcess

    public sealed class Evaluated(pid: Long, exitCode: Int, io: List<IO>, status: String) :
        ProcessState.Terminated(pid, exitCode, io, status), ReturnValue {

        override fun format(): CharSequence = status

        public class Successful(
            pid: Long,
            exitCode: Int,
            io: List<IO>,
        ) : ManagedProcess.Evaluated(pid, exitCode, io, "Process $pid terminated successfully at $Now.") {
            override val successful: Boolean = true
        }

        public sealed class Failed(
            pid: Long,
            status: String,
            exitCode: Int,
            io: List<IO>,
        ) : ManagedProcess.Evaluated(pid, exitCode, io, status) {
            override val successful: Boolean = false

            public class UnexpectedExitCode(
                pid: Long,
                public val expectedExitValue: Int,
                public val commandLine: CommandLine,
                status: String,
                exitCode: Int,
                io: List<IO>,
            ) : Failed(pid, status, exitCode, io)

            public class ExecutionException(
                pid: Long,
                public val exception: Throwable,
                status: String,
                exitCode: Int,
                io: List<IO>,
            ) : Failed(pid, status, exitCode, io)
        }
    }
}

private fun CommandLine.toJavaProcess(): JavaProcess {
    val scriptFile: String = if (command.asPath().isScriptFile()) command else toShellScriptFile().asString()

    val shell = PlexusCommandLine().shell
    val shellCommandLine = shell.getShellCommandLine(arrayOf(scriptFile))
    val processBuilder = ProcessBuilder(shellCommandLine).also { pb ->
        pb.environment().putAll(environment)
        pb.directory(workingDirectory.toAbsolutePath().run {
            require(exists()) { "Working directory $this does not exist." }
            require(isDirectory()) { "Working directory $this is no directory." }
            toFile()
        })
    }
    return processBuilder.start()
}

/**
 * Process that wraps an existing [JavaProcess] and forwards all
 * calls with the following features:
 *
 * 1) It can be specified what should be called on process termination and
 *    if the started process should be destroyed if the JVM shuts down.
 *
 * 2) The command line used to create the lazy process can be provided so
 *    it can always be reliably displayed.
 *
 * 3) The expected exit value can be specified. [waitFor] and [onExit]
 *    will throw an exception if the process exits with a different one.
 *
 * 4) The actual process can be [Lazy]. Nothing  in this implementation
 *    triggers it. Also this class can delegate itself without changing its
 *    behaviour.
 */
private open class ManagedJavaProcess(
    protected val commandLine: CommandLine,
    protected val expectedExitValue: Int? = 0,
    protected val processTerminationCallback: ProcessTerminationCallback? = {},
    protected val destroyOnShutdown: Boolean = true,
) : DelegatingProcess({
    kotlin.runCatching {
        commandLine.toJavaProcess().apply {
            metaStream.emit(IO.META.STARTING(commandLine))
            commandLine.includedFiles.forEach { metaStream.emit(IO.META.FILE(it)) }

            if (destroyOnShutdown) {
                val shutdownHook = thread(start = false, name = "shutdown hook for $this", contextClassLoader = null) { destroy() }
                ShutdownHookUtils.addShutDownHook(shutdownHook)

                onExit().handle { _, _ -> ShutdownHookUtils.removeShutdownHook(shutdownHook) }
            }

            processTerminationCallback?.let { callback ->
                onExit().handle { _, ex -> runCatching { callback.invoke(ex) } }
            }
        }
    }.onFailure {
        kotlin.runCatching { processTerminationCallback?.invoke(it) }
    }.getOrThrow()
}), ManagedProcess {
    companion object;

    override fun start(): ManagedProcess = also { super.start() }
    override fun waitForTermination(): Evaluated = onExit.join()

    private var termination: Evaluated? = null
    override val state: ProcessState get() = termination ?: run { if (!started) ProcessState.Prepared() else ProcessState.Running(pid) }

    private val capturingInputStream: OutputStream by lazy { TeeOutputStream(javaProcess.outputStream, RedirectingOutputStream { ioLog.input + it }) }
    private val capturingOutputStream: InputStream by lazy { TeeInputStream(javaProcess.inputStream, RedirectingOutputStream { ioLog.out + it }) }
    private val capturingErrorStream: InputStream by lazy { TeeInputStream(javaProcess.errorStream, RedirectingOutputStream { ioLog.err + it }) }

    final override val metaStream: MetaStream = MetaStream({ ioLog + it })
    final override val inputStream: OutputStream get() = capturingInputStream
    final override val outputStream: InputStream get() = capturingOutputStream
    final override val errorStream: InputStream get() = capturingErrorStream

    override val ioLog: IOLog by lazy { IOLog() }

    private val preTerminationCallbacks = synchronizedSetOf<ManagedProcess.() -> Unit>()
    public override fun addPreTerminationCallback(callback: ManagedProcess.() -> Unit): ManagedProcess =
        apply { preTerminationCallbacks.add(callback) }

    protected val cachedOnExit: CompletableFuture<out Evaluated> by lazy<CompletableFuture<out Evaluated>> {
        val process: ManagedProcess = this@ManagedJavaProcess
        val callbackStage: CompletableFuture<Process> = preTerminationCallbacks.mapNotNull {
            runCatching { process.it() }.exceptionOrNull()
        }.firstOrNull()?.let { failedFuture(it) }
            ?: completedFuture(process)

        callbackStage.thenCombine(javaProcess.onExit()) { _, _ ->
            ioLog.flush()
            process
        }.handle { _, throwable ->
            if (throwable != null) {
                val cause: Throwable = (throwable as? CompletionException)?.cause ?: throwable
                val dump = commandLine.workingDirectory.dump("""
                    Process ${commandLine.summary} terminated with ${cause.toCompactString()}.
                """.trimIndent()) { ioLog.dump() }.also { dump -> metaStream.emit(IO.META.DUMP(dump)) }
                Failed.ExecutionException(pid, cause, dump.removeEscapeSequences(), exitValue, ioLog.getCopy())
            } else if (expectedExitValue != null && exitValue != expectedExitValue) {
                val message = StringBuilder("Process $pid terminated with exit code $exitValue. Expected $expectedExitValue.").apply {
                    append(LF + commandLine.includedFiles.joinToString(LF) { IO.META typed it })
                }.toString()
                message.also { metaStream.emit(IO.META typed it) }
                val dump = commandLine.workingDirectory.dump(null) { ioLog.dump() }.also { dump -> metaStream.emit(IO.META.DUMP(dump)) }
                UnexpectedExitCode(pid, expectedExitValue, commandLine, message.withTrailingLineSeparator() + dump, exitValue, ioLog.getCopy())
            } else {
                metaStream.emit(IO.META.TERMINATED(process))
                Successful(pid, exitValue, ioLog.getCopy())
            }.also { termination = it }
        }.thenAlso { term, ex ->
            postTerminationCallbacks.forEach {
                process.it(term ?: Failed.ExecutionException(pid, ex!!,
                    "Unexpected exception in process termination handling.",
                    exitValue, ioLog.getCopy()))
            }
        }
    }

    private val postTerminationCallbacks = synchronizedSetOf<ManagedProcess.(Evaluated) -> Unit>()
    public override fun addPostTerminationCallback(callback: ManagedProcess.(Evaluated) -> Unit): ManagedProcess =
        apply { postTerminationCallbacks.add(callback) }


    override val successful: Boolean? get() = termination?.successful
    override fun format(): CharSequence = termination?.format() ?: state.status

    override val onExit: CompletableFuture<out Evaluated> get() = termination?.let { completedFuture(it) } ?: cachedOnExit

    override fun toString(): String =
        super.toString().substringBeforeLast(")") +
            ", commandLine=${commandLine.commandLine.toCompactString().truncate(50, MIDDLE, " … ")}" +
            ", expectedExitValue=${expectedExitValue}" +
            ", processTerminationCallback=${processTerminationCallback.asEmoji}" +
            ", destroyOnShutdown=${destroyOnShutdown.asEmoji})"
}

public typealias ProcessTerminationCallback = (Throwable?) -> Unit
