package com.bkahlert.kommons.exec

import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.exec.io.NonBlockingLineReader
import com.bkahlert.kommons.logging.SLF4J
import org.slf4j.Logger
import java.nio.file.Path

/** An [Executor] that runs the specified [commandLine] synchronously. */
public data class SyncExecutor(
    private val commandLine: CommandLine,
) : Executor<ExitState> {

    /** [Executor] that runs the [Executable] asynchronously.*/
    public val async: AsyncExecutor get() = AsyncExecutor(commandLine)

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
    ): ExitState = prepare(workingDirectory, environment, customize).finalize()

    public override fun logging(
        logger: (Process) -> Logger,
        customize: ProcessBuilder.() -> Unit,
        workingDirectory: Path?,
        vararg environment: Pair<String, String>,
    ): ExitState = prepare(workingDirectory, environment, customize)
        .apply {
            val ioLogger = logger(this)

            val readers = listOf(
                NonBlockingLineReader(inputStream, ioLogger::info),
                NonBlockingLineReader(errorStream, ioLogger::error),
            )

            while (readers.any { !it.done }) {
                readers.filter { !it.done }.forEach { reader ->
                    reader.use { it.read() }
                }
            }
        }
        .finalize()

    private fun Process.finalize(): ExitState {
        logger.debug("Waiting for $this")
        waitFor()
        return state.also {
            logger.info(it.status)
        } as? ExitState ?: error("Process $this expected to be terminated")
    }

    override fun toString(): String = buildString {
        append(this@SyncExecutor::class.simpleName)
        appendLine("(\"\"\"")
        appendLine(commandLine.toString(pretty = true))
        append("\"\"\")")
    }

    public companion object {
        private val logger by SLF4J
    }
}
