package koodies.concurrent.process

import koodies.collections.synchronizedSetOf
import koodies.concurrent.isScriptFile
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
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate
import koodies.time.sleep
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture
import java.util.concurrent.CompletableFuture.failedFuture
import java.util.concurrent.CompletionException
import kotlin.concurrent.thread
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.time.milliseconds
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
    public fun addPostTerminationCallback(callback: ManagedProcess.(Throwable?) -> Unit): ManagedProcess
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

    private val capturingInputStream: OutputStream by lazy {
        TeeOutputStream(
            RedirectingOutputStream {
                // ugly hack; IN logs are just there and the processor is just notified;
                // whereas OUT and ERR have to be processed first, are delayed and don't show in right order
                // therefore we delay here
                1.milliseconds.sleep { ioLog.input + it }
            },
            javaProcess.outputStream,
        )
    }
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

    protected val cachedOnExit: CompletableFuture<Process> by lazy<CompletableFuture<Process>> {
        val process: ManagedProcess = this@ManagedJavaProcess
        val callbackStage: CompletableFuture<Process> = preTerminationCallbacks.mapNotNull {
            runCatching { process.it() }.exceptionOrNull()
        }.firstOrNull()?.let { failedFuture(it) }
            ?: completedFuture(process)

        callbackStage.thenCombine(javaProcess.onExit()) { _, _ ->
            process
        }.exceptionally { throwable ->
            val cause = if (throwable is CompletionException) throwable.cause else throwable
            val dump = commandLine.workingDirectory.dump("""
                Process ${commandLine.summary} terminated with ${cause.toCompactString()}.
            """.trimIndent()) { ioLog.dump() }.also { dump -> metaStream.emit(IO.META.DUMP(dump)) }
            throw RuntimeException(dump.removeEscapeSequences(), cause)
        }.thenApply { _ ->
            if (expectedExitValue != null && exitValue != expectedExitValue) {
                val message = ProcessExecutionException(pid, commandLine, exitValue, expectedExitValue).message
                message?.also { metaStream.emit(IO.META typed it) }
                val dump = commandLine.workingDirectory.dump(null) { ioLog.dump() }.also { dump -> metaStream.emit(IO.META.DUMP(dump)) }
                throw ProcessExecutionException(pid, commandLine, exitValue, expectedExitValue, dump.removeEscapeSequences())
            }
            metaStream.emit(IO.META.TERMINATED(process))
            process as Process
        }.thenAlso { _, ex: Throwable? ->
            val cause = if (ex is CompletionException) ex.cause else ex
            postTerminationCallbacks.forEach { process.it(cause) }
        }
    }

    private val postTerminationCallbacks = synchronizedSetOf<ManagedProcess.(Throwable?) -> Unit>()
    public override fun addPostTerminationCallback(callback: ManagedProcess.(Throwable?) -> Unit): ManagedProcess =
        apply { postTerminationCallbacks.add(callback) }


    override val successful: Boolean?
        get() = if (!started) null
        else {
            kotlin.runCatching { exitValue }.fold({ value ->
                expectedExitValue?.let { value == it } ?: true
            }, {
                null
            })
        }

    override val onExit: CompletableFuture<Process> get() = cachedOnExit

    override fun toString(): String =
        super.toString().substringBeforeLast(")") +
            ", commandLine=${commandLine.commandLine.toCompactString().truncate(50, MIDDLE, " … ")}" +
            ", expectedExitValue=${expectedExitValue}" +
            ", processTerminationCallback=${processTerminationCallback.asEmoji}" +
            ", destroyOnShutdown=${destroyOnShutdown.asEmoji})"
}

public typealias ProcessTerminationCallback = (Throwable?) -> Unit
