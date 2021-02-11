package koodies.concurrent

import com.github.ajalt.mordant.AnsiColorCode
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processors
import koodies.concurrent.process.process
import koodies.concurrent.process.processSynchronously
import koodies.docker.DockerProcess
import koodies.docker.DockerRunCommandLine
import koodies.logging.RenderingLogger
import koodies.logging.logging
import koodies.terminal.ANSI
import koodies.text.LineSeparators
import java.io.InputStream
import java.nio.file.Path

/* ALL PROCESS METHODS BELOW ALWAYS START THE PROCESS BUT DONT PROCESS */

/**
 * Creates a [DockerProcess] that executes this command line.
 */
fun DockerRunCommandLine.toManagedProcess(expectedExitValue: Int?, processTerminationCallback: (() -> Unit)?): DockerProcess =
    DockerProcess.from(this, expectedExitValue, processTerminationCallback)

/**
 * Creates a [ManagedProcess] that executes this command line.
 */
fun CommandLine.toManagedProcess(expectedExitValue: Int? = 0, processTerminationCallback: (() -> Unit)? = null): ManagedProcess =
    ManagedProcess.from(this, expectedExitValue, processTerminationCallback)

/**
 * Runs the specified [commandLine] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * **Important:** This function does just that—starting the process. To do something with the returned [ManagedProcess]
 * you can use one of the provided [Processors] or implement one on your own, e.g.
 * - `process(...).process()` defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console
 * - `process(...).process { io -> doSomething(io) }` to process the [IO] the way you like
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
fun process(
    commandLine: CommandLine,
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = commandLine.toManagedProcess(expectedExitValue, processTerminationCallback)

/**
 * Runs the specified [command] using the specified [arguments] in `this` working directory.
 *
 * Optionally [redirects], the [environment] and the [expectedExitValue] (default: `0`) can be provided.
 *
 * **Important:** This function does just that—starting the process. To do something with the returned [ManagedProcess]
 * you can use one of the provided [Processors] or implement one on your own, e.g.
 * - `process(...).process()` defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console
 * - `process(...).process { io -> doSomething(io) }` to process the [IO] the way you like.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
fun Path.process(
    command: String,
    vararg arguments: String,
    redirects: List<String> = emptyList(),
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = process(CommandLine(redirects, environment, this, command, arguments.toList()), expectedExitValue, processTerminationCallback)


// TODO refactor so there is a family of functions that starts and processes a commandline

/**
 * Starts a new [ManagedProcess] that runs this command line.
 */
fun CommandLine.execute(expectedExitValue: Int = 0, processTerminationCallback: (() -> Unit)? = null): ManagedProcess =
    process(this, expectedExitValue, processTerminationCallback)//.apply { start() }

/**
 * Starts a new [ManagedProcess] that runs this command line
 * and has it fully processed using `this` [RenderingLogger].
 */
fun CommandLine.executeLogging(
    caption: String,
    bordered: Boolean = true,
    ansiCode: AnsiColorCode = ANSI.termColors.brightBlue,
    nonBlockingReader: Boolean = false,
    expectedExitValue: Int = 0,
): Int = logging(caption = caption, bordered = bordered, ansiCode = ansiCode) {
    execute(expectedExitValue).process(
        nonBlockingReader = nonBlockingReader,
        processInputStream = InputStream.nullInputStream(),
        processor = Processors.loggingProcessor(this)
    )
}.waitForTermination()

/**
 * Returns (and possibly blocks until finished) the output of `this` [ManagedProcess].
 *
 * This method is idempotent.
 */
fun ManagedProcess.output(
): String = run {
    processSynchronously(Processors.noopProcessor())
    ioLog.logged.filter { it.type == IO.Type.OUT }.joinToString(LineSeparators.LF) { it.unformatted }
}
