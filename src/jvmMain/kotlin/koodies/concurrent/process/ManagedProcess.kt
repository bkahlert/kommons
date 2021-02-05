package koodies.concurrent.process

import koodies.concurrent.isScriptFile
import koodies.debug.asEmoji
import koodies.exception.dump
import koodies.exception.toCompactString
import koodies.io.RedirectingOutputStream
import koodies.io.path.asPath
import koodies.io.path.asString
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.text.TruncationStrategy.MIDDLE
import koodies.text.truncate
import koodies.time.Now
import koodies.time.sleep
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.concurrent.thread
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.time.milliseconds
import org.codehaus.plexus.util.cli.Commandline as PlexusCommandLine
import java.lang.Process as JavaProcess

interface ManagedProcess : Process {
    companion object {
        fun from(
            commandLine: CommandLine,
            expectedExitValue: Int? = 0,
            processTerminationCallback: (() -> Unit)? = null,
        ): ManagedProcess = ManagedJavaProcess(
            commandLine = commandLine,
            expectedExitValue = expectedExitValue,
            processTerminationCallback = processTerminationCallback)
    }

    val ioLog: IOLog

    var inputCallback: (IO) -> Unit

    /**
     * If set, the finalization of the process will be delayed until
     * [externalSync] completes.
     */
    var externalSync: CompletableFuture<*>

    override fun start(): ManagedProcess
}

private fun CommandLine.toJavaProcess(): JavaProcess {
    val scriptFile: String = if (command.asPath().isScriptFile()) command else toShellScript().asString()

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
    protected val processTerminationCallback: (() -> Unit)? = {},
    protected val destroyOnShutdown: Boolean = true,
) : DelegatingProcess({
    kotlin.runCatching {
        commandLine.toJavaProcess().apply {
            metaLog("Executing ${commandLine.commandLine}")
            commandLine.formattedIncludesFiles.takeIf { it.isNotBlank() }?.let { metaLog(it) }

            if (destroyOnShutdown) {
                val shutdownHook = thread(start = false, name = "shutdown hook for $this", contextClassLoader = null) { destroy() }
                ShutdownHookUtils.addShutDownHook(shutdownHook)

                onExit().handle { _, _ -> ShutdownHookUtils.removeShutdownHook(shutdownHook) }
            }

            processTerminationCallback?.let { callback ->
                onExit().handle { _, _ -> runCatching { callback.invoke() } }
            }
        }
    }.onFailure {
        kotlin.runCatching { processTerminationCallback?.invoke() }
    }.getOrThrow()
}), ManagedProcess {
    companion object;

    override fun start(): ManagedProcess {
        super.start()
        return this
    }

    override var inputCallback: (IO) -> Unit = {}

    private val capturingMetaStream: OutputStream by lazy {
        TeeOutputStream(
            RedirectingOutputStream {
                // ugly hack; META logs are just there and the processor is just notified;
                // whereas OUT and ERR have to be processed first, are delayed and don't show in right order
                // therefor we delay here
                1.milliseconds.sleep { ioLog.add(IO.Type.META, it) }
            },
            RedirectingOutputStream { inputCallback(IO.Type.META typed it.decodeToString()) },
        )
    }
    private val capturingOutputStream: OutputStream by lazy {
        TeeOutputStream(
            RedirectingOutputStream {
                // ugly hack; IN logs are just there and the processor is just notified;
                // whereas OUT and ERR have to be processed first, are delayed and don't show in right order
                // therefor we delay here
                1.milliseconds.sleep { ioLog.add(IO.Type.IN, it) }
            },
            javaProcess.outputStream,
            RedirectingOutputStream { inputCallback(IO.Type.IN typed it.decodeToString()) },
        )
    }
    private val capturingInputStream: InputStream by lazy { TeeInputStream(javaProcess.inputStream, RedirectingOutputStream { ioLog.add(IO.Type.OUT, it) }) }
    private val capturingErrorStream: InputStream by lazy { TeeInputStream(javaProcess.errorStream, RedirectingOutputStream { ioLog.add(IO.Type.ERR, it) }) }

    final override val metaStream: OutputStream get() = capturingMetaStream
    final override val outputStream: OutputStream get() = capturingOutputStream
    final override val inputStream: InputStream get() = capturingInputStream
    final override val errorStream: InputStream get() = capturingErrorStream

    override val ioLog: IOLog by lazy { IOLog() }

    override var externalSync: CompletableFuture<*> = CompletableFuture.completedFuture(Unit)
    protected val oneTimeOnExit: CompletableFuture<Process> by lazy {
        externalSync.thenCombine(javaProcess.onExit()) { _, process ->
            process
        }.exceptionally { throwable ->
            val cause = if (throwable is CompletionException) throwable.cause else throwable
            val dump = commandLine.workingDirectory.dump("""
                Process $commandLine terminated with ${cause.toCompactString()}.
            """.trimIndent()) { ioLog.dump() }.also { dump -> metaLog(dump) }
            throw RuntimeException(dump.removeEscapeSequences(), cause)
        }.thenApply { _ ->
            if (expectedExitValue != null && exitValue != expectedExitValue) {
                val message = ProcessExecutionException(pid, commandLine, exitValue, expectedExitValue).message
                message?.also { metaLog(it) }
                val dump = commandLine.workingDirectory.dump(null) { ioLog.dump() }.also { dump -> metaLog(dump) }
                throw ProcessExecutionException(pid, commandLine, exitValue, expectedExitValue, dump.removeEscapeSequences())
            }
            metaLog("Process $pid terminated successfully at $Now.")
            this@ManagedJavaProcess
        }
    }
    override var onExit: CompletableFuture<Process>
        get() = oneTimeOnExit
        set(value) = value.let { externalSync = it }

    override val preparedToString = super.preparedToString.apply {
        append(";")
        append(" commandLine=${commandLine.commandLine.toCompactString().truncate(50, MIDDLE, " … ")};")
        append(" expectedExitValue=$expectedExitValue;")
        append(" processTerminationCallback=${processTerminationCallback.asEmoji};")
        append(" destroyOnShutdown=${destroyOnShutdown.asEmoji}")
    }
}
