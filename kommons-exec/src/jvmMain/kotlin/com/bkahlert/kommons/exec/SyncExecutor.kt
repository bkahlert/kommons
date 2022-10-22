package com.bkahlert.kommons.exec

import com.bkahlert.kommons.exec.IOType.Error
import com.bkahlert.kommons.exec.IOType.Output
import com.bkahlert.kommons.exec.Process.ExitState
import com.bkahlert.kommons.exec.io.NonBlockingLineReader
import com.bkahlert.kommons.io.ComposingInputStream
import com.bkahlert.kommons.io.RedirectingOutputStream
import com.bkahlert.kommons.io.TeeInputStream
import com.bkahlert.kommons.logging.SLF4J
import org.slf4j.Logger
import java.io.InputStream
import java.nio.file.Path

/** An [Executor] that runs the specified [commandLine] synchronously. */
internal data class SyncExecutor(
    private val commandLine: CommandLine,
) : Executor<ExitState> {

    private fun prepare(
        workingDirectory: Path?,
        environment: Array<out Pair<String, String>>,
        customize: ProcessBuilder.() -> Unit,
    ): Process {
        logger.debug("Starting $commandLine")
        val process = ProcessBuilder(commandLine).start(workingDirectory, *environment, customize = customize)
        logger.info("Started $process")
        return process
    }

    override operator fun invoke(
        customize: ProcessBuilder.() -> Unit,
        workingDirectory: Path?,
        vararg environment: Pair<String, String>,
    ): ExitState = prepare(workingDirectory, environment, customize).process({}, {}).finalize()

    override fun logging(
        logger: (Process) -> Logger,
        customize: ProcessBuilder.() -> Unit,
        workingDirectory: Path?,
        vararg environment: Pair<String, String>,
    ): ExitState {
        val process = prepare(workingDirectory, environment, customize)
        val ioLogger = logger(process)
        return process.process(ioLogger::info, ioLogger::error).finalize()
    }

    private fun Process.process(
        inputLineProcessor: (String) -> Unit,
        errorLineProcessor: (String) -> Unit,
    ): Process = let { process ->
        val io: MutableList<Pair<IOType, ByteArray>> = mutableListOf()

        val inputStream = TeeInputStream(
            process.inputStream, RedirectingOutputStream { bytes -> io.add(Output to bytes) },
        )

        val errorStream = TeeInputStream(
            process.errorStream, RedirectingOutputStream { bytes -> io.add(Error to bytes) },
        )

        val readers = listOf(
            NonBlockingLineReader(inputStream, inputLineProcessor),
            NonBlockingLineReader(errorStream, errorLineProcessor),
        )

        while (readers.any { !it.done }) {
            readers.filter { !it.done }.forEach { reader ->
                reader.use { it.read() }
            }
        }

        object : Process(process) {
            override fun getInputStream(): InputStream = ComposingInputStream(io.mapNotNull { (type, bytes) -> bytes.takeIf { type == Output } })

            override fun getErrorStream(): InputStream = ComposingInputStream(io.mapNotNull { (type, bytes) -> bytes.takeIf { type == Error } })
        }
    }

    private fun Process.finalize(): ExitState {
        logger.debug("Waiting for $this")
        waitFor()
        return exitState?.also {
            logger.info(it.status)
        } ?: error("Process $this expected to be terminated")
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
