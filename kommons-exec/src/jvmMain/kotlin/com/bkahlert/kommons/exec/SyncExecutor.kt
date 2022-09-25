package com.bkahlert.kommons.exec

import com.bkahlert.kommons.exec.Process.State.Terminated
import com.bkahlert.kommons.exec.io.NonBlockingLineReader
import com.bkahlert.kommons.logging.SLF4J
import com.bkahlert.kommons.text.LineSeparators.unifyLineSeparators
import org.slf4j.Logger
import java.nio.file.Path

/** An [Executor] that runs the specified [commandLine] synchronously. */
public data class SyncExecutor(
    private val commandLine: CommandLine,
) : Executor<Terminated> {

    private fun prepare(
        workingDirectory: Path?,
        environment: Array<out Pair<String, String>>,
        customize: ProcessBuilder.() -> Unit,
    ): Process {
        logger.info("Starting ${commandLine.toString().unifyLineSeparators("⏎")}")
        val process = ProcessBuilder(commandLine)
            .start(workingDirectory, *environment, customize = customize)
        logger.info("Started $process")
        return process
    }

    public override operator fun invoke(
        workingDirectory: Path?,
        vararg environment: Pair<String, String>,
        customize: ProcessBuilder.() -> Unit,
    ): Terminated = prepare(workingDirectory, environment, customize).finalize()

    public override fun logging(
        workingDirectory: Path?,
        vararg environment: Pair<String, String>,
        logger: (Process) -> Logger,
        customize: ProcessBuilder.() -> Unit,
    ): Terminated = prepare(workingDirectory, environment, customize)
        .apply {
            val ioLogger = logger(this)

            val readers = listOf(
                NonBlockingLineReader(inputStream) { line ->
                    ioLogger.info(line)
                },
                NonBlockingLineReader(errorStream) { line ->
                    ioLogger.error(line)
                },
            )

            while (readers.any { !it.done }) {
                readers.filter { !it.done }.forEach { reader ->
                    reader.use { it.read() }
                }
            }
        }
        .finalize()

    private fun Process.finalize(): Terminated {
        logger.info("Waiting for $this")
        waitFor()
        return state.also {
            logger.info(it.status)
        } as? Terminated ?: error("Process $this expected to be terminated")
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
