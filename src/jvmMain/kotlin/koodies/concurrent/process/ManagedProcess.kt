package koodies.concurrent.process

import koodies.concurrent.isScriptFile
import koodies.debug.asEmoji
import koodies.exception.dump
import koodies.exception.toSingleLineString
import koodies.io.RedirectingOutputStream
import koodies.io.path.asString
import koodies.io.path.toPath
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import org.apache.commons.io.output.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.concurrent.thread
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
    var externalSync: CompletableFuture<*>

    override fun start(): ManagedProcess
}

fun CommandLine.toManagedProcess(
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = ManagedProcess.from(this, expectedExitValue, processTerminationCallback)

private fun CommandLine.toJavaProcess(): JavaProcess {
    val scriptFile: String = if (command.toPath().isScriptFile()) command else toShellScript().asString()

    return PlexusCommandLine(scriptFile).let {
        it.workingDirectory = workingDirectory.toFile()
        environment.forEach { env -> it.addEnvironment(env.key, env.value) }
        it.execute()
    }
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

    private val capturingMetaStream: OutputStream by lazy { TeeOutputStream(ByteArrayOutputStream(), RedirectingOutputStream { ioLog.add(IO.Type.META, it) }) }
    private val capturingOutputStream: OutputStream by lazy { TeeOutputStream(javaProcess.outputStream, RedirectingOutputStream { ioLog.add(IO.Type.IN, it) }) }
    private val capturingInputStream: InputStream by lazy { TeeInputStream(javaProcess.inputStream, RedirectingOutputStream { ioLog.add(IO.Type.OUT, it) }) }
    private val capturingErrorStream: InputStream by lazy { TeeInputStream(javaProcess.errorStream, RedirectingOutputStream { ioLog.add(IO.Type.ERR, it) }) }

    final override val metaStream: OutputStream get() = capturingMetaStream
    final override val outputStream: OutputStream get() = capturingOutputStream
    final override val inputStream: InputStream get() = capturingInputStream
    final override val errorStream: InputStream get() = capturingErrorStream

    override val ioLog: IOLog by lazy { IOLog() }

    override var externalSync: CompletableFuture<*> = CompletableFuture.completedFuture(Unit)
    override var onExit: CompletableFuture<Process>
        get() {
            return externalSync.thenCombine(javaProcess.onExit()) { _, process ->
                process
            }.exceptionally { throwable ->
                val cause = if (throwable is CompletionException) throwable.cause else throwable
                val dump = commandLine.workingDirectory.dump("""
                Process $commandLine terminated with ${cause.toSingleLineString()}.
            """.trimIndent()) { ioLog.dump() }.also { dump -> metaLog(dump) }
                throw RuntimeException(dump.removeEscapeSequences(), cause)
            }.thenApply { process ->
                if (expectedExitValue != null && exitValue != expectedExitValue) {
                    val message = ProcessExecutionException(pid, commandLine, exitValue, expectedExitValue).message
                    message?.also { metaLog(it) }
                    val dump = commandLine.workingDirectory.dump(null) { ioLog.dump() }.also { dump -> metaLog(dump) }
                    throw ProcessExecutionException(pid, commandLine, exitValue, expectedExitValue, dump.removeEscapeSequences())
                }
                metaLog("Process $pid terminated successfully.")
                this@ManagedJavaProcess
            }
        }
        set(value) {
            externalSync = value
        }

    override val preparedToString = super.preparedToString.apply {
        append(";")
        append(" commandLine=${commandLine.commandLine};")
        append(" expectedExitValue=$expectedExitValue;")
        append(" processTerminationCallback=${processTerminationCallback.asEmoji};")
        append(" destroyOnShutdown=${destroyOnShutdown.asEmoji}")
    }
}
