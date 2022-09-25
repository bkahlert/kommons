package com.bkahlert.kommons.exec

import com.bkahlert.kommons.exec.Executor.Companion.ioLogger
import com.bkahlert.kommons.exec.io.InputStreamReader
import com.bkahlert.kommons.logging.SLF4J
import org.slf4j.Logger
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

/** An [Executor] that runs the specified [commandLine] asynchronously. */
public data class AsyncExecutor(
    private val commandLine: CommandLine,
) : Executor<Process> {

    private fun prepare(
        workingDirectory: Path?,
        environment: Array<out Pair<String, String>>,
        customize: ProcessBuilder.() -> Unit,
    ): Process {
        logger.debug("Starting $commandLine")
        val process = ProcessBuilder(commandLine)
            .start(workingDirectory, *environment, customize = customize)
        logger.info("Started $process")
        return process
    }

    public override operator fun invoke(
        customize: ProcessBuilder.() -> Unit,
        workingDirectory: Path?,
        vararg environment: Pair<String, String>,
    ): Process = invoke(customize, workingDirectory, *environment, onExit = {})

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment].
     *
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     * @param onExit callback which is invoked when the process exits
     */
    public operator fun invoke(
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
        onExit: (Process.ExitState) -> Unit,
    ): Process = invoke({}, workingDirectory, *environment, onExit = onExit)

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment].
     *
     * @param customize optional lambda to make adaptions to the underlying [ProcessBuilder]
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     * @param onExit callback which is invoked when the process exits
     */
    public operator fun invoke(
        customize: ProcessBuilder.() -> Unit,
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
        onExit: (Process.ExitState) -> Unit,
    ): Process = prepare(workingDirectory, environment, customize)
        .apply {
            val threadPool = Executors.newFixedThreadPool(1)
            CompletableFuture.supplyAsync({
                Thread.currentThread().name = FinalizerThreadName
                threadPool.shutdown()
                waitFor()
                finalize(onExit)
            }, threadPool)
        }

    public override fun logging(
        logger: (Process) -> Logger,
        customize: ProcessBuilder.() -> Unit,
        workingDirectory: Path?,
        vararg environment: Pair<String, String>,
    ): Process = logging(logger, customize, workingDirectory, *environment) {}

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment] by logging all I/O using the given [logger].
     *
     * @param logger the logger to use for logging I/O
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     * @param onExit callback which is invoked when the process exits
     */
    public fun logging(
        logger: (Process) -> Logger = { it.ioLogger() },
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
        onExit: (Process.ExitState) -> Unit,
    ): Process = logging(logger, {}, workingDirectory, *environment, onExit = onExit)

    /**
     * Executes the [Executable] with the specified [workingDirectory] and [environment] by logging all I/O using the given [logger].
     *
     * @param logger the logger to use for logging I/O
     * @param customize optional lambda to make adaptions to the underlying [ProcessBuilder]
     * @param workingDirectory the working directory to be used during execution
     * @param environment the environment variables to add
     * @param onExit callback which is invoked when the process exits
     */
    public fun logging(
        logger: (Process) -> Logger = { it.ioLogger() },
        customize: ProcessBuilder.() -> Unit,
        workingDirectory: Path? = null,
        vararg environment: Pair<String, String>,
        onExit: (Process.ExitState) -> Unit,
    ): Process = prepare(workingDirectory, environment, customize)
        .apply {
            val ioLogger = logger(this)

            val threadPool = Executors.newFixedThreadPool(3)
            CompletableFuture.supplyAsync({
                Thread.currentThread().name = FinalizerThreadName
                val outputConsumer = CompletableFuture.supplyAsync({
                    Thread.currentThread().name = "process-standard-output"
                    InputStreamReader(inputStream).forEachLine(ioLogger::info)
                }, threadPool)

                val errorConsumer = CompletableFuture.supplyAsync({
                    Thread.currentThread().name = "process-standard-error"
                    InputStreamReader(errorStream).forEachLine(ioLogger::error)
                }, threadPool)

                CompletableFuture.allOf(outputConsumer, errorConsumer)
                    .handle { _, exception ->
                        if (exception != null) {
                            ioLogger.error("An error occurred while processing I/O of $this", exception)
                        }
                    }

                threadPool.shutdown()
                waitFor()
                finalize(onExit)
            }, threadPool)
        }

    private fun Process.finalize(
        onExit: (Process.ExitState) -> Unit,
    ) = onExit(state as? Process.ExitState ?: error("Process $this expected to be terminated"))

    override fun toString(): String = buildString {
        append(this@AsyncExecutor::class.simpleName)
        appendLine("(\"\"\"")
        appendLine(commandLine.toString(pretty = true))
        append("\"\"\")")
    }

    public companion object {
        private val logger by SLF4J
        private const val FinalizerThreadName = "process-finalizer"
    }
}
