package koodies.concurrent.process

import koodies.collections.synchronizedSetOf
import koodies.concurrent.isScriptFile
import koodies.concurrent.process.Process.ExitState
import koodies.concurrent.process.Process.ExitState.ExitStateHandler
import koodies.concurrent.process.Process.ExitState.Failure
import koodies.concurrent.process.Process.ExitState.Fatal
import koodies.concurrent.process.Process.ExitState.Success
import koodies.concurrent.process.Process.ProcessState
import koodies.concurrent.process.Process.ProcessState.Terminated
import koodies.concurrent.thenAlso
import koodies.debug.asEmoji
import koodies.exception.dump
import koodies.exception.toCompactString
import koodies.io.RedirectingOutputStream
import koodies.io.TeeInputStream
import koodies.io.TeeOutputStream
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.shell.HereDoc
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.formattedAs
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate
import org.codehaus.plexus.util.cli.CommandLineUtils
import org.codehaus.plexus.util.cli.shell.Shell
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Path
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
            exitStateHandler: ExitStateHandler? = null,
            processTerminationCallback: ProcessTerminationCallback? = null,
        ): ManagedProcess = ManagedJavaProcess(
            commandLine = commandLine,
            exitStateHandler = exitStateHandler,
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
    public fun addPostTerminationCallback(callback: ManagedProcess.(ExitState) -> Unit): ManagedProcess
}

private fun Path.runScriptAsJavaProcess(environment: Map<String, String>, workingDirectory: Path): JavaProcess {
    require(isScriptFile()) { "$this must be a script file." }
    val scriptFile: String = asString()

    val directory: File = requireValidWorkingDirectory(workingDirectory)

    val shell = PlexusCommandLine().shell
    val shellCommandLine = shell.getShellCommandLine(arrayOf(scriptFile))

    return ProcessBuilder(shellCommandLine).let { pb ->
        pb.environment().putAll(environment)
        pb.directory(directory)
        pb.start()
    }
}

private fun CommandLine.runCommandLineAsJavaProcess(): JavaProcess {

    require(redirects.isEmpty()) {
        "Redirects are only supported for shell scripts.$LF" +
            "Convert your command line first to a script file and execute that one."
    }
    val hereDocDelimiters = HereDoc.findAllDelimiters(CommandLineUtils.toString(commandLineParts))
    require(hereDocDelimiters.isEmpty()) {
        "The command line contained here documents ($hereDocDelimiters) which " +
            "will not be escaped and are not what you intended to do."
    }

    val directory: File = requireValidWorkingDirectory(workingDirectory)

    val shell = PlexusCommandLine().shell
    val shellCommandLine = shell.getShellCommandLine(commandLineParts)

    return ProcessBuilder(shellCommandLine).let { pb ->
        pb.environment().putAll(environment)
        pb.directory(directory)
        pb.start()
    }
}

// TODO implement toggle to run commandLine always as script to provide
// an easy way to see what was executed
/**
 * # THE function to execute command lines
 *
 * Checks if `this` command line points to a script file and if yes,
 * executes it using a [Shell].
 *
 * Otherwise the command line is taken as is and executes using the VM's
 * [ProcessBuilder].
 */
private fun CommandLine.toJavaProcess(): JavaProcess {
    val scriptFile: Path? = kotlin.runCatching { command.asPath() }.getOrNull()?.takeIf { it.isScriptFile() }
    return scriptFile
        ?.runScriptAsJavaProcess(environment, workingDirectory)
        ?: runCommandLineAsJavaProcess()
}

private fun requireValidWorkingDirectory(workingDirectory: Path): File =
    workingDirectory.toAbsolutePath().run {
        require(exists()) { "Working directory $this does not exist." }
        require(isDirectory()) { "Working directory $this is no directory." }
        toFile()
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
    protected val exitStateHandler: ExitStateHandler? = null,
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
    override fun waitForTermination(): ExitState = onExit.join()

    override val state: ProcessState get() = exitState ?: run { if (!started) ProcessState.Prepared() else ProcessState.Running(pid) }

    override var exitState: ExitState? = null
        protected set

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

    protected val cachedOnExit: CompletableFuture<out ExitState> by lazy<CompletableFuture<out ExitState>> {
        val process: ManagedProcess = this@ManagedJavaProcess
        val callbackStage: CompletableFuture<Process> = preTerminationCallbacks.mapNotNull {
            runCatching { process.it() }.exceptionOrNull()
        }.firstOrNull()?.let { failedFuture(it) }
            ?: completedFuture(process)

        callbackStage.thenCombine(javaProcess.onExit()) { _, _ ->
            ioLog.flush()
            process
        }.handle { _, throwable ->
            when {
                throwable != null -> {
                    val cause: Throwable = (throwable as? CompletionException)?.cause ?: throwable
                    val dump = commandLine.workingDirectory.dump("""
                            Process ${commandLine.summary} terminated with ${cause.toCompactString()}.
                        """.trimIndent()) { ioLog.dump() }.also { dump -> metaStream.emit(IO.META.DUMP(dump)) }
                    Fatal(cause, exitValue, pid, dump.removeEscapeSequences(), ioLog.getCopy())
                }
                exitStateHandler != null -> {
                    kotlin.runCatching {
                        exitStateHandler.handle(Terminated(pid, exitValue, ioLog.getCopy()))
                    }.getOrElse { ex ->
                        val message =
                            "Unexpected error terminating process ${pid.formattedAs.input} with exit code ${exitValue.formattedAs.input}:$LF\t${ex.message.formattedAs.error}"
                                .also { metaStream.emit(IO.META typed it) }
                        val dump = commandLine.workingDirectory.dump(null) { ioLog.dump() }.also { dump -> metaStream.emit(IO.META.DUMP(dump)) }
                        Fatal(ex, exitValue, pid, dump, io, message)
                    }
                }
                exitValue != 0 -> {
                    val message = StringBuilder("Process ${pid.formattedAs.input} terminated with exit code ${exitValue.formattedAs.input}").apply {
                        append(LF + commandLine.includedFiles.joinToString(LF) { IO.META typed it })
                    }.toString()
                    message.also { metaStream.emit(IO.META typed it) }
                    val dump = commandLine.workingDirectory.dump(null) { ioLog.dump() }.also { dump -> metaStream.emit(IO.META.DUMP(dump)) }
                    Failure(exitValue, pid, commandLine.includedFiles.map { it.toUri() }, dump, ioLog.getCopy())
                }
                else -> {
                    metaStream.emit(IO.META.TERMINATED(process))
                    Success(pid, ioLog.getCopy())
                }
            }.also { exitState = it }
        }.thenAlso { term, ex ->
            postTerminationCallbacks.forEach {
                process.it(term ?: Fatal(ex!!, exitValue, pid, "Unexpected exception in process termination handling.", ioLog.getCopy()))
            }
        }
    }

    private val postTerminationCallbacks = synchronizedSetOf<ManagedProcess.(ExitState) -> Unit>()
    public override fun addPostTerminationCallback(callback: ManagedProcess.(ExitState) -> Unit): ManagedProcess =
        apply { postTerminationCallbacks.add(callback) }

    override val onExit: CompletableFuture<out ExitState> get() = exitState?.let { completedFuture(it) } ?: cachedOnExit

    override fun toString(): String =
        super.toString().substringBeforeLast(")") +
            ", commandLine=${commandLine.commandLine.toCompactString().truncate(50, MIDDLE, " … ")}" +
            ", processTerminationCallback=${processTerminationCallback.asEmoji}" +
            ", destroyOnShutdown=${destroyOnShutdown.asEmoji})"
}

public typealias ProcessTerminationCallback = (Throwable?) -> Unit
