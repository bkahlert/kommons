package koodies.concurrent

import koodies.builder.Init
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.CommandLine.Companion.CommandLineContext
import koodies.concurrent.process.IO
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processor
import koodies.concurrent.process.Processors
import koodies.concurrent.process.processSynchronously
import koodies.concurrent.process.toProcessor
import koodies.docker.DockerProcess
import koodies.docker.DockerRunCommandLine
import koodies.logging.LoggingOptions
import koodies.logging.RenderingLogger
import koodies.logging.logging
import koodies.terminal.ANSI
import koodies.text.LineSeparators
import java.nio.file.Path

/* ALL PROCESS METHODS BELOW ALWAYS START THE PROCESS BUT DONT PROCESS */

/**
 * Creates a [DockerProcess] that executes this command line.
 */
public fun DockerRunCommandLine.toManagedProcess(expectedExitValue: Int?, processTerminationCallback: (() -> Unit)?): DockerProcess =
    DockerProcess.from(this, expectedExitValue, processTerminationCallback)

/**
 * Creates a [ManagedProcess] that executes this command line.
 */
public fun CommandLine.toManagedProcess(expectedExitValue: Int? = 0, processTerminationCallback: (() -> Unit)? = null): ManagedProcess =
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
public fun process(
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
public fun Path.process(
    command: String,
    vararg arguments: String,
    redirects: List<String> = emptyList(),
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
): ManagedProcess = process(CommandLine(redirects, environment, this, command, arguments.toList()), expectedExitValue, processTerminationCallback)


/* ALL EXECUTE METHODS BELOW ALWAYS START THE COMMANDLINE/PROCESS AND AND PROCESS IT SYNCHRONOUSLY */

/**
 * Runs `this` [CommandLine] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the created [ManagedProcess] will be processed by the specified [processor]
 * which defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun CommandLine.execute(
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
): ManagedProcess {
    return process(this, expectedExitValue, processTerminationCallback).processSynchronously(processor)
}

/**
 * Runs the specified [commandLine] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the created [ManagedProcess] will be processed by the specified [processor]
 * which defaults to [Processors.consoleLoggingProcessor] which prints all [IO] to the console.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun execute(
    processor: Processor<ManagedProcess> = Processors.consoleLoggingProcessor(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    commandLine: Init<CommandLineContext>,
): ManagedProcess = CommandLine(commandLine).execute(expectedExitValue, processTerminationCallback, processor)

/**
 * Runs the specified [commandLine] optionally checking the specified [expectedExitValue] (default: `0`).
 *
 * The output of the created [ManagedProcess] will be logged by the specified [logger]
 * which prints all [IO] to the console if `null`.
 *
 * If provided, the [processTerminationCallback] will be called on process
 * termination and before other [ManagedProcess.onExit] registered listeners
 * get called.
 */
public fun execute(
    logger: RenderingLogger?,
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    commandLine: Init<CommandLineContext>,
): ManagedProcess = execute(
    processor = logger.toProcessor(),
    expectedExitValue = expectedExitValue,
    processTerminationCallback = processTerminationCallback,
    commandLine = commandLine
)

/**
 * Runs the [CommandLine] like [CommandLine.execute] but takes this [RenderingLogger]
 * as the parent logger, that is, the [IO] gets logged in a sub logger.
 *
 * @see [CommandLine.execute]
 */
public val CommandLine.execute: RenderingLogger.(
    expectedExitValue: Int?,
    processTerminationCallback: (() -> Unit)?,
    loggingOptions: LoggingOptions,
) -> ManagedProcess
    get() = { expectedExitValue, processTerminationCallback, (caption, ansiCode, bordered) ->
        logging(caption = caption, bordered = bordered, ansiCode = ansiCode ?: ANSI.termColors.brightBlue) {
            execute(expectedExitValue, processTerminationCallback, toProcessor())
        }
    }

/**
 * Runs the [CommandLine] like [CommandLine.execute] but takes this [RenderingLogger]
 * as the parent logger, that is, the [IO] gets logged in a sub logger.
 *
 * @see [CommandLine.execute]
 */
public val RenderingLogger.execute: CommandLine.(
    expectedExitValue: Int?,
    processTerminationCallback: (() -> Unit)?,
    loggingOptions: LoggingOptions,
) -> ManagedProcess
    get() = { expectedExitValue, processTerminationCallback, loggingOptions ->
        execute(this@execute, expectedExitValue, processTerminationCallback, loggingOptions)
    }

/**
 * Returns (and possibly blocks until finished) the output of `this` [ManagedProcess].
 *
 * This method is idempotent.
 */
public fun ManagedProcess.output(): String = run {
    processSynchronously(Processors.noopProcessor())
    ioLog.logged.filter { it.type == IO.Type.OUT }.joinToString(LineSeparators.LF) { it.unformatted }
}
